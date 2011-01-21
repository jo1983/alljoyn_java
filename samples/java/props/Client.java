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

package org.alljoyn.bus.samples.props;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.Variant;
import org.alljoyn.bus.ifaces.Properties;

import java.util.Map;

/**
 * Client accesses properties of the org.alljoyn.samples.props_interface implemented
 * by the /testProperties object of org.alljoyn.samples.props namespace.
 */
public class Client {
    static {
        System.loadLibrary("alljoyn_java");
    }

    /** Main entry point */
    public static void main(String[] args) {

        try {
            Client client = new Client(args);
        } catch (BusException ex) {
            System.err.println("BusException: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private Client(String[] args) throws BusException {

        /* Create a bus connection and connect to the bus */
        BusAttachment bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        if (Status.OK != status) {
            System.out.println("BusAttachment.connect() failed with " + status.toString());
            return;
        }

        /* Get a remote object */
        ProxyBusObject proxyObj =  bus.getProxyBusObject("org.alljoyn.bus.samples.props",
                                                          "/testProperties",
                                                          new Class<?>[] { PropsInterface.class,
                                                                           Properties.class });
        PropsInterface proxy = proxyObj.getInterface(PropsInterface.class);

        /* Get a property */
        System.out.println("StringProp = " + proxy.getStringProp());

        /* Set a property */
        proxy.setStringProp("MyNewValue");

        /* Get all of the properties of the interface */
        System.out.println("StringProp = " + proxy.getStringProp());
        System.out.println("IntProp = " + proxy.getIntProp());

        /* Use the org.freedesktop.DBus.Properties interface to get all the properties */
        Properties properties = proxyObj.getInterface(Properties.class);
        Map<String, Variant> map = properties.GetAll("org.alljoyn.bus.samples.props.PropsInterface");
        System.out.println("StringProp = " + map.get("StringProp").getObject(String.class));
        System.out.println("IntProp = " + map.get("IntProp").getObject(Integer.class));
    }

}
