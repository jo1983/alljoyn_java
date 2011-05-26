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
import org.alljoyn.bus.Status;
import org.alljoyn.bus.ifaces.DBusProxyObj;

import static junit.framework.Assert.*;

import java.util.Map;
import java.util.TreeMap;
import junit.framework.TestCase;

public class InterfaceDescriptionTest extends TestCase {
    public InterfaceDescriptionTest(String name) {
        super(name);
    }

    static {
        System.loadLibrary("alljoyn_java");
    }

    public class Service implements SimpleInterface,
                                    BusObject {
        public String Ping(String inStr) throws BusException { return inStr; }
    }

    public class ServiceC implements SimpleInterfaceC,
                                     BusObject {
        public String Ping(String inStr) throws BusException { return inStr; }
        public String Pong(String inStr) throws BusException { return inStr; }
    }

    private BusAttachment bus;

    public void setUp() throws Exception {
        bus = new BusAttachment(getClass().getName());
        
        Status status = bus.connect();
        assertEquals(Status.OK, status);

        DBusProxyObj control = bus.getDBusProxyObj();
        DBusProxyObj.RequestNameResult res = control.RequestName("org.alljoyn.bus.InterfaceDescriptionTest", 
                                                                 DBusProxyObj.REQUEST_NAME_NO_FLAGS);
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, res);
    }

    public void tearDown() throws Exception {
        DBusProxyObj control = bus.getDBusProxyObj();
        DBusProxyObj.ReleaseNameResult res = control.ReleaseName("org.alljoyn.bus.InterfaceDescriptionTest");
        assertEquals(DBusProxyObj.ReleaseNameResult.Released, res);

        bus.disconnect();
        bus = null;
    }

    public void testDifferentSignature() throws Exception {
        Service service = new Service();
        Status status = bus.registerBusObject(service, "/service");
        assertEquals(Status.OK, status);

        boolean thrown = false;
        try {
            SimpleInterfaceB proxy = bus.getProxyBusObject("org.alljoyn.bus.InterfaceDescriptionTest", 
                "/service", 
                BusAttachment.SESSION_ID_ANY, 
                new Class[] { SimpleInterfaceB.class }).getInterface(SimpleInterfaceB.class);
            proxy.Ping(1);
        } catch (BusException ex) {
            thrown = true;
        }
        assertTrue(thrown);

        bus.unregisterBusObject(service);
    }

    public void testProxyInterfaceSubset() throws Exception {
        ServiceC service = new ServiceC();
        Status status = bus.registerBusObject(service, "/service");
        assertEquals(Status.OK, status);

        boolean thrown = false;
        try {
            SimpleInterface proxy = bus.getProxyBusObject("org.alljoyn.bus.InterfaceDescriptionTest", "/service", 
                BusAttachment.SESSION_ID_ANY,
                new Class[] { SimpleInterface.class }).getInterface(SimpleInterface.class);
            proxy.Ping("str");
        } catch (BusException ex) {
            thrown = true;
        }
        assertTrue(thrown);

        bus.unregisterBusObject(service);
    }

    public void testProxyInterfaceSuperset() throws Exception {
        Service service = new Service();
        Status status = bus.registerBusObject(service, "/service");
        assertEquals(Status.OK, status);

        boolean thrown = false;
        try {
            SimpleInterfaceC proxy = bus.getProxyBusObject("org.alljoyn.bus.InterfaceDescriptionTest", 
                "/service", 
                BusAttachment.SESSION_ID_ANY,
                new Class[] { SimpleInterfaceC.class }).getInterface(SimpleInterfaceC.class);
            proxy.Ping("str");
        } catch (BusException ex) {
            thrown = true;
        } 
        assertTrue(thrown);

        bus.unregisterBusObject(service);
    }
}
