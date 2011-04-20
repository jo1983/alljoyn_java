/*
 * Copyright 2010-2011, Qualcomm Innovation Center, Inc.
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.alljoyn.bus.samples.rawclient;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Constructor;

import java.io.OutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

public class Client extends Activity {
    /* Load the native alljoyn_java library. */
    static {
        System.loadLibrary("alljoyn_java");
    }
    
    private static final int MESSAGE_PING = 1;
    private static final int MESSAGE_PING_REPLY = 2;
    private static final int MESSAGE_POST_TOAST = 3;

    private static final String TAG = "RawClient";

    private EditText mEditText;
    private ArrayAdapter<String> mListViewArrayAdapter;
    private ListView mListView;
    private Menu menu;
    
    /* Handler used to make calls to AllJoyn methods. See onCreate(). */
    private BusHandler mBusHandler;
    
    private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MESSAGE_PING:
                    String ping = (String) msg.obj;
                    mListViewArrayAdapter.add("Ping:  " + ping);
                    break;
                case MESSAGE_PING_REPLY:
                    String ret = (String) msg.obj;
                    mListViewArrayAdapter.add("Reply:  " + ret);
                    mEditText.setText("");
                    break;
                case MESSAGE_POST_TOAST:
                	Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
                	break;
                default:
                    break;
                }
            }
        };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mListViewArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mListView = (ListView) findViewById(R.id.ListView);
        mListView.setAdapter(mListViewArrayAdapter);

        mEditText = (EditText) findViewById(R.id.EditText);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_NULL
                        && event.getAction() == KeyEvent.ACTION_UP) {
                        /* Call the remote object's Ping method. */
                        Message msg = mBusHandler.obtainMessage(BusHandler.PING, 
                                                                view.getText().toString());
                        mBusHandler.sendMessage(msg);
                    }
                    return true;
                }
            });

        /* Make all AllJoyn calls through a separate handler thread to prevent blocking the UI. */
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());

        /* Connect to an AllJoyn object. */
        mBusHandler.sendEmptyMessage(BusHandler.CONNECT);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        this.menu = menu;
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.quit:
	    	finish();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        /* Disconnect to prevent resource leaks. */
        mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
    }
    
    /* This class will handle all AllJoyn calls. See onCreate(). */
    class BusHandler extends Handler {
        
    	/*
    	 * Extend the BusListener class to respond to AllJoyn's bus signals
    	 */
    	public class MyBusListener extends BusListener {
        	@Override
    		public void foundAdvertisedName(String name, short transport, String namePrefix) {
                logInfo(String.format("MyBusListener.foundAdvertisedName(%s, 0x%04x, %s)", name, transport, namePrefix));
            	Message msg = obtainMessage(JOIN_SESSION, name);
            	sendMessage(msg);
            }

            @Override
            public void lostAdvertisedName(String name, short transport, String namePrefix) {
                logInfo(String.format("MyBusListener.lostAdvertisedName(%s, 0x%04x, %s)", name, transport, namePrefix));
            }

            @Override
            public void nameOwnerChanged(String busName, String previousOwner, String newOwner) {
                logInfo(String.format("MyBusListener.nameOwnerChanged(%s, %s, %s)", busName, previousOwner, newOwner));
            }

            @Override
            public void sessionLost(int sessionId) {
                logInfo(String.format("MyBusListener.sessionLost(%d)", sessionId));
            }
            
            @Override
            public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                logInfo(String.format("MyBusListener.acceptSessionJoiner(%d, %s, %s)", sessionPort, joiner, 
                	sessionOpts.toString()));
        		if (sessionPort == CONTACT_PORT) {
        			return true;
        		} else {
        			return false;
        		}
        	}

            @Override
            public void sessionJoined(short sessionPort, int id, String joiner) {
                logInfo(String.format("MyBusListener.sessionJoined(%d, %d, %s)", sessionPort, id, joiner));
            }

            @Override
            public void busStopping() {
                logInfo("MyBusListener.busStopping()");
            }
        }
    	
        /*
         * Name used as the well-known name and the advertised name of the service this client is
         * interested in.  This name must be a unique name both to the bus and to the network as a
         * whole.
         *
         * The name uses reverse URL style of naming, and matches the name used by the service.
         */
        private static final String SERVICE_NAME = "org.alljoyn.bus.samples.raw";
        private static final short CONTACT_PORT=42;

        private BusAttachment mBus;
        private ProxyBusObject mProxyObj;
        private MyBusListener mMyBusListener;
        private RawInterface mRawInterface;
        
        private int 	mSessionId;
        private boolean mIsConnected;
        private boolean mIsStoppingDiscovery;
        
        private OutputStream mOutputStream;
        private boolean mStreamUp;
        
        /* These are the messages sent to the BusHandler from the UI. */
        public static final int CONNECT = 1;
        public static final int JOIN_SESSION = 2;
        public static final int DISCONNECT = 3;
        public static final int PING = 4;

        public BusHandler(Looper looper) {
            super(looper);
            
            mIsConnected = false;
            mIsStoppingDiscovery = false;
            mStreamUp = false;
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            /* Connect to a remote instance of an object implementing the RawInterface. */
            case CONNECT: {
                /*
                 * All communication through AllJoyn begins with a BusAttachment.
                 *
                 * A BusAttachment needs a name. The actual name is unimportant except for internal
                 * security. As a default we use the class name as the name.
                 *
                 * By default AllJoyn does not allow communication between devices (i.e. bus to bus
                 * communication). The second argument must be set to Receive to allow communication
                 * between devices.
                 */
                mBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
                
                /*
                 * Create a bus listener class to handle callbacks from the 
                 * BusAttachement and tell the attachment about it
                 */
                mMyBusListener = new MyBusListener();
                mBus.registerBusListener(mMyBusListener);

                /* To communicate with AllJoyn objects, we must connect the BusAttachment to the bus. */
                Status status = mBus.connect();
                logStatus("BusAttachment.connect()", status);
                if (Status.OK != status) {
                    finish();
                    return;
                }

                /*
                 * Now find an instance of the AllJoyn object we want to call.  We start by looking for
                 * a name, then connecting to the device that is advertising that name.
                 *
                 * In this case, we are looking for the well-known SERVICE_NAME.
                 */
                status = mBus.findAdvertisedName(SERVICE_NAME);
                logStatus(String.format("BusAttachement.findAdvertisedName(%s)", SERVICE_NAME), status);
                if (Status.OK != status) {
                	finish();
                	return;
                }

                break;
            }
            case (JOIN_SESSION): {
            	/*
                 * If discovery is currently being stopped don't join to any other sessions.
                 */
                if (mIsStoppingDiscovery) {
                    break;
                }
                
                /*
                 * In order to join the session, we need to provide the well-known
                 * contact port.  This is pre-arranged between both sides as part
                 * of the definition of the chat service.  As a result of joining
                 * the session, we get a session identifier which we must use to 
                 * identify the created session communication channel whenever we
                 * talk to the remote side.
                 */
                short contactPort = CONTACT_PORT;
                SessionOpts sessionOpts = new SessionOpts();
                Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
                
                Status status = mBus.joinSession((String) msg.obj, contactPort, sessionId, sessionOpts);
                logStatus("BusAttachment.joinSession()", status);
                    
                if (status == Status.OK) {
                	/*
                     * To communicate with an AllJoyn object, we create a ProxyBusObject.  
                     * A ProxyBusObject is composed of a name, path, sessionID and interfaces.
                     * 
                     * This ProxyBusObject is located at the well-known SERVICE_NAME, under path
                     * "/RawService", uses sessionID of CONTACT_PORT, and implements the RawInterface.
                     */
                	mProxyObj =  mBus.getProxyBusObject(SERVICE_NAME, 
                										"/RawService",
                										sessionId.value,
                										new Class<?>[] { RawInterface.class });

                	/* We make calls to the methods of the AllJoyn object through one of its interfaces. */
                	mRawInterface =  mProxyObj.getInterface(RawInterface.class);
                	
                	mSessionId = sessionId.value;
                	mIsConnected = true;
                }
                break;
            }
            
            /* Release all resources acquired in the connect. */
            case DISCONNECT: {
            	mIsStoppingDiscovery = true;
            	if (mIsConnected) {
                	Status status = mBus.leaveSession(mSessionId);
                    logStatus("BusAttachment.leaveSession()", status);
            	}
                mBus.disconnect();
                if (mStreamUp == true) {
                	try {
                      mOutputStream.close();
                	} catch (IOException ex) {
                        logInfo("Exception closing output stream");
                	}
                }
                getLooper().quit();
                break;
            }
            
            /*
             * We have a string to send to the server via the raw session
             * socket.  If this is the first string we've ever sent on this
             * session, we need to tell the service to switch the session to
             * raw mode.  Don't confuse this with raw sockets in the sense of
             * BSD sockets (which allow you to provide your own IP or Ethernet
             * headers -- raw sessions only means that the data will not be
             * encapsulated in AllJoyn messages).
             * 
             * Once we have the session in raw mode, we build a Java output
             * stream using the underlying socket FD and remember it for future
             * use.
             *
             * Once/If we have the raw session all set up, we simply send the
             * bytes of the string to the server directly over the TCP socket
             * using the Java stream.
             */
            case PING: {
                if (mStreamUp == false) {
                    try {
                        /*
                         * Tell the service to go into raw session mode.  This
                         * way it won't be expecting to get any AllJoyn messages
                         * over the session.  We are taking control of this
                         * conversation.
                         */
                        String reply = mRawInterface.GoRaw();
                        if (reply == "OK") {
                            /*
                             * Go into raw session mode on the local side.  We
                             * get a socket file descriptor back from AllJoyn
                             * that represents the established connection to the
                             * service.  We are then free to do whatever we want
                             * with the connection.
                             */
                            Mutable.IntegerValue sockFd = new Mutable.IntegerValue();
                            Status status = mBus.getSessionFd(mSessionId, sockFd);
                            logStatus("BusAttachment.getSessionFd()", status);
                            if (Status.OK != status) {
                                break;
                            }

                            /*
                             * What we are going to do with the connection is to 
                             * create a Java file descriptor out of the socket
                             * file descriptor, and then use that FD to create
                             * an output stream.
                             */
                            Class<FileDescriptor> clazz = FileDescriptor.class;
                            Constructor<FileDescriptor> c = clazz.getDeclaredConstructor(new Class[] { Integer.TYPE });
                            c.setAccessible(true);
                            FileDescriptor fd = c.newInstance(sockFd.value);
                            mOutputStream = new FileOutputStream(fd);
                            mStreamUp = true;
                        }
                    } catch (Throwable ex) {
                        logInfo("Exception Bringing up raw stream");
                    }
                }

                /*
                 * If we've sucessfully created an output stream from the raw
                 * session connection established by AllJoyn, we write the bytes
                 * to the TCP stream that underlies it all.
                 */
                if (mStreamUp == true) {
                	String string = (String)msg.obj;
                	try {
                		mOutputStream.write(string.getBytes());
                		mOutputStream.flush();
                	} catch (IOException ex) {
                        logInfo("Exception writing and flushing to output stream");
                	}
                }
                break;
            }
            default:
                break;
            }
        }
        
        /* Helper function to send a message to the UI thread. */
        private void sendUiMessage(int what, Object obj) {
            mHandler.sendMessage(mHandler.obtainMessage(what, obj));
        }
    }

    private void logStatus(String msg, Status status) {
        String log = String.format("%s: %s", msg, status);
        if (status == Status.OK) {
            Log.i(TAG, log);
        } else {
        	Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
            mHandler.sendMessage(toastMsg);
            Log.e(TAG, log);
        }
    }

    private void logException(String msg, BusException ex) {
        String log = String.format("%s: %s", msg, ex);
        Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
        mHandler.sendMessage(toastMsg);
        Log.e(TAG, log, ex);
    }
    
    /*
     * print the status or result to the Android log. If the result is the expected
     * result only print it to the log.  Otherwise print it to the error log and
     * Sent a Toast to the users screen. 
     */
    private void logInfo(String msg) {
            Log.i(TAG, msg);
    }
}
