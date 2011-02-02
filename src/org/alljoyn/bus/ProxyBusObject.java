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
import org.alljoyn.bus.ifaces.AllJoynProxyObj;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

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

    private String busAddress;

    private String nameOwner;

    /**
     * Construct a ProxyBusObject.
     *
     * @param busAttachment  The connection the remote object is on.
     * @param busName        Well-known or unique bus name of remote object.
     * @param objPath        Object path of remote object.
     * @param busInterfaces  A list of BusInterfaces that this proxy should respond to.
     */
    protected ProxyBusObject(BusAttachment busAttachment, String busName, String objPath,
                             Class[] busInterfaces) {
        this.bus = busAttachment;
        this.busName = busName;
        this.objPath = objPath;
        create(busAttachment, busName, objPath);
        replyTimeoutMsecs = 25000;
        proxy = Proxy.newProxyInstance(getClass().getClassLoader(), busInterfaces, new Handler());
    }

    /** Allocate native resources. */
    private native void create(BusAttachment busAttachment, String busName, String objPath);

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

        public Object invoke(Object proxy, Method method, Object[] args) throws BusException {
            for (Class<?> i : proxy.getClass().getInterfaces()) {
                for (Method m : i.getMethods()) {
                    if (method.getName().equals(m.getName())) {
                        Object value = null;
                        String outSig = null;
                        if (m.getAnnotation(BusProperty.class) != null) {
                            outSig = InterfaceDescription.getPropertySig(m);
                            if (m.getName().startsWith("get")) {
                                Variant v = getProperty(bus, 
                                                        InterfaceDescription.getName(i),
                                                        InterfaceDescription.getName(m));
                                value = v.getObject(m.getGenericReturnType());
                            } else {
                                setProperty(bus,
                                            InterfaceDescription.getName(i),
                                            InterfaceDescription.getName(m),
                                            InterfaceDescription.getPropertySig(m),
                                            args[0]);
                            }
                        } else {
                            outSig = InterfaceDescription.getOutSig(m);
                            value = methodCall(bus,
                                               InterfaceDescription.getName(i),
                                               InterfaceDescription.getName(m),
                                               InterfaceDescription.getInputSig(m),
                                               m.getGenericReturnType(),
                                               args,
                                               replyTimeoutMsecs,
                                               flags);
                        }
                        /* 
                         * The JNI layer can't perform complete type checking (at least not easily),
                         * so this extra code is here.  The conditions below are taken from the
                         * InvocationHandler documentation.
                         */
                        boolean doThrow = false;
                        Class<?> returnType = m.getReturnType();
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
                            throw new MarshalBusException("cannot marshal '" + outSig + "' into " + returnType);
                        }
                        return value;
                    }
                }
            }
            throw new BusException("No such method: " + method);
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

    /**
     * Connects the local daemon to a remote AllJoyn address and causes the current
     * thread to wait until an owner of this object's bus name appears.  This is
     * equivalent to {@link #connect(String, long) connect(busAddress, 0)}.
     *
     * @param busAddress remote bus address to connect to
     *                (e.g. {@code bluetooth:addr=00.11.22.33.44.55}, or
     *                {@code tcp:addr=1.2.3.4,port=1234})
     * @return a status code indicating success or failure. {@code
     *         Status.CANCELLED} if the request is cancelled via {@code
     *         cancelConnect()}.
     * @see #connect(String, long)
     * @see #cancelConnect()
     * @see #disconnect()
     * @see #getBusName()
     */
    public Status connect(String busAddress) {
        return connect(busAddress, 0);
    }

    /**
     * Connects the local daemon to a remote AllJoyn address and causes the current
     * thread to wait until an owner of this object's bus name appears, or a
     * certain amount of time has elapsed.
     * <p>
     * Note that this method returning success does not guarantee that the
     * remote object exists or that it will continue to exist until {@code
     * disconnect()} is called. 
     * <p>
     * This method does the following:
     * <pre>
     * busAttachment.getAllJoynProxyObj().Connect(busAddress);
     *
     * if (!busAttachment.getDBusProxyObj().NameHasOwner(getBusName())) {
     * 
     *     Method nameOwnerChanged = myObj.getClass().getMethod("nameOwnerChanged", String.class, String.class, String.class);
     *     busAttachment.registerSignalHandler("org.freedesktop.DBus", "NameOwnerChanged", myObj, nameOwnerChanged);
     *
     *     while (newOwner == null) {
     *         myObj.wait();
     *     }
     * }
     *
     * ...
     *
     * public void nameOwnerChanged(String name, String oldOwner, String newOwner) {
     *     if (name.equals(getBusName())) {
     *         notify();
     *     }
     * }</pre>
     *
     * @param busAddress remote bus address to connect to
     *                (e.g. {@code bluetooth:addr=00.11.22.33.44.55}, or
     *                {@code tcp:addr=1.2.3.4,port=1234})
     * @param timeout how many milliseconds to wait for an owner to be found.
     *                If {@code timeout} is zero, then this thread waits
     *                forever.
     * @return a status code indicating success or failure. {@code
     *         Status.CANCELLED} if the request is cancelled via {@code
     *         cancelConnect()}.  {@code Status.TIMEOUT} if the name owner is
     *         not found before the timeout is reached.
     * @see #cancelConnect()
     * @see #disconnect()
     * @see #getBusName()
     */
    public Status connect(String busAddress, long timeout) {
        Method nameOwnerChanged = null;
        try {
            nameOwnerChanged = 
                getClass().getDeclaredMethod("nameOwnerChanged", String.class, String.class,
                                             String.class);
            nameOwnerChanged.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            /* This will not happen */
        }
        /* 
         * Wrap everything up here in a try {} finally {} to ensure resources
         * get cleaned up correctly. 
         */
        Status status = Status.OK;
        AllJoynProxyObj.ConnectResult connectResult = AllJoynProxyObj.ConnectResult.Invalid;
        try {
            /* Part 1: connect to the remote bus */
            if (this.busAddress != null) {
                status = Status.BUS_ALREADY_CONNECTED;
            }
            if (status == Status.OK) {
                connectResult = bus.getAllJoynProxyObj().Connect(busAddress);
                if (connectResult == AllJoynProxyObj.ConnectResult.Success) {
                    this.busAddress = busAddress;
                } else {
                    status = Status.FAIL;
                }
            }
            
            /* 
             * Part 2: look for an owner of this object's well-known name.
             * Without this, method calls will not succeed.
             */
            if (status == Status.OK && !bus.getDBusProxyObj().NameHasOwner(getBusName())) {
                status = bus.registerSignalHandler(
                    "org.freedesktop.DBus", "NameOwnerChanged", this, nameOwnerChanged);
            }
            /* 
             * NameOwnerChanged may have signalled before the handler was
             * registered, so double check.
             */
            if (status == Status.OK && !bus.getDBusProxyObj().NameHasOwner(getBusName())) {
                
                /* Now wait for the NameOwnerChanged signal */
                synchronized (this) {
                    if (timeout == 0) {
                        while (nameOwner == null) {
                            wait();
                        }
                        if ("cancelled".equals(nameOwner)) {
                            status = Status.CANCELLED;
                        }
                    } else {
                        timeout += System.currentTimeMillis();
                        while (nameOwner == null && System.currentTimeMillis() < timeout) {
                            wait(timeout - System.currentTimeMillis());
                        }
                        if ("cancelled".equals(nameOwner)) {
                            status = Status.CANCELLED;
                        } else if (timeout <= System.currentTimeMillis()) {
                            status = Status.TIMEOUT;
                        }
                    }
                    nameOwner = null;
                }
            }
        } catch (BusException ex) {
            BusException.log(ex);
            status = Status.FAIL;
        } catch (InterruptedException ex) {
            BusException.log(ex);
            status = Status.FAIL;
        } finally {
            if (status != Status.OK) {
                disconnect();
            }
            bus.deregisterSignalHandler(this, nameOwnerChanged);
        }
        return status;
    }

    /**
     * Requests to cancel the current connect request. 
     *
     * @see #connect(String)
     */
    public void cancelConnect() {
        synchronized (this) {
            nameOwner = "cancelled"; /* Not a valid bus name, so safe to use here */
            notify();
        }
    }

    /**
     * Requests the local daemon to disconnect from the remote AllJoyn address
     * previously connected via a call to {@link #connect(String)}.
     * <p>
     * This method does the following:
     * <pre>
     * busAttachment.getAllJoynProxyObj().Disconnect(busAddress);</pre>
     */
    public void disconnect() {
        try {
            if (busAddress != null) {
                bus.getAllJoynProxyObj().Disconnect(busAddress);
                busAddress = null;
            }
        } catch (BusException ex) {
            BusException.log(ex);
            /* Ignore exception, assume already disconnected. */
        }
    }
}

