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

/**
 * Implemented by a user-defined class that is responsible for loading and
 * storing keys.  Encryption and decryption of key data is handled internally
 * using the password supplied by {@code getPassword}.
 */
public interface KeyStoreListener {

    /**
     * Reads encrytped key data.
     *
     * @return the key data
     */
    byte[] getKeys() throws BusException;

    /**
     * Reads the password required to decrypt/encrypt the key store.
     *
     * @return the password
     */
    char[] getPassword() throws BusException;

    /**
     * Writes the encrypted keys.
     *
     * @param keys the key data
     */
    void putKeys(byte[] keys) throws BusException;
}
