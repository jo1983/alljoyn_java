/**
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
 */

package org.alljoyn.bus.samples.addressbook;

import org.alljoyn.bus.AuthListener;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.ifaces.DBusProxyObj;

import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

/**
 * ContactService is an implementation of a simple DBus service that stores and
 * retrievs Contacts (address book entries).
 */
public class ContactService implements AddressBookInterface, BusObject {
    static {
        System.loadLibrary("alljoyn_java");
    }

    /** ContactService specific exceptions */
    class ServiceException extends Exception {
        ServiceException(String msg) {
            super(msg);
        }
    }

    /* Store of contacts */
    private Map<String, Contact> contactMap = new TreeMap<String, Contact>();

    /* Bus connection */
    BusAttachment bus = null;

    /**
     * Main entry point for org.alljoyn.bus.samples.simple.Service
     */
    public static void main(String[] args) {

        try {
            /* Create a service and wait for Ctrl-C to exit */
            ContactService cc = new ContactService(args);
            while (true) {
                Thread.currentThread().sleep(10000);
            }
        } catch (BusException ex) {
            System.err.println("BusException: " + ex.getMessage());
            ex.printStackTrace();
        } catch (ServiceException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (InterruptedException ex) {
            System.out.println("Interrupted");
        }
    }

    private ContactService(String[] args) throws BusException, ServiceException,
                                                 InterruptedException {

        /* Create a bus connection */
        bus = new BusAttachment(getClass().getName());

        /* Register the authentication listener for the service */
        Status status = bus.registerAuthListener("ALLJOYN_SRP_KEYX", new AuthListener() {
                public boolean requested(String mechanism, String authPeer, int count, String userName,
                                         AuthRequest[] requests) {
                    for (AuthRequest request : requests) {
                        if (request instanceof PasswordRequest) {
                            ((PasswordRequest) request).setPassword("123456".toCharArray());
                        } else {
                            return false;
                        }
                    }
                    return true;
                }

                public void completed(String mechanism, String authPeer, boolean authenticated) {}
            });
        if (Status.OK != status) {
            throw new BusException("BusAttachment.registerAuthListener() failed with " + status.toString());
        }

        /* Connect to the bus */
        status = bus.connect();
        if (Status.OK != status) {
            throw new ServiceException("BusAttachment.connect() failed with " + status.toString());
        }

        /* Register the service */
        status = bus.registerBusObject(this, "/addressbook");
        if (Status.OK != status) {
            throw new ServiceException("BusAttachment.registerBusObject() failed: "
                                       + status.toString());
        }

        /* Request a well-known name */
        DBusProxyObj control = bus.getDBusProxyObj();
        DBusProxyObj.RequestNameResult res = 
            control.RequestName("org.alljoyn.bus.samples.addressbook",
                                DBusProxyObj.REQUEST_NAME_NO_FLAGS);
        if (res != DBusProxyObj.RequestNameResult.PrimaryOwner) {
            throw new ServiceException("Failed to obtain well-known name");
        }
    }

    /**
     * @inheritDoc
     */
    public void setContact(Contact contact) {
        contactMap.put(contact.lastName, contact);
    }

    /**
     * @inheritDoc
     */
    public Contact getContact(String lastName) throws BusException {
        Contact contact = contactMap.get(lastName);
        if (null == contact) {
            throw new BusException("No such contact");
        }
        return contact;
    }

    /**
     * @inheritDoc
     * Notice that DBus allows for the sending of an empty array.
     */
    public Contact[] getContacts(String[] lastNames) {
        Vector<Contact> contactVec = new Vector<Contact>();
        for (String lastName: lastNames) {
            Contact c = contactMap.get(lastName);
            if (null != c) {
                contactVec.add(c);
            }
        }
        return contactVec.toArray(new Contact[0]);
    }
}
