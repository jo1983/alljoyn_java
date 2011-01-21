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
import org.alljoyn.bus.annotation.Secure;

import java.util.Map;

/** 
 * AddressBookInterface is an example of an AllJoyn interface that uses complex 
 * data types (Struct, Dictionary, etc.) to publish a simple addressbook.
 */
@BusInterface
@Secure
public interface AddressBookInterface {

    /**
     * Add or replace a contact in the address book.
     * Only one contact per last name is allowed.
     *
     * @param contact  The contact to add/replace.
     */
    @BusMethod(signature="r")
    public void setContact(Contact contact) throws BusException;

    /**
     * Find a contact in the address book based on last name.
     *
     * @param lastName   Last name of contact.
     * @return Matching contact or null if no such contact.
     */
    @BusMethod(signature="s", replySignature="r")
    public Contact getContact(String lastName) throws BusException;

    /**
     * Return all contacts whose last names match.
     *
     * @param lastNames   Array of last names to find in address book.
     * @return An array of contacts with last name as keys.
     */
    @BusMethod(signature="as", replySignature="ar")
    public Contact[] getContacts(String[] lastNames) throws BusException;
}

