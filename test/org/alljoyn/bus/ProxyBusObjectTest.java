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
import org.alljoyn.bus.ifaces.AllJoynProxyObj;

import static junit.framework.Assert.*;
import junit.framework.TestCase;

public class ProxyBusObjectTest extends TestCase {
    public ProxyBusObjectTest(String name) {
        super(name);
    }

    static {
        System.loadLibrary("alljoyn_java");
    }

    private String address;
    private String name;
    private AllJoynDaemon daemon;
    private BusAttachment otherBus;
    private Service service;
    private BusAttachment bus;
    private ProxyBusObject proxyObj;

    public void setUp() throws Exception {
        address = System.getProperty("org.alljoyn.bus.address", "unix:abstract=bluebus");
        name = "org.alljoyn.bus.ProxyBusObjectTest.advertise";

        daemon = new AllJoynDaemon();
        System.setProperty("org.alljoyn.bus.address", daemon.address());
        otherBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        service = new Service();
        assertEquals(Status.OK, otherBus.registerBusObject(service, "/simple"));
        assertEquals(Status.OK, otherBus.connect());

        System.setProperty("org.alljoyn.bus.address", address);
        bus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        assertEquals(Status.OK, bus.connect());
        while (bus.getDBusProxyObj().NameHasOwner(name)) {
            Thread.currentThread().sleep(100);
        }

    }

    public void tearDown() throws Exception {
        proxyObj.disconnect();
        bus.disconnect();

        if (daemon != null) {
            otherBus.getAllJoynProxyObj().CancelAdvertiseName(name);
            otherBus.getDBusProxyObj().ReleaseName(name);
            otherBus.deregisterBusObject(service);
            otherBus.disconnect();
            daemon.stop();
            daemon = null;
        }
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
        DBusProxyObj dbus = bus.getDBusProxyObj();
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     dbus.RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS));

        DelayReply service = new DelayReply();
        assertEquals(Status.OK, bus.registerBusObject(service, "/delayreply"));
        
        proxyObj = bus.getProxyBusObject(name, "/delayreply", new Class[] { SimpleInterface.class });
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

    public void testConnect() throws Exception {
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     otherBus.getDBusProxyObj().RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS));
        assertEquals(AllJoynProxyObj.AdvertiseNameResult.Success, otherBus.getAllJoynProxyObj().AdvertiseName(name));

        proxyObj = bus.getProxyBusObject(name, "/simple", new Class[] { SimpleInterface.class });
        assertEquals(Status.OK, proxyObj.connect(daemon.remoteAddress(), 5 * 1000));
    }

    public void testConnectTimeout() throws Exception {
        proxyObj = bus.getProxyBusObject("org.alljoyn.bus.ProxyBusObjectTest.unknown", 
                                         "/simple", new Class[] { SimpleInterface.class });
        assertEquals(Status.TIMEOUT, proxyObj.connect(daemon.remoteAddress(), 1 * 1000));
    }

    public void testConnectDisconnect() throws Exception {
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     otherBus.getDBusProxyObj().RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS));
        assertEquals(AllJoynProxyObj.AdvertiseNameResult.Success, otherBus.getAllJoynProxyObj().AdvertiseName(name));

        proxyObj = bus.getProxyBusObject(name, "/simple", new Class[] { SimpleInterface.class });
        SimpleInterface proxy = proxyObj.getInterface(SimpleInterface.class);
        for (int i = 0; i < 10; ++i) {
            assertEquals(Status.OK, proxyObj.connect(daemon.remoteAddress()));
            assertEquals("ping", proxy.Ping("ping"));
            proxyObj.disconnect();
        }
    }

    public void testCancelConnect() throws Exception {
        proxyObj = bus.getProxyBusObject(name, "/simple", new Class[] { SimpleInterface.class });
        new Thread(new Runnable() {
                public void run() {
                    assertEquals(Status.CANCELLED, proxyObj.connect(daemon.remoteAddress(), 10 * 1000));
                }
            }).start();
        Thread.currentThread().sleep(1 * 1000);
        proxyObj.cancelConnect();
        Thread.currentThread().sleep(2 * 1000);
    }

    public void testMultipleConnect() throws Exception {
        // Add a name owner
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     otherBus.getDBusProxyObj().RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS));

        // Connect two proxy objects
        proxyObj = bus.getProxyBusObject(name, "/simple", new Class[] { SimpleInterface.class });
        assertEquals(Status.OK, proxyObj.connect(daemon.remoteAddress()));
        ProxyBusObject proxyObj2 = bus.getProxyBusObject(name, "/simple", new Class[] { SimpleInterface.class });
        assertEquals(Status.OK, proxyObj2.connect(daemon.remoteAddress()));

        // Verify they're both operating
        assertEquals("ping", proxyObj.getInterface(SimpleInterface.class).Ping("ping"));
        assertEquals("ping2", proxyObj2.getInterface(SimpleInterface.class).Ping("ping2"));

        // Disconnect one of them
        proxyObj2.disconnect();

        // Verify the other is still working
        assertEquals("ping", proxyObj.getInterface(SimpleInterface.class).Ping("ping"));

        // Disconnect other one
        proxyObj.disconnect();

        // Verify that nothing is connected
        boolean thrown = false;
        try {
            proxyObj.getInterface(SimpleInterface.class).Ping("ping");
        } catch (BusException ex) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    private String disconnectedAddress;

    public class DisconnectedListener implements ProxyBusObjectListener {
        public void disconnected(String busAddress) {
            disconnectedAddress = busAddress;
        }
    }

    public void testDisconnectedListener() throws Exception {
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     otherBus.getDBusProxyObj().RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS));
        assertEquals(AllJoynProxyObj.AdvertiseNameResult.Success, otherBus.getAllJoynProxyObj().AdvertiseName(name));

        proxyObj = bus.getProxyBusObject(name, "/simple", new Class[] { SimpleInterface.class });
        assertEquals(Status.OK, proxyObj.connect(daemon.remoteAddress(), 5 * 1000, new DisconnectedListener()));
        disconnectedAddress = null;
        daemon.stop();

        Thread.currentThread().sleep(1000);
        assertEquals(daemon.remoteAddress(), disconnectedAddress);
        daemon = null;
    }

    public class Emitter implements EmitterInterface, 
                                    BusObject {
        public void Emit(String str) { /* Do nothing, this is a signal. */ }
    }

    /* Call a @BusSignal on a ProxyBusObject interface. */
    public void testSignalFromInterface() throws Exception {
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     bus.getDBusProxyObj().RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS));

        Emitter service = new Emitter();
        assertEquals(Status.OK, bus.registerBusObject(service, "/emitter"));

        boolean thrown = false;
        try {
            proxyObj = bus.getProxyBusObject(name, "/emitter", new Class[] { EmitterInterface.class });
            proxyObj.getInterface(EmitterInterface.class).Emit("emit");
        } catch (BusException ex) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    private boolean Methods;
    private boolean Methodi;

    public class MultiMethod implements MultiMethodInterfaceA, MultiMethodInterfaceB,
                                        BusObject {
        public void Method(String str) { Methods = true; };
        public void Method(int i) { Methodi = true; };
    };

    public void testMultiMethod() throws Exception {
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     bus.getDBusProxyObj().RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS));

        MultiMethod service = new MultiMethod();
        assertEquals(Status.OK, bus.registerBusObject(service, "/multimethod"));

        proxyObj = bus.getProxyBusObject(name, "/multimethod", new Class[] { MultiMethodInterfaceA.class, 
                                                                             MultiMethodInterfaceB.class });
        
        Methods = Methodi = false;
        proxyObj.getInterface(MultiMethodInterfaceA.class).Method("str");
        assertEquals(true, Methods);
        assertEquals(false, Methodi);
        
        Methods = Methodi = false;
        proxyObj.getInterface(MultiMethodInterfaceB.class).Method(10);
        assertEquals(false, Methods);
        assertEquals(true, Methodi);
    }    
}
