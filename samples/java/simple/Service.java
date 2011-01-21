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
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.ifaces.DBusProxyObj;

/**
 * Service is an implementation of a simple AllJoyn service that acquires the
 * well-known name org.alljoyn.bus.samples.simple and exports one object
 * (/testservice) that implements the org.alljoyn.bus.samples.simple.SimpleInteface
 * interface.
 */
public class Service implements SimpleInterface, BusObject {
    static {
        System.loadLibrary("alljoyn_java");
    }

    /**
     * Main entry point for org.alljoyn.bus.samples.simple.Service
     */
    public static void main(String[] args) {

        try {
            /* Create a bus connection and connect to the bus */
            BusAttachment bus = new BusAttachment(Service.class.getName());
            Status status = bus.connect();
            if (Status.OK != status) {
                System.out.println("BusAttachment.connect() failed with " + status.toString());
                return;
            }

            /* Register the service */
            Service service = new Service();
            status = bus.registerBusObject(service, "/testobject");
            if (Status.OK != status) {
                System.out.println("BusAttachment.registerBusObject() failed: " + status.toString());
                return;
            }

            /* Request a well-known name */
            try {
                DBusProxyObj control = bus.getDBusProxyObj();
                DBusProxyObj.RequestNameResult res = control.RequestName(
                                                              "org.alljoyn.bus.samples.simple",
                                                              DBusProxyObj.REQUEST_NAME_NO_FLAGS);
                if (res != DBusProxyObj.RequestNameResult.PrimaryOwner) {
                    System.out.println("Failed to obtain well-known name");
                    return;
                }
            } catch (BusException ex) {
                System.out.println("DBusProxyObj.RequestName failed: " + ex.toString());
                return;
            }

            /* Wait forever (or until control-c) */
            while (true) {
                Thread.currentThread().sleep(10000);
            }
        } catch (InterruptedException ex) {
            System.out.println("Interrupted");
        }
    }

    /**
     * @inheritDoc
     */
    public String Ping(String inStr) {
        return inStr;
    }
}
