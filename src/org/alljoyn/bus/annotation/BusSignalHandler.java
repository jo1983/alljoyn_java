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
 * Indicates that a method should receive AllJoyn signals.
 * <p>
 * {@code iface} and {@code signal} may be either the DBus interface
 * and signal name, or the Java interface and method name.  For the
 * following interface definition:
 * <p><blockquote><pre>
 *     package org.myapp;
 *     &#64;BusInterface(name = "org.sample.MyInterface")
 *     public interface IMyInterface {
 *         &#64;BusSignal(name = "MySignal")
 *         public void EmitMySignal(String str) throws BusException;
 *     }
 * </pre></blockquote><p>
 * either of the following may be used to annotate a signal handler:
 * <p><blockquote><pre>
 *     &#64;BusSignalHandler(iface = "org.sample.MyInterface", signal = "MySignal")
 *     public void handleSignal(String str) {}
 *
 *     &#64;BusSignalHandler(iface = "org.myapp.IMyInterface", signal = "EmitMySignal")
 *     public void handleSignal(String str) {}
 * </pre></blockquote><p>
 * The first example may be used succesfully when {@code IMyInterface}
 * is known to the BusAttachment via a previous call to {@link
 * org.alljoyn.bus.BusAttachment#registerBusObject(BusObject, String)}
 * or {@link org.alljoyn.bus.BusAttachment#getProxyBusObject(String,
 * String, int, Class[])}.
 * <p>
 * The second example may be used succesfully when {@code
 * IMyInterface} is unknown to the BusAttachment.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BusSignalHandler {

    /**
     * The inferface name of the signal.
     */
    String iface();

    /**
     * The signal name of the signal.
     */
    String signal();

    /**
     * The object path of the emitter of the signal or unspecified for all
     * paths.
     */
    String source() default "";
}
