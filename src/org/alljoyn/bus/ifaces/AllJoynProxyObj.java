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

/**
 * A predefined bus interface that is implemented by the local AllJoyn daemon.  It
 * is used to control advanced bus operations such as P2P-aware operations and
 * authentication.
 */
@BusInterface(name = "org.alljoyn.Bus")
public interface AllJoynProxyObj {

    /** The options structure used by sessions (cf. socket options). */
    public class SessionOpts {
        Byte traffic;
        Byte proximity;
        Short transports;
    }

    /** {@link #BindSessionPort(Integer,Boolean,SessionOpts)} return value. */
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
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "qb(yyq)", replySignature = "uq")
    BindSessionPortResult BindSessionPort(Short sessionPort, Boolean isMultipoint, SessionOpts sessionOpts, Short boundPort) throws BusException;

    /** {@link #JoinSession(String,Short,SessionOpts,Integer,SessionOpts)} return value. */
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

    /**
     * Requests the local daemon to join to a session hosted on a given bus
     * address, over a given contact session port.
     *
     * @param sessionHost  The bus name of an endpoint that is hosting the 
     *     session.
     * @param sessionPort  The contact session port of the hosted session.
     * @param inOpts       The session options describing the desired
     *                     characteristics of the new session.
     * @param sessionId    The session identifier of the resulting session.
     * @param outOpts      The session options describing the actual
     *                     characteristics of the new session.
     *
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "sq(yyq)", replySignature = "uu(yyq)")
    JoinSessionResult JoinSession(String sessionHost, Short sessionPort, SessionOpts inOpts, Integer sessionId, SessionOpts outOpts) throws BusException;

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

    /** {@link #AdvertiseName(String)} return value.*/
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
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "s", replySignature = "u")
    AdvertiseNameResult AdvertiseName(String wellKnownName) throws BusException;

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
     * advertisement it will send a {@link #FoundAdvertisedName(String, String,
     * String, String)} signal. This attachment can then choose to ignore the
     * advertisement or to connect to the remote bus by calling {@link
     * #Connect(String)}.
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
     * @param guid the GUID of the remote bus that was found to be advertising name
     * @param namePrefix well-known name prefix used in call to {@link
     *                   #FindName(String)} that triggered this notification
     * @param busAddress the bus address of the remote bus
     * @throws BusException
     */
    @BusSignal(signature = "ssss")
    void FoundAdvertisedName(String name, String guid, String namePrefix, String busAddress) 
        throws BusException;

    /**
     * Called by the bus when an advertisement previously reported through FoundName has become
     * unavailable.
     *
     * @param name a well known name that the remote bus is advertising that is of interest to this
     *             attachment
     * @param guid the GUID of the remote bus daemon
     * @param namePrefix the well-known name prefix that was used in a call to FindName that
     *                   triggered this callback
     * @param busAddress the connection address of the remote bus (used for informational purposes
     *                   only)
     */
    @BusSignal(signature = "ssss")
    void LostAdvertisedName(String name, String guid, String namePrefix, String busAddress)
        throws BusException;
}
