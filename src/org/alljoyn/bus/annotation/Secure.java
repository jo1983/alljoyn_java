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

package org.alljoyn.bus.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a given interface is secured by an authentication mechanism.
 *
 * Valid value declarations are:
 * <ol>
 *   <li>required(default value) - interface methods on the interface can only be
 *   called by an authenticated peer</li>
 *   <li>off - if security is off interface authentication is never required
 *   even when implemented by a secure object.</li>
 *   <li>inherit - if the Secure annotation is omitted the interface will use a
 *   security of inherit.  If security is inherit or security is not specified for an
 *   interface the interface inherits the security of the objects that implements
 *   the interface.</li>
 *   <li>if an unknown value is given inherit will be used</li>
 * </ol>
 * @see org.alljoyn.bus.BusAttachment#registerAuthListener(String, AuthListener)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Secure {
    String value() default "required";
}
