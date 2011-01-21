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
 * Indicates that a particular member of an AllJoyn exportable interface
 * is defined to be a AllJoyn property.  In addition to the annotation,
 * property methods must be of the form "{@code getProperty}"
 * and/or "{@code setProperty}".  In this case, "Property" is the
 * AllJoyn name of the property and it is both a read and write property.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BusProperty {

    /**
     * Override of property name.
     * The default property name is the method name without the "get"
     * or "set" prefix.  For example, if the method is getState() then
     * the default property name is "State".
     */
    String name() default "";

    /**
     * Signature of property.
     *
     * @see Signature
     */
    String signature() default "";
}