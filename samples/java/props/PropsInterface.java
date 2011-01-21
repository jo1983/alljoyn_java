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

package org.alljoyn.bus.samples.props;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusProperty;

/**
 * PropsInterface is an example of an interface that is published onto
 * alljoyn by org.alljoyn.bus.samples.props.Service and is subscribed
 * to by org.alljoyn.bus.samples.props.Client.  The interface
 * contains two read/write properties: 'StringProp' and 'IntProp'.
 */
@BusInterface
public interface PropsInterface {

    /**
     * Get the property named 'StringProp'.
     *
     * @return The property value.
     */
    @BusProperty
    public String getStringProp();

    /**
     * Set the property named 'StringProp' to the value.
     *
     * @param value The new value of 'StringProp'.
     */
    @BusProperty
    public void setStringProp(String value);

    /**
     * Get the property named 'IntProp'.
     *
     * @return The property value.
     */
    @BusProperty
    public int getIntProp();

    /**
     * Set the property named 'IntProp' to the value.
     *
     * @param value The new value of 'IntProp'.
     */
    @BusProperty
    public void setIntProp(int value);
}

