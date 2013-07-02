/**
 * Copyright 2009-2011, 2013 Qualcomm Innovation Center, Inc.
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

public class ProxyBusObjectTest extends TestCase {
    public ProxyBusObjectTest(String name) {
        super(name);
    }

    static {
        System.loadLibrary("alljoyn_java");
    }

    private String name;
    private BusAttachment otherBus;
    private Service service;
    private BusAttachment bus;
    private ProxyBusObject proxyObj;

    public void setUp() throws Exception {
        name = "org.alljoyn.bus.ProxyBusObjectTest.advertise";
        otherBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        service = new Service();
        assertEquals(Status.OK, otherBus.registerBusObject(service, "/simple"));
        assertEquals(Status.OK, otherBus.connect());

        bus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        assertEquals(Status.OK, bus.connect());
        while (bus.getDBusProxyObj().NameHasOwner(name)) {
            Thread.currentThread().sleep(100);
        }

        assertEquals(Status.OK, otherBus.requestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS));
    }

    public void tearDown() throws Exception {
        bus.disconnect();

        otherBus.releaseName(name);
        otherBus.unregisterBusObject(service);
        otherBus.disconnect();

        bus.release();
        otherBus.release();
    }

    public class DelayReply implements SimpleInterface,
                                       BusObject {
        public String Ping(String str) {
            try {
                Thread.currentThread().sleep(100);
            } catch (InterruptedException ex) {
            }
            return str;
        }
    }

    public void testReplyTimeout() throws Exception {
        DelayReply service = new DelayReply();
        assertEquals(Status.OK, otherBus.registerBusObject(service, "/delayreply"));

        proxyObj = bus.getProxyBusObject(name, "/delayreply", BusAttachment.SESSION_ID_ANY, new Class[] { SimpleInterface.class });
        proxyObj.setReplyTimeout(10);

        boolean thrown = false;
        try {
            SimpleInterface simple = proxyObj.getInterface(SimpleInterface.class);
            simple.Ping("testReplyTimeout");
        } catch (ErrorReplyBusException ex) {
            thrown = true;
        }
        assertEquals(true, thrown);
    }

    public class Service implements SimpleInterface, BusObject {
        public String Ping(String inStr) { return inStr; }
    }

    public void testCreateRelease() throws Exception {
        proxyObj = bus.getProxyBusObject(name, "/simple", BusAttachment.SESSION_ID_ANY, new Class[] { SimpleInterface.class });
        proxyObj.release();
    }


    public void testMethodCall() throws Exception {
        assertEquals(Status.OK, otherBus.advertiseName(name, SessionOpts.TRANSPORT_ANY));

        proxyObj = bus.getProxyBusObject(name, "/simple", BusAttachment.SESSION_ID_ANY, new Class[] { SimpleInterface.class });
        SimpleInterface proxy = proxyObj.getInterface(SimpleInterface.class);
        for (int i = 0; i < 10; ++i) {
            assertEquals("ping", proxy.Ping("ping"));
        }

        proxyObj.release();

        otherBus.cancelAdvertiseName(name, SessionOpts.TRANSPORT_ANY);
    }

    public void testMultipleProxyBusObjects() throws Exception {
        // Connect two proxy objects
        proxyObj = bus.getProxyBusObject(name, "/simple", BusAttachment.SESSION_ID_ANY, new Class[] { SimpleInterface.class });
        ProxyBusObject proxyObj2 = bus.getProxyBusObject(name, "/simple", BusAttachment.SESSION_PORT_ANY, new Class[] { SimpleInterface.class });
        
        // Verify they're both operating
        assertEquals("ping", proxyObj.getInterface(SimpleInterface.class).Ping("ping"));
        assertEquals("ping2", proxyObj2.getInterface(SimpleInterface.class).Ping("ping2"));

        // release one of them
        proxyObj2.release();

        // Verify the other is still working
        assertEquals("ping", proxyObj.getInterface(SimpleInterface.class).Ping("ping"));

        // Disconnect other one
        proxyObj.release();
    }

    public class Emitter implements EmitterInterface, 
                                    BusObject {
        public void Emit(String str) { /* Do nothing, this is a signal. */ }
    }

    /* Call a @BusSignal on a ProxyBusObject interface. */
    public void testSignalFromInterface() throws Exception {
        Emitter service = new Emitter();
        assertEquals(Status.OK, otherBus.registerBusObject(service, "/emitter"));

        boolean thrown = false;
        try {
            proxyObj = bus.getProxyBusObject(name, "/emitter", BusAttachment.SESSION_ID_ANY, new Class[] { EmitterInterface.class });
            proxyObj.getInterface(EmitterInterface.class).Emit("emit");
        } catch (BusException ex) {
            thrown = true;
        }
        assertTrue(thrown);

        proxyObj.release();
    }

    private boolean Methods;
    private boolean Methodi;

    public class MultiMethod implements MultiMethodInterfaceA, MultiMethodInterfaceB,
                                        BusObject {
        public void Method(String str) { Methods = true; };
        public void Method(int i) { Methodi = true; };
    };

    public void testMultiMethod() throws Exception {
        MultiMethod service = new MultiMethod();
        assertEquals(Status.OK, otherBus.registerBusObject(service, "/multimethod"));

        proxyObj = bus.getProxyBusObject(name, "/multimethod", BusAttachment.SESSION_ID_ANY,
                                         new Class[] { MultiMethodInterfaceA.class, MultiMethodInterfaceB.class });

        try {
            Methods = Methodi = false;
            MultiMethodInterfaceA ifacA = proxyObj.getInterface(MultiMethodInterfaceA.class);
            ifacA.Method("str");
            assertEquals(true, Methods);
            assertEquals(false, Methodi);

            Methods = Methodi = false;
            proxyObj.getInterface(MultiMethodInterfaceB.class).Method(10);
            assertEquals(false, Methods);
            assertEquals(true, Methodi);
        } catch(BusException ex) {
            /*
             * This catch statement should not be run if it is run print out a
             * stack trace and fail the unit test.
             */
            ex.printStackTrace();
            System.out.println(ex.getMessage());
            assertTrue(false);
        }
        proxyObj.release();
    }

    public void testGetBusName() throws Exception {
        proxyObj = bus.getProxyBusObject(name, "/simple", BusAttachment.SESSION_ID_ANY, new Class[] { SimpleInterface.class });
        assertEquals(name, proxyObj.getBusName());
        proxyObj.release();
    }

    public void testGetObjPath() throws Exception {
        proxyObj = bus.getProxyBusObject(name, "/simple", BusAttachment.SESSION_ID_ANY, new Class[] { SimpleInterface.class });
        assertEquals("/simple", proxyObj.getObjPath());
        proxyObj.release();
    }
}
