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

import static junit.framework.Assert.*;
import junit.framework.TestCase;

public class MultipleAuthListenersTest extends TestCase {
    static {
        System.loadLibrary("alljoyn_java");
    }

    private BusAttachment serviceBus;
    private SecureService service;
    private BusAuthListener serviceAuthListener;
    private BusAttachment clientBus;
    private BusAuthListener clientAuthListener;
    private SecureInterface proxy;

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
                } else if (request instanceof CertificateRequest) {
                } else if (request instanceof PrivateKeyRequest) {
                } else if (request instanceof VerifyRequest) {
                } else {
                    return false;
                }
            }
            return true;
        }

        public void completed(String mechanism, String authPeer, boolean authenticated) {}

        public String getAuthMechanismRequested() { return authMechanismRequested; }
    }

    public MultipleAuthListenersTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        serviceBus = new BusAttachment(getClass().getName());
        serviceBus.registerKeyStoreListener(new NullKeyStoreListener());
        service = new SecureService();
        assertEquals(Status.OK, serviceBus.registerBusObject(service, "/secure"));
        assertEquals(Status.OK, serviceBus.connect());
        DBusProxyObj control = serviceBus.getDBusProxyObj();
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     control.RequestName("org.alljoyn.bus.BusAttachmentTest", 
                                         DBusProxyObj.REQUEST_NAME_NO_FLAGS));
        serviceAuthListener = new BusAuthListener();

        clientBus = new BusAttachment(getClass().getName());
        clientBus.registerKeyStoreListener(new NullKeyStoreListener());
        assertEquals(Status.OK, clientBus.connect());
        clientAuthListener = new BusAuthListener();
        ProxyBusObject proxyObj = clientBus.getProxyBusObject("org.alljoyn.bus.BusAttachmentTest",
                                                              "/secure", 
                                                              BusAttachment.SESSION_ID_ANY,
                                                              new Class[] { SecureInterface.class });
        proxy = proxyObj.getInterface(SecureInterface.class);
    }

    public void tearDown() throws Exception {
        proxy = null;
        clientBus.disconnect();
        clientBus = null;

        DBusProxyObj control = serviceBus.getDBusProxyObj();
        assertEquals(DBusProxyObj.ReleaseNameResult.Released, 
                     control.ReleaseName("org.alljoyn.bus.BusAttachmentTest"));
        serviceBus.disconnect();
        serviceBus.unregisterBusObject(service);
        serviceBus = null;
    }

    public void testSrpAndRsaAuthListeners() throws Exception {
        assertEquals(Status.OK, serviceBus.registerAuthListener("ALLJOYN_SRP_KEYX ALLJOYN_RSA_KEYX", 
                                                                serviceAuthListener));
        assertEquals(Status.OK, clientBus.registerAuthListener("ALLJOYN_SRP_KEYX ALLJOYN_RSA_KEYX", 
                                                               clientAuthListener));
        proxy.Ping("hello");
        assertEquals(serviceAuthListener.getAuthMechanismRequested(), 
                     clientAuthListener.getAuthMechanismRequested());
    }

    public void testNoCommonAuthMechanism() throws Exception {
        assertEquals(Status.OK, serviceBus.registerAuthListener("ALLJOYN_RSA_KEYX", serviceAuthListener));
        assertEquals(Status.OK, clientBus.registerAuthListener("ALLJOYN_SRP_KEYX", clientAuthListener));
        boolean thrown = false;
        try {
            proxy.Ping("hello");
        } catch (BusException ex) {
            thrown = true;
        }
        assertEquals(true, thrown);
    }
}