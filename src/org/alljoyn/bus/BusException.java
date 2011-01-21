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
 * Base class of AllJoyn exceptions.
 */
public class BusException extends java.lang.Exception {

    /** Constructs a default BusException. */
    public BusException() {
        super();
    }

    /**
     * Constructs a BusException with a user-defined message.
     *
     * @param msg user-defined message
     */
    public BusException(String msg) {
        super(msg);
    }

    /**
     * Constructs a chained BusException with a user-defined message.
     *
     * @param msg user-defined message
     * @param cause the cause of this exception
     */
    public BusException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /** Prints a line to the native log. */
    private static native void logln(String line);

    /** Logs a Throwable to the native log. */
    static void log(Throwable th) {
        String prefix = "";
        do {
            logln(prefix + th.getClass().getName() 
                  + (th.getMessage() == null ? "" : ": " + th.getMessage()));
            StackTraceElement[] stack = th.getStackTrace();
            for (int frame = 0; frame < stack.length; ++frame) {
                logln("    at " + stack[frame]);
            }
            th = th.getCause();
            prefix = "Caused by: ";
        } while (th != null);
    }
}
