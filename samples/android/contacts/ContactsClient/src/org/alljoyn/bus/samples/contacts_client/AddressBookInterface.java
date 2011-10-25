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

package org.alljoyn.bus.samples.contacts_client;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.AccessPermission;

/*
 * AddressBookInterface is an example of an AllJoyn interface that uses complex
 * data types to publish simple contact information.
 */
@BusInterface (name = "org.alljoyn.bus.addressbook")
public interface AddressBookInterface {
    /*
     * The BusMethod annotation signifies that this function should be used as part of the AllJoyn
     * interface.  
     * for this BusMethod we have manually specified the signature and replySignature.  In most 
     * circumstances runtime can figure out what the signature should be.
     * 
     * In this instance we inform the bus that the input is a String and integer. while the output
     * is a struct.  All AllJoyn structs must specify there marshaling order using the the @position 
     * annotation see Contact.java  
     *
     * replySignature could also be "(sa(sis)a(sis))" however it is best to let runtime figure out 
     * as much as it can with out manually specifying the signature.
     * 
     * All methods that use the BusMethod annotation can throw a BusException and should indicate
     * this fact.
     *
     * The AccessPermission annotation signifies that this function uses system resources that
     * require system permission "android.permission.READ_CONTACTS". Thus the application should
     * declare the required permissions in its manifest file. If there are more than one permission
     * item, they should be separated by ';'.
     */
    @BusMethod(signature = "si", replySignature = "r") 
    @AccessPermission("android.permission.READ_CONTACTS")
    public Contact getContact(String name, int userId) throws BusException;

    /*
     * the replySignature indicates that this BusMethod will return an array of structs.
     */
    @BusMethod(replySignature = "ar")
    @AccessPermission("android.permission.READ_CONTACTS")
    public NameId[] getAllContactNames() throws BusException;
}

