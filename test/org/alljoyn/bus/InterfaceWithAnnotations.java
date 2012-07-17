/******************************************************************************
 * Copyright 2012, Qualcomm Innovation Center, Inc.
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
 ******************************************************************************/

package org.alljoyn.bus;

import org.alljoyn.bus.annotation.BusAnnotation;
import org.alljoyn.bus.annotation.BusAnnotations;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusSignal;

@BusInterface(name="org.alljoyn.bus.InterfaceWithAnnotations")
@BusAnnotations({@BusAnnotation(name="org.freedesktop.DBus.Deprecated", value="true")})
public interface InterfaceWithAnnotations {

    @BusMethod(signature="s", replySignature="s")
    @BusAnnotations({@BusAnnotation(name="name", value="value"), @BusAnnotation(name="name2", value="value2")})
    public String Ping(String inStr) throws BusException;

    @BusMethod(signature="s", replySignature="s")
    public String Pong(String inStr) throws BusException;


    @BusMethod(signature="s")
    @BusAnnotations({@BusAnnotation(name="org.freedesktop.DBus.Deprecated", value="true"),
        @BusAnnotation(name="org.freedesktop.DBus.Method.NoReply", value="true")})
    public void Pong2(String inStr) throws BusException;


    @BusSignal()
    @BusAnnotations({@BusAnnotation(name="org.freedesktop.DBus.Deprecated", value="true")})
    public void Signal() throws BusException;
}
