/*
 * Copyright 2011, Qualcomm Innovation Center, Inc.
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
 * An OnJoinSessionListener is responsible for receiving completion indcations
 * from asynchronous join operations.  It is expected that an AllJoyn session
 * user will specialize this class in order to handle the callback.
 */
public class OnJoinSessionListener {

    /**
     * Create native resources held by objects of this class.
     */
    public OnJoinSessionListener() {
        create();
    }

    /**
     * Destroy native resources held by objects of this class.
     */
    protected void finalize() throws Throwable {
        try {
            destroy();
        } finally {
            super.finalize();
        }
    }

    /**
     * Create any native resources held by objects of this class.  Specifically,
     * we allocate a C++ counterpart of this listener object.
     */
    private native void create();

    /**
     * Release any native resources held by objects of this class.
     * Specifically, we may delete a C++ counterpart of this listener object.
     */
    private native void destroy();

    /**
     * Called when {@link #joinSession(String, short, SessionOpts, SessionListener,
     * OnJoinSessionListener)} completes.
     *
     * @param status <ul><li>OK if the session was joined.</li>
     *                   <li>BUS_NOT_CONNECTED if a connection has not been made with a local
     *                       bus</li>
     *                   <li>other error status codes indicating a failure.</li></ul>
     * @param sessionId Set to the unique identifier for session.
     * @param opts      Set to the actual session options of the joined session.
     * @param context   User-defined context object.  Users can provide anything they want.
     */
    public void onJoinSession(Status status, int sessionId, SessionOpts opts, Object context) {
    }

    /*
     * The opaque pointer to the underlying C++ object which is actually tied
     * to the AllJoyn code.
     */
    private long handle = 0;
}
