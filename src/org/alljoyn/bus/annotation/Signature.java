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
 * Declares the AllJoyn data type of a Java data type.
 *
 * Valid type declarations are:
 * <table border="1" width="100%" cellpadding="3" cellspacing="0">
 *   <tr bgcolor="#ccccff" class="tableheadingcolor"><th><b>Type Id</b></th>
 *      <th><b>AllJoyn type</b></th><th><b>Compatible Java types</b></th></tr>
 *   <tr><td>y</td><td>BYTE</td><td>byte, Enum<tt>*</tt></td></tr>
 *   <tr><td>b</td><td>BOOLEAN</td><td>boolean</td></tr>
 *   <tr><td>n</td><td>INT16</td><td>short, Enum<tt>*</tt></td></tr>
 *   <tr><td>q</td><td>UINT16</td><td>short, Enum<tt>*</tt></td></tr>
 *   <tr><td>i</td><td>INT32</td><td>int, Enum<tt>*</tt></td></tr>
 *   <tr><td>u</td><td>UINT32</td><td>int, Enum<tt>*</tt></td></tr>
 *   <tr><td>x</td><td>INT64</td><td>long, Enum<tt>*</tt></td></tr>
 *   <tr><td>t</td><td>UINT64</td><td>long, Enum<tt>*</tt></td></tr>
 *   <tr><td>d</td><td>DOUBLE</td><td>double</td></tr>
 *   <tr><td>s</td><td>STRING</td><td>String<tt>**</tt></td></tr>
 *   <tr><td>o</td><td>OBJECT_PATH</td><td>String<tt>**</tt></td></tr>
 *   <tr><td>g</td><td>SIGNATURE</td><td>String<tt>**</tt></td></tr>
 *   <tr><td>a</td><td>ARRAY</td><td>Array. The array type code must be followed by a <em>single
 *      complete type</em>.</td></tr>
 *   <tr><td>r</td><td>STRUCT</td><td>User-defined type<tt>***</tt> whose fields are annotated with
 *      {@link Position} and {@link Signature}</td></tr>
 *   <tr><td>v</td><td>VARIANT</td><td>{@link org.alljoyn.bus.Variant}</td></tr>
 *   <tr><td>a{TS}</td><td>DICTIONARY</td><td>Map<JT,JS> where T and S are AllJoyn type ids and JT and
 *      JS are compatible Java types</td></tr>
 * </table>
 * <tt>*</tt> = The ordinal numbers of the enumeration constant must correspond to the values of the
 *      AllJoyn type.<br>
 * <tt>**</tt> = Automatically converted to/from UTF-8(AllJoyn) to UTF-16 (Java).<br>
 * <tt>***</tt> = The user-defined type must supply a parameterless constructor.  Any nested classes
 *        of the user-defined type must be static nested classes.<br>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Signature {

    /**
     * AllJoyn type declaration.
     * The default AllJoyn type is the "natural" Java type.  For example:
     * <ul>
     * <li>Java short defaults to "n", not "q",
     * <li>Java int defaults to "i", not "u",
     * <li>and Java String defaults to "s".
     * </ul>
     */
    String value() default "";
}