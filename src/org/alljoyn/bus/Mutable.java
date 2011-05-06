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
 * The underlying C++ implementation of the AllJoyn bus is fond of using in/out
 * parameters.  Rather than fight this, changing the calling functions to
 * something almost unrecognizable to a person who speaks the C++ version, we
 * provide classes to allow this.  This does mean that we need to allocate
 * parameters on the heap, but we expect infrequent use of the bus methods
 * that use these.
 */
public class Mutable {

    /**
     * A class providing [in,out] parameter semantics for Java Strings.
     */
    static public class StringValue {
        /**
         * Construct a StringValue.
         */
        public StringValue() {
            value = "";
        }

        /**
         * Construct a StringValue with the given value.
         */
        public StringValue(String string) {
            value = string;
        }

        /**
         * The string in question.
         */
        public String value;
    }

    /**
     * A class providing inout parameter semantics for Java ints.
     */
    static public class IntegerValue {
        /**
         * Construct an IntegerValue.
         */
        public IntegerValue() {
            value = 0;
        }

        /**
         * Construct an IntegerValue with the given value.
         */
        public IntegerValue(int v) {
            value = v;
        }

        /**
         * The int in question.
         */
        public int value;
    }

    /**
     * A class providing inout parameter semantics for Java shorts.
     */
    static public class ShortValue {
        /**
         * Construct a ShortValue.
         */
        public ShortValue() {
            value = 0;
        }

        /**
         * Construct an ShortValue with the given value.
         */
        public ShortValue(short v) {
            value = v;
        }

        /**
         * The short in question.
         */
        public short value;
    }
}
