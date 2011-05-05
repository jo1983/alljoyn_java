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
 * A SessionPortListener is responsible for handling session port related callbacks
 * from the AllJoyn system. It is expected that an AllJoyn session creator will
 * specialize this class in order to handle callbacks required for accepting session
 * joiners.
 */
public class SessionPortListener {

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
     * The opaque pointer to the underlying C++ object which is actually tied
     * to the AllJoyn code.
     */
    private long handle = 0;
}
