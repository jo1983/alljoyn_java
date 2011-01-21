/*
 * This is an Android sample on how to use the AllJoyn properties.
 * This is the Bus Interface description.  It contains two set properties.
 * and two get properties.
 *
 * Copyright 2010-2011, Qualcomm Innovation Center, Inc.
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
package org.alljoyn.bus.samples.properties_service;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusProperty;

/*
 * BusInterface with the well-known name org.alljoyn.bus.samples.properties.
 */
@BusInterface (name = "org.alljoyn.bus.samples.properties")
public interface PropertiesInterface {
    /*
     * The BusProperty annotation signifies that this function should be used as part of the AllJoyn
     * interface. A BusProperty is always a method that starts with get or set.  The set method
     * always takes a single value.  while the get method always returns a single value. The single 
     * value can be a complex data type such as an array or an Object.
     *
     * All methods that use the BusProperty annotation can throw a BusException and should indicate
     * this fact.
     */
    @BusProperty
    public String getBackGroundColor()throws BusException;

    @BusProperty
    public void setBackGroundColor(String color) throws BusException;

    @BusProperty
    public int getTextSize() throws BusException;

    @BusProperty
    public void setTextSize(int size) throws BusException;
}