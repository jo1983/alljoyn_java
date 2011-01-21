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
import org.alljoyn.bus.annotation.Position;

import java.util.Arrays;
import java.util.Map;

@BusInterface
public interface MultipleReturnValuesInterface {

    public class Values {
        public static class Inner {
            @Position(0) public int x;
            public boolean equals(Object obj) {
                return x == ((Inner)obj).x;
            }
        }
        @Position(0) public int a;
        @Position(1) public int b;
        @Position(2) public Map<String, String> c;
        @Position(3) public Map<String, String>[] d;
        @Position(4) public long[] e;
        @Position(5) public Inner f;
        public boolean equals(Object obj) {
            Values v = (Values)obj;
            return a == v.a &&
                b == v.b &&
                c.equals(v.c) &&
                Arrays.equals(d, v.d) &&
                Arrays.equals(e, v.e) &&
                f.equals(v.f);
        }
    }

    @BusMethod(replySignature="iia{ss}aa{ss}ax(i)")
    public Values Method() throws BusException;
}
