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
 * A SessionListener is responsible for handling session related callbacks from
 * the AllJoyn system. It is expected that a user of the AllJoyn bus will specialize
 * this class in order to respond to AllJoyn session related events.
 */
public class SessionListener {

    /**
     * Called by the bus when a session becomes disconnected.
     *
     * @param sessionId     Id of session that was lost.
     */
    public void sessionLost(int sessionId) {}

    /**
     * The opaque pointer to the underlying C++ object which is actually tied
     * to the AllJoyn code.
     */
    private long handle = 0;
}
