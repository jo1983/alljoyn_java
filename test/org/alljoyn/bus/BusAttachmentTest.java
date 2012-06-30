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

import junit.framework.TestCase;
import java.lang.ref.WeakReference;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.bus.ifaces.DBusProxyObj;
import org.alljoyn.bus.ifaces.Introspectable;

public class BusAttachmentTest extends TestCase {
    static {
        System.loadLibrary("alljoyn_java");
    }

    public class Emitter implements EmitterInterface, BusObject {
        private SignalEmitter emitter;

        public Emitter() { 
            emitter = new SignalEmitter(this); 
            emitter.setTimeToLive(1000);
        }
        public void Emit(String string) throws BusException {
            emitter.getInterface(EmitterInterface.class).Emit(string);
        }
    }

    public class Service implements BusObject {
    }

    public class SimpleService implements SimpleInterface, BusObject {
        public String Ping(String str) { 
            MessageContext ctx = bus.getMessageContext();
            assertEquals(false, ctx.isUnreliable);
            assertEquals("/simple", ctx.objectPath);
            assertEquals("org.alljoyn.bus.SimpleInterface", ctx.interfaceName);
            assertEquals("Ping", ctx.memberName);
            assertEquals("s", ctx.signature);
            assertEquals("", ctx.authMechanism);
            return str; 
        }
    }

    public class SecureService implements SecureInterface, BusObject {
        public String Ping(String str) { return str; }
    }

    public class ExceptionService implements SimpleInterface, BusObject {
        public String Ping(String str) throws BusException { throw new BusException("ExceptionService"); }
    }

    public class AnnotationService implements AnnotationInterface,
                                              BusObject {
        public String DeprecatedMethod(String str) throws BusException { return str; }
        public void NoReplyMethod(String str) throws BusException {}
        public void DeprecatedNoReplyMethod(String str) throws BusException {}
        public void DeprecatedSignal(String str) throws BusException {}
    }

    private BusAttachment bus;
    private WeakReference busRef = new WeakReference<BusAttachment>(bus);
    private BusAttachment otherBus;
    private WeakReference otherBusRef = new WeakReference<BusAttachment>(otherBus);
    private int handledSignals1;
    private int handledSignals2;
    private int handledSignals3;
    private int handledSignals4;
    private boolean pinRequested;
    private String name;
    private String address;

    public void setUp() throws Exception {
        bus = null;
        otherBus = null;
        name = "org.alljoyn.bus.BusAttachmentTest.advertise";
        address = System.getProperty("org.alljoyn.bus.address", "unix:abstract=alljoyn");
    }

    public void tearDown() throws Exception {
        System.setProperty("org.alljoyn.bus.address", address);

        if (bus != null) {
            bus.disconnect();
            bus.release();
            bus = null;
        }
        
        if (otherBus != null) {
            otherBus.disconnect();
            otherBus.release();
            otherBus = null;
        }
        /*
         * Each BusAttachment is a very heavy object that creates many file 
         * descripters for each BusAttachment.  This will force Java's Garbage
         * collector to remove the BusAttachments 'bus' and 'serviceBus' before 
         * continuing on to the next test.
         */
        do{
            System.gc();
            Thread.sleep(5);
        }
        while (busRef.get() != null || otherBusRef.get() != null);
    }
    
    public synchronized void stopWait() {
        this.notifyAll();
    }

    public void testEmitSignalFromUnregisteredSource() throws Exception {
        boolean thrown = false;
        try {
            Emitter emitter = new Emitter();
            emitter.Emit("emit1");
            emitter = null;
        } catch (BusException ex) {
            thrown = true;
        } finally {
            assertTrue(thrown);
        }
        
    }

    public void signalHandler1(String string) throws BusException {
        ++handledSignals1;
    }

    public void signalHandler2(String string) throws BusException {
        ++handledSignals2;
    }

    public synchronized void testRegisterMultipleSignalHandlersForOneSignal() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);

        Emitter emitter = new Emitter();
        status = bus.registerBusObject(emitter, "/emitter");
        assertEquals(Status.OK, status);

        handledSignals1 = handledSignals2 = 0;
        status = bus.registerSignalHandler("org.alljoyn.bus.EmitterInterface", "Emit",
                                            this, getClass().getMethod("signalHandler1", String.class));
        assertEquals(Status.OK, status);
        status = bus.registerSignalHandler("org.alljoyn.bus.EmitterInterface", "Emit",
                                            this, getClass().getMethod("signalHandler2", String.class));
        assertEquals(Status.OK, status);

        //
        // Emit a signal and make sure that both of the handlers we registered
        // receive that signal.
        //
        emitter.Emit("emit1");

        //
        // It's hard to say what the minimum time to wait is, but we have to
        // impose a maximum time or we wait forever.  Any conceivable machine
        // whether running on a VM or not should be able to send and receive a
        // signal in ten seconds.  We'll do the wait in increments of one second
        // to minimize the time in the best case.
        //
        for (int i = 0; i < 10; ++i) {
            this.wait(1000);
            //
            // Break out as soon as we see the result we need.
            //
            if(handledSignals1 == 1 && handledSignals2 == 1 ) {
                break;
            }
        }
        
        assertEquals(1, handledSignals1);
        assertEquals(1, handledSignals2);

        handledSignals1 = handledSignals2 = 0;

        //
        // Unregister one of the handlers and make sure that the unregistered
        // handler does not receive the signal but the second handler continues
        // to get the signal.
        //
        bus.unregisterSignalHandler(this, getClass().getMethod("signalHandler1", String.class));
        emitter.Emit("emit2");

        //
        // We want to wait go make sure that we *don't* see a signal, so we
        // have to wait long enough for any conceivable machine to possible
        // receive one.  We can't really wait until the one is received since
        // the driving thread could be blocked for unknown amounts of time and
        // could simply come back later and deliver the signal.  So this test
        // is hard to do in a minimum time.  Since we pulled a time out of the
        // hat above when we decided that any reasonable machine must be able
        // to do a signal in ten seconds, we should use that here as well.
        //
        this.wait(10000);
        assertEquals(0, handledSignals1);
        assertEquals(1, handledSignals2);
        bus.unregisterSignalHandler(this, getClass().getMethod("signalHandler2", String.class));
        emitter = null;
        status = null;
    }

    public void testRegisterSignalHandlerNoLocalObject() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);

        status = bus.registerSignalHandler("org.alljoyn.bus.EmitterInterface", "Emit", 
                                            this, getClass().getMethod("signalHandler1", String.class));
        assertEquals(Status.OK, status);
        bus.unregisterSignalHandler(this, getClass().getMethod("signalHandler1", String.class));
    }

    public class SignalHandlers {
        //@BusSignalHandler(iface = "org.alljoyn.bus.EmitterFoo", signal = "Emit") // ClassNotFound exception
        public void signalHandler1(String string) throws BusException {
        }

        @BusSignalHandler(iface = "org.alljoyn.bus.EmitterBarInterface", signal = "Emit")
        public void signalHandler2(String string) throws BusException {
        }

        @BusSignalHandler(iface = "org.alljoyn.bus.EmitterBarInterface", signal = "EmitBar")
        public void signalHandler3(String string) throws BusException {
        }
    }

    public void testRegisterSignalHandlersNoLocalObject() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);

        SignalHandlers handlers = new SignalHandlers();
        status = bus.registerSignalHandlers(handlers);
        assertEquals(Status.OK, status);
    }

    public synchronized void testRegisterSignalHandlerBeforeLocalObject() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);
        
        status = bus.registerSignalHandler("org.alljoyn.bus.EmitterInterface", "Emit",
                                            this, getClass().getMethod("signalHandler1", String.class));
        assertEquals(Status.OK, status);

        Emitter emitter = new Emitter();
        status = bus.registerBusObject(emitter, "/emitter");
        assertEquals(Status.OK, status);

        handledSignals1 = 0;
        emitter.Emit("emit1");
        this.wait(500);
        assertEquals(1, handledSignals1);
        emitter = null;
    }

    public synchronized void testRegisterSourcedSignalHandler() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);

        Emitter emitter = new Emitter();
        status = bus.registerBusObject(emitter, "/emitter");
        assertEquals(Status.OK, status);

        status = bus.registerSignalHandler("org.alljoyn.bus.EmitterInterface", "Emit",
                                           this, getClass().getMethod("signalHandler1", String.class),
                                           "/emitter");
        assertEquals(Status.OK, status);
        handledSignals1 = 0;
        emitter.Emit("emit1");
        this.wait(500);
        assertEquals(1, handledSignals1);

        bus.unregisterSignalHandler(this, getClass().getMethod("signalHandler1", String.class));
        status = bus.registerSignalHandler("org.alljoyn.bus.EmitterInterface", "Emit",
                                           this, getClass().getMethod("signalHandler1", String.class),
                                           "/doesntexist");
        assertEquals(Status.OK, status);
        handledSignals1 = 0;
        emitter.Emit("emit1");
        this.wait(500);
        assertEquals(0, handledSignals1);
        emitter = null;
    }

    private void signalHandler3(String string) throws BusException {
        ++handledSignals3;
    }

    public void testRegisterPrivateSignalHandler() throws Exception {
        boolean thrown = false;
        try {
            bus = new BusAttachment(getClass().getName());
            Status status = bus.connect();
            assertEquals(Status.OK, status);
    
            status = bus.registerSignalHandler("org.alljoyn.bus.EmitterInterface", "Emit", 
                                                this, getClass().getMethod("signalHandler3", String.class));
            assertEquals(Status.OK, status);
        } catch (NoSuchMethodException ex) {
            thrown = true;
        } finally {
            assertTrue(thrown);
        }
    }

    public void testReregisterNameAfterDisconnect() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);
        
        status = bus.requestName("org.alljoyn.bus.BusAttachmentTest", 0);
        assertEquals(Status.OK, status);

        bus.release();

        bus = new BusAttachment(getClass().getName());
        status = bus.connect();
        assertEquals(Status.OK, status);

        status = bus.requestName("org.alljoyn.bus.BusAttachmentTest", 0);
        assertEquals(Status.OK, status);
    }

    public void testRegisterSameLocalObjectTwice() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);

        Service service = new Service();
        status = bus.registerBusObject(service, "/testobject");
        assertEquals(Status.OK, status);
        status = bus.registerBusObject(service, "/testobject");
        assertEquals(Status.BUS_OBJ_ALREADY_EXISTS, status);
    }

    public void signalHandler4(String string) throws BusException {
        ++handledSignals4;
        MessageContext ctx = bus.getMessageContext();
        assertEquals(true, ctx.isUnreliable);
        assertEquals("/emitter", ctx.objectPath);
        assertEquals("org.alljoyn.bus.EmitterInterface", ctx.interfaceName);
        assertEquals("Emit", ctx.memberName);
        assertEquals("s", ctx.signature);
        assertEquals("", ctx.authMechanism);
        stopWait();
    }

    public synchronized void testSignalMessageContext() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);

        Emitter emitter = new Emitter();
        status = bus.registerBusObject(emitter, "/emitter");
        assertEquals(Status.OK, status);

        handledSignals4 = 0;
        status = bus.registerSignalHandler("org.alljoyn.bus.EmitterInterface", "Emit",
                                            this, getClass().getMethod("signalHandler4", String.class));
        assertEquals(Status.OK, status);

        emitter.Emit("emit4");
        this.wait(500);
        assertEquals(1, handledSignals4);
        emitter = null;
    }

    public void testMethodMessageContext() throws Exception {
        bus = new BusAttachment(getClass().getName());
        SimpleService service = new SimpleService();
        assertEquals(Status.OK, bus.registerBusObject(service, "/simple"));

        assertEquals(Status.OK, bus.connect());
        DBusProxyObj control = bus.getDBusProxyObj();
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner,
                     control.RequestName("org.alljoyn.bus.BusAttachmentTest", 
                                         DBusProxyObj.REQUEST_NAME_NO_FLAGS));
        ProxyBusObject proxyObj = bus.getProxyBusObject("org.alljoyn.bus.BusAttachmentTest",
                                                        "/simple", 
                                                        BusAttachment.SESSION_ID_ANY,
                                                        new Class[] { SimpleInterface.class });
        SimpleInterface proxy = proxyObj.getInterface(SimpleInterface.class);
        proxy.Ping("hello");
    }

    /* ALLJOYN-26 */
    public void testRegisterUnknownAuthListener() throws Exception {
        bus = new BusAttachment(getClass().getName());
        bus.registerKeyStoreListener(new NullKeyStoreListener());
        Status status = bus.connect();
        assertEquals(Status.OK, status);
        status = bus.registerAuthListener("ALLJOYN_BOGUS_KEYX", new AuthListener() {
                public boolean requested(String mechanism, String authPeer, int count, String userName, 
                                         AuthRequest[] requests) {
                    return false;
                }
                public void completed(String mechanism, String authPeer, boolean authenticated) {}
            });
        assertEquals(Status.BUS_INVALID_AUTH_MECHANISM, status);
    }

    private AuthListener authListener = new AuthListener() {
            public boolean requested(String mechanism, String authPeer, int count, String userName, 
                                     AuthRequest[] requests) {
                pinRequested = true;
                for (AuthRequest request : requests) {
                    if (request instanceof PasswordRequest) {
                        ((PasswordRequest) request).setPassword("123456".toCharArray());
                    } else if (request instanceof ExpirationRequest) {
                    } else {
                        return false;
                    }
                }
                return true;
            }

            public void completed(String mechanism, String authPeer, boolean authenticated) {}
        };

    public void testRegisterAuthListenerBeforeConnect() throws Exception {
        bus = new BusAttachment(getClass().getName());
        bus.registerKeyStoreListener(new NullKeyStoreListener());
        SecureService service = new SecureService();
        Status status = bus.registerBusObject(service, "/secure");
        assertEquals(Status.OK, status);
        status = bus.registerAuthListener("ALLJOYN_SRP_KEYX", authListener);
        assertEquals(Status.OK, status);
        status = bus.connect();
        assertEquals(Status.OK, status);
        DBusProxyObj control = bus.getDBusProxyObj();
        DBusProxyObj.RequestNameResult res = control.RequestName("org.alljoyn.bus.BusAttachmentTest", 
                                                                DBusProxyObj.REQUEST_NAME_NO_FLAGS);
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, res);

        BusAttachment otherBus = new BusAttachment(getClass().getName());
        otherBus.registerKeyStoreListener(new NullKeyStoreListener());
        status = otherBus.registerAuthListener("ALLJOYN_SRP_KEYX", authListener);
        assertEquals(Status.OK, status);
        status = otherBus.connect();
        assertEquals(Status.OK, status);
        ProxyBusObject proxyObj = otherBus.getProxyBusObject("org.alljoyn.bus.BusAttachmentTest",
                                                             "/secure", 
                                                             BusAttachment.SESSION_ID_ANY,
                                                             new Class[] { SecureInterface.class });
        SecureInterface proxy = proxyObj.getInterface(SecureInterface.class);
        pinRequested = false;
        proxy.Ping("hello");
        assertEquals(true, pinRequested);

        otherBus.disconnect();
        otherBus.release();
        otherBus = null;
    }

    public void testGetUniqueName() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);
        assertFalse(bus.getUniqueName().equals(""));
    }

    public void testMethodSignalAnnotation() throws Exception {
        bus = new BusAttachment(getClass().getName());
        assertEquals(Status.OK, bus.connect());
        DBusProxyObj control = bus.getDBusProxyObj();
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     control.RequestName("org.alljoyn.bus.BusAttachmentTest", 
                                         DBusProxyObj.REQUEST_NAME_NO_FLAGS));

        AnnotationService service = new AnnotationService();
        assertEquals(Status.OK, bus.registerBusObject(service, "/annotation"));
        
        ProxyBusObject proxyObj = bus.getProxyBusObject("org.alljoyn.bus.BusAttachmentTest",
                                                        "/annotation",
                                                        BusAttachment.SESSION_ID_ANY,
                                                        new Class[] { Introspectable.class });

        Introspectable introspectable = proxyObj.getInterface(Introspectable.class);
        String actual = introspectable.Introspect();
        String expected = 
            "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\"\n" +
            "\"http://standards.freedesktop.org/dbus/introspect-1.0.dtd\">\n" +
            "<node>\n" +
            "  <interface name=\"org.alljoyn.bus.AnnotationInterface\">\n" +
            "    <method name=\"DeprecatedMethod\">\n" +
            "      <arg type=\"s\" direction=\"in\"/>\n" + 
            "      <arg type=\"s\" direction=\"out\"/>\n" + 
            "      <annotation name=\"org.freedesktop.DBus.Deprecated\" value=\"true\"/>\n" +
            "    </method>\n" +
            "    <method name=\"DeprecatedNoReplyMethod\">\n" +
            "      <arg type=\"s\" direction=\"in\"/>\n" + 
            "      <annotation name=\"org.freedesktop.DBus.Method.NoReply\" value=\"true\"/>\n" +
            "      <annotation name=\"org.freedesktop.DBus.Deprecated\" value=\"true\"/>\n" +
            "    </method>\n" +
            "    <signal name=\"DeprecatedSignal\">\n" +
            "      <arg type=\"s\" direction=\"out\"/>\n" + 
            "      <annotation name=\"org.freedesktop.DBus.Deprecated\" value=\"true\"/>\n" +
            "    </signal>\n" +
            "    <method name=\"NoReplyMethod\">\n" +
            "      <arg type=\"s\" direction=\"in\"/>\n" + 
            "      <annotation name=\"org.freedesktop.DBus.Method.NoReply\" value=\"true\"/>\n" +
            "    </method>\n" +
            "  </interface>\n" +
            "  <interface name=\"org.freedesktop.DBus.Introspectable\">\n" +
            "    <method name=\"Introspect\">\n" +
            "      <arg name=\"data\" type=\"s\" direction=\"out\"/>\n" +
            "    </method>\n" +
            "  </interface>\n" +
            "</node>\n";
        assertEquals(expected, actual);
    }

    public void testNoReplyMethod() throws Exception {
        bus = new BusAttachment(getClass().getName());
        assertEquals(Status.OK, bus.connect());
        DBusProxyObj control = bus.getDBusProxyObj();
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner, 
                     control.RequestName("org.alljoyn.bus.BusAttachmentTest", 
                                         DBusProxyObj.REQUEST_NAME_NO_FLAGS));

        AnnotationService service = new AnnotationService();
        assertEquals(Status.OK, bus.registerBusObject(service, "/annotation"));
        
        ProxyBusObject proxyObj = bus.getProxyBusObject("org.alljoyn.bus.BusAttachmentTest",
                                                        "/annotation",
                                                        BusAttachment.SESSION_ID_ANY,
                                                        new Class[] { AnnotationInterface.class });
        AnnotationInterface proxy = proxyObj.getInterface(AnnotationInterface.class);
        proxy.NoReplyMethod("noreply");
        /* No assert here, the test is to make sure the above call doesn't throw an exception. */
    }

    public void testMethodException() throws Exception {
        bus = new BusAttachment(getClass().getName());
        ExceptionService service = new ExceptionService();
        assertEquals(Status.OK, bus.registerBusObject(service, "/exception"));

        assertEquals(Status.OK, bus.connect());
        DBusProxyObj control = bus.getDBusProxyObj();
        assertEquals(DBusProxyObj.RequestNameResult.PrimaryOwner,
                     control.RequestName("org.alljoyn.bus.BusAttachmentTest", 
                                         DBusProxyObj.REQUEST_NAME_NO_FLAGS));
        ProxyBusObject proxyObj = bus.getProxyBusObject("org.alljoyn.bus.BusAttachmentTest",
                                                        "/exception", 
                                                        BusAttachment.SESSION_ID_ANY,
                                                        new Class[] { SimpleInterface.class });
        SimpleInterface proxy = proxyObj.getInterface(SimpleInterface.class);
        try {
            proxy.Ping("hello");
        } catch (BusException ex) {
        }
    }

    private boolean found;
    private boolean lost;

    public class FindNewNameBusListener extends BusListener {
        public FindNewNameBusListener(BusAttachment bus) {
            this.bus = bus;
        }

        public void foundAdvertisedName(String name, short transport, String namePrefix) {
            found = true;

            // stopWait seems to block sometimes so we need to enable concurrency
            bus.enableConcurrentCallbacks();

            stopWait();
        }

        private BusAttachment bus;
    }
    
    public synchronized void testFindNewName() throws Exception {
        
        
        bus = new BusAttachment(getClass().getName());
        BusListener testBusListener = new FindNewNameBusListener(bus);
        bus.registerBusListener(testBusListener);
        
        assertEquals(Status.OK, bus.connect());
        found = false;
        lost = false;
        assertEquals(Status.OK, bus.findAdvertisedName(name));
        
        otherBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        assertEquals(Status.OK, otherBus.connect());
        int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
        assertEquals(Status.OK, otherBus.requestName(name, flag));
        assertEquals(Status.OK, otherBus.advertiseName(name, SessionOpts.TRANSPORT_ANY));
     
        this.wait(5 * 1000);
        assertEquals(true, found);
        
        assertEquals(Status.OK, bus.cancelFindAdvertisedName(name));
    }
    
    public class LostExistingNameBusListener extends BusListener {
        public LostExistingNameBusListener(BusAttachment bus) {
            this.bus = bus;
        }

        public void foundAdvertisedName(String name, short transport, String namePrefix) {
            //System.out.println("Name is :  "+name);
            found = true;

            // stopWait seems to block sometimes so we need to enable concurrency
            bus.enableConcurrentCallbacks();

            stopWait();
        }

        public void lostAdvertisedName(String name, short transport, String namePrefix) {
            lost = true;

            // stopWait seems to block sometimes so we need to enable concurrency
            bus.enableConcurrentCallbacks();

            stopWait();
        }

        private BusAttachment bus;
    }
    
    public synchronized void testLostExistingName() throws Exception {
        // The client part
        bus = new BusAttachment(getClass().getName());
        assertEquals(Status.OK, bus.connect());

        BusListener testBusListener = new LostExistingNameBusListener(bus);
        bus.registerBusListener(testBusListener);

        
        otherBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        assertEquals(Status.OK, otherBus.connect());
        
        int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
        assertEquals(Status.OK, otherBus.requestName(name, flag));
        assertEquals(Status.OK, otherBus.advertiseName(name, SessionOpts.TRANSPORT_ANY));

        found = false;
        lost = false;

        assertEquals(Status.OK, bus.findAdvertisedName(name));

        // Advertise a name and then cancel it.
        this.wait(4 * 1000);
        assertEquals(Status.OK, otherBus.advertiseName("org.alljoyn.bus.BusAttachmentTest.advertise.happy", SessionOpts.TRANSPORT_ANY));
        this.wait(4 * 1000);
        assertEquals(true , found);
        assertEquals(Status.OK, otherBus.cancelAdvertiseName("org.alljoyn.bus.BusAttachmentTest.advertise.happy", SessionOpts.TRANSPORT_ANY));
        this.wait(20 * 1000);
        assertEquals(true, lost);
    }

    public class FindExistingNameBusListener extends BusListener {
        public FindExistingNameBusListener(BusAttachment bus) {
            this.bus = bus;
        }

        public void foundAdvertisedName(String name, short transport, String namePrefix) {
            found = true;

            // stopWait seems to block sometimes so we need to enable concurrency
            bus.enableConcurrentCallbacks();

            stopWait();
        }

        public void lostAdvertisedName(String name, short transport, String namePrefix) {
            lost = true;
        }
        
        private BusAttachment bus;
    }
    
    public synchronized void testFindExistingName() throws Exception {
        bus = new BusAttachment(getClass().getName());
        
        BusListener testBusListener = new FindExistingNameBusListener(bus);
        bus.registerBusListener(testBusListener);
        
        assertEquals(Status.OK, bus.connect());
        
        otherBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        assertEquals(Status.OK, otherBus.connect());
        
        int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
        assertEquals(Status.OK, otherBus.requestName(name, flag));
        assertEquals(Status.OK, otherBus.advertiseName(name, SessionOpts.TRANSPORT_ANY));

        found = false;
        lost = false;
        
        assertEquals(Status.OK, bus.findAdvertisedName(name));
            
        this.wait(2 * 1000);
        assertEquals(true, found);
    }

    // TODO figure out if this is still a valid test.
    public void testFindNullName() throws Exception {
        bus = new BusAttachment(getClass().getName());
        assertEquals(Status.OK, bus.connect());
        assertEquals(Status.OK, bus.findAdvertisedName(null));
//        boolean thrown = false;
//        try {
//            // TODO fix text
////            bus.findName(null, new FindNameListener() {
////                    public void foundName(String name, String guid, String namePrefix, String busAddress) {
////                    }
////                    public void lostAdvertisedName(String name, String guid, String namePrefix, String busAddress) {
////                    }
////                });
//        } catch (IllegalArgumentException ex) {
//            thrown = true;
//        }
//        assertTrue(thrown);
//        thrown = false;
//        try {
//            bus.cancelAdvertiseName(null, SessionOpts.TRANSPORT_ANY);
//        } catch (IllegalArgumentException ex) {
//            thrown = true;
//        }
//        assertTrue(thrown);
    }        

    public class FindMultipleNamesBusListener extends BusListener {
        public FindMultipleNamesBusListener(BusAttachment bus) {
            this.bus = bus;
        }

        public void foundAdvertisedName(String name, short transport, String namePrefix) {
            if (name.equals("name.A")) {
                foundNameA = true;    
            }
            if ( name.equals("name.B")) {
                foundNameB = true;
            }

            // stopWait seems to block sometimes so we need to enable concurrency
            bus.enableConcurrentCallbacks();

            stopWait();
        }

        private BusAttachment bus;
    }
    
    
    private boolean foundNameA;
    private boolean foundNameB;

    public synchronized void testFindMultipleNames() throws Exception {
        bus = new BusAttachment(getClass().getName());
        
        BusListener testBusListener = new FindMultipleNamesBusListener(bus);
        bus.registerBusListener(testBusListener);
        
        assertEquals(Status.OK, bus.connect());
        foundNameA = foundNameB = false;

        assertEquals(Status.OK, bus.findAdvertisedName("name.A"));
        assertEquals(Status.OK, bus.findAdvertisedName("name.B"));

        otherBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        assertEquals(Status.OK, otherBus.connect());
        
        assertEquals(Status.OK, otherBus.advertiseName("name.A", SessionOpts.TRANSPORT_ANY));
        assertEquals(Status.OK, otherBus.advertiseName("name.B", SessionOpts.TRANSPORT_ANY));
        this.wait(4 * 1000);
        // check to see if we have found both names if not wait a little longer
        // not thread safe, but worst case situation is it will wait the full 2 
        // seconds
        if(!foundNameA || !foundNameB){
            this.wait(4 * 1000);
        }
        

        assertEquals(true, foundNameA);
        assertEquals(true, foundNameB);
        
        assertEquals(Status.OK, otherBus.cancelAdvertiseName("name.A", SessionOpts.TRANSPORT_ANY));
        assertEquals(Status.OK, otherBus.cancelAdvertiseName("name.B", SessionOpts.TRANSPORT_ANY));

        assertEquals(Status.OK, bus.cancelFindAdvertisedName("name.B"));
        foundNameA = foundNameB = false;
        assertEquals(Status.OK, otherBus.advertiseName("name.A", SessionOpts.TRANSPORT_ANY));
        assertEquals(Status.OK, otherBus.advertiseName("name.B", SessionOpts.TRANSPORT_ANY));
        //this will unlock when name.A is found
        this.wait(2 * 1000);

        assertEquals(Status.OK, bus.cancelFindAdvertisedName("name.A"));
        assertEquals(Status.OK, otherBus.cancelAdvertiseName("name.A", SessionOpts.TRANSPORT_ANY));
        assertEquals(Status.OK, otherBus.cancelAdvertiseName("name.B", SessionOpts.TRANSPORT_ANY));
        assertEquals(true, foundNameA);
        assertEquals(false, foundNameB);
    }

    public class CancelFindNameInsideListenerBusListener extends BusListener {
        public void foundAdvertisedName(String name, short transport, String namePrefix) {
            if (!found) {
                found = true;
                assertEquals(Status.OK, bus.cancelFindAdvertisedName(namePrefix));
                stopWait();
            }
        }
    }
    
    public synchronized void testCancelFindNameInsideListener() throws Exception {
        bus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        
        BusListener testBusListener = new CancelFindNameInsideListenerBusListener();
        bus.registerBusListener(testBusListener);
        
        assertEquals(Status.OK, bus.connect());
        found = false;
        assertEquals(Status.OK, bus.findAdvertisedName(name));

        otherBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        assertEquals(Status.OK, otherBus.connect());
        
        assertEquals(Status.OK, otherBus.advertiseName(name, SessionOpts.TRANSPORT_ANY));

        this.wait(2 * 1000);
        assertEquals(true, found);
    }

    public void testFindSameName() throws Exception {
        bus = new BusAttachment(getClass().getName());
        assertEquals(Status.OK, bus.connect());
        
        assertEquals(Status.OK, bus.findAdvertisedName(name));
        assertEquals(Status.ALLJOYN_FINDADVERTISEDNAME_REPLY_ALREADY_DISCOVERING, bus.findAdvertisedName(name));
        assertEquals(Status.OK, bus.cancelFindAdvertisedName(name));
    }
    
    /*
     * this test verifies when bindSessionPort is called when the bus is not 
     * connected the BUS_NOT_CONNECTED status is returned.
     * 
     * Also test bindSessionPort after connecting with the bus.
     */
    public void testBindSessionPort() throws Exception {
        bus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        
        SessionOpts sessionOpts = new SessionOpts();
        sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
        sessionOpts.isMultipoint = false;
        sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
        sessionOpts.transports = SessionOpts.TRANSPORT_ANY;
        
        Mutable.ShortValue sessionPort = new Mutable.ShortValue((short) 42);
        
        assertEquals(Status.BUS_NOT_CONNECTED, bus.bindSessionPort(sessionPort, sessionOpts, new SessionPortListener()));
        
        assertEquals(Status.OK, bus.connect());
        
        assertEquals(Status.OK, bus.bindSessionPort(sessionPort, sessionOpts, new SessionPortListener()));
        
    }

    /*
     * test unBindSessionPort by binding the with the sessionPort and instantly
     * unbinding with the session port.
     */
    public void testUnBindSessionPort() throws Exception {
        bus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        
        SessionOpts sessionOpts = new SessionOpts();
        sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
        sessionOpts.isMultipoint = false;
        sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
        sessionOpts.transports = SessionOpts.TRANSPORT_ANY;
        
        Mutable.ShortValue sessionPort = new Mutable.ShortValue((short) 42);
        
        assertEquals(Status.BUS_NOT_CONNECTED, bus.unbindSessionPort(sessionPort.value));
        
        assertEquals(Status.OK, bus.connect());
        
        assertEquals(Status.OK, bus.bindSessionPort(sessionPort, sessionOpts, new SessionPortListener()));
        assertEquals(Status.OK, bus.unbindSessionPort(sessionPort.value));
    }
    
    private boolean sessionAccepted;
    private boolean sessionJoined;
    private boolean onJoined;
    private Status joinSessionStatus;
    private int busSessionId;
    private int otherBusSessionId;
    
    public class JoinSessionSessionListener extends SessionListener{
        
    }
    
    public synchronized void testJoinSession() throws Exception {
        
        found = false;
        sessionAccepted = false;
        sessionJoined = false;
        
        // create new BusAttachment
        bus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        assertEquals(Status.OK, bus.connect());
        
        // Set up SessionOpts
        SessionOpts sessionOpts = new SessionOpts();
        sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
        sessionOpts.isMultipoint = false;
        sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
        sessionOpts.transports = SessionOpts.TRANSPORT_ANY;
        
        // User defined sessionPort Number
        Mutable.ShortValue sessionPort = new Mutable.ShortValue((short) 42);
        
        //bindSessionPort new SessionPortListener
        assertEquals(Status.OK, bus.bindSessionPort(sessionPort, sessionOpts, 
                new SessionPortListener(){
            public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                if (sessionPort == 42) {
                    sessionAccepted = true;
                    stopWait();
                    return true;
                } else {
                    sessionAccepted = false;
                    return false;
                }
            }
            
            public void sessionJoined(short sessionPort, int id, String joiner) {
                if (sessionPort == 42) {
                    busSessionId = id;
                    sessionJoined = true;
                } else {
                    sessionJoined = false;
                }
            }
        }));
        // Request name from bus
        int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
        assertEquals(Status.OK, bus.requestName(name, flag));
        // Advertise same bus name 
        assertEquals(Status.OK, bus.advertiseName(name, SessionOpts.TRANSPORT_ANY));
        
        // Create Second BusAttachment
        otherBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        assertEquals(Status.OK, otherBus.connect());
        
        // Register BusListener for the foundAdvertisedName Listener
        otherBus.registerBusListener(new BusListener() {
                public void foundAdvertisedName(String name, short transport, String namePrefix) {
                    found = true;
                    SessionOpts sessionOpts = new SessionOpts();
                    sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
                    sessionOpts.isMultipoint = false;
                    sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
                    sessionOpts.transports = SessionOpts.TRANSPORT_ANY;
                    
                    Mutable.IntegerValue sessionId = new Mutable.IntegerValue(0);
                    // Since we are using blocking form of joinSession, we need to enable concurrency
                    otherBus.enableConcurrentCallbacks();
                    // Join session once the AdvertisedName has been found
                    joinSessionStatus = otherBus.joinSession(name, (short)42, sessionId, 
                            sessionOpts, new JoinSessionSessionListener());
                    otherBusSessionId = sessionId.value;
                    stopWait();
                }
        });
        
        // find the AdvertisedName
        assertEquals(Status.OK, otherBus.findAdvertisedName(name));
        
        this.wait(4 * 1000);
        
        assertEquals(true, found);
        
        this.wait(4 * 1000);
        if(!sessionAccepted || !sessionJoined) {
            this.wait(4 * 1000);
        }
        
        assertEquals(Status.OK, joinSessionStatus);
        assertEquals(true, sessionAccepted);
        assertEquals(true, sessionJoined);
        assertEquals(busSessionId, otherBusSessionId);
    }

    public synchronized void testJoinSessionAsync() throws Exception {
        /*
         * Setup to create a session, advertise a name on one bus attachment and find the name
         * on another bus attachment.
         */
        bus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        assertEquals(Status.OK, bus.connect());
        assertEquals(Status.OK, bus.bindSessionPort(new Mutable.ShortValue((short) 42), new SessionOpts(), 
                new SessionPortListener(){
                    public boolean acceptSessionJoiner(short sessionPort, String joiner, 
                                                       SessionOpts sessionOpts) {
                        assertEquals(42, sessionPort);
                        return true;
                    }
                    public void sessionJoined(short sessionPort, int id, String joiner) {
                        assertEquals(42, sessionPort);
                    }
                }));
        int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | 
                   BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
        assertEquals(Status.OK, bus.requestName(name, flag));
        assertEquals(Status.OK, bus.advertiseName(name, SessionOpts.TRANSPORT_ANY));
        
        otherBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        assertEquals(Status.OK, otherBus.connect());
        otherBus.registerBusListener(new BusListener() {
                public void foundAdvertisedName(String advertisedName, short transport, String namePrefix) {
                    assertEquals(name, advertisedName);
                    found = true;
                    stopWait();
                }
            });

        found = false;
        assertEquals(Status.OK, otherBus.findAdvertisedName(name));
        this.wait(4 * 1000);
        assertEquals(true, found);

        /*
         * The actual test begins here.
         */
        onJoined = false;
        Integer context = new Integer(0xacceeded);
        assertEquals(Status.OK, otherBus.joinSession(name, (short) 42, new SessionOpts(), 
            new SessionListener(), new OnJoinSessionListener() {
                public void onJoinSession(Status status, int sessionId, SessionOpts opts, Object context) {
                    assertEquals(Status.OK, status);
                    int i = ((Integer)context).intValue();
                    assertEquals(0xacceeded, i);
                    onJoined = true;
                    stopWait();
                }
            }, context));
        this.wait(4 * 1000);
        assertEquals(true, onJoined);
    }
    
    private boolean sessionLost;
    
    public class LeaveSessionSessionListener extends SessionListener {
        public LeaveSessionSessionListener(BusAttachment bus) {
            this.bus = bus;
        }

        public void sessionLost(int sessionId) {
            sessionLost = true;

            // stopWait seems to block sometimes so we need to enable concurrency
            bus.enableConcurrentCallbacks();

            // @@ Seems like stopWait blocks sometimes (Todd?)
            stopWait();
        }
        
        private BusAttachment bus;
    }
    //TODO figure out how to produce the sessionLost signal
    public synchronized void testLeaveSession() throws Exception {
        found = false;
        sessionAccepted = false;
        sessionJoined = false;
        sessionLost = false;
        
        // create new BusAttachment
        bus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        /*
         * The number '1337' is just a dummy number, the BUS_NOT_CONNECTED 
         * status is expected regardless of the number.
         */ 
        assertEquals(Status.BUS_NOT_CONNECTED, bus.leaveSession(1337));
        assertEquals(Status.OK, bus.connect());
        
        // Set up SessionOpts
        SessionOpts sessionOpts = new SessionOpts();
        sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
        sessionOpts.isMultipoint = false;
        sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
        sessionOpts.transports = SessionOpts.TRANSPORT_ANY;
        
        // User defined sessionPort Number  
        Mutable.ShortValue sessionPort = new Mutable.ShortValue((short) 42);
        
        //bindSessionPort new SessionPortListener
        assertEquals(Status.OK, bus.bindSessionPort(sessionPort, sessionOpts, 
                new SessionPortListener(){
            public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                if (sessionPort == 42) {
                    sessionAccepted = true;
                    stopWait();
                    return true;
                } else {
                    sessionAccepted = false;
                    return false;
                }
            }

            public void sessionJoined(short sessionPort, int id, String joiner) {
                if (sessionPort == 42) {
                    sessionJoined = true;
                    busSessionId = id;
                } else {
                    sessionJoined = false;
                }
            }
        }));
        // Request name from bus
        int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
        assertEquals(Status.OK, bus.requestName(name, flag));
        // Advertise same bus name 
        assertEquals(Status.OK, bus.advertiseName(name, SessionOpts.TRANSPORT_ANY));
        
        // Create Second BusAttachment
        otherBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
        
        assertEquals(Status.OK, otherBus.connect());
        
        // Register BusListener for the foundAdvertisedName Listener
        otherBus.registerBusListener(new BusListener() {
                public void foundAdvertisedName(String name, short transport, String namePrefix) {
                    found = true;
                    SessionOpts sessionOpts = new SessionOpts();
                    sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
                    sessionOpts.isMultipoint = false;
                    sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
                    sessionOpts.transports = SessionOpts.TRANSPORT_ANY;
                    
                    Mutable.IntegerValue sessionId = new Mutable.IntegerValue(0);

                    // Since we are using blocking form of joinSession and setLinKTimeout, we need to enable concurrency
                    otherBus.enableConcurrentCallbacks();

                    // Join session once the AdvertisedName has been found
                    joinSessionStatus = otherBus.joinSession(name, (short)42, sessionId, 
                            sessionOpts, new LeaveSessionSessionListener(otherBus));
                    otherBusSessionId = sessionId.value;

                    // Set a link timeout
                    if (joinSessionStatus == Status.OK) {
                        Mutable.IntegerValue timeout = new Mutable.IntegerValue(60);
                        joinSessionStatus = otherBus.setLinkTimeout(sessionId.value, timeout);
                        if (joinSessionStatus == Status.OK) {
                            if ((timeout.value < 60) && (timeout.value != 0)) {
                                joinSessionStatus = Status.FAIL;
                            }
                        }
                    }
                    
                    stopWait();
                }
        });
        
        // find the AdvertisedName
        assertEquals(Status.OK, otherBus.findAdvertisedName(name));
        
        this.wait(4 * 1000);
        
        assertEquals(true, found);
        
        this.wait(4 * 1000);
        if(!sessionAccepted || !sessionJoined) {
            this.wait(4 * 1000);
        }
        
        assertEquals(Status.OK, joinSessionStatus);
        assertEquals(true, sessionAccepted);
        assertEquals(true, sessionJoined);
        assertEquals(busSessionId, otherBusSessionId);
        
        assertEquals(Status.OK, otherBus.leaveSession(otherBusSessionId));
        assertEquals(Status.ALLJOYN_LEAVESESSION_REPLY_NO_SESSION, bus.leaveSession(busSessionId));
        
        found = false;
        sessionAccepted = sessionJoined = false;
        busSessionId = otherBusSessionId = 0;
        
        otherBus.cancelFindAdvertisedName(name);
        otherBus.findAdvertisedName(name);
        
        this.wait(4 * 1000);
        
        assertEquals(true, found);
        
        this.wait(4 * 1000);
        if(!sessionAccepted || !sessionJoined) {
            this.wait(4 * 1000);
        }
        
        assertEquals(Status.OK, joinSessionStatus);
        assertEquals(true, sessionAccepted);
        assertEquals(true, sessionJoined);
        assertEquals(busSessionId, otherBusSessionId);
        
        assertEquals(Status.OK, bus.leaveSession(busSessionId));
        assertEquals(Status.ALLJOYN_LEAVESESSION_REPLY_NO_SESSION, otherBus.leaveSession(otherBusSessionId));
        
        this.wait(4 * 1000);
        assertEquals(true, sessionLost);
    }

    /* ALLJOYN-958 */
    public synchronized void testUnregisterBusListener() throws Exception {
        boolean thrown = false;
        bus = new BusAttachment(getClass().getName());

        BusListener testBusListener = new FindNewNameBusListener(bus);
        bus.registerBusListener(testBusListener);

        assertEquals(Status.OK, bus.connect());

        /* This is the actual test- unregister the bus listener */
        /* The Exception catch will only catch java exceptions and will not catch jni crashes. So, if jni crashes, the whole junit will stop. */
        try {
          bus.unregisterBusListener(testBusListener);
          /* This wait is to ensure that the unregister operation has completed successfully. There are no jni crashes like the one found in ALLJOYN-958 */
          this.wait(1 * 1000);
       } catch (Exception ex) {
            thrown = true;
       } finally {
            assertTrue(!thrown);
       }

    }

    /*
     *  TODO
     *  Verify that all of the BusAttachment methods are tested
     *  addMatch, removeMatch
     *  findAdvertiseName, and setDaemonDebug.
     */
    /*
     * Note: The following BusAttachement methods are tested through indirect 
     * testing (i.e. there is not a test named for the method but the methods 
     * are still being tested)
     * advertisedName - is being tested by testFindNewName, testLostExistingName, 
     *                  testFindExistingName, testFindMultipleNames
     * requestName - is being tested by testFindNewName, testLostExistingName, 
     *               testFindExistingName, testFindMultipleNames
     * cancelAdvertiseName -  is being tested by the testFindMultipleNames
     * releaseName - is being tested in the tearDown of every test 
     */
}
