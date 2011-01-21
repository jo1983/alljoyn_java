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

import java.util.LinkedList;
import java.util.List;

import org.alljoyn.bus.BusAttachment;
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
     * This empty class must be implemented in order to provide a reference BusObject
     * to use when registering for the BusAttachement and when Creating a new signal 
     * emitter. See the BusHandler CONNECT and the BusHandler DISCONNECT state to see 
     * how this class is used in the code.
     */
    
    class ChatService implements ChatInterface, BusObject {
        
        /*
         * Empty implementation of Chat method.  Since the Chat Method is only 
         * used as a signal emitter this will never be called directly. This 
         * class will always be called from an emitter interface and all values
         * will be handled by the Bus. This Method must exist in the ChatInterface
         * so that signals can be sent.  
         */
        public void Chat(String str) throws BusException {
            // Empty method
        }
        
        
        
    }
    
    public class BusHandler extends Handler {
        
        private static final String CHAT_SERVICE_PATH = "/chatService";
        private static final String NAME_PREFIX = "org.alljoyn.bus.samples.chat";
        private static final String CHAT_INTERFACE_NAME = "org.alljoyn.bus.samples.chat";
        
        public static final int CONNECT = 1;
        public static final int START_DISCOVER = 2;
        private static final int CONNECT_WITH_REMOTE_BUS = 3;
        public static final int END_DISCOVER = 4;
        public static final int CHAT = 5;
        public static final int DISCONNECT = 6;
        
        //AllJoyn specific elements
        private BusAttachment mBus;
        private ChatInterface mChatInterface;
        private boolean mIsConnected;
        private boolean mIsStoppingDiscovery;
        
        private ChatService mChatService;
        private List<String> mBusAddressList;
        
        DBusProxyObj dbusProxy;
        AllJoynProxyObj alljoynProxy;
        
        
        
        public BusHandler(Looper looper) {
            super(looper);
            
            mChatService = new ChatService();
            mBusAddressList = new LinkedList<String>();
            mIsStoppingDiscovery = false;
        }
        
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case (CONNECT): {
                //Create new BusAttachment
                mBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);

                //Register the BusObject with the path "/chatService"
                Status status = mBus.registerBusObject(mChatService, CHAT_SERVICE_PATH);
                logStatus("BusAttachment.registerBusObject()", status);
                if (Status.OK != status) {
                    finish();
                    return;
                }

                //connect the BusAttachment to the daemon.
                status = mBus.connect();
                logStatus("BusAttachment.connect()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }

                /*
                 *  Create a signal emitter to send out the Chat signal.
                 *  
                 *  When using signals a signal emitter is used to control the signals. 
                 *  
                 *  SignalEmitter allows us to set the behavior of signals when they are sent.  
                 *  For the most part the default behavior will be fine.  In this sample the 
                 *  signals need to be available to other devices so global broadcasting of 
                 *  signals has been set to true. 
                 */
                SignalEmitter emitter = new SignalEmitter(mChatService, SignalEmitter.GlobalBroadcast.On);
                mChatInterface = emitter.getInterface(ChatInterface.class);

                /*
                 * When a signal handler  that has been implemented in this 
                 * class using the @BusSignalHandler annotation is detected 
                 * this informs AllJoyn that this program is interested in the 
                 * signals. 
                 * 
                 * This is registers for the Chat signal and the FoundName signal.  
                 */
                status = mBus.registerSignalHandlers(this);
                logStatus("BusAttachement.registerSignalHandlers()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                }
                
                
                // The DBusProxyObj creates a way to make calls to methods built into the DBus standard
                dbusProxy = mBus.getDBusProxyObj();
                // The AllJoynProxyObj creates a way to make calls to methods that are part of AllJoyn interface.
                alljoynProxy = mBus.getAllJoynProxyObj();
                break;
            }
            case (START_DISCOVER): {
                //Request a well-known Name
                try {
                    int flags = (DBusProxyObj.REQUEST_NAME_REPLACE_EXISTING |
                                 DBusProxyObj.REQUEST_NAME_ALLOW_REPLACEMENT |
                                 DBusProxyObj.REQUEST_NAME_DO_NOT_QUEUE);
                    String wellKnownName = NAME_PREFIX + "." + (String) msg.obj;
                    DBusProxyObj.RequestNameResult requestNameResult = dbusProxy.RequestName(wellKnownName, flags);
                    logStatus("DBusProxyObj.RequestName()", requestNameResult, DBusProxyObj.RequestNameResult.PrimaryOwner);
                    
                    if (requestNameResult == DBusProxyObj.RequestNameResult.PrimaryOwner) {
                        //advertise the same well-known name
                        AllJoynProxyObj.AdvertiseNameResult advertiseNameResult = alljoynProxy.AdvertiseName(wellKnownName);
                        logStatus(String.format("AllJoynProxyObj.AdvertiseName(%s)", wellKnownName), 
                                  advertiseNameResult, AllJoynProxyObj.AdvertiseNameResult.Success);
                        
                        if (advertiseNameResult != AllJoynProxyObj.AdvertiseNameResult.Success && 
                            advertiseNameResult != AllJoynProxyObj.AdvertiseNameResult.AlreadyAdvertising) {
                            //if we are unable to advertise Name release the name from the local bus. 
                            DBusProxyObj.ReleaseNameResult releaseNameRes = dbusProxy.ReleaseName(wellKnownName);
                            logStatus(String.format("DBusProxyObj.ReleaseName(%s)", wellKnownName), 
                                      releaseNameRes, DBusProxyObj.ReleaseNameResult.Released);
                            mIsConnected = false;
                        } else {
                            mIsConnected = true;
                        }
                    }
                
                    /*
                     * Each device running the the AllJoyn Chat sample advertises a name NAME_PREFIX.<a_user_entered_name>
                     * for example if the user uses foo as there name the name requested from the bus and advertised is
                     * "org.alljoyn.bus.samples.chat.foo" since buses must advertise a unique name we don't know
                     * the name the other buses will advertise however we can know part of the name each bus will 
                     * advertise.
                     * 
                     * For the AllJoyn Chat sample all of the Bus names advertised will start with 
                     * "org.alljoyn.bus.samples.chat" this will tell the local bus to look for any remote bus that
                     * is advertising a name that uses that prefix. If found the bus will send out a "FoundName" signal
                     * We must register a signal handler for the 'FoundName' signal to know about any remote buses. 
                     */
                    AllJoynProxyObj.FindNameResult findNameResult = alljoynProxy.FindName(NAME_PREFIX);
                    logStatus("AllJoynProxyObj.FindName()", findNameResult, AllJoynProxyObj.FindNameResult.Success);
                } catch (BusException ex) {
                    logException("BusException while trying to Advertize service", ex);
                }
                break;
            }
            /*
             * When the 'FoundName' signal is received it will send the address of the remote bus 
             * to this BusHandler case. The AllJoyn connect method is used to make a P2P connection between
             * two separate buses. 
             */
            case (CONNECT_WITH_REMOTE_BUS): {
                // If discovery is currently being stopped don't connect to any other remote buses.
                if (mIsStoppingDiscovery) {
                    break;
                }
                try { 
                    AllJoynProxyObj.ConnectResult connResult = alljoynProxy.Connect((String) msg.obj);
                    logStatus("AllJoynProxyObj.ConnectResult()", connResult, AllJoynProxyObj.ConnectResult.Success);
                    if (AllJoynProxyObj.ConnectResult.Success == connResult) {
                        mBusAddressList.add((String) msg.obj);
                        mIsConnected = true; 
                    }
                } catch (BusException ex) {
                    logException("BusException while trying to connect with remote bus", ex);
                }
                break;
            }
            /*
             * Disconnect the local bus from all of the other buses that have been found. 
             * Stop looking for the NAME_PREFIX
             * Stop the local bus from advertising its own well known name so no other 
             * buses will try and connect with the local bus.
             * Remove the wellKnownName from the local bus.
             */
            case (END_DISCOVER): {
                mIsStoppingDiscovery = true;
                try {
                    for (String busAddress : mBusAddressList) {
                        AllJoynProxyObj.DisconnectResult disConnResult = alljoynProxy.Disconnect(busAddress);
                        logStatus("AllJoynProxyObj.Disconnect()", disConnResult, AllJoynProxyObj.DisconnectResult.Success);
                        if (AllJoynProxyObj.DisconnectResult.Success == disConnResult) {
                            mIsConnected = false;
                        }
                    }
                    
                    mBusAddressList.clear();               

                    AllJoynProxyObj.CancelFindNameResult cancelFNResult = alljoynProxy.CancelFindName(NAME_PREFIX);
                    logStatus("AllJoynProxyObj.CancelFindName()", cancelFNResult, AllJoynProxyObj.CancelFindNameResult.Success);
                    

                    String wellKnownName = NAME_PREFIX + "." + (String) msg.obj;
                    AllJoynProxyObj.CancelAdvertiseNameResult cancelANResult = alljoynProxy.CancelAdvertiseName(wellKnownName);
                    logStatus(String.format("AllJoynProxyObj.CancelAdvertiseName(%s)", wellKnownName), 
                              cancelANResult, AllJoynProxyObj.CancelAdvertiseNameResult.Success);
                    
                    DBusProxyObj.ReleaseNameResult releaseNameRes = dbusProxy.ReleaseName(wellKnownName);
                    logStatus(String.format("DBusProxyObj.ReleaseName(%s)", wellKnownName), 
                              releaseNameRes, DBusProxyObj.ReleaseNameResult.Released);
                } catch (BusException ex) {
                    logException("BusException while trying to stop advertising", ex);
                }
                mIsStoppingDiscovery = false;
                break;
            }
            case (CHAT): {
                try {
                    mChatInterface.Chat((String) msg.obj);
                    Log.i(TAG, String.format("Chat(%s) msg sent", (String) msg.obj));
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
        
        /*
         * The BusSignalHandler is subscribing to the 'FoundName' signal.  
         * The found name signal is part of the AllJoyn interface built into AllJoyn.
         * In this example we are interested in the busAddress of the remote bus 
         * that was found advertising a specified well-known name. 
         * The bus address is sent to the AllJoyn connect method using the BusHandler 
         * CONNECT_WITH_REMOTE_BUS case.  
         */
        @BusSignalHandler(iface = "org.alljoyn.Bus", signal = "FoundName")
        public void FoundName(String name, String guid, String namePrefix, String busAddress) {
            Log.i(TAG, String.format("org.alljoyn.Bus.FoundName signal detected."));
            Message msg = obtainMessage(CONNECT_WITH_REMOTE_BUS, busAddress);
            sendMessage(msg);
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
}