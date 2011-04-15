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
 */

package org.alljoyn.bus.ifaces;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.Variant;
import org.alljoyn.bus.ifaces.DBusProxyObj;
import org.alljoyn.bus.ifaces.Properties;

import java.util.Map;
import static junit.framework.Assert.*;
import junit.framework.TestCase;

public class PropertiesTest extends TestCase {
    public PropertiesTest(String name) {
        super(name);
    }

    static {
        System.loadLibrary("alljoyn_java");
    }

    public class PropertiesTestService implements PropertiesTestServiceInterface,
                                                  BusObject {

        private String string;

        public PropertiesTestService() { this.string = "get"; }

        public String getStringProp() { return this.string; }

        public void setStringProp(String string) { this.string = string; }
    }

    private BusAttachment bus;

    private PropertiesTestService service;

    public void setUp() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);

        service = new PropertiesTestService();
        status = bus.registerBusObject(service, "/testobject");
        assertEquals(Status.OK, status);

        DBusProxyObj control = bus.getDBusProxyObj();
        DBusProxyObj.RequestNameResult res = control.RequestName("org.alljoyn.bus.ifaces.PropertiesTest", 
                                                                DBusProxyObj.REQUEST_NAME_NO_FLAGS);
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, res);
    }

    public void tearDown() throws Exception {
        DBusProxyObj control = bus.getDBusProxyObj();
        DBusProxyObj.ReleaseNameResult res = control.ReleaseName("org.alljoyn.bus.ifaces.PropertiesTest");
        assertEquals(DBusProxyObj.ReleaseNameResult.Released, res);

        bus.unregisterBusObject(service);
        service = null;

        bus.disconnect();
        bus = null;
    }

    public void testGet() throws Exception {
        ProxyBusObject remoteObj = bus.getProxyBusObject("org.alljoyn.bus.ifaces.PropertiesTest",
                                                         "/testobject",  BusAttachment.SESSION_ID_ANY,
                                                         new Class<?>[] { Properties.class });
        Properties properties = remoteObj.getInterface(Properties.class);
        Variant stringProp = properties.Get("org.alljoyn.bus.ifaces.PropertiesTestServiceInterface", 
                                            "StringProp");
        assertEquals("get", stringProp.getObject(String.class));
    }

    public void testSet() throws Exception {
        ProxyBusObject remoteObj = bus.getProxyBusObject("org.alljoyn.bus.ifaces.PropertiesTest",
                                                         "/testobject",  BusAttachment.SESSION_ID_ANY,
                                                         new Class<?>[] { Properties.class });
        Properties properties = remoteObj.getInterface(Properties.class);
        properties.Set("org.alljoyn.bus.ifaces.PropertiesTestServiceInterface", 
                       "StringProp", new Variant("set"));
        Variant stringProp = properties.Get("org.alljoyn.bus.ifaces.PropertiesTestServiceInterface", 
                                            "StringProp");
        assertEquals("set", stringProp.getObject(String.class));
    }

    public void testGetAll() throws Exception {
        ProxyBusObject remoteObj = bus.getProxyBusObject("org.alljoyn.bus.ifaces.PropertiesTest",
                                                         "/testobject",  BusAttachment.SESSION_ID_ANY,
                                                         new Class<?>[] { Properties.class });
        Properties properties = remoteObj.getInterface(Properties.class);
        Map<String, Variant> map = properties.GetAll("org.alljoyn.bus.ifaces.PropertiesTestServiceInterface");
        assertEquals("get", map.get("StringProp").getObject(String.class));
    }

    public void testGetAllOnUnknownInterface() throws Exception {
        boolean thrown = false;
        try {
            ProxyBusObject remoteObj = bus.getProxyBusObject("org.alljoyn.bus.ifaces.PropertiesTest",
                                                             "/testobject", BusAttachment.SESSION_ID_ANY,
                                                             new Class<?>[] { Properties.class });
            Properties properties = remoteObj.getInterface(Properties.class);
            Map<String, Variant> map = properties.GetAll("unknownInterface");
        } catch (BusException ex) {
            thrown = true;
        } finally {
            assertTrue(thrown);
        }
    }
}
