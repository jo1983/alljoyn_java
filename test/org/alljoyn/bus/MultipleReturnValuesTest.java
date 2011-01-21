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
import static org.alljoyn.bus.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static junit.framework.Assert.*;
import junit.framework.TestCase;

public class MultipleReturnValuesTest extends TestCase {
    public MultipleReturnValuesTest(String name) {
        super(name);
    }

    static {
        System.loadLibrary("alljoyn_java");
    }

    private BusAttachment bus;

    public void setUp() throws Exception {
        bus = null;
    }

    public void tearDown() throws Exception {
        if (bus != null) {
            bus.disconnect();
            bus = null;
        }
    }

    public class Service implements MultipleReturnValuesInterface,
                                    BusObject {

        public Service() {
        }

        public Values Method() throws BusException {
            Values v = new Values();
            v.a = 1;
            v.b = 2;
            v.c = new TreeMap<String, String>();
            v.c.put("3", "4");
            @SuppressWarnings(value="unchecked")
            HashMap<String,String>[] hm = (HashMap<String,String>[]) new HashMap[2];
            v.d = hm;
            v.d[0] = new HashMap<String, String>();
            v.d[0].put("5", "6");
            v.d[1] = new HashMap<String, String>();
            v.d[1].put("7", "8");
            v.e = new long[] { 9, 10, 11 };
            v.f = new MultipleReturnValuesInterface.Values.Inner();
            v.f.x = 12;
            return v;
        }
    }

    public void testMethod() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);

        Service service = new Service();
        status = bus.registerBusObject(service, "/service");
        assertEquals(Status.OK, status);

        DBusProxyObj control = bus.getDBusProxyObj();
        DBusProxyObj.RequestNameResult res = control.RequestName("org.alljoyn.bus.MultipleReturnValuesTest",
                                                                DBusProxyObj.REQUEST_NAME_NO_FLAGS);
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, res);
        
        ProxyBusObject remoteObj = bus.getProxyBusObject("org.alljoyn.bus.MultipleReturnValuesTest",
                                                         "/service",
                                                         new Class[] { MultipleReturnValuesInterface.class });
        MultipleReturnValuesInterface proxy = remoteObj.getInterface(MultipleReturnValuesInterface.class);
        MultipleReturnValuesInterface.Values ret = proxy.Method();
        assertEquals(1, ret.a);
        assertEquals(2, ret.b);
        Map<String, String> vc = new TreeMap<String, String>();
        vc.put("3", "4");
        assertEquals(vc, ret.c);
        @SuppressWarnings(value="unchecked")
        Map<String, String>[] vd = (HashMap<String, String>[]) new HashMap[2];
        vd[0] = new HashMap<String, String>();
        vd[0].put("5", "6");
        vd[1] = new HashMap<String, String>();
        vd[1].put("7", "8");
        assertArrayEquals(vd, ret.d);
        assertArrayEquals(new long[] { 9, 10, 11 }, ret.e);
        MultipleReturnValuesInterface.Values.Inner vf = new MultipleReturnValuesInterface.Values.Inner();
        vf.x = 12;
        assertEquals(vf, ret.f);

        bus.deregisterBusObject(service);
    }
}
