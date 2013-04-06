/**
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

import org.alljoyn.bus.PasswordManager;
import junit.framework.TestCase;

public class PasswordManagerTest extends TestCase {

    static {
        System.loadLibrary("alljoyn_java");
    }

    public PasswordManagerTest(String name) {
        super(name);
    }

    public void testSetCredentials() throws Exception {
	PasswordManager.setCredentials("ABC","1234");
    }
}

