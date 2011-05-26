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

/*
 * A version of simple interface with the same name, but different method count.
 */
@BusInterface(name="org.alljoyn.bus.SimpleInterface")
public interface SimpleInterfaceC {

    @BusMethod(signature="s", replySignature="s")
    public String Ping(String inStr) throws BusException;

    @BusMethod(signature="s", replySignature="s")
    public String Pong(String inStr) throws BusException;
}

