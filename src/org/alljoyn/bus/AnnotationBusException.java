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
 * AllJoyn annotation related exceptions.  
 * This exception will occur if
 * <ul>
 * <li>a field of a user-defined type is not annotated with its position,
 * <li>a Java data type that is not supported is used,
 * <li>an Enum data type is not annotated with a valid AllJoyn type
 * </ul>
 */
public class AnnotationBusException extends BusException {

    /** Constructs a default AnnotationBusException. */
    public AnnotationBusException() {
        super();
    }

    /**
     * Constructs a AnnotationBusException with a user-defined message.
     *
     * @param msg user-defined message
     */
    public AnnotationBusException(String msg) {
        super(msg);
    }
}
