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
 * Contains information about a specific method call or signal
 * message.
 */
public final class MessageContext {

    /**
     * {@code true} if the message is unreliable.  Unreliable messages have a non-zero
     * time-to-live and may be silently discarded.
     */
    public boolean isUnreliable;

    /**
     * The object path for this message.  An empty string if unable to find the
     * AllJoyn object path.
     */
    public String objectPath;

    /**
     * The interface for this message.  An empty string if unable to find the
     * AllJoyn interface.
     */
    public String interfaceName;

    /**
     * The member (method/signal) name for this message.  An empty string if
     * unable to find the member name.
     */
    public String memberName;

    /**
     * The destination for this message.  An empty string if unable to find the
     * message destination.
     */
    public String destination;

    /**
     * The sender for this message.  An empty string if the message did not
     * specify a sender.
     */
    public String sender;

    /**
     * The signature for this message.  An empty string if unable to find the
     * AllJoyn signature.
     */
    public String signature;

    /**
     * The authentication mechanism in use for this message.
     */
    public String authMechanism;

    private MessageContext(boolean isUnreliable, String objectPath, String interfaceName,
                           String memberName, String destination, String sender, String signature,
                           String authMechanism) {
        this.isUnreliable = isUnreliable;
        this.objectPath = objectPath;
        this.interfaceName = interfaceName;
        this.memberName = memberName;
        this.destination = destination;
        this.sender = sender;
        this.signature = signature;
        this.authMechanism = authMechanism;
    }
}
