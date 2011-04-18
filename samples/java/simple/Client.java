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

package org.alljoyn.bus.samples.simple;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.Status;

/**
 * AllJoyn client sample.
 * Implements a simple AllJoyn client that connects to the bus and executes a "Ping" method
 * on a remote object.
 */
public class Client {
    static {
        System.loadLibrary("alljoyn_java");
    }

    public static void main(String[] args) {

        /* Create a bus connection and connect to the bus */
        BusAttachment bus = new BusAttachment(Client.class.getName());
        Status status = bus.connect();
        if (Status.OK != status) {
            System.out.println("BusAttachment.connect() failed with " + status.toString());
            return;
        }

        /* Get a remote object */
        Class[] ifaces = { SimpleInterface.class };
        ProxyBusObject proxyObj = bus.getProxyBusObject("org.alljoyn.bus.samples.simple",
                                                         "/testobject",
                                                         0, // zero = sessions are not being used
                                                         ifaces);
        SimpleInterface proxy = proxyObj.getInterface(SimpleInterface.class);

        /* Call the ping method on the remote object */
        try {
            String ret = proxy.Ping("Hello World");
            System.out.println("Ping returned: " + ret);
        } catch (BusException ex) {
            System.err.println("methodCall failed: " + ex.toString());
            return;
        }
    }
}