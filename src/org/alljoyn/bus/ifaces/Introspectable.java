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
 * The standard org.freedesktop.DBus.Introspectable interface.  This
 * interface allows bus objects to be introspected for the interfaces
 * that they implement and the child objects that they may contain.
 */
@BusInterface(name = "org.freedesktop.DBus.Introspectable")
public interface Introspectable {

    /**
     * Gets the XML introspection data for the object.
     * The schema for the DBus introspection data is described in the
     * DBus specification.
     *
     * @return XML introspection data for the object
     * @throws BusException
     */
    @BusMethod
    String Introspect() throws BusException;
}
