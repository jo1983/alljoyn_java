/*
 * Copyright 2009-2013, Qualcomm Innovation Center, Inc.
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

import org.alljoyn.bus.Variant;

import static junit.framework.Assert.*;
import junit.framework.TestCase;

public class VariantTest extends TestCase {
    public VariantTest(String name) {
        super(name);
    }

    static {
        System.loadLibrary("alljoyn_java");
    }

    public void testClassCastException() throws BusException {
        boolean thrown = false;
        try {
            Variant v = new Variant(1);
            String s = v.getObject(String.class);
        } catch (ClassCastException ex) {
            thrown = true;
        } finally {
            assertTrue(thrown);
        }
    }

    public void testGetSignature() throws Exception {
    	 Variant v = new Variant((byte)1);
         assertEquals("y", v.getSignature());
         v = new Variant(true);
         assertEquals("b", v.getSignature());
         v = new Variant((short)2);
         assertEquals("n", v.getSignature());
         v = new Variant(3);
         assertEquals("i", v.getSignature());
         v = new Variant((long)4);
         assertEquals("x", v.getSignature());
         v = new Variant(4.1);
         assertEquals("d", v.getSignature());
         v = new Variant("five");
         assertEquals("s", v.getSignature());
         v = new Variant(new byte[] { 6 });
         assertEquals("ay", v.getSignature());
         v = new Variant(new boolean[] { true });
         assertEquals("ab", v.getSignature());
         v = new Variant(new short[] { 7 });
         assertEquals("an", v.getSignature());
         v = new Variant(new int[] { 8 });
         assertEquals("ai", v.getSignature());
         v = new Variant(new long[] { 10 });
         assertEquals("ax", v.getSignature());
         v = new Variant(new double[] { 10.1 });
         assertEquals("ad", v.getSignature());
         v = new Variant(new String[] { "eleven" });
         assertEquals("as", v.getSignature());
         v = new Variant(new InferredTypesInterface.InnerStruct(12));
         assertEquals("(i)", v.getSignature());
         v = new Variant(new Variant(new String("thirteen")));
         assertEquals("v", v.getSignature());
         
         v = new Variant();
         assertNull(v.getSignature());
    }
}
