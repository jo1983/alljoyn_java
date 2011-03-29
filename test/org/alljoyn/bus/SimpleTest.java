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
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.ifaces.DBusProxyObj;
import org.alljoyn.bus.ifaces.AllJoynProxyObj;
import org.alljoyn.bus.Status;

import java.lang.Thread;
import junit.framework.TestCase;

public class SimpleTest extends TestCase {

    static {
        System.loadLibrary("alljoyn_java");
    }

    public SimpleTest(String name) {
        super(name);
    }

    public class Service implements SimpleInterface, BusObject  {

        public String Ping(String inStr) {
            return inStr;
        }
    }

    public void testPing() throws Exception {

        /* Create a bus connection and connect to the bus */
        BusAttachment bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        if (Status.OK != status) {
            throw new BusException(status.toString());
        }

        /* Register the service */
        Service service = new Service();
        status = bus.registerBusObject(service, "/testobject");
        if (Status.OK != status) {
            throw new BusException(status.toString());
        }
            
        /* Request a well-known name */
        DBusProxyObj control = bus.getDBusProxyObj();
        DBusProxyObj.RequestNameResult res = control.RequestName("org.alljoyn.bus.samples.simple",
                                                                DBusProxyObj.REQUEST_NAME_NO_FLAGS);
        if (res != DBusProxyObj.RequestNameResult.PrimaryOwner) {
            throw new BusException("Failed to obtain well-known name");
        }

        /* Get a remote object */
        Class[] ifaces = { SimpleInterface.class };
        ProxyBusObject remoteObj = bus.getProxyBusObject("org.alljoyn.bus.samples.simple",
                                                         "/testobject",
                                                         AllJoynProxyObj.SESSION_ID_ANY,
                                                         ifaces);
        SimpleInterface proxy = remoteObj.getInterface(SimpleInterface.class);

        /* Call the ping method on the remote object */
        assertEquals("Hello World", proxy.Ping("Hello World"));
    }
}
