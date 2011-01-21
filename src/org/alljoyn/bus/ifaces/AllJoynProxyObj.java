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

    /** {@link #Connect(String)} return value. */
    enum ConnectResult {

        /** Invalid. */
        Invalid,

        /** Success. */
        Success,

        /** Invalid connect specification. */
        InvalidSpec,

        /** 
         * Invalid.  This value exists only to fill this hole in the
         * over-the-air ordinal values. 
         */
        Invalid3,

        /** Connect failed. */
        Failed;
    }

    /**
     * Requests the local daemon to connect to a given remote AllJoyn address.
     *
     * @param busAddress remote bus address to connect to
     *                (e.g. {@code bluetooth:addr=00.11.22.33.44.55}, or
     *                {@code tcp:addr=1.2.3.4,port=1234})
     *
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "s", replySignature = "u")
    ConnectResult Connect(String busAddress) throws BusException;

    /** {@link #Disconnect(String)} return value. */
    enum DisconnectResult {

        /** Invalid. */
        Invalid,

        /** Success. */
        Success,

        /** No connection matching spec was found. */
        NoConn,

        /** Disconnect failed. */
        Failed;
    };

    /**
     * Requests the local daemon to disconnect from a given remote AllJoyn address
     * previously connected via a call to {@link #Connect(String)}.
     *
     * @param busAddress remote bus address to disconnect. Must match busAddress
     *                previously passed to {@link #Connect(String)}.
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "s", replySignature = "u")
    DisconnectResult Disconnect(String busAddress) throws BusException;

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

    /** {@link #FindName(String)} return value.*/
    enum FindNameResult {

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
     * advertisement it will send a {@link #FoundName(String, String, String,
     * String)} signal. This attachment can then choose to ignore the
     * advertisement or to connect to the remote bus by calling {@link
     * #Connect(String)}.
     *
     * @param wellKnownNamePrefix well-known name prefix of the attachment that client is
     *                            interested in
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "s", replySignature = "u")
    FindNameResult FindName(String wellKnownNamePrefix) throws BusException;

    /** {@link #CancelFindName(String)} return value.*/
    enum CancelFindNameResult {

        /** Invalid. */
        Invalid,

        /** Success. */
        Success,

        /** Failed. */
        Failed,
    }

    /**
     * Cancels interest in a well-known attachment name that was previously
     * included in a call to {@link #FindName(String)}.
     * 
     * @param wellKnownNamePrefix well-known name prefix of the attachment that
     *                            client is no longer interested in
     * @return a status code indicating success or failure
     * @throws BusException
     */
    @BusMethod(signature = "s", replySignature = "u")
    CancelFindNameResult CancelFindName(String wellKnownNamePrefix) throws BusException;

    /**
     * Returns the list of currently advertised names of the local AllJoyn daemon.
     *
     * @return the advertised names
     * @throws BusException
     */
    @BusMethod(replySignature = "as")
    String[] ListAdvertisedNames() throws BusException;

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
    void FoundName(String name, String guid, String namePrefix, String busAddress) 
        throws BusException;
}
