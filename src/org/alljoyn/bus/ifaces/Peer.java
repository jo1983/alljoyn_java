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

package org.alljoyn.bus.ifaces;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;

/**
 * The standard org.freedesktop.DBus.Peer interface.
 */
@BusInterface(name = "org.freedesktop.DBus.Peer")
public interface Peer {

    /**
     * Send a ping message to a remote connection and get a method reply in
     * response.
     *
     * @throws BusException
     */
    @BusMethod
    void Ping() throws BusException;

    /**
     * Get the DBus machine id.
     * The machine id corresponds to the AllJoyn daemon's guid in this
     * implementation.
     *
     * @return guid of local AllJoyn daemon
     * @throws BusException
     */
    @BusMethod
    String GetMachineId() throws BusException;
}
