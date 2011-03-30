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

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.bus.annotation.Position;
import org.alljoyn.bus.annotation.Signature;

/**
 * A predefined bus interface that is implemented by the local AllJoyn daemon.  It
 * is used to control advanced bus operations such as P2P-aware operations and
 * authentication.
 */
@BusInterface(name = "org.alljoyn.Bus")
public interface AllJoynProxyObj {

    /**
     * Use reliable message-based communication to move data between session endpoints.
     */
    public static Byte TRAFFIC_MESSAGES       = 0x01;

    /**
     * Use unreliable (e.g., UDP) socket-based communication to move data between
     * session endpoints.  RAW does not imply raw sockets (that bypass ALL
     * enapsulation possibly down to the MAC level), it implies raw in an AllJoyn
     * sense --MESSAGE encapsulation is not used, but for example UDP + IP + MAC
     * encapsulation is used.
     */
    public static Byte TRAFFIC_RAW_UNRELIABLE = 0x02;

    /**
     * Use reliable (e.g., TCP) socket-based communication to move data between
     * session endpoints.  RAW does not imply raw sockets (that bypass ALL
     * enapsulation possibly down to the MAC level), it implies raw in an AllJoyn
     * sense --MESSAGE encapsulation is not used, but for example UDP + IP + MAC
     * encapsulation is used.
     */
    public static Byte TRAFFIC_RAW_RELIABLE   = 0x04;
    
    /**
     * Do not limit the spatial scope of sessions.  This means that sessions may
     * be joined by jointers located anywhere.
     */
    public static Byte PROXIMITY_ANY      = (byte)0xff;

    /**
     * Limit the spatial scope of sessions to the local host.  Interpret as 
     * "the same physical machine."  This means that sessions may be joined by
     * jointers located only on the same physical machine as the one hosting the
     * session.
     */
    public static Byte PROXIMITY_PHYSICAL = 0x01;

    /**
     * Limit the spatial scope of sessions to anwhere on the local logical
     * network segment.  This means that sessions may be joined by jointers
     * located somewhere on the network.
     */
    public static Byte PROXIMITY_NETWORK  = 0x02;
    
    /**
     * Use no transport to communicate with a given session.
     */
    public static Short TRANSPORT_NONE      = 0x0000;

    /**
     * Use any available transport to communicate with a given session.
     */
    public static Short TRANSPORT_ANY       = (short)0xffff;

    /**
     * Use only the local transport to communicate with a given session.
     */
    public static Short TRANSPORT_LOCAL     = 0x0001;

    /**
     * Use only Bluetooth transport to communicate with a given session.
     */
    public static Short TRANSPORT_BLUETOOTH = 0x0002;

    /**
     * Use only a wireless local area network to communicate with a given session.
     */
    public static Short TRANSPORT_WLAN      = 0x0004;

    /**
     * Use only a wireless wide area network to communicate with a given session.
     */
    public static Short TRANSPORT_WWAN      = 0x0008;

    /** 
     * The session options (characteristics) used by sessions (cf. socket options).
     */
    public class SessionOpts {

        @Position(0) 
        @Signature("y")
        public Byte traffic;

        @Position(1)
        @Signature("y")
        public Byte proximity;

        @Position(2)
        @Signature("q")
        public Short transports;

        public SessionOpts() {
            traffic = TRAFFIC_MESSAGES;
            proximity = PROXIMITY_ANY;
            transports = TRANSPORT_ANY;
        }
    }

    /** 
     * When passed to BindSessionPort as the requested port, the system will
     * assign an ephemeral session port
     */
    public static Short SESSION_PORT_ANY = 0;

    /** 
     * When passed to ProxyBusObject, the system will use any available connection.
     */
    public static Integer SESSION_ID_ANY = 0;

    enum BindSessionPortResult {
        /** Invalid. */
        Invalid,

        /** Success. */
        Success,

        /** The specified session port is already bound. */
        AlreadyExists,

        /** Connect failed. */
        Failed;
    }

    /** {@link #BindSessionPort(Short, Boolean, SessionOpts, Short)} return values. */
    public class BindSessionPortReturns {
        @Position(0) public BindSessionPortResult rc;
        @Position(1) public short boundPort;
    }

    /**
     * Requests the local daemon to bind a new session to a given contact 
     * SessionPort, creating a new session port if requested.
     *
     * @param sessionPort  The session port to use as the well-known contact
     *     port for the session.  If SESSION_PORT_ANY, the system will assign
     *     a new, unused port.
     * @param isMultipoint  If false, the new session will be a point-to-point
     *     session supporting only two participants.  If true, the new session
     *     will be a point-to-multipoint session supporting moer than two 
     *     participants.
     * @param SessionOpts The session options describing the characteristics of
     *     session instances.
     *
     * @return a structure containing a status code indicating success or failure
     *     and a boundPort containing the actual port bound (useful if 
     *     SESSION_PORT_ANY was specified.
     *
     * @throws BusException
     */
    @BusMethod(signature = "qb(yyq)", replySignature = "uq")
    BindSessionPortReturns BindSessionPort(Short sessionPort, Boolean isMultipoint, SessionOpts sessionOpts) throws BusException;

    /** {@link #JoinSession(String,Short,SessionOpts)} result codes. */
    enum JoinSessionResult {
        /** Invalid. */
        Invalid,

        /** Success. */
        Success,

        /** The specified session port doesn not exist. */
        NoSession,

        /** Failed to find a suitable transport. */
        Unreachable,

        /** Underlying bus connect failed. */
        ConnectFailed,

        /** The join request was rejected by the session creator. */
        Rejected,

        /** The join request failed due to incompatible session options. */
        BadSessionOpts,

        /** Connect failed. */
        Failed;
    }

    /** {@link #JoinSession(String,Short,SessionOpts,Integer,SessionOpts)} return values. */
    public class JoinSessionReturns {
        @Position(0) public JoinSessionResult rc;
        @Position(1) public int sessionId;
        @Position(2) public SessionOpts sessionOpts;
    }

    /**
     * Requests the local daemon to join to a session hosted on a given bus
     * address, over a given contact session port.
     *
     * @param sessionHost  The bus name of an endpoint that is hosting the session.
     * @param sessionPort  The contact session port of the hosted session.
     * @param sessionOpts  The session options describing the desired characteristics of the new session.
     *
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "sq(yyq)", replySignature = "uu(yyq)")
    JoinSessionReturns JoinSession(String sessionHost, Short sessionPort, SessionOpts sessionOpts) throws BusException;

    /** {@link #LeaveSession(Integer)} return value. */
    enum LeaveSessionResult {

        /** Invalid. */
        Invalid,

        /** Success. */
        Success,

        /** No such session exists. */
        NoSession,

        /** Disconnect failed. */
        Failed;
    };

    /**
     * Requests the local daemon to disconnect from a given session previously
     * joined via a call to {@link #JoinSession(String,Short,SessionOpts,Integer,SessionOpts)}.
     *
     * @param sessionId  The session ID corresponding to the session to leave.
     *
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "u", replySignature = "u")
    LeaveSessionResult LeaveSession(Integer sessionId) throws BusException;

    /** {@link #AdvertiseName(String, Short)} return value.*/
    enum AdvertiseNameResult {

        /** Invalid. */
        Invalid,

        /** Success. */
        Success,

        /** This endpoint has already requested advertising this name. */
        AlreadyAdvertising,

        /** Advertise failed. */
        Failed;
    }

    /**
     * Requests the local daemon to advertise the already obtained well-known
     * attachment name to other AllJoyn instances that might be interested in
     * connecting to the named service.
     * 
     * @param wellKnownName well-known name of the attachment that wishes to be
     *                      advertised to remote AllJoyn instances
     * @param transports    a 16-bit bit-field with '1's indicating the transports
     *                      over which this advertisement should be made
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "sq", replySignature = "u")
    AdvertiseNameResult AdvertiseName(String wellKnownName, Short transports) throws BusException;

    /** {@link #CancelAdvertiseName(String)} return value.*/
    enum CancelAdvertiseNameResult {

        /** Invalid. */
        Invalid,

        /** Success. */
        Success,

        /** Cancel advertise failed. */
        Failed;
    }

    /**
     * Requests the local daemon to stop advertising the well-known attachment
     * name to other AllJoyn instances. The well-known name must have previously
     * been advertised via a call to {@link #AdvertiseName(String)}.
     *
     * @param wellKnownName well-known name of the attachment that should end
     *                      advertising
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "s", replySignature = "u")
    CancelAdvertiseNameResult CancelAdvertiseName(String wellKnownName) throws BusException;

    /** {@link #FindAdvertisedName(String)} return value.*/
    enum FindAdvertisedNameResult {

        /** Invalid. */
        Invalid,

        /** Success. */
        Success,

        /** This enpoint has already requested discover for name. */
        AlreadyDiscovering,

        /** Failed */
        Failed;
    }

    /**
     * Registers interest in a well-known attachment name being advertised by a
     * remote AllJoyn instance.  When the local AllJoyn daemon receives such an
     * advertisement it will send a {@link #FoundAdvertisedName(String, Short,
     * String)} signal. This attachment can then choose to ignore the
     * advertisement or to join the corresponding session {@link 
     * #JoinSession(String,Short,SessionOpts)}
     * 
     *
     * @param wellKnownNamePrefix well-known name prefix of the attachment that client is
     *                            interested in
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "s", replySignature = "u")
    FindAdvertisedNameResult FindAdvertisedName(String wellKnownNamePrefix) throws BusException;

    /** {@link #CancelFindAdvertisedName(String)} return value.*/
    enum CancelFindAdvertisedNameResult {

        /** Invalid. */
        Invalid,

        /** Success. */
        Success,

        /** Failed. */
        Failed,
    }

    /**
     * Cancels interest in a well-known attachment name that was previously
     * included in a call to {@link #FindAdvertisedName(String)}.
     * 
     * @param wellKnownNamePrefix well-known name prefix of the attachment that
     *                            client is no longer interested in
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "s", replySignature = "u")
    CancelFindAdvertisedNameResult CancelFindAdvertisedName(String wellKnownNamePrefix) throws BusException;

    /**
     * Returns the list of currently advertised names of the local AllJoyn daemon.
     *
     * @return the advertised names
     * @throws BusException
     */
    @BusMethod(replySignature = "as")
    String[] GetAdvertisedNames() throws BusException;

    /**
     * Called by the bus when a daemon to daemon connection is unexpectedly lost.
     *
     * @param busAddress the bus address of the connection that was lost
     * @throws BusException
     */
    @BusSignal(signature = "s")
    void BusConnectionLost(String busAddress) throws BusException;

    /**
     * Signal broadcast when the local AllJoyn daemon receives an advertisement of
     * well-known names from a remote AllJoyn instance.
     *
     * @param name well-known name that was found
     * @param transport the bit-field indicating the kind of transport over which the
     *        advertisement was received.
     * @param namePrefix well-known name prefix used in call to {@link
     *                   #FindName(String)} that triggered this notification
     * @throws BusException
     */
    @BusSignal(signature = "sqs")
    void FoundAdvertisedName(String name, Short transport, String namePrefix) 
        throws BusException;

    /**
     * Called by the bus when an advertisement previously reported through
     * FoundAdvertisedName has become unavailable.
     *
     * @param name a well known name that the remote bus is advertising that is of interest to this
     *             attachment
     * @param transport the bit-field indicating the kind of transport over which the original
     *        advertisement was received.
     * @param namePrefix the well-known name prefix that was used in a call to FindName that
     *                   triggered this callback
     */
    @BusSignal(signature = "sqs")
    void LostAdvertisedName(String name, Short transport, String namePrefix)
        throws BusException;
}
