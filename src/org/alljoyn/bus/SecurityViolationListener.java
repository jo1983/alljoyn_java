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
 * Allows an application to monitor security violations.
 */
public interface SecurityViolationListener {

    /**
     * Called when an attempt to decrypt an encrypted messages failed or when an
     * unencrypted message was received on an interface that requires
     * encryption.
     *
     * @param status a status code indicating the type of security violation
     * @see BusAttachment#getMessageContext()
     */
    void violated(Status status);
}
