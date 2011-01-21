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

import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusProperty;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.bus.annotation.Secure;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InterfaceDescription represents a message bus interface.
 * This class is used internally by registered bus objects.
 */
class InterfaceDescription {

    private static final int INVALID     = 0; /**< An invalid member type. */
    private static final int METHOD_CALL = 1; /**< A method call member. */
    private static final int SIGNAL      = 4; /**< A signal member. */

    private static final int READ        = 1;              /**< Read access type. */
    private static final int WRITE       = 2;              /**< Write access type. */
    private static final int RW          = READ | WRITE;   /**< Read-write access type. */

    private class Property {

        public String name;

        public String signature;

        public Method get;

        public Method set;

        public Property(String name, String signature) {
            this.name = name;
            this.signature = signature;
        }
    }

    /** The native interface description handle. */
    private long handle;

    /** The members of this interface. */
    private List<Method> members;

    /** The properties of this interface. */
    private Map<String, Property> properties;

    public InterfaceDescription() {
        members = new ArrayList<Method>();
        properties = new HashMap<String, Property>();
    }

    /** Allocate native resources. */
    private native Status create(BusAttachment busAttachment, String name, boolean secure);

    /** Add a member to the native interface description. */
    private native Status addMember(int type, String name, String inputSig, String outSig,
                                    int annotation);

    /** Add a property to the native interface description. */
    private native Status addProperty(String name, String signature, int access);

    /** Activate the interface on the bus. */
    private native void activate();

    /**
     * Called by the native code when registering bus objects to obtain the member
     * implementations.
     */
    private Method getMember(String name) {
        for (Method m : members) {
            if (InterfaceDescription.getName(m).equals(name)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Called by the native code when registering bus objects to obtain the property
     * implementations.
     */
    private Method[] getProperty(String name) {
        for (Property p : properties.values()) {
            if (p.name.equals(name)) {
                return new Method[] { p.get, p.set };
            }
        }
        return null;
    }

    /**
     * Create the native interface description for the busInterface.
     *
     * @param busAttachment the connection the interface is on
     * @param busInterface the interface
     */
    public Status create(BusAttachment busAttachment, Class<?> busInterface) 
            throws AnnotationBusException {
        boolean secure = busInterface.getAnnotation(Secure.class) != null;
        Status status = create(busAttachment, getName(busInterface), secure);
        if (status != Status.OK) {
            return status;
        }
        status = addProperties(busInterface);
        if (status != Status.OK) {
            return status;
        }
        status = addMembers(busInterface);
        if (status != Status.OK) {
            return status;
        }
        activate();
        return Status.OK;
    }

    private Status addProperties(Class<?> busInterface) throws AnnotationBusException {
        for (Method method : busInterface.getMethods()) {
            if (method.getAnnotation(BusProperty.class) != null) {
                String name = getName(method);
                Property property = properties.get(name);
                if (property == null) {
                    property = new Property(name, getPropertySig(method));
                } else if (!property.signature.equals(getPropertySig(method))) {
                    return Status.BAD_ANNOTATION;
                }
                if (method.getName().startsWith("get")) {
                    property.get = method;
                } else if (method.getName().startsWith("set")
                           && (method.getGenericReturnType().equals(void.class))) {
                    property.set = method;
                } else {
                    return Status.BAD_ANNOTATION;
                }
                properties.put(name, property);
            }
        }
        for (Property property : properties.values()) {
            int access = ((property.get != null) ? READ : 0) | ((property.set != null) ? WRITE : 0);
            Status status = addProperty(property.name, property.signature, access);
            if (status != Status.OK) {
                return status;
            }
        }
        return Status.OK;
    }

    private Status addMembers(Class<?> busInterface) throws AnnotationBusException {
        for (Method method : busInterface.getMethods()) {
            int type = INVALID;
            int annotation = 0;
            BusMethod m = method.getAnnotation(BusMethod.class);
            BusSignal s = method.getAnnotation(BusSignal.class);
            if (m != null) {
                type = METHOD_CALL;
                annotation = m.annotation();
            } else if (s != null) {
                type = SIGNAL;
                annotation = s.annotation();
            }
            if (type != INVALID) {
                Status status = addMember(type, getName(method), getInputSig(method),
                                          getOutSig(method), annotation);
                if (status != Status.OK) {
                    return status;
                }
                members.add(method);
            }
        }
        return Status.OK;
    }

    /**
     * Create the native interface descriptions needed by
     * busInterfaces.  The Java interface descriptions are returned
     * in the descs list.
     * @param busAttachment The connection the interfaces are on.
     * @param busInterfaces The interfaces.
     * @param descs The returned interface descriptions.
     */
    public static Status create(BusAttachment busAttachment, Class<?>[] busInterfaces,
                                List<InterfaceDescription> descs) throws AnnotationBusException {
        for (Class<?> intf : busInterfaces) {
            if ("org.freedesktop.DBus.Properties".equals(getName(intf))) {
                /* The Properties interface is handled automatically by the underlying library. */
                continue;
            }
            if (intf.getAnnotation(BusInterface.class) != null) {
                InterfaceDescription desc = new InterfaceDescription();
                Status status = desc.create(busAttachment, intf);
                if (status != Status.OK) {
                    return status;
                }
                descs.add(desc);
            }
        }
        return Status.OK;
    }

    /**
     * Get the DBus interface name.
     *
     * @param intf The interface.
     */
    public static String getName(Class<?> intf) {
        BusInterface busIntf = intf.getAnnotation(BusInterface.class);
        if (busIntf != null && busIntf.name().length() > 0) {
            return busIntf.name();
        } else {
            return intf.getName();
        }
    }

    /**
     * Get the DBus member or property name.
     *
     * @param method The method.
     */
    public static String getName(Method method) {
        BusMethod busMethod = method.getAnnotation(BusMethod.class);
        if (busMethod != null && busMethod.name().length() > 0) {
            return busMethod.name();
        }
        BusSignal busSignal = method.getAnnotation(BusSignal.class);
        if (busSignal != null && busSignal.name().length() > 0) {
            return busSignal.name();
        }
        BusProperty busProperty = method.getAnnotation(BusProperty.class);
        if (busProperty != null) {
            if (busProperty.name().length() > 0) {
                return busProperty.name();
            } else {
                /* The rest of the method name following the "get" or "set" prefix. */
                return method.getName().substring(3);
            }
        }
        return method.getName();
    }

    /**
     * Get the DBus member input signature.
     *
     * @param method The method.
     */
    public static String getInputSig(Method method) throws AnnotationBusException {
        BusMethod busMethod = method.getAnnotation(BusMethod.class);
        if (busMethod != null && busMethod.signature().length() > 0) {
            return Signature.typeSig(method.getGenericParameterTypes(), busMethod.signature());
        }
        BusSignal busSignal = method.getAnnotation(BusSignal.class);
        if (busSignal != null && busSignal.signature().length() > 0) {
            return Signature.typeSig(method.getGenericParameterTypes(), busSignal.signature());
        }
        return Signature.typeSig(method.getGenericParameterTypes(), null);
    }

    /**
     * Get the DBus member output signature.
     *
     * @param method the method
     */
    public static String getOutSig(Method method) throws AnnotationBusException {
        BusMethod busMethod = method.getAnnotation(BusMethod.class);
        if (busMethod != null && busMethod.replySignature().length() > 0) {
            return Signature.typeSig(method.getGenericReturnType(), busMethod.replySignature());
        }
        BusSignal busSignal = method.getAnnotation(BusSignal.class);
        if (busSignal != null && busSignal.replySignature().length() > 0) {
            return Signature.typeSig(method.getGenericReturnType(), busSignal.replySignature());
        }
        return Signature.typeSig(method.getGenericReturnType(), null);
    }

    /**
     * Get the DBus property signature.
     *
     * @param method the method
     */
    public static String getPropertySig(Method method) throws AnnotationBusException {
        Type type = null;
        if (method.getName().startsWith("get")) {
            type = method.getGenericReturnType();
        } else if (method.getName().startsWith("set")) {
            type = method.getGenericParameterTypes()[0];
        }
        BusProperty busProperty = method.getAnnotation(BusProperty.class);
        if (busProperty != null && busProperty.signature().length() > 0) {
            return Signature.typeSig(type, busProperty.signature());
        }
        return Signature.typeSig(type, null);
    }
}
