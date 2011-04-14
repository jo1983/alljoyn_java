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
 * 
 * AllJoyn Chat Android Sample code.
 *
 */
package org.alljoyn.bus.sample.chat;

import java.util.List;
import java.util.LinkedList;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.MessageContext;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.bus.ifaces.DBusProxyObj;
import org.alljoyn.bus.ifaces.AllJoynProxyObj;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Simple Chat application that will send a string between multiple devices via AllJoyn.
 */
public class AllJoynChat extends Activity {
    static {
        System.loadLibrary("alljoyn_java");
    }

    private static final int DIALOG_ADVERTISE = 1;

    private static final int MESSAGE_CHAT = 1;

    private static final String TAG = "AllJoynChat";

    private EditText mEditText;
    private ArrayAdapter<String> mListViewArrayAdapter;
    private Menu mMenu;
    
    private BusHandler mBusHandler;
    private String mName;
    
    /** UI Handler */
    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_CHAT: {
                mListViewArrayAdapter.add((String) msg.obj); 
                break;
            }
            default: 
                break;
            }
        }
    };

    public void test(Integer i) {
    	i = 100;
    }
    /* Called when the activity is first created. */
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
        
        /* Make all AllJoyn calls through a separate handler thread to prevent blocking the UI. */
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());
        
        /* Connect to an AllJoyn object. */
        mBusHandler.sendEmptyMessage(BusHandler.CONNECT);

        showDialog(DIALOG_ADVERTISE);
    }

    /* Called when the activity is exited. */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Use the mBusHandler to disconnect from the bus. Failing to to this could result in memory leaks
        mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
    }

    /* Called when the menu button is pressed. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        this.mMenu = menu;
        setConnectedState(mBusHandler.usingDiscovery());
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        setConnectedState(mBusHandler.usingDiscovery());
        return true; //must return true for options menu to display
    }
    
    /* Called when a menu item is selected */
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

    /* Called to draw on the screen dialogs.  
     * In this case it only draws the Advertise name dialog.*/
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

    /*
     * used to enable and disable the Connect and Disconnect options for the menu.
     */
    private void setConnectedState(boolean isConnected) {
        if (null != mMenu) {
            mMenu.getItem(0).setEnabled(!isConnected);
            mMenu.getItem(1).setEnabled(isConnected);
        }
    }
    
    /*
     * This class must be implemented in order to provide a reference
     * BusObject to use when registering for the BusAttachement.
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
     
    public class BusHandler extends Handler {
    	
    	private static final short CONTACT_PORT = 42;
        
        private static final String CHAT_SERVICE_PATH = "/chatService";
        private static final String NAME_PREFIX = "org.alljoyn.bus.samples.chat";
        private static final String CHAT_INTERFACE_NAME = "org.alljoyn.bus.samples.chat";
        
        public static final int CONNECT = 1;
        public static final int START_DISCOVER = 2;
        private static final int JOIN_SESSION = 3;
        public static final int END_DISCOVER = 4;
        public static final int CHAT = 5;
        public static final int DISCONNECT = 6;
        
        /*
         * AllJoyn specific elements.
         */
        private BusAttachment mBus;
        
        public class MyBusListener extends BusListener {
        	@Override
    		public void foundAdvertisedName(String name, short transport, String namePrefix) {
                logInfo(String.format("MyBusListener.foundAdvertisedName(%s, 0x%04x, %s)", name, transport, namePrefix));
            	Message msg = obtainMessage(JOIN_SESSION, name);
            	sendMessage(msg);
            }

            @Override
            public void lostAdvertisedName(String name, short transport, String namePrefix) {
                logInfo(String.format("MyBusListener.lostdvertisedName(%s, 0x%04x, %s)", name, transport, namePrefix));
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
                return true;
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
        
        public MyBusListener mMyBusListener;
        
        private boolean mIsConnected;
        private boolean mIsStoppingDiscovery;
        
        private ChatService mChatService;
        private List<ChatInterface> mChatList;
        private List<Integer> mSessionList;
        
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
        
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case (CONNECT): {
                /*
                 * Create a new BusAttachment.
                 */
                mBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);

                /*
                 * Create a bus listener class to handle callbacks from the
                 * BusAttachment and tell the attachment about it
                 */
                mMyBusListener = new MyBusListener();
                mBus.registerBusListener(mMyBusListener);

                /*
                 * Register the BusObject with the path "/chatService"
                 */
                Status status = mBus.registerBusObject(mChatService, CHAT_SERVICE_PATH);
                logStatus("BusAttachment.registerBusObject()", status);
                if (Status.OK != status) {
                    finish();
                    return;
                }

                /*
                 * Connect the BusAttachment to the daemon.
                 */
                status = mBus.connect();
                logStatus("BusAttachment.connect()", status);
                if (status != Status.OK) {
                    finish();
                    return;
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
                
                Mutable.IntegerValue disposition = new Mutable.IntegerValue();

                status = mBus.bindSessionPort(contactPort, sessionOpts, disposition);                
                logStatus(String.format("BusAttachment.bindSessionPort(%d, %s, %d)", 
                    contactPort.value, sessionOpts.toString(), disposition.value), status);
                
                if (status != Status.OK || disposition.value != BusAttachment.ALLJOYN_BINDSESSIONPORT_REPLY_SUCCESS) {
                    finish();
                    return;
                }
                
                /*
                 * When a signal handler  that has been implemented in this 
                 * class using the @BusSignalHandler annotation is detected 
                 * this informs AllJoyn that this program is interested in the 
                 * signals. 
                 * 
                 * This is registers for the Chat signal and the FoundAvertisedName signal.  
                 */
                status = mBus.registerSignalHandlers(this);
                logStatus("BusAttachement.registerSignalHandlers()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }
                
                break;
            }
            case (START_DISCOVER): {
            	/*
                 * Request that a well-known bus name (composed of the chat 
                 * service well-known name prefix and the user-entered 
                 * nickname) be assigned to our bus attachment.  Tell the
                 * bus that we want to own the name immediately and not to
                 * queue the request.
                 */
                Status status;
                Mutable.IntegerValue disposition = new Mutable.IntegerValue();
                
                String wellKnownName = NAME_PREFIX + "." + (String) msg.obj;
                
                status = mBus.requestName(wellKnownName, BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE, disposition);
                logStatus(String.format("BusAttachment.requestName(%s, 0x%08x, %d)", 
                    wellKnownName, BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE, disposition.value), status);
                        
                if (status == Status.OK && disposition.value == BusAttachment.ALLJOYN_REQUESTNAME_REPLY_PRIMARY_OWNER) {
                    /*
                     * Advertise the same well-known name over all of the
                     * available transports.
                     */
                 	status = mBus.advertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY, disposition);
                    logStatus(String.format("BusAttachment.advertiseName(%s, 0x%04x, %d)", 
                        wellKnownName, SessionOpts.TRANSPORT_ANY, disposition.value), status, Status.OK);

                    if (status != Status.OK || disposition.value != BusAttachment.ALLJOYN_ADVERTISENAME_REPLY_SUCCESS) {
                        /*
                         * If we are unable to advertise the name, release
                         * the name from the local bus.
                         */
                        status = mBus.releaseName(wellKnownName, disposition);
                        logStatus(String.format("BusAttachment.releaseName(%s, %d)", wellKnownName, disposition.value), 
                            status);
                        mIsConnected = false;
                    } else {
                        mIsConnected = true;
                    }
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
              	status = mBus.findAdvertisedName(NAME_PREFIX, disposition);
                logStatus(String.format("BusAttachment.findAdvertisedName(%s, %d)", 
                    wellKnownName, disposition.value), status, Status.OK);
                break;
            }

            /*
             * When the 'FoundAdvertisedName' signal is received it will send
             * the well-known name of the found service to this BusHandler case. The
             * AllJoyn JoinSession method is used to make a P2P connection between
             * our client and the remote service.
             */
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
                Mutable.IntegerValue disposition = new Mutable.IntegerValue();
                SessionOpts sessionOpts = new SessionOpts();
                Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
                
                Status status = mBus.joinSession((String) msg.obj, contactPort, disposition, sessionId, sessionOpts);
                logStatus("BusAttachment.joinSession()", status);
                    
                if (status == Status.OK && disposition.value == BusAttachment.ALLJOYN_JOINSESSION_REPLY_SUCCESS) {

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
                	mIsConnected = true; 
                }
                break;
            }
 
           /*
             * - Disconnect from all of the session that have been found.
             * - Stop looking for the NAME_PREFIX
             * - Stop the local bus from advertising its own well known name so
             *   no other buses will try and connect with the local bus.
             * - Remove the sess from the local bus.
             */
            case (END_DISCOVER): {
                mIsStoppingDiscovery = true;
                Status status;
            	Mutable.IntegerValue disposition = new Mutable.IntegerValue();

                for (Integer sid : mSessionList) {
                	status = mBus.leaveSession(sid, disposition);
                    logStatus("BusAttachment.leaveSession()", status);
                }
                    
                mIsConnected = false;
                mSessionList.clear();
                mChatList.clear();
                    
                /*
                 * Ask the bus to stop telling us about new instances of
                 * the NAME_PREFIX service we've been hunting for.
                 */
             	status = mBus.cancelFindAdvertisedName(NAME_PREFIX, disposition);
                logStatus(String.format("BusAttachment.cancelFindAdvertisedName(%s, %d)", 
                    NAME_PREFIX, disposition), status, Status.OK);
                /*
                 * Ask the bus to stop advertising us as an instance of
                 * the chat service.
                 */
                String wellKnownName = NAME_PREFIX + "." + (String) msg.obj;
                status = mBus.cancelAdvertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY, disposition);
                logStatus(String.format("BusAttachment.cancelFindAdvertisedName(%s, 0x%04x, %d)", 
                    NAME_PREFIX, SessionOpts.TRANSPORT_ANY, disposition), status, Status.OK);
                                       
                /*
                 * Ask the bus to release the well-known name we have had
                 * reserved as an alias for our bus attachment.
                 */
                status = mBus.releaseName(wellKnownName, disposition);
                logStatus(String.format("BusAttachment.releaseName(%s, %d)", wellKnownName, disposition.value), 
                    status);

                mIsStoppingDiscovery = false;
                break;
            }
            case (CHAT): {
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
            case (DISCONNECT): {
                mBus.deregisterSignalHandlers(this);
                mBus.deregisterSignalHandlers(mChatService);
                mBus.deregisterBusObject(mChatService);
                mBus.disconnect();
                getLooper().quit();
                break;
            }
            default:
                break;
            }
        }
        
        public boolean usingDiscovery(){
            return this.mIsConnected;
        }

        /*
         * The @BussignalHandler annotation is used to identify this as a signal listener.  When 
         * BusAttachment.registerSignalHandlers(Object) is called all methods in the specified 
         * Object that contain the @BusSignalHandler annotation will be called when the specified 
         * signal comes from the specified interface.  
         * 
         * In this case it is the 'Chat' signal from the 'org.alljoyn.bus.samples.chat' interface.  
         */
        @BusSignalHandler(iface = CHAT_INTERFACE_NAME, signal = "Chat")
        public void  Chat(String str) {
            Log.i(TAG, String.format("Chat(%s) signal recieved", str));
            Message msg = mHandler.obtainMessage(MESSAGE_CHAT);
            MessageContext ctx = mBus.getMessageContext();
            String sender = ctx.sender;
            sender = sender.substring(sender.length()-10, sender.length());
            msg.obj = "Message from " + sender + " : " + str;
            mHandler.sendMessage(msg);
        }
    }
    
    private void logStatus(String msg, Status status) {
        logStatus(msg, status, Status.OK);
    }
    
    /*
     * print the status or result to the Android log. If the result is the expected
     * result only print it to the log.  Otherwise print it to the error log and
     * Sent a Toast to the users screen. 
     */
    private void logStatus(String msg, Object status, Object passStatus) {
        String log = String.format("%s: %s", msg, status);
        if (status == passStatus) {
            Log.i(TAG, log);
        } else {
            Toast.makeText(this, log, Toast.LENGTH_LONG).show();
            Log.e(TAG, log);
        }
    }

    /*
     * When an exception is thrown use this to Toast the name of the exception 
     * and send a log of the exception to the Android log.
     */
    private void logException(String msg, BusException ex) {
        String log = String.format("%s: %s", msg, ex);
        Toast.makeText(this, log, Toast.LENGTH_LONG).show();
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