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

import org.alljoyn.bus.annotation.Position;
import org.alljoyn.bus.annotation.Signature;

import java.util.TreeMap;

/** 
 * Contact is a data container that describes an entry in the address book.
 */
public class Contact {

    /** First Name */
    @Position(0)
    public String firstName;

    /** Last Name */
    @Position(1)
    public String lastName;

    /** 
     * Map of phone numbers.
     * Key is type of phone number such as "Home", "Main", etc.
     * Value is the actual phone number as a String.
     */
    @Position(2)
    @Signature("a{ss}")
    public TreeMap<String, String> phoneNumberMap;

    public Contact() {
        phoneNumberMap = new TreeMap<String, String>();
    }

}

