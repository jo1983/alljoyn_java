/*
 * Copyright 2013, Qualcomm Innovation Center, Inc.
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
import org.alljoyn.bus.annotation.BusProperty;
import org.alljoyn.bus.annotation.Secure;

/**
 * PropsInterfaceSecure is an example of a secure interface that is published
 * onto alljoyn by org.alljoyn.bus.samples.props.ServiceSecure and is subscribed
 * to by org.alljoyn.bus.samples.props.Client.  The interface
 * contains two read/write properties: 'StringProp' and 'IntProp'.
 */
@BusInterface
@Secure
public interface PropsInterfaceSecure {
    /**
     * Get the property named 'StringProp'.
     *
     * @return The property value.
     */
    @BusProperty
    public String getStringProp() throws BusException;

    /**
     * Set the property named 'StringProp' to the value.
     *
     * @param value The new value of 'StringProp'.
     */
    @BusProperty
    public void setStringProp(String value) throws BusException;

    /**
     * Get the property named 'IntProp'.
     *
     * @return The property value.
     */
    @BusProperty
    public int getIntProp() throws BusException;

    /**
     * Set the property named 'IntProp' to the value.
     *
     * @param value The new value of 'IntProp'.
     */
    @BusProperty
    public void setIntProp(int value) throws BusException;
}

