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
 * AllJoyn ERROR message exceptions.  AllJoyn ERROR messages are sent as
 * replies to method calls to signal that an exceptional failure has
 * occurred.
 */
public class ErrorReplyBusException extends BusException {

    private final String name;

    private final String message;

    /**
     * Constructs an ErrorReplyBusException with an error name and message from
     * an error reply message.
     *
     * @param name the error name header field
     * @param message the (optional) error message
     */
    public ErrorReplyBusException(String name, String message) {
        super(name);
        this.name = name;
        this.message = message;
    }

    /**
     * Gets the error name.
     *
     * @return the name
     */
    public String getErrorName() { return name; }

    /**
     * Gets the (optional) error message.
     *
     * @return the message
     */
    public String getErrorMessage() { return message; }
}
