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
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;

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
    /*
     * Load the native alljoyn_java library.  The actual AllJoyn code is
     * written in C++ and the alljoyn_java library provides the language
     * bindings from Java to C++ and vice versa.
     */
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
    
    /*
     * An event loop used to make calls to AllJoyn methods. See onCreate().
     */
    private Handler mBusHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mListViewArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mListView = (ListView) findViewById(R.id.ListView);
        mListView.setAdapter(mListViewArrayAdapter);
        
        /*
         * You always need to make all AllJoyn calls through a separate handler
         * thread to prevent blocking the Activity.
         */
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());

        /*
         * Create a ervice bus object and send a message to the AllJoyn bus
         * handler asking it to do whatever it takes to start the service 
         * and make it available out to the rest of the world.
         */
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
        /*
         * Explicitly disconnect from the AllJoyn bus to prevent any resource
         * leaks.
         */
        mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);        
    }
    
    /*
     * Our service is actually an AllJoyn bus object that will be located at
     * a certain object path in a bus attachment.  This bus object implements
     * a named interface.  The name of the interface implies a contract that
     * certain methods, signals and properties will be available at the object.
     * The fact that there is a bus object implementing a certain named
     * interface is implied by virtue of the fact that the attachment has
     * requested a particular well-known bus name.  The presence of the well-
     * known name also implies the existence of a well-known or contact session
     * port which clients can use to join sessions.
     * 
     * The contact ports, bus objects, methods, etc., are all implied by the
     * associated names.  In the case of the "org.alljoyn.bus.samples.raw" well
     * known name, the presence of an interface is implied.  This interrace is
     * named "org.alljoyn.bus.samples.raw.RawInterface", and you can find its
     * definition in RawInterface.java as the specified @BusInterface.  The 
     * well-known name also implies that the interface is implemented by a bus
     * object which can be found at a particular location defined by an object
     * path.  Also implied by the well-known name is the session contact port.  
     *
     * In order to export an AllJoyn service, you then must declare a named
     * interface that you want your service to support, for example:
     * 
     *   @BusInterface(name = "org.alljoyn.bus.samples.raw.RawInterface")
     *   public interface RawInterface {
     *   	@BusMethod
     *      void YourMethod() throws BusException;
     *   }
     *   
     * You must provide an implementation of this interface as an AllJoyn bus
     * object (that inherits from class BusObject), for example:
     * 
     *   class YourService implements YourInterface, BusObject {
     *     public void YourMethod() { }
     *   }
     *
     * You must create an instance of your bus obejct and register it at the
     * object path which will be implied by the well-known name, for example:
     * 
     *   YourService yourService;
     *   mBus.registerBusObject(yourService, "/YourServiceObjectPath");
     * 
     * You must request that the AllJoyn bus grant you the well-known name.
     * This is done since there can be at most one service on the distributed
     * bus with a given name.  If you require more than one instance of your
     * service, you can append an instance number to your name.
     * 
     *   mBus.requestName(YOUR_SERVICE_WELL_KNOWN_NAME, flags);
     *   
     * Now that you have your object all set up, you need to bind the well
     * known session port (also known as the contact port).  This turns the
     * provided session port number into a half-association (associated with
     * your service) which clients can connect to.  
     * 
     *   mBus.bindSessionPort(YOUR_CONTACT_SESSION_PORT, sessionOptions);
     *   
     * The last step in the service creation process is to advertise the 
     * existence of your service to the outside world.
     * 
     *   mBus.advertiseName(YOUR_SERVICE_WELL_KNOWN_NAME, sessionOptions);
     *
     * You can find these steps in the message loop dedicated to handling the
     * AllJoyn bus events, below. 
     */
    class RawService implements RawInterface, BusObject {
        /*
         * This is the code run when the client makes a call to the 
         * RequestRawSession method of the RawInterface.  This implementation
         * just returns the previously bound RAW_PORT.
         * 
         * The actual mapping of the bus object to the declared interface
         * and finally to this method is handled somewhat magically using
         * Java reflection based on annotations at the various levels.
         */
        public short RequestRawSession() {
            return BusHandler.RAW_PORT;
        }        
    }

    /*
     * The bus object that actually implements the service we are providing
     * to the world.
     */
    private RawService mRawService;

    /*
     * This class will handle all AllJoyn calls.  It implements an event loop
     * used to avoid blocking the Android Activity when interacting with the
     * bus.  See onCreate().
     */
    class BusHandler extends Handler {	
        /*
         * Name used as the well-known name and the advertised name.  This name must be a unique name
         * both to the bus and to the network as a whole.  The name uses reverse URL style of naming.
         */
        private static final String SERVICE_NAME = "org.alljoyn.bus.samples.raw";
        private static final short CONTACT_PORT = 88;
        
        /*
         * TODO: Remove this when we ephemeral session ports is fully implemented.
         */
        private static final String RAW_SERVICE_NAME = "org.alljoyn.bus.samples.rawraw";
        private static final short RAW_PORT = 888;
        
        private BusAttachment mBus = null;
        private int mSessionId = -1;
    	public BufferedReader mInputStream = null;
        boolean mStreamUp = false;

        /*
         * These are the (event) messages sent to the BusHandler to tell it
         * what to do.
         */
        public static final int CONNECT = 1;
        public static final int DISCONNECT = 2;
        public static final int JOINED = 3;

        public BusHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            /*
             * When we recieve the CONNECT message, it instructs the handler to
             * build a bus attachment and service and connect it to the AllJoyn
             * bus to start our service.
             */
            case CONNECT: { 
                /*
                 * All communication through AllJoyn begins with a BusAttachment.
                 * A BusAttachment needs a name. The actual name is unimportant
                 * except for internal security. As a default we use the class
                 * name.
                 *
                 * By default AllJoyn does not allow communication between
                 * physically remote bus attachments.  The second argument must
                 * be set to Receive to enable this communication.
                 */ 
                mBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
                
                /*
                 * If using the debug version of the AllJoyn libraries, tell
                 * them to write debug output to the OS log so we can see it
                 * using adb logcat.  Turn on all of the debugging output from
                 * the Java language bindings (module ALLJOYN_JAVA).  When one
                 * builds the samples in alljoyn_java, the library appropriate
                 * to the build variant is copied in; so if the variant is 
                 * debug, the following will work.
                 */
                mBus.useOSLogging(true);
                mBus.setDebugLevel("ALLJOYN_JAVA", 7);
                
                /*
                 * register a bus listener object with the BusAttachment to
                 * handle callbacks indicating important bus events.  The ones
                 * we are concerned with are assoicated with new sessions.
                 */
                mBus.registerBusListener(new BusListener() {
                    @Override
                    public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                    	/*
                    	 * We accept any request to join either the CONTACT_PORT
                    	 * session or the RAW_PORT session.
                    	 */
                    	if (sessionPort == CONTACT_PORT || sessionPort == RAW_PORT) {
                        	logInfo(String.format("BusListener.acceptSessionJoiner(%d, %s, %s): accepted",
                        			sessionPort, joiner, sessionOpts.toString()));
                    		return true;
                    	} else {
                        	logInfo(String.format("BusListener.acceptSessionJoiner(%d, %s, %s): rejected", 
                        			sessionPort, joiner, sessionOpts.toString()));
                    		return false;
                    	}
                    }

                    @Override
                    public void sessionJoined(short sessionPort, int id, String joiner) {
                    	logInfo(String.format("MyBusListener.sessionJoined(%d, %d, %s)", sessionPort, id, joiner));
                    	/*
                    	 * We expect two sessionJoined callbacks.  The first
                    	 * happens when the client joins the session 
                    	 * corresponding to the contact port.  That isn't very
                    	 * interesting.  The second callback happens when the
                    	 * client joins the raw session.  When this happens,
                    	 * we need to bug the AllJoyn handler to tell it that
                    	 * our client has joined the raw session.  On this
                    	 * side we will take the provided session ID and get
                    	 * its raw socket out which we will use to read any
                    	 * data sent by the client.
                    	 */
                    	if (sessionPort == RAW_PORT) {
                    		mSessionId = id;
                    		mBusHandler.sendEmptyMessage(BusHandler.JOINED);
                    	}
                    }
                });
                
                /* 
                 * To make a service available to other AllJoyn peers, first
                 * register a BusObject with the BusAttachment at a specific
                 * object path.  Our service is implemented by the RawService
                 * BusObject found at the "/RawService" object path.
                 */
                Status status = mBus.registerBusObject(mRawService, "/RawService");
                logStatus("BusAttachment.registerBusObject()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }           
                
                /*
                 * The next step in making a service available to other peers
                 * is to connect the BusAttachment to the AllJoyn bus.  
                 */
                status = mBus.connect();
                logStatus("BusAttachment.connect()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }
                
                /*
                 * We now request a well-known name from the bus.  This is an
                 * alias for the unique name which we are automatically given
                 * when we hook up to the bus.  This name must be unique across
                 * the distributed bus and acts as the human-readable name of
                 * our service.
                 * 
                 * We have the oppportunity to ask the underlying system to 
                 * queue our request to be granted the well-known name, but we
                 * decline this opportunity so we will fail if another instance
                 * of this service is already running.
                 */
                status = mBus.requestName(SERVICE_NAME, BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE);
                logStatus(String.format("BusAttachment.requestName(%s, 0x%08x)", SERVICE_NAME, 
                                         BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE), status);
                if (status != Status.OK) {
                    finish();
                    return;
                }

                /*
                 * If we sucessfully receive permission to use the requested
                 * service name, we need to Create a new session listening on
                 * the contact port of the raw service.  The default session
                 * options are sufficient for our purposes
                 */
                Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
                SessionOpts sessionOpts = new SessionOpts();

                status = mBus.bindSessionPort(contactPort, sessionOpts);
                logStatus("BusAttachment.bindSessionPort()", status);
                if (status != Status.OK) {
                	finish();
                	return;
                }
                    
                /*
                 * Create a new raw session listening on the another port.
                 * This can be done on-demand in a "real" application, but it
                 * is convenient to do the job as a well-known contact port 
                 * here.  We will just return this well-known port as if it
                 * were an ephemeral session port when our (single) client asks
                 * for a new raw session.    
                 *
                 * We have to change the session options object to reflect that
                 * it is a raw session we want to bind.  The difference is in
                 * the traffic flowing across the session, so we need to change
                 * the traffic type to RAW_RELIABLE, which will imply TCP, for
                 * example, if we are using an IP based transport mechanism.
                 */
                contactPort.value = RAW_PORT;
                sessionOpts.traffic = SessionOpts.TRAFFIC_RAW_RELIABLE;

                status = mBus.bindSessionPort(contactPort, sessionOpts);
                logStatus("BusAttachment.bindSessionPort()", status);
                if (status != Status.OK) {
                	finish();
                	return;
                }              

                /*
                 * Advertise the same well-known name so future clients can
                 * discover our existence.
                 */
                status = mBus.advertiseName(SERVICE_NAME, SessionOpts.TRANSPORT_ANY);
                logStatus(String.format("BusAttachement.advertiseName(%s)", SERVICE_NAME), status);
                if (status != Status.OK) {
                	finish();
                	return;
                } 
                
                /*
                 * TODO:  Remove this extraneous advertisement when ephemeral
                 * session ports are fully implemented.
                 */
                mBus.advertiseName(RAW_SERVICE_NAME, SessionOpts.TRANSPORT_ANY);
                break;
            }
            
            /*
             * We have a new raw session that has joined our bound session.
             * This is provided to us in the BusListener SessionJoined
             * callback.  This join has provided us with a session ID 
             * corresponding to the conversation between the client and the
             * service.
             * 
             * The session we got is in raw mode, but we need to get a socket
             * file descriptor back from AllJoyn that represents the 
             * established connection.  Once we have the sock FD we are then
             * free to do whatever we want with it.
             *  
             * What we are going to do with the sock FD is to create a Java
             * file descriptor out of it, and then use that Java FD to create
             * an input stream.
             */
            case JOINED: {               
                /*
                 * Get the socket FD from AllJoyn.  It is a socket FD just
                 * any other file descriptor.
                 */
            	Mutable.IntegerValue sockFd = new Mutable.IntegerValue();
                Status status = mBus.getSessionFd(mSessionId, sockFd);
                logStatus("BusAttachment.getSession()", status);
                try {
                	/*
                	 * The Java FileDescriptor class has a private constructor
                	 * that allows one to pass a file descriptor.  This is
                	 * exactly what we want, and we can use reflection to 
                	 * find this constructor, make it accessible and use it
                	 * to create a new FileDescriptor with the socket FD we
                	 * got from AllJoyn.
                	 */
                	Class<FileDescriptor> clazz = FileDescriptor.class;
                	Constructor<FileDescriptor> c = clazz.getDeclaredConstructor(new Class[] { Integer.TYPE });
                	c.setAccessible(true);
                	FileDescriptor fd = c.newInstance(sockFd.value);
                	
                	/*
                	 * Now that we have a FileDescriptor, we can use it like
                	 * any other "normal" FileDescriptor.
                	 */
                	InputStream is = new FileInputStream(fd);
                	final BufferedReader br = new BufferedReader(new InputStreamReader(is));
                	
                	logInfo("Spinning up thread to read raw Java stream");
                	new Thread("Reader") {
                		@Override
                		public void run() {
                			String line;
                			try {
                				while ((line = br.readLine()) != null) {
                	                logInfo(String.format("Read %s from raw Java stream", line));
                				}
                			} catch (Throwable ex) {
                            	logInfo("Exception reading raw Java stream");
                			}
                		}
                	};
                } catch (Throwable ex) {
                	logInfo("Exception Bringing up raw Java stream");
                }
                break;
            }
            
            /*
             * Release all of the bus-related resources acquired during and
             * after the case CONNECT.
             */
            case DISCONNECT: {
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
     * Print the status or result to the Android log. If the result is the expected
     * result only print it to the log.  Otherwise print it to the error log and
     * Sent a Toast to the users screen. 
     */
    private void logInfo(String msg) {
            Log.i(TAG, msg);
    }
}
