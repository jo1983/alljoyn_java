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

package org.alljoyn.bus;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.ifaces.DBusProxyObj;

import java.io.File;

import static junit.framework.Assert.*;
import junit.framework.TestCase;

public class KeyStoreListenerTest extends TestCase {
    static {
        System.loadLibrary("alljoyn_java");
    }

    private BusAttachment bus;
    private BusAttachment otherBus;
    private SecureService service;
    private SecureInterface proxy;
    private BusAuthListener authListener;
    private BusAuthListener otherAuthListener;

    public class InMemoryKeyStoreListener implements KeyStoreListener {
        private byte[] keys;

        public byte[] getKeys() { return keys; }
        public char[] getPassword() { return "password".toCharArray(); }
        public void putKeys(byte[] keys) { this.keys = keys; }
    }

    public class SecureService implements SecureInterface, BusObject {
        public String Ping(String str) { return str; }
    }

    public class BusAuthListener implements AuthListener {
        private String authMechanismRequested;

        public boolean requested(String mechanism, String authPeer, int count, String userName, 
                                 AuthRequest[] requests) {
            authMechanismRequested = mechanism;
            assertEquals("", userName);
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
    }

    public KeyStoreListenerTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        bus = new BusAttachment(getClass().getName());
        authListener = new BusAuthListener();
        assertEquals(Status.OK, 
                     bus.registerAuthListener("ALLJOYN_SRP_KEYX", authListener,
                                              File.createTempFile("alljoyn", "ks").getAbsolutePath()));
        service = new SecureService();
        assertEquals(Status.OK, bus.registerBusObject(service, "/secure"));        

        otherBus = new BusAttachment(getClass().getName() + ".other");
        otherAuthListener = new BusAuthListener();
        assertEquals(Status.OK, 
                     otherBus.registerAuthListener("ALLJOYN_SRP_KEYX", otherAuthListener,
                                                   File.createTempFile("alljoyn_other", "ks").getAbsolutePath()));
    }

    /* Must register key store listener before connecting, so setUp is split into two functions. */
    public void setUp2() throws Exception {
        assertEquals(Status.OK, bus.connect());
        DBusProxyObj control = bus.getDBusProxyObj();
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     control.RequestName("org.alljoyn.bus.BusAttachmentTest", 
                                         DBusProxyObj.REQUEST_NAME_NO_FLAGS));

        assertEquals(Status.OK, otherBus.connect());
        ProxyBusObject proxyObj = otherBus.getProxyBusObject("org.alljoyn.bus.BusAttachmentTest",
                                                             "/secure", 
                                                             BusAttachment.SESSION_ID_ANY,
                                                             new Class[] { SecureInterface.class });
        proxy = proxyObj.getInterface(SecureInterface.class);
    }

    public void tearDown() throws Exception {
        proxy = null;
        otherBus.disconnect();
        otherBus = null;

        DBusProxyObj control = bus.getDBusProxyObj();
        assertEquals(DBusProxyObj.ReleaseNameResult.Released, 
                     control.ReleaseName("org.alljoyn.bus.BusAttachmentTest"));
        bus.disconnect();
        bus.unregisterBusObject(service);
        bus = null;
    }

    public void testInMemoryKeyStoreListener() throws Exception {
        InMemoryKeyStoreListener keyStoreListener = new InMemoryKeyStoreListener();
        bus.registerKeyStoreListener(keyStoreListener);
        InMemoryKeyStoreListener otherKeyStoreListener = new InMemoryKeyStoreListener();
        otherBus.registerKeyStoreListener(otherKeyStoreListener);
        setUp2();

        proxy.Ping("hello");
        assertEquals("ALLJOYN_SRP_KEYX", authListener.authMechanismRequested);
        assertEquals("ALLJOYN_SRP_KEYX", otherAuthListener.authMechanismRequested);

        tearDown();
        setUp();
        bus.registerKeyStoreListener(keyStoreListener);
        otherBus.registerKeyStoreListener(otherKeyStoreListener);
        setUp2();

        proxy.Ping("hello");
        assertEquals(null, authListener.authMechanismRequested);
        assertEquals(null, otherAuthListener.authMechanismRequested);
    }

    public void testDefaultKeyStoreListener() throws Exception {
        setUp2();
        /*
         * The first method call may result in an auth request depending on
         * whether the default key store has been configured already.  But the
         * second request should not result in a request.
         */
        proxy.Ping("hello");
        authListener.authMechanismRequested = null;
        proxy.Ping("hello");
        assertEquals(null, authListener.authMechanismRequested);
    }

    public void testClearKeyStore() throws Exception {
        InMemoryKeyStoreListener keyStoreListener = new InMemoryKeyStoreListener();
        bus.registerKeyStoreListener(keyStoreListener);
        InMemoryKeyStoreListener otherKeyStoreListener = new InMemoryKeyStoreListener();
        otherBus.registerKeyStoreListener(otherKeyStoreListener);
        setUp2();

        proxy.Ping("hello");
        assertEquals("ALLJOYN_SRP_KEYX", authListener.authMechanismRequested);
        assertEquals("ALLJOYN_SRP_KEYX", otherAuthListener.authMechanismRequested);

        tearDown();
        setUp();
        setUp2();

        bus.registerKeyStoreListener(keyStoreListener);
        bus.clearKeyStore();
        otherBus.registerKeyStoreListener(otherKeyStoreListener);
        otherBus.clearKeyStore();
        proxy.Ping("hello");
        assertEquals("ALLJOYN_SRP_KEYX", authListener.authMechanismRequested);
        assertEquals("ALLJOYN_SRP_KEYX", otherAuthListener.authMechanismRequested);
    }
}