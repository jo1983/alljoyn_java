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

/**
 * Called by AllJoyn to inform users of found names.
 * @see BusAttachment#findAdvertisedName(String)
 */
public interface FindAdvertisedNameListener {

    /**
     * Called by the bus when an external bus is discovered that is advertising
     * a well-known name that this attachment has registered interest in via
     * {@link BusAttachment#findAdvertisedName(String)}.
     *
     * @param name a well known name that the remote bus is advertising that is
     *             of interest to this attachment
     * @param transport the bit-field indicating the kind of transport over which the
     *        advertisement was received.
     * @param namePrefix the well-known name prefix that was used in a call to
     *                   {@code findAdvertisedName} that triggered this callback
     */
    void foundAdvertisedName(String name, Short transport, String namePrefix);

    /**
     * Called by the bus when a previously discovered name advertisement (reported
     * through foundName) is determined to have become unavailable.
     * {@link BusAttachment#findAdvertisedName(String)}.
     *
     * @param name a well-known name that the remote bus was advertising that is
     *             of interest to this attachment.
     * @param transport the bit-field indicating the kind of transport over which the
     *        original advertisement was received.
     * @param namePrefix the well-known name prefix that was used in a call to
     *                   {@code findAdvertisedName} that triggered this callback.
     */
    void lostAdvertisedName(String name, Short transport, String namePrefix);
}
