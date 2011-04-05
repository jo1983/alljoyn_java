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
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.bus.ifaces.AllJoynProxyObj;

import org.alljoyn.bus.AllJoynDaemon;

import static junit.framework.Assert.*;
import junit.framework.TestCase;

public class AllJoynProxyObjTest extends TestCase {
    public AllJoynProxyObjTest(String name) {
        super(name);
        if ("The Android Project".equals(System.getProperty("java.vendor"))) 
        {
            isAndroid = true;
        }
    }

    static {
        System.loadLibrary("alljoyn_java");
    }

    private AllJoynDaemon daemon;
    private BusAttachment bus;
    private BusAttachment otherBus;
    private DBusProxyObj dbus;
    private AllJoynProxyObj alljoyn;
    private String name;
    private String foundName;
    private int foundNameCount;
    private String address;
    private String lostAddress;
    private String lostName;
    private boolean isAndroid = false; // running on android device?

    public void setUp() throws Exception {
        address = System.getProperty("org.alljoyn.bus.address", "unix:abstract=bluebus");
        bus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        assertEquals(Status.OK, bus.connect());
        dbus = bus.getDBusProxyObj();
        alljoyn = bus.getAllJoynProxyObj();
        name = "org.alljoyn.bus.ifaces.AllJoynProxyObjTest";
        foundNameCount = 0;
    }

    public void tearDown() throws Exception {
        dbus.ReleaseName(name);
        alljoyn = null;
        dbus = null;
        bus.disconnect();
        bus = null;

        if (otherBus != null) {
            otherBus.getDBusProxyObj().ReleaseName(name);
            otherBus.disconnect();
            otherBus = null;
        }

        if (daemon != null) {
            daemon.stop();
        }

        System.setProperty("org.alljoyn.bus.address", address);
    }

    public void testConnectDisconnect() throws Exception {
        daemon = new AllJoynDaemon();
        assertEquals(AllJoynProxyObj.ConnectResult.Success, alljoyn.Connect(daemon.remoteAddress()));
        assertEquals(AllJoynProxyObj.DisconnectResult.Success, alljoyn.Disconnect(daemon.remoteAddress()));
    }

    public void testAdvertiseNameCancelAdvertiseName() throws Exception {
        BusObject busObj = new BusObject() {};
        assertEquals(Status.OK, bus.registerBusObject(busObj, "/advertise"));
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     dbus.RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS));

        assertEquals(AllJoynProxyObj.AdvertiseNameResult.Success, alljoyn.AdvertiseName(name));
        assertEquals(AllJoynProxyObj.CancelAdvertiseNameResult.Success, alljoyn.CancelAdvertiseName(name));

        bus.deregisterBusObject(busObj);
    }

    public void testFindNameCancelFindName() throws Exception {
        assertEquals(AllJoynProxyObj.FindNameResult.Success, alljoyn.FindName(name));
        assertEquals(AllJoynProxyObj.CancelFindNameResult.Success, alljoyn.CancelFindName(name));
    }

    public void testListAdvertisedNames() throws Exception {
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     dbus.RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS));
        assertEquals(AllJoynProxyObj.AdvertiseNameResult.Success, alljoyn.AdvertiseName(name));
        String[] names = alljoyn.ListAdvertisedNames();
        assertEquals(1, names.length);
        assertEquals(name, names[0]);
    }

    @BusSignalHandler(iface = "org.alljoyn.Bus", signal = "FoundName")
    public void FoundName(String name, String guid, String namePrefix, String busAddress) {
        foundName = name;
        ++foundNameCount;
    }

    public void testFoundNameDifferentDaemon() throws Exception {
        if (!isAndroid) { // Android device always fails this test
            daemon = new AllJoynDaemon();
            System.setProperty("org.alljoyn.bus.address", daemon.address());
            otherBus = new BusAttachment(getClass().getName());
            assertEquals(Status.OK, otherBus.connect());
            assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     otherBus.getDBusProxyObj().RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS));
            assertEquals(AllJoynProxyObj.AdvertiseNameResult.Success, otherBus.getAllJoynProxyObj().AdvertiseName(name));

            bus.registerSignalHandlers(this);
            assertEquals(AllJoynProxyObj.FindNameResult.Success, alljoyn.FindName(name));
            Thread.currentThread().sleep(1000);
            assertEquals(name, foundName);
            assertTrue(foundNameCount > 0);
        }
    }

    @BusSignalHandler(iface = "org.alljoyn.Bus", signal = "BusConnectionLost")
    public void BusConnectionLost(String busAddress) {
        lostAddress = busAddress;
    }

    public void testBusConnectionLost() throws Exception {
        if (!isAndroid) { // Android device sometimes fails this test
            daemon = new AllJoynDaemon();
            bus.registerSignalHandlers(this);
            assertEquals(AllJoynProxyObj.ConnectResult.Success, alljoyn.Connect(daemon.remoteAddress()));
            lostAddress = null;
            daemon.stop();

            Thread.currentThread().sleep(1000);
            assertEquals(daemon.remoteAddress(), lostAddress);
            daemon = null;
        }
    }

    @BusSignalHandler(iface = "org.alljoyn.Bus", signal = "LostAdvertisedName")
    public void LostAdvertisedName(String name, String guid, String namePrefix, String busAddress) {
        lostName = name;
    }

    public void testLostAdvertisedName() throws Exception {
        if (!isAndroid) { // Android device always fails this test
            daemon = new AllJoynDaemon();
            System.setProperty("org.alljoyn.bus.address", daemon.address());
            otherBus = new BusAttachment(getClass().getName());
            assertEquals(Status.OK, otherBus.connect());
            assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     otherBus.getDBusProxyObj().RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS));
            assertEquals(AllJoynProxyObj.AdvertiseNameResult.Success, otherBus.getAllJoynProxyObj().AdvertiseName(name));

            bus.registerSignalHandlers(this);
            lostName = null;
            assertEquals(AllJoynProxyObj.FindNameResult.Success, alljoyn.FindName(name));
            Thread.currentThread().sleep(1000);
            assertEquals(AllJoynProxyObj.CancelAdvertiseNameResult.Success, otherBus.getAllJoynProxyObj().CancelAdvertiseName(name));
            Thread.currentThread().sleep(1000);
            assertEquals(name, lostName);
        }
    }

    public void testNullDisconnect() throws Exception {
        assertEquals(AllJoynProxyObj.DisconnectResult.Failed, alljoyn.Disconnect(""));
    }

    public void testInvalidDisconnect() throws Exception {
        assertEquals(AllJoynProxyObj.DisconnectResult.Failed, alljoyn.Disconnect("unix:abstract=InvalidAddress"));
    }

    public void testNullConnect() throws Exception {
        assertEquals(AllJoynProxyObj.ConnectResult.InvalidSpec, alljoyn.Connect(""));
    }

    public void testInvalidConnect() throws Exception {
        assertEquals(AllJoynProxyObj.ConnectResult.Failed, alljoyn.Connect("unix:abstract=InvalidAddress"));
    }
}
