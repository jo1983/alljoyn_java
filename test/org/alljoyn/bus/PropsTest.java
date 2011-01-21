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
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.Variant;
import org.alljoyn.bus.ifaces.DBusProxyObj;
import org.alljoyn.bus.ifaces.Properties;

import java.util.Map;
import java.util.TreeMap;
import junit.framework.TestCase;

public class PropsTest extends TestCase {

    static {
        System.loadLibrary("alljoyn_java");
    }

    public PropsTest(String name) {
        super(name);
    }

    public class Service implements PropsInterface, BusObject {

        private String stringProperty = "Hello";
    
        private int intProperty = 6;

        public String getStringProp() { return stringProperty; }

        public void setStringProp(String stringProperty) { this.stringProperty = stringProperty; }

        public int getIntProp() { return intProperty; }

        public void setIntProp(int intProperty) { this.intProperty = intProperty; }
    }

    public void testProps() throws Exception {
 
        /* Create a bus connection and connect to the bus */
        BusAttachment bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        if (Status.OK != status) {
            throw new BusException("BusAttachment.connect() failed with " + status.toString());
        }
        
        /* Register the service */
        Service service = new Service();
        status = bus.registerBusObject(service, "/testProperties");
        if (Status.OK != status) {
            throw new BusException("BusAttachment.registerBusObject() failed: " + status.toString());
        }
            
        /* Request a well-known name */
        DBusProxyObj control = bus.getDBusProxyObj();
        DBusProxyObj.RequestNameResult res = control.RequestName("org.alljoyn.bus.samples.props", 
                                                                DBusProxyObj.REQUEST_NAME_NO_FLAGS);
        if (res != DBusProxyObj.RequestNameResult.PrimaryOwner) {
            throw new BusException("Failed to obtain well-known name");
        }

        /* Get a remote object */
        ProxyBusObject remoteObj = bus.getProxyBusObject("org.alljoyn.bus.samples.props",
                                                         "/testProperties",
                                                         new Class<?>[] { PropsInterface.class,
                                                                          Properties.class });
        PropsInterface proxy = remoteObj.getInterface(PropsInterface.class);

        /* Get a property */
        assertEquals("Hello", proxy.getStringProp());

        /* Set a property */
        proxy.setStringProp("MyNewValue");
        
        /* Get all of the properties of the interface */
        assertEquals("MyNewValue", proxy.getStringProp());
        assertEquals(6, proxy.getIntProp());

        /* Use the org.freedesktop.DBus.Properties interface to get all the properties */
        Properties properties = remoteObj.getInterface(Properties.class);
        Map<String, Variant> map = properties.GetAll("org.alljoyn.bus.PropsInterface");
        assertEquals("MyNewValue", map.get("StringProp").getObject(String.class));
        assertEquals(6, (int)map.get("IntProp").getObject(Integer.class));
   }
}