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

package org.alljoyn.bus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.alljoyn.bus.BusAttachment;

/**
 * A helper proxy used by BusObjects to send signals.  A SignalEmitter
 * instance can be used to send any signal from a given AllJoyn interface.
 */
public class SignalEmitter {

    private static final int GLOBAL_BROADCAST = 0x20;
    private static final int COMPRESSED = 0x40;

    private BusObject source;
    private String destination;
    private int sessionId;
    private int timeToLive;
    private int flags;
    private Object proxy;

    /** Controls behavior of broadcast signals ({@code null} desintation). */
    public enum GlobalBroadcast {
        
        /** 
         * Broadcast signals will not be forwarded across bus-to-bus
         * connections.  This is the default.
         */
        Off,

        /** 
         * Broadcast signals will be forwarded across bus-to-bus
         * connections.
         */
        On
    }

    /**
     * Constructs a SignalEmitter.
     *
     * @param source the source object of any signals sent from this emitter
     * @param destination well-known or unique name of destination for signal
     * @param sessionId A unique SessionId for this AllJoyn session instance
     * @param globalBroadcast whether to forward broadcast signals
     *                        across bus-to-bus connections
     */
    public SignalEmitter(BusObject source, String destination, int sessionId, GlobalBroadcast globalBroadcast) {
        this.source = source;
        this.destination = destination;
        this.sessionId = sessionId;
        this.flags = (globalBroadcast == GlobalBroadcast.On)
            ? this.flags | GLOBAL_BROADCAST 
            : this.flags & ~GLOBAL_BROADCAST;
        proxy = Proxy.newProxyInstance(source.getClass().getClassLoader(),
                                       source.getClass().getInterfaces(), new Emitter());
    }
    
    /**
     * Construct a SignalEmitter used for broadcasting to a session
     * 
     * @param source the source object of any signals sent from this emitter
     * @param sessionId A unique SessionId for this AllJoyn session instance
     * @param globalBroadcast whether to forward broadcast signals
     *                        across bus-to-bus connections
     */
    public SignalEmitter(BusObject source, int sessionId, GlobalBroadcast globalBroadcast) {
    	this(source, null, sessionId, globalBroadcast);
    }
    
    /**
     * Constructs a SignalEmitter used for broadcasting.
     *
     * @param source the source object of any signals sent from this emitter
     * @param globalBroadcast whether to forward broadcast signals
     *                        across bus-to-bus connections
     */
    public SignalEmitter(BusObject source, GlobalBroadcast globalBroadcast) {
        this(source, null, BusAttachment.SESSION_ID_ANY, globalBroadcast);
    }

    /**
     * Constructs a SignalEmitter used for local broadcasting.
     *
     * @param source the source object of any signals sent from this emitter
     */
    public SignalEmitter(BusObject source) {
        this(source, null, BusAttachment.SESSION_ID_ANY, GlobalBroadcast.Off);
    }

    /** Sends the signal. */
    private native void signal(BusObject busObj, String destination, int sessionId, String ifaceName,
                               String signalName, String inputSig, Object[] args, int timeToLive,
                               int flags) throws BusException;

    private class Emitter implements InvocationHandler {

        public Object invoke(Object proxy, Method method, Object[] args) throws BusException {
            for (Class<?> i : proxy.getClass().getInterfaces()) {
                for (Method m : i.getMethods()) {
                    if (method.getName().equals(m.getName())) {
                        signal(source,
                               destination,
                               sessionId,
                               InterfaceDescription.getName(i),
                               InterfaceDescription.getName(m),
                               InterfaceDescription.getInputSig(m),
                               args,
                               timeToLive,
                               flags);
                    }
                }
            }
            return null;
        }
    }

    /**
     * Sets the time-to-live of future signals sent from this emitter.
     *
     * @param timeToLive if non-zero this specifies in milliseconds the useful
     *                   lifetime for this signal. If delivery of the signal is
     *                   delayed beyond the timeToLive due to network congestion
     *                   or other factors the signal may be discarded. There is
     *                   no guarantee that expired signals will not still be
     *                   delivered.
     */
    public void setTimeToLive(int timeToLive) {
        this.timeToLive = timeToLive;
    }

    /**
     * Enables header compression of future signals sent from this emitter.
     *
     * @param compress if {@code true} compress header for destinations that can handle
     *                 header compression
     */
    public void setCompressHeader(boolean compress) {
        this.flags = compress ? this.flags | COMPRESSED : this.flags & ~COMPRESSED;
    }

    /**
     * Gets a proxy to the interface that emits signals.
     *
     * @param intf the interface of the bus object that emits the signals
     * @return the proxy implementing the signal emitter
     */
    public <T> T getInterface(Class<T> intf) {
        @SuppressWarnings(value = "unchecked")
        T p = (T) proxy;
        return p;
    }
}
