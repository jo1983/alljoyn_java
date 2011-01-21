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

import org.alljoyn.bus.annotation.Position;
import org.alljoyn.bus.annotation.Signature;

/*
 * Contact is a data container that describes an entry in the address book for a single contact.
 */
public class Contact {

    /*
     * Contact Name
     * The Position annotation indicates the numeric position of a field within an 
     * AllJoyn struct (Java class). This value defines the order that fields are 
     * marshalled and unmarshalled.
     * 
     * Each element in the class must have a unique position index. The Position 
     * count starts with a zero index and is incremented by one for each element.
     */
    @Position(0)
    @Signature("r")
    public String name;

    /*
     * Array of phone numbers
     */
    @Position(1)
    @Signature("ar")
    public Phone[] phone;

    @Position(2)
    @Signature("ar")
    public Email[] email;

    /*
     * buy default assume at least one phone number and one email.
     */
    public Contact() {
        phone = new Phone[1];
        phone[0] = new Phone();
        email = new Email[1];
        email[0] = new Email();
    }

    /*
     * If the contact has more than one phone number then this must be specified
     * before trying to put the phone numbers into the contact container.
     * @param size - number of phone numbers.
     */
    public void setPhoneCount(int size) {
        this.phone = new Phone[size];
        for (int i = 0; i < this.phone.length; i++) {
            this.phone[i] = new Phone();
        }
    }

    /*
     * If the contact has more than one e-mail then this must be specified
     * before trying to put the e-mail addresses into the contact container.
     * @param size - number of e-mail addresses
     */
    public void setEmailCount(int size) {
        this.email = new Email[size];
        for (int i = 0; i < this.email.length; i++) {
            this.email[i] = new Email();
        }
    }
    
    /*
     * Simple container to hold information about a single phone number.
     * 
     * When specifying an AllJoyn struct (Java class) inside another class
     * it must be declared as a static class with out any methods.  
     */
    public static class Phone {
        @Position(0)
        public String number;
        @Position(1)
        public int type;
        @Position(2)
        public String label;
    }
    
    /*
     * simple container to hold information about a single email
     */
    public static class Email {
        @Position(0)
        public String address;
        @Position(1)
        public int type;
        @Position(2)
        public String label;
    }
}
