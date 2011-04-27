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

import org.alljoyn.bus.annotation.BusProperty;
import org.alljoyn.bus.annotation.BusSignalHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * A bus object that exists and is managed by some other connection to
 * the bus. Use ProxyBusObject to call methods on remote objects.
 */
public class ProxyBusObject {

    private static final int AUTO_START = 0x02;

    /** The bus the remote object is connected to. */
    private BusAttachment bus;

    /** Well-known or unique name of remote object. */
    private String busName;

    /** Object path. */
    private String objPath;

    /** Native proxy bus object handle. */
    private long handle;

    /** Remote interfaces proxy. */
    private Object proxy;

    private int replyTimeoutMsecs;

    private int flags;

    private Method busConnectionLost;

    private String busAddress;

    private ProxyBusObjectListener listener;

    private String nameOwner;

    /**
     * Construct a ProxyBusObject.
     *
     * @param busAttachment  The connection the remote object is on.
     * @param busName        Well-known or unique bus name of remote object.
     * @param objPath        Object path of remote object.
     * @param sessionId      The session ID corresponding to the connection to the object.
     * @param busInterfaces  A list of BusInterfaces that this proxy should respond to.
     */
    protected ProxyBusObject(BusAttachment busAttachment, String busName, String objPath, int sessionId,
                             Class[] busInterfaces) {
        this.bus = busAttachment;
        this.busName = busName;
        this.objPath = objPath;
        create(busAttachment, busName, objPath, sessionId);
        replyTimeoutMsecs = 25000;
        proxy = Proxy.newProxyInstance(busInterfaces[0].getClassLoader(), busInterfaces, new Handler());
        try {
            busConnectionLost = 
                getClass().getDeclaredMethod("busConnectionLost", String.class);
            busConnectionLost.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            /* This will not happen */
        }
    }

    /** Allocate native resources. */
    private native void create(BusAttachment busAttachment, String busName, String objPath, int sessionId);

    /** Release native resources. */
    private synchronized native void destroy();

    /** Called by native code to lazily add an interface when a proxy method is invoked. */
    private int addInterface(String name) throws AnnotationBusException {
        for (Class<?> intf : proxy.getClass().getInterfaces()) {
            if (name.equals(InterfaceDescription.getName(intf))) {
                InterfaceDescription desc = new InterfaceDescription();
                Status status = desc.create(bus, intf);
                return status.getErrorCode();
            }
        }
        return Status.BUS_NO_SUCH_INTERFACE.getErrorCode();
    }

    /** Perform a method call on the remote object. */
    private native Object methodCall(BusAttachment busAttachment, String interfaceName,
            String methodName, String inputSig, Type outType, Object[] args, int replyTimeoutMsecs,
            int flags) throws BusException;

    /** Get a property of the remote object. */
    private native Variant getProperty(BusAttachment busAttachment, String interfaceName,
            String propertyName) throws BusException;

    /** Set a property of the remote object. */
    private native void setProperty(BusAttachment busAttachment, String interfaceName,
            String propertyName, String signature, Object value) throws BusException;

    /** The invocation handler for the bus interfaces. */
    private class Handler implements InvocationHandler {

        private class Invocation {
            public Method method;

            public boolean isMethod;
            public boolean isGet;

            public String inputSig;
            public String outSig;

            public String interfaceName;
            public String methodName;

            public Type genericReturnType;
            public Class<?> returnType;

            public Invocation(Method method) throws BusException {
                this.method = method;
                if (method.getAnnotation(BusProperty.class) != null) {
                    this.isGet = method.getName().startsWith("get");
                    this.outSig = InterfaceDescription.getPropertySig(method);
                } else {
                    this.isMethod = true;
                    this.outSig = InterfaceDescription.getOutSig(method);
                    this.inputSig = InterfaceDescription.getInputSig(method);
                }
                this.interfaceName = InterfaceDescription.getName(method.getDeclaringClass());
                this.methodName = InterfaceDescription.getName(method);
                this.genericReturnType = method.getGenericReturnType();
                this.returnType = method.getReturnType();
            }
        };

        private Map<String, List<Invocation>> invocationCache;

        public Handler() {
            this.invocationCache = new HashMap<String, List<Invocation>>();
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws BusException {
            /*
             * Some notes on performance.
             *
             * Reflection is very expensive.  So first pass at optimization is to cache the
             * reflection calls that lookup names and annotations the first time the method is
             * invoked.
             *
             * Using a Method as a HashMap key is expensive.  Using method.getName() as the key
             * is less expensive.  But method names may not be unique (they can be overloaded), so
             * need to fall back to Method.equals if more than one method with the same name exists.
             */
            Invocation invocation = null;
            String methodName = method.getName();
            List<Invocation> invocationList = invocationCache.get(methodName);
            if (invocationList != null) {
                if (invocationList.size() == 1) {
                    /* The fast path. */
                    invocation = invocationList.get(0);
                } else {
                    /* The slow path.  Two Java methods exist with the same name for this proxy. */
                    for (Invocation i : invocationList) {
                        if (method.equals(i.method)) {
                            invocation = i;
                            break;
                        }
                    }
                    if (invocation == null) {
                        invocation = new Invocation(method);
                        invocationList.add(invocation);
                    }
                }
            } else {
                /* 
                 * The very slow path.  The first time a proxy method is invoked. 
                 *
                 * Walk through all the methods looking for ones that match the invoked method name.
                 * This creates a list of all the cached invocation information that we'll use later
                 * on the next method call.
                 */
                invocationList = new ArrayList<Invocation>();
                for (Class<?> i : proxy.getClass().getInterfaces()) {
                    for (Method m : i.getMethods()) {
                        if (methodName.equals(m.getName())) {
                            Invocation in = new Invocation(m);
                            invocationList.add(in);
                            if (method.equals(in.method)) {
                                invocation = in;
                            }
                        }
                    }
                }
                if (invocation == null) {
                    throw new BusException("No such method: " + method);
                }
                invocationCache.put(methodName, invocationList);
            }

            Object value = null;
            if (invocation.isMethod) {
                value = methodCall(bus,
                                   invocation.interfaceName,
                                   invocation.methodName,
                                   invocation.inputSig,
                                   invocation.genericReturnType,
                                   args,
                                   replyTimeoutMsecs,
                                   flags);
            } else {
                if (invocation.isGet) {
                    Variant v = getProperty(bus, 
                                            invocation.interfaceName,
                                            invocation.methodName);
                    value = v.getObject(invocation.genericReturnType);
                } else {
                    setProperty(bus,
                                invocation.interfaceName,
                                invocation.methodName,
                                invocation.outSig,
                                args[0]);
                }
            }

            /* 
             * The JNI layer can't perform complete type checking (at least not easily),
             * so this extra code is here.  The conditions below are taken from the
             * InvocationHandler documentation.
             */
            boolean doThrow = false;
            Class<?> returnType = invocation.returnType;
            if (value == null) {
                doThrow = returnType.isPrimitive() && !returnType.isAssignableFrom(Void.TYPE);
            } else if (returnType.isPrimitive()) {
                if ((returnType.isAssignableFrom(Byte.TYPE) && !(value instanceof Byte))
                    || (returnType.isAssignableFrom(Short.TYPE) && !(value instanceof Short))
                    || (returnType.isAssignableFrom(Integer.TYPE) &&  !(value instanceof Integer))
                    || (returnType.isAssignableFrom(Long.TYPE) && !(value instanceof Long))
                    || (returnType.isAssignableFrom(Double.TYPE) && !(value instanceof Double))
                    || (returnType.isAssignableFrom(Boolean.TYPE) && !(value instanceof Boolean))) {
                    doThrow = true;
                }
            } else if (!returnType.isAssignableFrom(value.getClass())) {
                doThrow = true;
            }
            if (doThrow) {
                throw new MarshalBusException("cannot marshal '" + invocation.outSig + "' into " + returnType);
            }
            return value;
        }
    }

    /** Release native resources. */
    protected void finalize() {
        destroy();
    }

    /**
     * Gets the bus name.
     *
     * @return the bus name
     */
    public String getBusName() {
        return busName;
    }

    /**
     * Gets the object path.
     *
     * @return the object path
     */
    public String getObjPath() {
        return objPath;
    }

    /**
     * Gets a proxy to an interface of this remote bus object.
     *
     * @param intf one of the interfaces supplied when the proxy bus object was
     *             created
     * @return the proxy implementing the interface
     * @see BusAttachment#getProxyBusObject(String, String, Class[])
     */
    public <T> T getInterface(Class<T> intf) {
        @SuppressWarnings(value = "unchecked")
        T p = (T) proxy;
        return p;
    }

    /**
     * Sets the reply timeout for method invocations on this remote bus object.
     * This affects all future method invocations of this bus object.  The
     * default reply timeout is 25 seconds.
     *
     * @param timeoutMsecs the timeout to wait for a reply
     */
    public void setReplyTimeout(int timeoutMsecs) {
        replyTimeoutMsecs = timeoutMsecs;
    }

    /**
     * Tells the bus to start an application to handle a method invocation of
     * this bus object if needed.
     * This affects all future method invocations of this bus object.  The
     * default behavior is to not start an application.
     *
     * @param autoStart if {@code true} the bus should automatically start an
     *                  application to handle the method
     */
    public void setAutoStart(boolean autoStart) {
        this.flags = autoStart ? this.flags | AUTO_START : this.flags & ~AUTO_START;
    }

    private void nameOwnerChanged(String name, String oldOwner, String newOwner) {
        if (name.equals(getBusName())) {
            synchronized (this) {
                nameOwner = newOwner;
                notify();
            }
        }
    }

    private void busConnectionLost(String busAddress) {
        if ((this.busAddress != null) && this.busAddress.equals(busAddress)) {
            final ProxyBusObjectListener l = listener;
            if (l != null) {
                final String ba = busAddress;
                bus.execute(new Runnable() {
                        public void run() {
                            l.disconnected(ba);
                        }
                    });
            }
        }
    }
}

