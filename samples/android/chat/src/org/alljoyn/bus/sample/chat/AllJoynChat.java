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

// ================================================================================
// WARNING.  This sample is deprecated and should not be used as a model for actual
// AllJoyn development.  It is a conversion of a historical sample that, becuase of
// its history is implemented in an inefficient manner.  Specifically, it is very
// wasteful of network resources, particularly sessions -- it does not take advantage
// of multicast sessions, which it should.
//
// We continue to provide this code since it does work and may provide some insights
// into details of AllJoyn if perused.  We are replacing this sample with one which
// better illustrates best practices with respect to session resources.  Until the
// new chat sample is available, we recommend using the C++ chat sample as an 
// architectural guide.
//
// We do provide an extensive comment discussing the shortcomings of this sample
// and its duplication, and general inefficient use, of resources. Search for the
// string "Session Sidebar Comment" for the details. 
// ================================================================================

package org.alljoyn.bus.sample.chat;

import java.util.List;
import java.util.LinkedList;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.MessageContext;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;

import android.app.Activity;
import android.app.Dialog;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Simple Android Activity that allows a user to send strings to multiple
 * "chatters" via AllJoyn signals.
 * 
 * Be aware that since this is just an activity, it may be relaunched when,
 * for example, the phone orientation is changed.  This will have the effect
 * of disconnecting from a chat and reconnecting.  Although this is not very
 * desirable behavior for a "real" application, just using an activity without
 * a corresponding service makes for simple samples,
 */
public class AllJoynChat extends Activity {
    /*
     * Load the native alljoyn_java library.  The actual AllJoyn code is
     * written in C++ and the alljoyn_java library provides the language
     * bindings from Java to C++ and vice versa.
     */
    static {
        System.loadLibrary("alljoyn_java");
    }

    private static final int DIALOG_ADVERTISE = 1;
    private static final int MESSAGE_CHAT = 1;
    private static final int MESSAGE_POST_TOAST = 2;
    private static final String TAG = "AllJoynChat";

    private EditText mEditText;
    private ArrayAdapter<String> mListViewArrayAdapter;
    private Menu mMenu;

    /**
     * This is the name that the user provides as the nickname to be used
     * in the chat.  This name is also used to make the bus name requested
     * by the application unique and serves (when combined with the
     * application well-known-name) as the advertised service name.
     */
    private String mName;
    
    /**
     * We always make AllJoyn calls through a handler thread to prevent
     * blocking the Android UI and getting the dreaded Force Close message.
     * This is that handler.
     */
    public BusHandler mBusHandler;
    
    /**
     * Android-specific Activity lifecycle method that is called when the
     * application user interface is first created.  You must be aware of what
     * happens with respect to the Android lifecycle when tying the AllJoyn
     * service lifecycle to the Android Activity lifecycle.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mListViewArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.test_list_item);
        ListView lv = (ListView) findViewById(R.id.ListView);
        lv.setAdapter(mListViewArrayAdapter);

        mEditText = (EditText) findViewById(R.id.EditText);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                    String message = view.getText().toString();
                    Message msg = mBusHandler.obtainMessage(BusHandler.CHAT, message);
                    mBusHandler.sendMessage(msg);
                    view.setText("");
                }
                return true;
            }
        });
        
        /*
         * Always make all AllJoyn calls through a separate handler thread to
         * prevent blocking the Android UI and getting the dreaded Force Close
         * message.
         */
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());
        
        /*
         * Send a message to the handler thread asking it to connect to the
         * AllJoyn bus and start the process of bringing up the chat servcie.
         */
        mBusHandler.sendEmptyMessage(BusHandler.CONNECT);

        showDialog(DIALOG_ADVERTISE);
    }

    /**
     * This is an Android Activity lifecycle method that is called when the
     * activity is being torn down for some reason (such as redisplaying the
     * activity due to a screen orientation change).
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Use the mBusHandler to disconnect from the bus. Failing to to this could result in memory leaks
        mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
    }

    /**
     * This is an Android UI method that is called when the menu button is
     * pressed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        this.mMenu = menu;
        setConnectedState(mBusHandler.isConnected());
        return true;
    }
    
    /**
     * This is an Android UI method that is called in order to build the
     * standard options menu before the user gets his or her hands on it.
     * We must return true for teh options menu to be displayed.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        setConnectedState(mBusHandler.isConnected());
        return true;
    }
    
    /**
     * This is an Android UI method that is called when a menu item is
     * selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.connect:
            showDialog(DIALOG_ADVERTISE);
            return true;
        case R.id.disconnect:
            Message msg = mBusHandler.obtainMessage(BusHandler.END_DISCOVER, mName);
            mBusHandler.sendMessage(msg);
            return true;
        case R.id.quit:
            onDestroy();
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This Android UI mehtod is called to draw the on the screen dialogs.  
     * In this case it only draws the Advertise name dialog.  The advertise
     * name corresponds to a user nickname for the chat session.
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch(id) {
        case DIALOG_ADVERTISE:
            dialog = new Dialog(AllJoynChat.this);
            dialog.setContentView(R.layout.advertise);
            dialog.setTitle(getString(R.string.advertise_dialog_title));
            Button okButton = (Button) dialog.findViewById(R.id.AdvertiseOk);
            okButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    View r = v.getRootView();
                    TextView text = (TextView) r.findViewById(R.id.AdvertiseText);
                    mName = text.getText().toString();
                    Message msg = mBusHandler.obtainMessage(BusHandler.START_DISCOVER, mName);
                    mBusHandler.sendMessage(msg);
                    dismissDialog(DIALOG_ADVERTISE);
                }
            }); 
            Button cancelButton = (Button) dialog.findViewById(R.id.AdvertiseCancel);
            cancelButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    dismissDialog(DIALOG_ADVERTISE);
                }
            }); 
            break;            
        default:
            dialog = null;
            break;
        }
        return dialog;
    }

    /**
     * Simple method used to enable and disable the Connect and Disconnect
     * options for the menu depending on whether or not we are connected to
     * the AllJoyn bus.
     */
    private void setConnectedState(boolean isConnected) {
        if (null != mMenu) {
            mMenu.getItem(0).setEnabled(!isConnected);
            mMenu.getItem(1).setEnabled(isConnected);
        }
    }
    
    /**
     * Print a status message to the Android system log so we can see what is
     * happening in the application using logcat.
     */
    private void logStatus(String msg, Status status) {
        logStatus(msg, status, Status.OK);
    }
    
    /**
     * print the status or result to the Android log if the result is 
     * unexpected. If the result is the expected result (as specified by
     * passStatus) we only send the message to the Android log.  If we get
     * an unexpected reslt we log the message and send a Toast for the
     * user to see. Displaying the toast must be done on the UI thread so
     * we send a message back to the activity.
     */
    private void logStatus(String msg, Object status, Object passStatus) {
        String log = String.format("%s: %s", msg, status);
        if (status == passStatus) {
            Log.i(TAG, log);
        } else {
            Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST);
            toastMsg.obj = log;
            mHandler.sendMessage(toastMsg);

        }
    }

    /**
     * When an exception is thrown, we use this method to Toast the name of
     * the exception and send a log of the exception to the Android log.  Note
     * that the toast must be posted from the main UI thread, so we send it a
     * message asking for the toast.
     */
    private void logException(String msg, BusException ex) {
        String log = String.format("%s: %s", msg, ex);
        
        Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST);
        toastMsg.obj = log;
        mHandler.sendMessage(toastMsg);
        
        Log.e(TAG, log, ex);
    }
    
    /**
     * Send a message to the Android log.  We use this method to log general
     * status messages not associated with possible errors. 
     */
    private void logInfo(String msg) {
            Log.i(TAG, msg);
    }
       
    /**
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
     * associated names.  In the case of the "org.alljoyn.bus.samples.chat" well
     * known name, the presence of an interface is implied.  This interrace is
     * named "org.alljoyn.bus.samples.chat", and you can find its definition in
     * ChatInterface.java as the specified @BusInterface.  The well-known name
     * also implies that the interface is implemented by a bus object which can
     * be found at a particular location defined by an object path, in this
     * case, "/chatService".  Also implied by the well-known name is the session
     * contact port.  
     */
    class ChatService implements ChatInterface, BusObject {
    	/*                                                                                                                          
         * Intentionally empty implementation of Chat method.  Since the Chat
         * Method is only used as a signal emitter this will never be called
         * directly. This method will always be called from an emitter 
         * interface and all values will be handled by the bus.
         * 
         * @see org.alljoyn.bus.sample.chat.ChatInterface#Chat(java.lang.String)
         */
    	public void Chat(String str) throws BusException {                                                                                              
        }     
    }
   
    /**
     * We need to provide a way for the AllJoyn signal handler that implements
     * message delivery from remote chat services to make Android user
     * interface calls without blocking AllJoyn while Android does its thing.
     * 
     * In Android, such things are composed of a handler and a looper.  The
     * looper represents the thread that is, well, looping waiting for work;
     * and the handler provides a mechanism to turn the looper into a worker
     * thread by providing message handling.
     * 
     * This handler allows AllJoyn to send the user interface a message that
     * contains the string the remote user has typed.  This message is just
     * displayed on the Android UI.
     */
    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_CHAT: {
                mListViewArrayAdapter.add((String) msg.obj); 
                break;
            }
            case MESSAGE_POST_TOAST:
                Toast toast = Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG);
            	toast.setGravity(Gravity.TOP, 0, 0);
                toast.show();
                break;
            default: 
                break;
            }
        }
    };
    
    /**
     * We need to provide a way for the Android user interface to make AllJoyn
     * calls without blocking the UI while AllJoyn is off doing its thing.
     * 
     * In Android, such things are composed from a handler and a looper.  The
     * looper represents the thread that is, well, looping waiting for work;
     * and the handler provides a mechanism to turn the looper into a worker
     * thread by providing message handling.
     * 
     * The BusHandler provides the mechanism for the event loop that the UI
     * will use to ask AllJoyn to  
     */
    public class BusHandler extends Handler {
    	
    	private static final short CONTACT_PORT = 42;
        private static final String CHAT_SERVICE_PATH = "/chatService";
        private static final String NAME_PREFIX = "org.alljoyn.bus.samples.chat";
        private static final String CHAT_INTERFACE_NAME = "org.alljoyn.bus.samples.chat";
        
        /*
         * The well known bus name that we request and advertise.
         */
        private String mWellKnownName;
        
        /*
         * The following constants correspond to the messages that can be sent
         * to the handler thread, and therefore to the actions that the AllJoyn
         * system needs to perform in support of the user interface.
         */
        public static final int CONNECT = 1;
        public static final int START_DISCOVER = 2;
        private static final int JOIN_SESSION = 3;
        public static final int END_DISCOVER = 4;
        public static final int CHAT = 5;
        public static final int DISCONNECT = 6;
        
        /*
         * The BusAttachment is the way that AllJoyn applications connect to
         * a bus and use or export services.
         */
        private BusAttachment mBus;
        private boolean mIsConnected;
        private boolean mIsStoppingDiscovery;
        
        /*
         * The ChatService is the instance of an AllJoyn interface that is 
         * exported on the bus and allows us to send chat signals.
         */
        private ChatService mChatService;
        
        /*
         * For every "chatter" whith whom we are exchanging messages, we keep
         * an instance of their interface, and we keep a corresponding session
         * that represents the communication path to the chatter. 
         */
        private List<ChatInterface> mChatList;
        private List<Integer> mSessionList;

        /**
         * Constructor for the handler thread.
         * 
         * @param looper
         */
        public BusHandler(Looper looper) {
            super(looper);          
            /*
             * Create an instance of the chat service.  This is the bus object
             * that implements the chat on the local service side. 
             */
            mChatService = new ChatService();
            mChatList = new LinkedList<ChatInterface>();
            mSessionList = new LinkedList<Integer>();
            mIsStoppingDiscovery = false;
        }
        
        /**
         * This is the heart of the AllJoyn operation here.  Wither the user
         * interface or AllJoyn itself will arrange to send the BusHandler 
         * messages requesting the AllJoyn bus to do something.  This results
         * in the handleMessage method being called with an event ID and a
         * parameter.
         * 
         * All we do is a big switch statement and then "handle" each request
         * translating the conceptual request into concrete sequences of AllJoyn
         * commands. 
         */
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            
            /*
             * When the user interface wants us to "connect", it is asking us
             * to connect to the AllJoyn bus and prepare to start a user chat
             * session.  It does not have a nickname for our user yet, so we
             * cannot start the actual session yet.
             */
            case (CONNECT): {
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
                 * Create a bus listener class to handle advertisement
                 * callbacks from the BusAttachment.  Whenever we find an
                 * adtertisement of chat service we send a message to ourselves
                 * asking to join the corresponding session and start 
                 * exchanging messages.  There are some subtleties here
                 * that are important to understand, so please look at the 
                 * JOIN_SESSION case.
                 */
                mBus.registerBusListener(new BusListener() {
                	@Override
                	public void foundAdvertisedName(String name, short transport, String namePrefix) {
                		logInfo(String.format("MyBusListener.foundAdvertisedName(%s, 0x%04x, %s)", name, transport, namePrefix));
                		Message msg = obtainMessage(JOIN_SESSION, name);
                		sendMessage(msg);
                	}
                });

                /* 
                 * To make a service available to other AllJoyn peers, first
                 * register a BusObject with the BusAttachment at a specific
                 * object path.  Our service is implemented by the ChatService
                 * BusObject found at the "/chatService" object path.
                 */
                Status status = mBus.registerBusObject(mChatService, CHAT_SERVICE_PATH);
                logStatus("BusAttachment.registerBusObject()", status);
                if (Status.OK != status) {
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
                 * In this sample, we communicate chat messages using AllJoyn
                 * signals.  The attachment acting as the service sends signals
                 * and the attachments acting a clients receive those signals.
                 * 
                 * The client needs to register a signal handler to receive the
                 * chat signals.  This is done with the help of Java reflection.  
                 * Signal handler are implemented in this class and annotated 
                 * using @BusSignalHandler.  When this annotation is detected 
                 * by the system when it does the reflection, it AllJoyn that
                 * the annotated method is an implementation of a signal handler.
                 * 
                 * For example, the chat signal handler is annotated as,
                 * 
                 *   @BusSignalHandler(iface = CHAT_INTERFACE_NAME, signal = "Chat")
                 *   public void  Chat(String str)
                 *  
                 * This is enough information to allow the system to connect the 
                 * bus signal named "Chat" with the handler provided in the class.
                 * 
                 * The call to registerSignalHandlers() instructs the system to
                 * perform the reflection and "hook up" the signal handlers in the
                 * provided object to the signals in the provided interface.  
                 */
                status = mBus.registerSignalHandlers(this);
                logStatus("BusAttachement.registerSignalHandlers()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }
                
                break;
            }
            
            /*
             * When the user interface wants us to "start_discover", it means
             * that a user has identified him or herself to the application
             * and the UI is asking us to announce ourselves on the bus and 
             * to begin trying to discover other "chatters" to whom we can 
             * connect.
             */
            case (START_DISCOVER): {
                /*
                 * This handler case is invoked once we have a chat nickname.
                 * We now request a well-known name from the bus.  This is an
                 * alias for the unique name which we are automatically given
                 * when we hook up to the bus.  This name must be unique across
                 * the distributed bus and acts as the human-readable name of
                 * our service.  In this case, we have a name prefix which is
                 * common across all cases of the distributed chat; and we make
                 * it unique by appending the user nickname. 
                 * 
                 * We have the oppportunity to ask the underlying system to 
                 * queue our request to be granted the well-known name, but we
                 * decline this opportunity so we will fail if another instance
                 * of this service is already running.
                 */
            	logInfo(String.format("Start discovering.  Name is %s", (String)msg.obj));
            	mWellKnownName = NAME_PREFIX + "." + (String)msg.obj;
                Status status = mBus.requestName(mWellKnownName, BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE);
                logStatus(String.format("BusAttachment.requestName(%s, 0x%08x)", 
                    mWellKnownName, BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE), status);
                if (status != Status.OK) {
                    finish();
                    return;
                }

                /*
                 * If we sucessfully receive permission to use the requested
                 * service name, we need to Create a new session listening on
                 * the contact port of the raw service.  We ask for reliable
                 * message-based traffic and point-to-multipoint sessions (true)
                 * so all of our chatters can talk with a single message send.
                 * We aren't worried about the proximity or the physical
                 * transport used to send get messages to us so we allow "ANY"
                 * of these.
                 */
                Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
                SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY,
                	SessionOpts.TRANSPORT_ANY);

                /*
                 * When we ask to bind the session port, we provide a listener
                 * that handles the callbacks related to session management.
                 * 
                 * The acceptSessionJoiner() callback provides for admission
                 * control to our sessions.  If we return true, we tell the
                 * system that we are allowing the remote to join and talk
                 * with us over the* resulting session.
                 * 
                 * The sessionJoined callback tells us that a remote has
                 * successfully joined our session.  We just note that this
                 * event has happened.
                 */
                status = mBus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() {
                    @Override
                	public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                    	logInfo(String.format("MySessionPortListener.acceptSessionJoiner(%d, %s)", sessionPort, joiner));
                    	return true;
                    }
                    
                    public void sessionJoined(short sessionPort, int id, String joiner) {
                    	logInfo(String.format("MySessionPortListener.sessionJoined(%d, %d, %s)", sessionPort, id, joiner));
                        mIsConnected = true;
                    }             
                });
                
                logStatus(String.format("BusAttachment.bindSessionPort(%d, %s)", contactPort.value, 
                    sessionOpts.toString()), status);
                if (status != Status.OK) {
                    finish();
                    return;
                }

                /*
                 * Advertise the existence of our chat well-known name so future
                 * clients can discover our existence.
                 */
                status = mBus.advertiseName(mWellKnownName, SessionOpts.TRANSPORT_ANY);
                logStatus(String.format("BusAttachment.advertiseName(%s, 0x%04x)", 
                	mWellKnownName, SessionOpts.TRANSPORT_ANY), status, Status.OK);

                if (status != Status.OK) {
                	/*
                	 * If we are unable to advertise the name, release
                	 * the name from the local bus.
                	 */
                	status = mBus.releaseName(mWellKnownName);
                	logStatus(String.format("BusAttachment.releaseName(%s)", mWellKnownName), 
                			status);
                    mIsConnected = false;
                } else {	
                	mIsConnected = true;
                }
            
                /*
                 * Each device running the AllJoyn Chat sample advertises
                 * a name that looks like "NAME_PREFIX.<a_user_entered_name>".
                 * For example, if the user uses foo as their name, the 
                 * well-known name requested from the bus and advertised is
                 * "org.alljoyn.bus.samples.chat.foo".  Since buses must
                 * advertise a unique name we don't know the name the other
                 * buses will advertise however we can know part of the name
                 * each bus will advertise.
                 * 
                 * For the AllJoyn Chat sample all of the Bus names
                 * advertised will start with "org.alljoyn.bus.samples.chat"
                 * this will tell the local bus to look for any remote bus
                 * that is advertising a name that uses that prefix. If
                 * found the bus will send out a "FoundAdvertisedName" signal.
                 */
              	status = mBus.findAdvertisedName(NAME_PREFIX);
                logStatus(String.format("BusAttachment.findAdvertisedName(%s)", 
                    NAME_PREFIX), status, Status.OK);
                break;
            }

            /*
             * Session Sidebar Comment
             * =======================
             * 
             * Whenever a 'FoundAdvertisedName' signal is received by the
             * AllJoyn system, it will percolate up into a BusListener
             * associated with our BusAttachment.  Our implementation of
             * BusListener will arrange for the well-known name of the found
             * service to be sent to this BusHandler case in the obj of the
             * associated message.  We don't filter the found names, so we will
             * run here when we get our own advertisements as well as those of
             * others. We run all of the advertisements in here primarily to
             * provide a point in this sample code for this rather lengthy
             * comment on sessions.
             * 
             * When a session is bound (bindSessionPort is called) what amounts
             * to an endpoint is created.  There is no actual session at that
             * time, there is only the possibility of a session.  This is
             * similar to the case of TCP when a listener is created, but no
             * connections have been accepted.
             * 
             * The scope of a session from our point of view is the bus
             * attachment we are using.  There are two ways to get a session up
             * and running from the point of view of a bus attachment:  either
             * the session can be explicitly joined (by calling joinSession) or
             * it can be implicilty joined by accepting a session (by returning
             * true in the acceptSessionJoiner callback)  If we accept a 
             * session, we will then get a session joined callback that provides
             * us with a session ID, as if we called join session.  The fact
             * that we joined is implied by accepting the session joiner and the
             * return code of that implied join is passed to the session joined
             * callbak.  One can start a session either way, explicitly or 
             * implicitly, but one cannot do both.
             * 
             * Observe that we are (1) acting as a service (by binding a
             * session, advertising it and accepting new sessions on it);
             * (2) acting as a client by discovering remote services and
             * joining their sessions; and (3) doing both in the same bus
             * attachment.  This means we need to be careful about joining the
             * session we advertise.
             * 
             * The end-result is if the advertisement that caused this case to
             * be executed is not from us (not for our own well-known name) we
             * go ahead and join the remote session explicitly here.  If the
             * precipitating advertisement is from us (for our own well-known
             * name) we do not join the session explicitly here and so ignore
             * the advertisement.  We let the implicit join from the accept
             * session joiner/session joined mechanism happen.
             * 
             * This is illustrated in the following sequence diagram.
             * 
             *                   [ chat.a ] <----- Advertisement ------ [chat.b]
             *                   [ chat.a ] ---------- Join ----------> [chat.b] --> session joined
             *     explicit ID x [ chat.a ] <-------- session --------> [chat.b] implicit ID x
             *                   [ chat.a ] ------ Advertisement -----> [chat.b]
             * session joined <--[ chat.a ] <--------- Join ----------- [chat.b]
             *     implicit ID y [ chat.a ] <-------- session --------> [chat.b] explicit ID y
             *
             * The service named chat.b advertises its existence and the client
             * "half" of chat.a receives that advertisement.  As a result, it
             * joins the session bound by chat.b which gets a session joined
             * calback and is provided with a session ID x from the implicit
             * join.  The client chat.a is returned a session ID x from the 
             * explicit join operation.  A similar sequence of events is repeated
             * when chat.a advertises resulting in a parallel session connection.
             * 
             * Since there are two parallel sessions, each side should choose only
             * one session to send information over.  In this case, we chose 
             * the explicitly obtained session ID.
             * 
             * We don't receive our own multicasts over a session so, if we want
             * the user interface to echo a message, we need to do so outside
             * the AllJoyn umbrella.  Additionally, even though we started an
             * app and have a chat session *bound*, there may really no actual
             * session until some remote device receives our advertisement and
             * attempts to join the session.  At that time, our accept session
             * joiner callback will be executed, and if we accept, our session
             * joined callback will be fired.  This results in a session being
             * created implicitly.  Until that session is created, typing a
             * message locally will not cause a message to be sent out over the
             * (non-existent) session.  In this case, we post a toast indicating
             * that there is no connected session.  This is not an error, it is
             * an advisory that there is no real session.
             */
            case (JOIN_SESSION): {
                /*
                 * If discovery is currently being stopped don't join to any other sessions.
                 */
                if (mIsStoppingDiscovery) {
                    break;
                }
                
                /*
                 * If the advertisement that caused the JOIN_SESSION message to be
                 * sent is our own advertisement, we ignore it (see above long
                 * comment).
                 */
                if (mWellKnownName.equals((String) msg.obj)) {
                    break;
                }
                                
                /*
                 * In order to join the session, we need to provide the well-known
                 * contact port.  This is pre-arranged between both sides as part
                 * of the definition of the chat service.  As a result of joining
                 * the session, we get a session identifier which we must use to 
                 * identify the created session communication channel whenever we
                 * talk to the remote side.  We use the same session options we 
                 * used when we bound our own session port, assuming that other
                 * chat implementations (the C++ version in particular) will do
                 * the same.
                 */
                short contactPort = CONTACT_PORT;
                SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY,
                    SessionOpts.TRANSPORT_ANY);
                Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
                
                /*
                 * When we join a session, we need to provide a callback that
                 * handles events from the system related to our session.  These
                 * are the loss of the session and another client joining the 
                 * session.  Since this is a point-to-point session, we don't
                 * need to handle the latter.  In a "real" application, we could
                 * do much more (like map the session ID to a user name), but we
                 * just toast a simple message if we lose one of the sessions.  The
                 * toast must be posted in the main UI thread, so we need to send
                 * it a request to post the toast.
                 * 
                 * Note that every time we receive an advertisement, we do a join
                 * session.  If for some reason, we receive an advertisement for
                 * the same service twice, we will attempt to join its session 
                 * twice.  The system prevents this for us.
                 */
                Status status = mBus.joinSession((String) msg.obj, contactPort, sessionId, sessionOpts, new SessionListener() {
                    @Override
                    public void sessionLost(int sessionId) {
                        logInfo(String.format("MyBusListener.sessionLost(%d)", sessionId));
                        String s = String.format("Session %d lost", sessionId);
                        
                        Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST);
                        toastMsg.obj = s;
                        mHandler.sendMessage(toastMsg);
                    }
                });
                logStatus("BusAttachment.joinSession()", status);
                    
                if (status == Status.OK) {
                	logInfo(String.format("========== Explicitly joined remote session to %s on session id %d ==========)",
                			(String)msg.obj, sessionId.value));
                	/*
                     * Create a signal emitter to send out the Chat signal on
                     * the new session.  The mChatService identifies the source
                     * of the signals which is the local service; the string in
                     * msg.obj is the well-known name of the discovered remote
                     * service and serves as the destination.  The sessionId
                     * serves to identify the communication path established in
                     * the joinSession.  We turn off the global broadcast bit
                     * since we are in a one-to-one relationship with the
                     * remote side.
                     */
                    SignalEmitter emitter = new SignalEmitter(mChatService, (String) msg.obj, sessionId.value,
                    		SignalEmitter.GlobalBroadcast.Off);
                    
                    /*
                     * The SignalEmitter we just created knows how to send
                     * signals and how to arrange for the session ID and
                     * destination to be passed.  We now need to get an
                     * instance of an interface which defines which signals
                     * are sent and the associated formal parameters.  This
                     * interface was defined in ChatInterface.java and we 
                     * use this interface to actually send the signals. 
                     */
                    ChatInterface chatInterface = emitter.getInterface(ChatInterface.class);
                	
                	/*
                	 * Add the new interface to a list so that we can send 
                	 * chat messages out over each one when our user types a
                	 * message; and add the session ID corresponding to the 
                	 * chat to a list so we can leave the session when we exit. 
                	 */
                    mChatList.add(chatInterface);
                    mSessionList.add(sessionId.value);
                }
                break;
            }
 
            /*
             * When the user interface decides it wants to stop advertising a
             * a name and also stop discovering new names it sends us this 
             * message.  
             */
            case (END_DISCOVER): {
                mIsStoppingDiscovery = true;
                Status status;

                /*
                 * Leave any sessions that we may be joined.
                 */
                for (Integer sid : mSessionList) {
                	status = mBus.leaveSession(sid);
                    logStatus("BusAttachment.leaveSession()", status);
                }
                
                /*
                 * Release the binding of the session port, preventing any new
                 * joiners from coming in.
                 */
             	status = mBus.unbindSessionPort(CONTACT_PORT);
                logStatus(String.format("BusAttachment.unbindSessionPort(%d)", 
                    CONTACT_PORT), status, Status.OK);
                                    
                mIsConnected = false;
                mSessionList.clear();
                mChatList.clear();
                    
                /*
                 * Ask the bus to stop telling us about new instances of
                 * the NAME_PREFIX service we've been hunting for.
                 */
             	status = mBus.cancelFindAdvertisedName(NAME_PREFIX);
                logStatus(String.format("BusAttachment.cancelFindAdvertisedName(%s)", 
                    NAME_PREFIX), status, Status.OK);
                /*
                 * Ask the bus to stop advertising us as an instance of
                 * the chat service.
                 */
                String wellKnownName = NAME_PREFIX + "." + (String) msg.obj;
                status = mBus.cancelAdvertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);
                logStatus(String.format("BusAttachment.cancelFindAdvertisedName(%s, 0x%04x)", 
                    NAME_PREFIX, SessionOpts.TRANSPORT_ANY), status, Status.OK);
                                       
                /*
                 * Ask the bus to release the well-known name we have had
                 * reserved as an alias for our bus attachment.
                 */
                status = mBus.releaseName(wellKnownName);
                logStatus(String.format("BusAttachment.releaseName(%s)", wellKnownName), 
                    status);

                mIsStoppingDiscovery = false;
                break;
            }
            
            /*
             * When the user enters a string, we need to send this string to
             * all of the sessions we have open.
             * 
             * Whenever we found a chat service via the found advertised name
             * signal, we started an AllJoyn session with the remote "chatter".
             * We also created a signal emitter to use when sending the "Chat" 
             * signal to that service.  As a result, we have a list of chat 
             * interfaces with associated signal emitters, and we remembered
             * the list of chat interfaces corresponding to our remote peers.
             * 
             * The chat interfaces allow us to talk to remote devices, but we
             * need to handle the local echo situation.  We do that first by
             * just posting a message back to the UI.
             * 
             * Then we walk the list of chat interfaces and call the "Chat"
             * method on each of them.  This will send the user string to all
             * of the peers.
             */
            case (CHAT): {

                Message localEchoMsg = mHandler.obtainMessage(MESSAGE_CHAT);
                localEchoMsg.obj = "Message from me : " + (String) msg.obj;
                mHandler.sendMessage(localEchoMsg);
                
            	if (mChatList.isEmpty()) {
                    Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST);
                    toastMsg.obj = "No session yet.  Not sending a message to remote devices";
                    mHandler.sendMessage(toastMsg);
                    break;
            	}

            	try {
                	for (ChatInterface chatInterface : mChatList) {
                		chatInterface.Chat((String) msg.obj);
                        Log.i(TAG, String.format("Chat(%s) msg sent to session", (String) msg.obj));
                	}
                } catch (BusException ex) {
                    logException("ChatInterface.Chat()", ex);
                }
                break;
            }
            
            /*
             * When the user decides she is all done, the resulting user 
             * interface actions will arrange to "disconnect" the application
             * from the AllJoyn bus.
             */
            case (DISCONNECT): {
                mBus.unregisterSignalHandlers(this);
                mBus.unregisterSignalHandlers(mChatService);
                mBus.unregisterBusObject(mChatService);
                mBus.disconnect();
                getLooper().quit();
                break;
            }
            
            default:
                break;
            }
        }
        
        /**
         * Function to tell teh user interface whether or not we are connected
         * to at least one other "chatter".
         */
        public boolean isConnected() {
            return this.mIsConnected;
        }

        /**
         * Receive a chat string from a remote "chatter".
         * 
         * In this sample, we communicate chat messages using AllJoyn
         * signals.  The attachment acting as the service sends signals
         * and the attachments acting a clients receive those signals.
         * This is the method used to actually receive a chat string.
         * 
         * The client side registers a signal handler to receive the
         * chat signals.  This is done with the help of Java reflection.  
         * Signal handlers are annotated using @BusSignalHandler.
         * 
         * A call to registerSignalHandlers() instructs the system to
         * perform the reflection and "hook up" the signal handlers in the
         * provided object to the signals in the provided interface. 
         *  
         * This is enough information to allow the system to connect the 
         * bus signal named "Chat" with the handler defined here.
         */
        @BusSignalHandler(iface = CHAT_INTERFACE_NAME, signal = "Chat")
        public void Chat(String str) {
            Log.i(TAG, String.format("Chat(%s) signal recieved", str));
            Message msg = mHandler.obtainMessage(MESSAGE_CHAT);
            MessageContext ctx = mBus.getMessageContext();
            String sender = ctx.sender;
            sender = sender.substring(sender.length()-10, sender.length());
            msg.obj = "Message from " + sender + " : " + str;
            mHandler.sendMessage(msg);
        }
    }
    

}