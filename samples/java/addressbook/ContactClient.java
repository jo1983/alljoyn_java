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
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.Status;

import java.util.Map;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Field;

/**
 * This class implements an DBus client that connects to the bus and queries an DBus
 * service that implements a simple addressbook/contact database.
 */
public class ContactClient {
    static {
        System.loadLibrary("alljoyn_java");
    }

    class ClientException extends Exception {
        ClientException(String msg) {
            super(msg);
        }
    }

    public static void Usage() {
        System.out.println("Usage:");
        System.out.println("  ContactClient add first_name last_name [<phone_type> <phone_number]*");
        System.out.println("  or");
        System.out.println("  ContactClient get [last_name]+");
    }

    public static void main(String[] args) {

        try {
            ContactClient cc = new ContactClient(args);
        } catch (BusException ex) {
            System.err.println("BusException: " + ex.getMessage());
            ex.printStackTrace();
        } catch (ClientException ex) {
            System.out.println("Error: " + ex.getMessage());
            Usage();
        }
    }

    private ContactClient(String[] args) throws BusException, ClientException {

        /* Create a bus connection */
        BusAttachment bus = new BusAttachment(getClass().getName());

        /* Register the authentication listener for the client */
        Status status = bus.registerAuthListener("ALLJOYN_SRP_KEYX", new AuthListener() {
                public boolean requested(String mechanism, int count, String userName,
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

                public void completed(String mechanism, boolean authenticated) {}
            });
        if (Status.OK != status) {
            throw new BusException("BusAttachment.registerAuthListener() failed with " + status.toString());
        }

        /* Connect to the bus */
        status = bus.connect();
        if (Status.OK != status) {
            System.out.println("BusAttachment.connect() failed with " + status.toString());
            return;
        }

        /* Get a remote object */
        Class[] ifaces = { AddressBookInterface.class };
        ProxyBusObject proxyObj = bus.getProxyBusObject(
                                                         "org.alljoyn.bus.samples.addressbook",
                                                         "/addressbook",
                                                         ifaces);
        AddressBookInterface proxy = proxyObj.getInterface(AddressBookInterface.class);

        /* Send request */
        if ("get".equals(args[0])) {
            /* Get one or more contacts from service */
            String[] lastNames = new String[args.length-1];
            System.arraycopy(args, 1, lastNames, 0, args.length-1);
            Contact[] contacts = proxy.getContacts(lastNames);

            /* Print out info on contacts */
            for (Contact c: contacts) {
                System.out.println("First Name: " + c.firstName);
                System.out.println("Last Name: " + c.lastName);
                for (Map.Entry<String, String> e: c.phoneNumberMap.entrySet()) {
                    System.out.println(e.getKey() + ": " + e.getValue());
                }
                System.out.println("--");
            }
        } else if ("add".equals(args[0]) && (3 <= args.length) && (1 == args.length % 2)) {
            /* Add a single contact */
            Contact contact = new Contact();
            contact.firstName = args[1];
            contact.lastName = args[2];
            int i = 3;
            while (i < args.length) {
                contact.phoneNumberMap.put(args[i], args[i+1]);
                i += 2;
            }
            proxy.setContact(contact);
        } else {
            /* Invalid args */
            throw new ClientException("Invalid Args");
        }
    }

}