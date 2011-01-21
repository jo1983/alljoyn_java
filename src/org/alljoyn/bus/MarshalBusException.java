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
 * AllJoyn marshalling related exceptions.  Marshalling exceptions occur when a
 * Java type cannot be marshalled into an AllJoyn type and vice versa.  
 * <p>
 * When a Java type cannot be marshalled into an AllJoyn type, the message reported
 * will be similar to {@code cannot marshal class java.lang.Byte into 'a{ss}'} .
 * When an AllJoyn type cannot be marshalled into a Java type, the message reported
 * will be similar to {@code cannot marshal '(i)' into byte}.
 */
public class MarshalBusException extends BusException {

    /** Constructs a default MarshalBusException. */
    public MarshalBusException() {
        super();
    }

    /**
     * Constructs a MarshalBusException with a user-defined message.
     *
     * @param msg user-defined message
     */
    public MarshalBusException(String msg) {
        super(msg);
    }

    /**
     * Constructs a chained MarshalBusException with a user-defined message.
     *
     * @param msg user-defined message
     * @param cause the cause of this exception
     */
    public MarshalBusException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
