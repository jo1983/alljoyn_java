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
import org.alljoyn.bus.ErrorReplyBusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.ifaces.DBusProxyObj;
import org.alljoyn.bus.annotation.BusSignalHandler;

import java.util.Map;
import static junit.framework.Assert.*;
import junit.framework.TestCase;

public class DBusProxyObjTest extends TestCase {
    public DBusProxyObjTest(String name) {
        super(name);
    }

    static {
        System.loadLibrary("alljoyn_java");
    }

    private BusAttachment bus;

    private DBusProxyObj dbus;

    public void setUp() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);

        dbus = bus.getDBusProxyObj();
    }

    public void tearDown() throws Exception {
        dbus = null;
        
        bus.disconnect();
        bus = null;
    }

    public void testListNames() throws Exception {
        String[] names = dbus.ListNames();
    }

    public void testListActivatableNames() throws Exception {
        String[] names = dbus.ListActivatableNames();
    }

    public void testRequestReleaseName() throws Exception {
        String name = "org.alljoyn.bus.ifaces.testRequestReleaseName";
        DBusProxyObj.RequestNameResult res1 = dbus.RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS);
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, res1);
        DBusProxyObj.ReleaseNameResult res2 = dbus.ReleaseName(name);
        assertEquals(DBusProxyObj.ReleaseNameResult.Released, res2);
    }

    public void testRequestNullName() throws Exception {
        boolean thrown = false;
        try {
            /* This shows up an ER_ALLJOYN_BAD_VALUE_TYPE log error. */
            dbus.RequestName(null, DBusProxyObj.REQUEST_NAME_NO_FLAGS);
        } catch (BusException ex) {
            thrown = true;
        } finally {
            assertTrue(thrown);
        }
    }

    public void testNameHasOwner() throws Exception {
        boolean res = dbus.NameHasOwner("org.alljoyn.bus.ifaces.DBusProxyObjTest");
    }

    public void testStartServiceByName() throws Exception {
        boolean thrown = false;
        try {
            DBusProxyObj.StartServiceByNameResult res = dbus.StartServiceByName("UNKNOWN_SERVICE", 0);
        } catch (ErrorReplyBusException ex) {
            thrown = true;
        } finally {
            assertTrue(thrown);
        }
    }

    public void testGetNameOwner() throws Exception {
        boolean thrown = false;
        try {
            String owner = dbus.GetNameOwner("name");
        } catch (ErrorReplyBusException ex) {
            thrown = true;
        } finally {
            assertTrue(thrown);
        }
    }

    public void testGetConnectionUnixUser() throws Exception {
        String name = "org.alljoyn.bus.ifaces.testGetConnectionUnixUser";
        DBusProxyObj.RequestNameResult res1 = dbus.RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS);
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, res1);

        int uid = dbus.GetConnectionUnixUser(name);

        DBusProxyObj.ReleaseNameResult res2 = dbus.ReleaseName(name);
        assertEquals(DBusProxyObj.ReleaseNameResult.Released, res2);
    }

    public void testGetConnectionUnixUserNoName() throws Exception {
        boolean thrown = false;
        try {
            int uid = dbus.GetConnectionUnixUser("name");
        } catch (ErrorReplyBusException ex) {
            thrown = true;
        } finally {
            assertTrue(thrown);
        }
    }

    public void testGetConnectionUnixProcessID() throws Exception {
        String name = "org.alljoyn.bus.ifaces.testGetConnectionUnixProcessID";
        DBusProxyObj.RequestNameResult res1 = dbus.RequestName(name, DBusProxyObj.REQUEST_NAME_NO_FLAGS);
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, res1);

        int pid = dbus.GetConnectionUnixProcessID(name);

        DBusProxyObj.ReleaseNameResult res2 = dbus.ReleaseName(name);
        assertEquals(DBusProxyObj.ReleaseNameResult.Released, res2);
    }

    public void testGetConnectionUnixProcessIDNoName() throws Exception {
        boolean thrown = false;
        try {
            int pid = dbus.GetConnectionUnixProcessID("name");
        } catch (ErrorReplyBusException ex) {
            thrown = true;
        } finally {
            assertTrue(thrown);
        }
    }

    public void testAddRemoveMatch() throws Exception {
        dbus.AddMatch("type='signal'");
        dbus.RemoveMatch("type='signal'");
    }

    /* 
     * Ignored because DBus daemon returns both a METHOD_RET and ERROR
     * message for this.  The ERROR message is discarded due to the
     * METHOD_RET (will see ER_ALLJOYN_UNMATCHED_REPLY_SERIAL in output),
     * so no exception.
     *
     * AllJoyn does not return an error.
     */
    /*
    public void testRemoveUnknownMatch() throws Exception {
        boolean thrown = false;
        try {
            dbus.RemoveMatch("type='signal'");
        } catch (BusException ex) {
            thrown = true;
        } finally {
            assertTrue(thrown);
        }
    }
    */

    public void testGetId() throws Exception {
        String id = dbus.GetId();
    }

    private String newOwner;
    private String nameAcquired;

    @BusSignalHandler(iface="org.freedesktop.DBus", signal="NameOwnerChanged")
    public void NameOwnerChanged(String name, String oldOwner, String newOwner) throws BusException {
        this.newOwner = newOwner;
        synchronized (this) {
            notify();
        }
    }
    
    @BusSignalHandler(iface="org.freedesktop.DBus", signal="NameLost")
    public void NameLost(String name) throws BusException {
        if (nameAcquired.equals(name)) {
            nameAcquired = "";
        }
        synchronized (this) {
            notify();
        }
    }

    @BusSignalHandler(iface="org.freedesktop.DBus", signal="NameAcquired")
    public void NameAcquired(String name) throws BusException {
        nameAcquired = name;
        synchronized (this) {
            notify();
        }
    }

    public void testNameSignals() throws Exception {
        Status status = bus.registerSignalHandlers(this);        
        if (Status.OK != status) {
            throw new BusException("Cannot register signal handler");
        }

        String name = "org.alljoyn.bus.ifaces.testNameSignals";
        newOwner = "";
        nameAcquired = "";
        int flags = DBusProxyObj.REQUEST_NAME_ALLOW_REPLACEMENT;
        DBusProxyObj.RequestNameResult res1 = dbus.RequestName(name, flags);
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, res1);
        synchronized (this) {
            long start = System.currentTimeMillis();
            while (newOwner.equals("") || !nameAcquired.equals(name)) {
                wait(1000);
                assertTrue("timed out waiting for name signals", (System.currentTimeMillis() - start) < 1000);
            }
        }

        DBusProxyObj.ReleaseNameResult res2 = dbus.ReleaseName(name);
        assertEquals(DBusProxyObj.ReleaseNameResult.Released, res2);
        synchronized (this) {
            long start = System.currentTimeMillis();
            while (!newOwner.equals("") || !nameAcquired.equals("")) {
                wait(1000);
                assertTrue("timed out waiting for name signals", (System.currentTimeMillis() - start) < 1000);
            }
        }

        bus.deregisterSignalHandlers(this);
    }
}
