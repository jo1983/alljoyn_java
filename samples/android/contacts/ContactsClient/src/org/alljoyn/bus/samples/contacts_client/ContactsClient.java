/*
 * Copyright 2009-2011, Qualcomm Innovation Center, Inc.
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
 *  This is a sample code demonstrating how to use AllJoyn messages to pass complex data types.
 *  This will get a String array containing all of the contact found on the phone.
 *  or the list of phone number(s) and e-mail addresses for a contact based on their name.
 */
package org.alljoyn.bus.samples.contacts_client;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.FindNameListener;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.Status;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

/*
 * This is a sample client that will receive information about the contacts stored on
 * the phone from a contacts service.
 */
public class ContactsClient extends Activity {
    static {
        System.loadLibrary("alljoyn_java");
    }


    private static final int DIALOG_CONTACT = 1;
    
    private static final int MESSAGE_DISPLAY_ALL_CONTACTS = 1;
    private static final int MESSAGE_DISPLAY_CONTACT = 2;
    private static final int MESSAGE_POST_TOAST = 3;

    private static final String TAG = "ContactsClient";
    
    private Button mGetContactsBtn;
    private ArrayAdapter<String> mContactsListAdapter;
    private ListView mContactsListView;
    private Menu menu;

    private Contact mAddressEntry;
    private NameId[] mContactNames;
    
    String mSingleName;
    int mSingleUserId;

    BusHandler mBusHandler;
    
    /** Handler */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_DISPLAY_ALL_CONTACTS: 
                mContactNames = (NameId[]) msg.obj;
                //make sure the Contacts list is clear of any old information before filling the list.
                mContactsListAdapter.clear();
                //change the name of the button from "Get Contacts List" to "Update Contacts List"
                mGetContactsBtn.setText(getString(R.string.update_contacts));
                for (int i = 0; i < mContactNames.length; i++) {
                    mContactsListAdapter.add(mContactNames[i].displayName);
                }
                break;
            case MESSAGE_DISPLAY_CONTACT:
                mAddressEntry = (Contact) msg.obj;
                showDialog(DIALOG_CONTACT);
                break;
            case MESSAGE_POST_TOAST:
                Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
                break;
            default:
                break;
            }
        }
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mContactsListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mContactsListView = (ListView) findViewById(R.id.contact_list);
        mContactsListView.setAdapter(mContactsListAdapter);
        mContactsListView.setTextFilterEnabled(true);
        mContactsListView.setOnItemClickListener(new GetContactInformation());

        mGetContactsBtn = (Button) findViewById(R.id.get_contacts_btn);
        mGetContactsBtn.setOnClickListener(new GetContactsListener());

        mAddressEntry = new Contact();

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

    
    /** Called when the activity is exited. */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
    }

    /**
     * Implementation of the OnClickListener attached to the
     * "Get Contacts List"/"Update Contacts List"  button.
     * when clicked this will fill the mcontactsListAdapter with an alphabetized
     * list of all the contacts on the phone.
     */
    private class GetContactsListener implements View.OnClickListener {
        public void onClick(View v) {
            mBusHandler.sendEmptyMessage(BusHandler.GET_ALL_CONTACT_NAMES);
        }
    }



    /**
     * Implementation of the OnItemClickListener for any item in the contacts list
     * The listener will use the the string from the list and use that name to lookup
     * an individual contact based on that name.
     */
    private class GetContactInformation implements AdapterView.OnItemClickListener {
        
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            Message msg = mBusHandler.obtainMessage(BusHandler.GET_CONTACT, mContactNames[position]);
            mBusHandler.sendMessage(msg);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        try {
            switch (id) {
            case DIALOG_CONTACT: {
                dialog = new Dialog(ContactsClient.this);
                break;
            }
            default: {
                dialog = null;
                break;
            }
            }
        } catch (Throwable ex ) {
            Log.e(TAG, String.format("Throwable exception found"), ex);
        }
        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        /*
         * build a dialog that show the contents of the an individual contact
         * This will dynamically build the dialog based on how much information 
         * is known about the contact. 
         */
        case DIALOG_CONTACT: {
            //individual dialog elements
            int PHONE_TABLE_OFFSET = 2; //two columns before the phone numbers start
            int EMAIL_TABLE_OFFSET = 1 + PHONE_TABLE_OFFSET + mAddressEntry.phone.length;

            //reset the dialog to a known starting point
            dialog.setContentView(R.layout.contact);
            dialog.setTitle(getString(R.string.contact_dialog_title));

            // add the contact Name to the top of the table
            TextView contactName = (TextView) dialog.findViewById(R.id.contact_name);
            contactName.setText(mAddressEntry.name);

            //get the table layout so items can be added to it.
            TableLayout contactTable = (TableLayout) dialog.findViewById(R.id.contact_table);

            //add a phone number entry to the dialog displayed on screen for each phone number.
            if (mAddressEntry.phone.length > 0) {
                for (int i = 0; i < mAddressEntry.phone.length; i++) {
                    insertPhoneToTable(contactTable, mAddressEntry.phone[i], i + PHONE_TABLE_OFFSET);
                }
            }

            //add an email number entry to the dialog displayed on screen for each email address.
            if (mAddressEntry.email.length > 0) {
                for (int i = 0; i < mAddressEntry.email.length; i++) {
                    insertEmailToTable(contactTable, mAddressEntry.email[i], i + EMAIL_TABLE_OFFSET);
                }
            }
            break;
        }
        default:
            break;
        }
    }
    // Insert a phone number into the table at the indicated position
    private void insertPhoneToTable(TableLayout table, Contact.Phone phone, int position) {
        TableRow tr = new TableRow(getApplicationContext());
        TextView type = new TextView(getApplicationContext());
        type.setLayoutParams(new TableRow.LayoutParams(1));
        // if the phone type has a custom label use that label other wise pull the type from
        // the phone_types string array.
        if (phone.type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
            type.setText(phone.label);
        } else {
            Resources res = getResources();
            String[] phoneTypes = res.getStringArray(R.array.phone_types);
            type.setText(phoneTypes[phone.type]);
        }

        TextView number = new TextView(getApplicationContext());
        number.setText(phone.number);

        tr.addView(type);
        tr.addView(number);
        table.addView(tr, position);
    }

    // Insert an email address into the table at the indicated position
    private void insertEmailToTable(TableLayout table, Contact.Email email, int position) {
        TableRow tr = new TableRow(getApplicationContext());
        TextView type = new TextView(getApplicationContext());
        type.setLayoutParams(new TableRow.LayoutParams(1));
        // if the email type has a custom label use that label other wise pull the type from
        // the email_types string array.
        if (email.type == ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM) {
            type.setText(email.label);
        } else {
            Resources res = getResources();
            String[] emailTypes = res.getStringArray(R.array.email_types);
            type.setText(emailTypes[email.type]);
        }

        TextView address = new TextView(getApplicationContext());
        address.setText(email.address);

        tr.addView(type);
        tr.addView(address);
        table.addView(tr, position);
    }
    
    /*
     * See the SimpleClient sample for a more complete description of the code used 
     * to connect this code to the Bus
     */
    class BusHandler extends Handler {
       
        private static final String SERVICE_NAME = "org.alljoyn.bus.addressbook";
        
        public static final int CONNECT = 1;
        public static final int DISCONNECT = 2;
        public static final int GET_CONTACT = 3;
        public static final int GET_ALL_CONTACT_NAMES = 4;
        
        private BusAttachment mBus;
        private ProxyBusObject mProxyObj;
        private AddressBookInterface mAddressBookInterface;
        
        public BusHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case CONNECT: {
                // Create a bus connection
                mBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);

                // Connect the bus
                Status status = mBus.connect();
                logStatus("BusAttachment.connect()", status);
                if (Status.OK != status) {
                    finish();
                    return;
                }

                // Get a remote object
                mProxyObj = mBus.getProxyBusObject(SERVICE_NAME, "/addressbook",
                                                   new Class[] { AddressBookInterface.class });
                mAddressBookInterface = mProxyObj.getInterface(AddressBookInterface.class);

                status = mBus.findName(SERVICE_NAME, new FindNameListener() {
                    public void foundName(String name, String guid, String namePrefix, 
                                          String busAddress) {

                        Status status = mProxyObj.connect(busAddress);
                        logStatus("ProxyBusObject.connect()", status);
                        if (status != Status.OK) {
                            finish();
                            return;
                        }
                        
                        // We're only looking for one instance, so stop looking for the name. 
                        mBus.cancelFindName(SERVICE_NAME);
                        logStatus("BusAttachment.cancelFindName()", status);
                        if (status != Status.OK) {
                            finish();
                            return;
                        }
                    }

                    public void lostAdvertisedName(String name, String guid, String namePrefix, String busAddr) { }
                });
                logStatus("BusAttachment.findName()", status);
                if (status != Status.OK) {
                    finish();
                    return;
                } 
                break;
            }
            case DISCONNECT: {
                mProxyObj.disconnect();
                mBus.disconnect();
                getLooper().quit();
                break;
            }
            // Call AddressBookInterface.getContact method and send the result to the UI handler.
            case GET_CONTACT: {
                try {
                    NameId nameId = (NameId)msg.obj;
                    Contact reply = mAddressBookInterface.getContact(nameId.displayName, nameId.userId);
                    Message replyMsg = mHandler.obtainMessage(MESSAGE_DISPLAY_CONTACT, reply);
                    mHandler.sendMessage(replyMsg);
                } catch (BusException ex) {
                    logException("AddressBookInterface.getContact()", ex);
                }
                break;
            }
            // Call AddressBookInterface.getAllContactNames and send the result to the UI handler
            case GET_ALL_CONTACT_NAMES: {
                try {
                    NameId[] reply = mAddressBookInterface.getAllContactNames();
                    Message replyMsg = mHandler.obtainMessage(MESSAGE_DISPLAY_ALL_CONTACTS, (Object) reply);
                    mHandler.sendMessage(replyMsg);
                } catch (BusException ex) {
                    logException("AddressBookInterface.getAllContactNames()", ex);
                }
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

    private void logException(String msg, BusException ex) {
        String log = String.format("%s: %s", msg, ex);
        Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
        mHandler.sendMessage(toastMsg);
        Log.e(TAG, log, ex);
    }
}