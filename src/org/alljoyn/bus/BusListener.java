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

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.SessionOpts;

/**
 * Bus listeners are responsible for handling callbacks from the AllJoyn system.
 * It is expected that a user of the AllJoyn bus will specialize this class in
 * order to handle required events from the bus.
 */
public class BusListener {

    /**
     * Called by the bus when an external bus is discovered that is advertising
     * a well-known name that this attachment has registered interest in via a
     * call to findAdvertisedName
     *
     * @param name         A well known name that the remote bus is advertising.
     * @param transport    Transport that received the advertisment.
     * @param namePrefix   The well-known name prefix used in call to
     *                     FindAdvertisedName that triggered this callback.
     */
    public void foundAdvertisedName(String name, short transport, String namePrefix) {}

    /**
     * Called by the bus when an advertisement previously reported through
     * foundAdvertisedName has become unavailable.
     *
     * @param name         A well known name that the remote bus is advertising
     *                     that is of interest to this attachment.
     * @param transport    Transport that stopped receiving the given advertised name.
     * @param namePrefix   The well-known name prefix that was used in a call to
     *                     FindAdvertisedName that triggered this callback.
     */
    public void lostAdvertisedName(String name, short transport, String namePrefix) {}

    /**
     * Called by the bus when the ownership of any well-known name changes.
     *
     * @param busName        The well-known name that has changed.
     * @param previousOwner  The unique name that previously owned the name or
     *                       NULL if there was no previous owner.
     * @param newOwner       The unique name that now owns the name or NULL if 
     *                       there is no new owner.
     */
    public void nameOwnerChanged(String busName, String previousOwner, String newOwner) {}

    /**
     * Called by the bus when a session becomes disconnected.
     *
     * @param sessionId     Id of session that was lost.
     */
    public void sessionLost(int sessionId) {}

    /**
     * Accept or reject an incoming JoinSession request. The session does not
     * exist until this after this function returns.
     *
     * @param sessionPort    Session port that was joined.
     * @param joiner         Unique name of potential joiner.
     * @param opts           Session options requested by the joiner.
     *
     * @return Return true if JoinSession request is accepted. false if rejected.
     */
    public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {return false;}

    /**
     * Called by the bus when a session has been successfully joined. The
     * session is fully up when this method is called.
     *
     * @param sessionPort    Session port that was joined.
     * @param id             Id of session.
     * @param joiner         Unique name of the joiner.
     */
    public void sessionJoined(short sessionPort, int id, String joiner) {}

    /**
     * Called when a bus that this listener is registered with is stopping.
     */
    public void busStopping() {}
}
