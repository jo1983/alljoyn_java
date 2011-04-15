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
import org.alljoyn.bus.ifaces.AllJoynProxyObj;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import static junit.framework.Assert.*;
import junit.framework.TestCase;

public class SignalEmitterTest extends TestCase {
    public SignalEmitterTest(String name) {
        super(name);
    }

    static {
        System.loadLibrary("alljoyn_java");
    }

    private BusAttachment bus;

    public class Emitter implements EmitterInterface,
                                    BusObject {

        private SignalEmitter local;
        private SignalEmitter global;
        private SignalEmitter emitter;

        public Emitter() {
            local = new SignalEmitter(this);
            global = new SignalEmitter(this, SignalEmitter.GlobalBroadcast.On);
            emitter = local;
        }

        public void Emit(String string) throws BusException {
            emitter.getInterface(EmitterInterface.class).Emit(string);
        }

        public void setTimeToLive(int timeToLive) { 
            local.setTimeToLive(timeToLive); 
            global.setTimeToLive(timeToLive); 
        }
        
        public void setCompressHeader(boolean compress) { 
            local.setCompressHeader(compress); 
            global.setCompressHeader(compress); 
        }

        public void setGlobalBroadcast(boolean globalBroadcast) { 
            emitter = globalBroadcast ? global : local;
        }
    }

    private Emitter emitter;

    public void setUp() throws Exception {
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        assertEquals(Status.OK, status);

        emitter = new Emitter();
        status = bus.registerBusObject(emitter, "/emitter");
        assertEquals(Status.OK, status);
    }

    public void tearDown() throws Exception {
        bus.unregisterBusObject(emitter);
        emitter = null;

        bus.disconnect();
        bus = null;
    }

    private int signalsHandled;

    public void signalHandler(String string) throws BusException {
        ++signalsHandled;
    }

    public void testTimeToLive() throws Exception {
        emitter.setTimeToLive(1);
        emitter.Emit("timeToLiveOn");

        emitter.setTimeToLive(0);
        emitter.Emit("timeToLiveOff");

        // TODO: how to verify?
    }

    public void testCompressHeader() throws Exception {
        emitter.setCompressHeader(true);
        emitter.Emit("compressHeaderOn1");
        emitter.Emit("compressHeaderOn2");

        emitter.setCompressHeader(false);
        emitter.Emit("compressHeaderOff");

        // TODO: how to verify?
    }

    public void testGlobalBroadcast() throws Exception {
    	// TODO fix this text
//        /* Set up another daemon to receive the global broadcast signal. */
//        AllJoynDaemon daemon = new AllJoynDaemon();
//        AllJoynProxyObj alljoyn = bus.getAllJoynProxyObj();
//        assertEquals(AllJoynProxyObj.ConnectResult.Success, alljoyn.Connect(daemon.remoteAddress()));
//
//        System.setProperty("org.alljoyn.bus.address", daemon.address());
//        BusAttachment otherConn = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);
//        assertEquals(Status.OK, otherConn.connect());
//        assertEquals(Status.OK, otherConn.registerSignalHandler("org.alljoyn.bus.EmitterInterface", "Emit",
//                                                                this, getClass().getMethod("signalHandler", 
//                                                                                           String.class)));
//
//        /* Emit the signal from this daemon. */
//        signalsHandled = 0;
//        emitter.setGlobalBroadcast(true);
//        emitter.Emit("globalBroadcastOn");
//        emitter.setGlobalBroadcast(false);
//        emitter.Emit("globalBroadcastOff");
//        Thread.currentThread().sleep(100);
//        assertEquals(1, signalsHandled);
//
//        otherConn.unregisterSignalHandler(this, getClass().getMethod("signalHandler", String.class));
//        otherConn.disconnect();
//        alljoyn.Disconnect(daemon.remoteAddress());
//        daemon.stop();
    }
}
