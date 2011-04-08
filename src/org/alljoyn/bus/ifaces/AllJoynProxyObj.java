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
