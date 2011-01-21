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

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;

/** 
 * SimpleInterface is an example of an interface that is published
 * onto alljoyn by org.alljoyn.bus.samples.simple.Service and is subscribed
 * to by org.alljoyn.bus.samples.simple.Client.
 */
@BusInterface
public interface SimpleInterface {

    /**
     * Echo a string.
     *
     * @param inStr   The string to be echoed by the service.
     * @return  The echoed string.
     */
    @BusMethod(signature="s", replySignature="s")
    public String Ping(String inStr) throws BusException;
}

