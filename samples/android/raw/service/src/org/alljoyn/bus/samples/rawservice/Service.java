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

package org.alljoyn.bus.samples.rawservice;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Constructor;

import java.io.InputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

public class Service extends Activity {
    /* Load the native alljoyn_java library. */
    static {
        System.loadLibrary("alljoyn_java");
    }
    
    private static final String TAG = "RawService";
    
    private static final int MESSAGE_PING = 1;
    private static final int MESSAGE_PING_REPLY = 2;
    private static final int MESSAGE_POST_TOAST = 3;

    private ArrayAdapter<String> mListViewArrayAdapter;
    private ListView mListView;
    private Menu menu;
    
    private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MESSAGE_PING:
                    String ping = (String) msg.obj;
                    mListViewArrayAdapter.add("Ping:  " + ping);
                    break;
                case MESSAGE_PING_REPLY:
                    String reply = (String) msg.obj;
                    mListViewArrayAdapter.add("Reply:  " + reply);
                    break;
                case MESSAGE_POST_TOAST:
                    Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
                }
            }
        };
    
    /* The AllJoyn object that is our service. */
    private RawService mRawService;

    /* Handler used to make calls to AllJoyn methods. See onCreate(). */
    private Handler mBusHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mListViewArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mListView = (ListView) findViewById(R.id.ListView);
        mListView.setAdapter(mListViewArrayAdapter);
        
        /* Make all AllJoyn calls through a separate handler thread to prevent blocking the UI. */
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());

        /* Start our service. */
        mRawService = new RawService();
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
        
        /* Disconnect to prevent any resource leaks. */
        mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);        
    }
    
    /* The class that is our AllJoyn service.  It implements the RawInterface. */
    class RawService implements RawInterface, BusObject {

        /*
         * This is the code run when the client makes a call to the GoRaw method of the
         * RawInterface.  This implementation just returns "OK" to the caller.
         */
        public String GoRaw() {
            mBusHandler.sendEmptyMessage(BusHandler.GO_RAW);
            return "OK";
        }        

        /* Helper function to send a message to the UI thread. */
        private void sendUiMessage(int what, Object obj) {
            mHandler.sendMessage(mHandler.obtainMessage(what, obj));
        }
    }

    /* This class will handle all AllJoyn calls. See onCreate(). */
    class BusHandler extends Handler {

    	/*
    	 * Extend the BusListener class to respond to AllJoyn's bus signals
    	 */
    	public class MyBusListener extends BusListener {

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
                mSessionId = id;
            }
        }
    	
        /*
         * Name used as the well-known name and the advertised name.  This name must be a unique name
         * both to the bus and to the network as a whole.  The name uses reverse URL style of naming.
         */
        private static final String SERVICE_NAME = "org.alljoyn.bus.samples.raw";
        private static final short CONTACT_PORT = 42;
        
        private BusAttachment mBus;
        private MyBusListener mMyBusListener;
        private int mSessionId;
        boolean mStreamUp = false;

        /* These are the messages sent to the BusHandler from the UI. */
        public static final int CONNECT = 1;
        public static final int DISCONNECT = 2;
        public static final int GO_RAW = 3;

        public BusHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            /* Connect to the bus and start our service. */
            case CONNECT: { 
                /*
                 * All communication through AllJoyn begins with a BusAttachment.
                 *
                 * A BusAttachment needs a name. The actual name is unimportant except for internal
                 * security. As a default we use the class name as the name.
                 *
                 * By default AllJoyn does not allow communication between devices (i.e. bus to bus
                 * communication).  The second argument must be set to Receive to allow
                 * communication between devices.
                 */ 
                mBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
                
                /*
                 * Create a bus listener class to handle callbacks from the 
                 * BusAttachement and tell the attachment about the callbacks
                 */
                mMyBusListener = new MyBusListener();
                mBus.registerBusListener(mMyBusListener);
                
                /* 
                 * To make a service available to other AllJoyn peers, first register a BusObject with
                 * the BusAttachment at a specific path.
                 *
                 * Our service is the RawService BusObject at the "/RawService" path.
                 */
                Status status = mBus.registerBusObject(mRawService, "/RawService");
                logStatus("BusAttachment.registerBusObject()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }
                
                
                
                /*
                 * The next step in making a service available to other AllJoyn peers is to connect the
                 * BusAttachment to the bus with a well-known name.  
                 */
                /*
                 * connect the BusAttachement to the bus
                 */
                status = mBus.connect();
                logStatus("BusAttachment.connect()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }
                
                /*
                 * request a well-known name from the bus
                 */
                int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
                
                status = mBus.requestName(SERVICE_NAME, flag);
                logStatus(String.format("BusAttachment.requestName(%s, 0x%08x)", SERVICE_NAME, flag), status);
                if (status == Status.OK) {
                	/*
                	 * If we successfully obtain a well-known name from the bus 
                	 * advertise the same well-known name
                	 */
                	status = mBus.advertiseName(SERVICE_NAME, SessionOpts.TRANSPORT_ANY);
                    logStatus(String.format("BusAttachement.advertiseName(%s)", SERVICE_NAME), status);
                    if (status != Status.OK) {
                    	/*
                         * If we are unable to advertise the name, release
                         * the well-known name from the local bus.
                         */
                        status = mBus.releaseName(SERVICE_NAME);
                        logStatus(String.format("BusAttachment.releaseName(%s)", SERVICE_NAME), status);
                    	finish();
                    	return;
                    }
                }
                
                /*
                 * Create a new session listening on the contact port of the chat service.
                 */
                Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
                
                SessionOpts sessionOpts = new SessionOpts();
                sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
                sessionOpts.isMultipoint = false;
                sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
                sessionOpts.transports = SessionOpts.TRANSPORT_ANY;

                status = mBus.bindSessionPort(contactPort, sessionOpts);
                logStatus("BusAttachment.bindSessionPort()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }
                
                break;
            }
            
            /* Go into raw session mode. */
            case GO_RAW: {
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
                    try {
                    	Class<FileDescriptor> clazz = FileDescriptor.class;
                    	Constructor<FileDescriptor> c = clazz.getDeclaredConstructor(new Class[] { Integer.TYPE });
                    	c.setAccessible(true);
                    	FileDescriptor fd = c.newInstance(sockFd.value);
                    	InputStream fis = new FileInputStream(fd);
                    	mStreamUp = true;
                    	
                    	BufferedReader input = new BufferedReader(new InputStreamReader(fis));
                    	
                    	for (String s = null; (s = input.readLine()) != null;) {
                    		logInfo(s);
                    	}
                    	
                    } catch (Throwable ex) {
                        logInfo("Exception Bringing up raw stream");
                    }
            	
            }
            
            /* Release all resources acquired in connect. */
            case DISCONNECT: {
            	mBus.unregisterBusListener(mMyBusListener);
                /* 
                 * It is important to unregister the BusObject before disconnecting from the bus.
                 * Failing to do so could result in a resource leak.
                 */
                mBus.unregisterBusObject(mRawService);
                mBus.disconnect();
                mBusHandler.getLooper().quit();
                break;   
            }

            default:
                break;
            }
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
    
    /*
     * print the status or result to the Android log. If the result is the expected
     * result only print it to the log.  Otherwise print it to the error log and
     * Sent a Toast to the users screen. 
     */
    private void logInfo(String msg) {
            Log.i(TAG, msg);
    }
}
