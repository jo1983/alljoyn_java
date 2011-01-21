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
 * Implemented by a user-defined {@link BusObject} that is interested in
 * customizing the D-Bus introspection XML presented to remote nodes.
 */
public interface IntrospectionListener {

    /**
     * Returns a description of the object in the D-Bus introspection XML
     * format.  Note that the DTD description and the root element are not
     * generated.
     *
     * @param deep include XML for all decendents rather than stopping at direct
     *             children
     * @param indent number of characters to indent the XML
     * @return the description of the object in D-Bus introspection XML format
     */
    String generateIntrospection(boolean deep, int indent);
}

