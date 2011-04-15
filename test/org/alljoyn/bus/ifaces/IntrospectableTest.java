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
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.ifaces.Introspectable;

import static junit.framework.Assert.*;
import junit.framework.TestCase;

public class IntrospectableTest extends TestCase {
    public IntrospectableTest(String name) {
        super(name);
    }

    static {
        System.loadLibrary("alljoyn_java");
    }

    private BusAttachment bus;

    public void setUp() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);
    }

    public void tearDown() throws Exception {
        bus.disconnect();
        bus = null;
    }

    public void testIntrospect() throws Exception {
        ProxyBusObject remoteObj = bus.getProxyBusObject("org.freedesktop.DBus",
                                                         "/org/freedesktop/DBus", 
                                                         BusAttachment.SESSION_ID_ANY,
                                                         new Class[] { Introspectable.class });
        Introspectable introspectable = remoteObj.getInterface(Introspectable.class);
        String data = introspectable.Introspect();
    }
}
