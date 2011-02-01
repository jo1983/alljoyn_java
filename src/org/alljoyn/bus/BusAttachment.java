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

import org.alljoyn.bus.AuthListener.AuthRequest;
import org.alljoyn.bus.AuthListener.CertificateRequest;
import org.alljoyn.bus.AuthListener.Credentials;
import org.alljoyn.bus.AuthListener.LogonEntryRequest;
import org.alljoyn.bus.AuthListener.PasswordRequest;
import org.alljoyn.bus.AuthListener.PrivateKeyRequest;
import org.alljoyn.bus.AuthListener.UserNameRequest;
import org.alljoyn.bus.AuthListener.VerifyRequest;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.bus.ifaces.DBusProxyObj;
import org.alljoyn.bus.ifaces.AllJoynProxyObj;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A connection to a message bus.
 * Using BusAttachment, an application may register objects on the bus for other
 * bus connections to access. BusAttachment is also used to access remote
 * objects that have been registered by other processes and/or remote devices.
 */
public class BusAttachment {

    /** The native connection handle. */
    private long handle;

    /** The connect spec. */
    private String address;

    /** 
     * {@code true} if this attachment is allowed to receive messages from
     * remote devices.
     */
    private boolean allowRemoteMessages;

    private KeyStoreListener keyStoreListener;

    private class AuthListenerInternal {

        private static final int PASSWORD       = 0x0001;
        private static final int USER_NAME      = 0x0002;
        private static final int CERT_CHAIN     = 0x0004;
        private static final int PRIVATE_KEY    = 0x0008;
        private static final int LOGON_ENTRY    = 0x0010;
        private static final int NEW_PASSWORD   = 0x1001;
        private static final int ONE_TIME_PWD   = 0x2001;

        private AuthListener authListener;
        private SecurityViolationListener violationListener;

        public void setAuthListener(AuthListener authListener) {
            this.authListener = authListener;
        }

        public void setSecurityViolationListener(SecurityViolationListener violationListener) {
            this.violationListener = violationListener;
        }

        public Credentials requestCredentials(String authMechanism, int authCount,
                String userName, int credMask) throws BusException {
            if (authListener == null) {
                throw new BusException("No registered application AuthListener");
            }
            
            Credentials credentials = new Credentials();
            List<AuthRequest> requests = new ArrayList<AuthRequest>();
            if ((credMask & PASSWORD) == PASSWORD) {
                boolean isNew = (credMask & NEW_PASSWORD) == NEW_PASSWORD;
                boolean isOneTime = (credMask & ONE_TIME_PWD) == ONE_TIME_PWD;
                requests.add(new PasswordRequest(credentials, isNew, isOneTime));
            }
            if ((credMask & USER_NAME) == USER_NAME) {
                requests.add(new UserNameRequest(credentials));
            }
            if ((credMask & CERT_CHAIN) == CERT_CHAIN) {
                requests.add(new CertificateRequest(credentials));
            }
            if ((credMask & PRIVATE_KEY) == PRIVATE_KEY) {
                requests.add(new PrivateKeyRequest(credentials));
            }
            if ((credMask & LOGON_ENTRY) == LOGON_ENTRY) {
                requests.add(new LogonEntryRequest(credentials));
            }

            if (authListener.requested(authMechanism, authCount, userName, 
                                       requests.toArray(new AuthRequest[0]))) {
                return credentials;
            }
            return null;
        }

        public boolean verifyCredentials(String authMechanism, String userName, 
                                         String cert) throws BusException {
            if (authListener == null) {
                throw new BusException("No registered application AuthListener");
            }
            /*
             * authCount is set to 0 here since it can't be cached from
             * requestCredentials, and it's assumed that the application will
             * not immediately reject a request with an authCount of 0.
             */
            return authListener.requested(authMechanism, 0, userName == null ? "" : userName,
                                          new AuthRequest[] { new VerifyRequest(cert) });
        }

        public void securityViolation(Status status) {
            if (violationListener != null) {
                violationListener.violated(status);
            }
        }

        public void authenticationComplete(String authMechanism, boolean success) {
            if (authListener != null) {
                authListener.completed(authMechanism, success);
            }
        }
    }

    private AuthListenerInternal busAuthListener;

    /** End-to-end authentication mechanisms. */
    private String authMechanisms;

    /** Default key store file name. */
    private String keyStoreFileName;

    private Map<String, FindNameListener> findNameListeners;

    private Method foundName;

    private ExecutorService foundNameExecutor;

    private Method lostAdvName;

    private DBusProxyObj dbus;

    private AllJoynProxyObj alljoyn;

    /** Policy for handling messages received from remote devices. */
    public enum RemoteMessage {

        /** This attachment will not receive messages from remote devices. */
        Ignore,

        /** This attachment will receive messages from remote devices. */
        Receive
    }

    /**
     * Constructs a BusAttachment.
     *
     * @param applicationName the name of the application
     * @param policy if this attachment is allowed to receive messages
     *               from remote devices
     */
    public BusAttachment(String applicationName, RemoteMessage policy) {
        this.allowRemoteMessages = (policy == RemoteMessage.Receive);
        busAuthListener = new AuthListenerInternal();
        findNameListeners = Collections.synchronizedMap(new HashMap<String, FindNameListener>());
        try {
            foundName = getClass().getDeclaredMethod(
                "foundName", String.class, String.class, String.class, String.class);
            foundName.setAccessible(true);
            lostAdvName = getClass().getDeclaredMethod(
                "lostAdvertisedName", String.class, String.class, String.class, String.class);
            lostAdvName.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            /* This will not happen */
        }
        create(applicationName, allowRemoteMessages);
        dbus = new ProxyBusObject(this, "org.freedesktop.DBus", "/org/freedesktop/DBus", 
                                  new Class[] { DBusProxyObj.class }).getInterface(DBusProxyObj.class);        
        alljoyn = new ProxyBusObject(this, "org.alljoyn.Bus", "/org/alljoyn/Bus",
                                  new Class[] { AllJoynProxyObj.class }).getInterface(AllJoynProxyObj.class);
    }

    /**
     * Construct a BusAttachment that will only communicate on the local device.
     *
     * @param applicationName the name of the application
     */
    public BusAttachment(String applicationName) {
        this(applicationName, RemoteMessage.Ignore);
    }

    /** Allocate native resources. */
    private native void create(String applicationName, boolean allowRemoteMessages);

    /** Release native resources. */
    private synchronized native void destroy();

    /** Start and connect to the bus. */
    private native Status connect(String connectArgs, KeyStoreListener keyStoreListener, 
                                  String authMechanisms, AuthListenerInternal busAuthListener, 
                                  String keyStoreFileName);

    /** Stop and disconnect from the bus. */
    private native void disconnect(String connectArgs);

    private native Status enablePeerSecurity(String authMechanisms,
        AuthListenerInternal busAuthListener, String keyStoreFileName);

    private native Status registerBusObject(String objPath, BusObject busObj,
                                            InterfaceDescription[] busInterfaces);

    private native Status registerNativeSignalHandler(String ifaceName, String signalName,
            Object obj, Method handlerMethod, String source);

    @BusSignalHandler(iface = "org.alljoyn.Bus", signal = "FoundName")
    private void foundName(String name, String guid, String namePrefix, String busAddress) {
        final FindNameListener listener;
        synchronized (findNameListeners) {
            listener = findNameListeners.get(namePrefix);
        }
        final String n = name;
        final String g = guid;
        final String np = namePrefix;
        final String ba = busAddress;
        if (listener != null) {
            foundNameExecutor.execute(new Runnable() {
                    public void run() { 
                        listener.foundName(n, g, np, ba); 
                    }
                });
        }
    }

    @BusSignalHandler(iface = "org.alljoyn.Bus", signal = "LostAdvertisedName")
    private void lostAdvertisedName(String name, String guid, String namePrefix, String busAddress) {
        final FindNameListener listener;
        synchronized (findNameListeners) {
            listener = findNameListeners.get(namePrefix);
        }
        final String n = name;
        final String g = guid;
        final String np = namePrefix;
        final String ba = busAddress;
        if (listener != null) {
            foundNameExecutor.execute(new Runnable() {
                    public void run() { 
                        listener.lostAdvertisedName(n, g, np, ba); 
                    }
                });
        }
    }

    /** Release resources. */
    protected void finalize() {
        destroy();
    }

    /**
     * Convert to UTF-8 for native code.  This is intended for sensitive string
     * data (i.e. passwords).  The native code must take care of scrubbing the
     * buffer when done.
     *
     * @param charArray the sensitive string
     * @return the UTF-8 encoded version of the string
     */
    static byte[] encode(char[] charArray) {
        try {
            Charset charset = Charset.forName("UTF-8");
            CharsetEncoder encoder = charset.newEncoder();
            ByteBuffer bb = encoder.encode(CharBuffer.wrap(charArray));
            byte[] ba = new byte[bb.limit()];
            bb.get(ba);
            return ba;
        } catch (CharacterCodingException ex) {
            BusException.log(ex);
            return null;
        }
    }

    /**
     * Starts the message bus and connects to the local daemon.
     * This method blocks until the connection attempt succeeds or fails.
     * <p>
     * {@link BusObjectListener#registered()} is called by the bus when the bus
     * is connected.
     *
     * @return OK if successful
     */
    public Status connect() {
        address = System.getProperty("org.alljoyn.bus.address", "unix:abstract=alljoyn");
        if (address != null) {
            Status status = connect(address, keyStoreListener, authMechanisms, busAuthListener,
                                    keyStoreFileName);
            if (status == Status.OK) {
                status = registerSignalHandler("org.alljoyn.Bus", "FoundName", this, foundName);
            }
            if (status == Status.OK) {
                status = registerSignalHandler("org.alljoyn.Bus", "LostAdvertisedName", this, lostAdvName);
            }
            return status;
        } else {
            return Status.INVALID_CONNECT_ARGS;
        }
    }

    /**
     * Starts the message bus, connects to the local daemon, and requests and
     * optionally advertises a well-known name.  The well-known name is
     * advertised if this BusAttachment is configured to allow remote messages.
     * <p>
     * This method does the following:
     * <pre>
     * busAttachment.connect()
     *
     * busAttachment.getDBusProxyObj().RequestName(wellKnownName,
     *                                             DBusProxyObj.REQUEST_NAME_REPLACE_EXISTING | DBusProxyObj.REQUEST_NAME_DO_NOT_QUEUE);
     * 
     * if (policy == BusAttachment.RemoteMessage.Receive) {
     *     busAttachment.getAllJoynProxyObj().AdvertiseName(wellKnownName);
     * }</pre>
     *
     * @param wellKnownName the well-known name to request and optionally
     *                      advertise
     * @return OK if successful
     * @see #BusAttachment(String, RemoteMessage)
     */
    public Status connect(String wellKnownName) {
        try {
            Status status = connect();
            if (status != Status.OK) {
                return status;
            }
            int flags = DBusProxyObj.REQUEST_NAME_REPLACE_EXISTING 
                | DBusProxyObj.REQUEST_NAME_DO_NOT_QUEUE;
            DBusProxyObj.RequestNameResult requestResult = dbus.RequestName(wellKnownName, flags);
            if (requestResult != DBusProxyObj.RequestNameResult.PrimaryOwner 
                && requestResult != DBusProxyObj.RequestNameResult.AlreadyOwner) {
                return Status.BUS_NOT_OWNER;
            }
            if (allowRemoteMessages) {
                AllJoynProxyObj.AdvertiseNameResult advertiseResult = alljoyn.AdvertiseName(wellKnownName);
                if (advertiseResult != AllJoynProxyObj.AdvertiseNameResult.Success 
                    && advertiseResult != AllJoynProxyObj.AdvertiseNameResult.AlreadyAdvertising) {
                    return Status.FAIL;
                }
            }
            return Status.OK;
        } catch (BusException ex) {
            BusException.log(ex);
            return Status.FAIL;
        }
    }

    /**
     * Disconnects from the local daemon and stops the message bus.
     */
    public void disconnect() {
        if (address != null) {
            deregisterSignalHandler(this, foundName);
            deregisterSignalHandler(this, lostAdvName);
            disconnect(address);
        }
    }

    /**
     * Registers a bus object.
     * Once registered, the bus object may communicate to and from other
     * objects via its implemented bus interfaces.
     * <p>
     * The same object may not be registered on multiple bus connections.
     *
     * @param busObj the BusObject to register
     * @param objPath the object path of the BusObject
     * @return OK if successful
     * @see org.alljoyn.bus.annotation.BusInterface
     */
    public Status registerBusObject(BusObject busObj, String objPath) {
        try {
            List<InterfaceDescription> descs = new ArrayList<InterfaceDescription>();
            Status status = InterfaceDescription.create(this, busObj.getClass().getInterfaces(), 
                                                        descs);
            if (status != Status.OK) {
                return status;
            }
            return registerBusObject(objPath, busObj, descs.toArray(new InterfaceDescription[0]));
        } catch (AnnotationBusException ex) {
            BusException.log(ex);
            return Status.BAD_ANNOTATION;
        }
    }

    /**
     * Deregisters a bus object.
     *
     * @param obj the BusObject to deregister
     */
    public native void deregisterBusObject(BusObject obj);

    /**
     * Creates a proxy bus object for a remote bus object.
     * Methods on the remote object can be invoked through the proxy object.
     * <p>
     * There is no guarantee that the remote object referred to by the proxy
     * acutally exists.  If the remote object does not exist, proxy method
     * calls will fail.
     * <p>
     * Java proxy classes do not allow methods from two different interfaces to
     * have the same name and calling parameters. If two AllJoyn methods from two
     * different interfaces are implemented by the same remote object, one (or
     * both) of the method names must be modified. You may then use an
     * annotation for the renamed method to cause AllJoyn to use the originally
     * expected method name in any "wire" operations.
     *
     * @param busName the remote endpoint name (well-known or unique)
     * @param objPath the absolute (non-relative) object path for the object
     * @param busInterfaces an array of BusInterfaces that this proxy should respond to
     * @return a ProxyBusObject for an object that implements all interfaces listed in busInterfaces
     * @see org.alljoyn.bus.annotation.BusMethod
     */
    public ProxyBusObject getProxyBusObject(String busName,
                                            String objPath,
                                            Class[] busInterfaces) {
        return new ProxyBusObject(this, busName, objPath, busInterfaces);
    }

    /**
     * Gets the DBusProxyObj interface of the org.freedesktop.DBus proxy object.
     * The DBusProxyObj interface is provided for backwards compatibility with
     * the DBus protocol. To use the extended AllJoyn features, use {@link
     * BusAttachment#getAllJoynProxyObj()} instead of this method.
     *
     * @return the DBusProxyObj interface
     */
    public DBusProxyObj getDBusProxyObj() {
        return dbus;
    }

    /**
     * Gets the AllJoynProxyObj interface of the org.alljoyn.Bus proxy object.
     *
     * @return the AllJoynProxyObj interface
     */
    public AllJoynProxyObj getAllJoynProxyObj() {
        return alljoyn;
    }

    /**
     * Get the unique name of this BusAttachment.
     *
     * @return the unique name of this BusAttachment
     */
    public native String getUniqueName();

    /**
     * Registers a public method to receive a signal from all objects emitting
     * it.
     * Once registered, the method of the object will receive the signal
     * specified from all objects implementing the interface.
     *
     * @param ifaceName the interface name of the signal
     * @param signalName the member name of the signal
     * @param obj the object receiving the signal
     * @param handlerMethod the signal handler method
     * @return OK if the register is succesful
     */
    public Status registerSignalHandler(String ifaceName,
                                        String signalName,
                                        Object obj,
                                        Method handlerMethod) {
        return registerSignalHandler(ifaceName, signalName, obj, handlerMethod, "");
    }

    /**
     * Registers a public method to receive a signal from specific objects
     * emitting it.
     * Once registered, the method of the object will receive the signal
     * specified from objects implementing the interface.
     *
     * @param ifaceName the interface name of the signal
     * @param signalName the member name of the signal
     * @param obj the object receiving the signal
     * @param handlerMethod the signal handler method
     * @param source the object path of the emitter of the signal
     * @return OK if the register is succesful
     */
    public Status registerSignalHandler(String ifaceName,
                                        String signalName,
                                        Object obj,
                                        Method handlerMethod,
                                        String source) {
        Status status = registerNativeSignalHandler(ifaceName, signalName, obj, handlerMethod,
                                                    source);
        if (status == Status.BUS_NO_SUCH_INTERFACE) {
            try {
                Class iface = Class.forName(ifaceName);
                InterfaceDescription desc = new InterfaceDescription();
                status = desc.create(this, iface);
                if (status == Status.OK) {
                    status = registerNativeSignalHandler(ifaceName, signalName, obj, handlerMethod,
                                                         source);
                }
            } catch (ClassNotFoundException ex) {
                BusException.log(ex);
                status = Status.BUS_NO_SUCH_INTERFACE;
            } catch (AnnotationBusException ex) {
                BusException.log(ex);
                status = Status.BAD_ANNOTATION;
            }
        }
        return status;
    }

    /**
     * Registers all public methods that are annotated as signal handlers.
     *
     * @param obj object with methods annotated with as signal handlers
     * @return OK if the register is succesful
     * @see org.alljoyn.bus.annotation.BusSignalHandler
     */
    public Status registerSignalHandlers(Object obj) {
        Status status = Status.OK;
        for (Method m : obj.getClass().getMethods()) {
            BusSignalHandler a = m.getAnnotation(BusSignalHandler.class);
            if (a != null) {
                status = registerSignalHandler(a.iface(), a.signal(), obj, m, a.source());
                if (status != Status.OK) {
                    break;
                }
            }
        }
        return status;
    }

    /**
     * Deregisters a signal handler.
     *
     * @param obj the object receiving the signal
     * @param handlerMethod the signal handler method
     */
    public native void deregisterSignalHandler(Object obj, Method handlerMethod);

    /**
     * Deregisters all public methods annotated as signal handlers.
     *
     * @param obj object with previously annotated signal handlers that have
     *            been registered
     * @see org.alljoyn.bus.annotation.BusSignalHandler
     */
    public void deregisterSignalHandlers(Object obj) {
        for (Method m : obj.getClass().getMethods()) {
            BusSignalHandler a = m.getAnnotation(BusSignalHandler.class);
            if (a != null) {
                deregisterSignalHandler(obj, m);
            }
        }
    }

    /**
     * Registers a user-defined key store listener to override the default key store.  This must be
     * called prior to {@link #connect()}.
     *
     * @param listener the key store listener
     */
    public void registerKeyStoreListener(KeyStoreListener listener) {
        keyStoreListener = listener;
    }

    /**
     * Clears all stored keys from the key store. All store keys and authentication information is
     * deleted and cannot be recovered. Any passwords or other credentials will need to be reentered
     * when establishing secure peer connections.
     */
    public native void clearKeyStore();

    /**
     * Registers a user-defined authentication listener class with a specific default key store.
     *
     * @param authMechanisms the authentication mechanism(s) to use for peer-to-peer authentication
     * @param listener the authentication listener
     * @param keyStoreFileName the name of the default key store.  Under Android, the recommended
     *                         value of this parameter is {@code
     *                         Context.getFileStreamPath("alljoyn_keystore").getAbsolutePath()}.  Note
     *                         that the default key store implementation may be overrided with
     *                         {@link #registerKeyStoreListener(KeyStoreListener)}.
     * @return OK if successful
     */
    public Status registerAuthListener(String authMechanisms, AuthListener listener, 
                                       String keyStoreFileName) {
        this.authMechanisms = authMechanisms;
        busAuthListener.setAuthListener(listener);
        this.keyStoreFileName = keyStoreFileName;
        Status status = enablePeerSecurity(this.authMechanisms, busAuthListener, 
                                           this.keyStoreFileName);
        if (status != Status.OK) {
            busAuthListener.setAuthListener(null);
            this.authMechanisms = null;
        }
        return status;
    }

    /**
     * Registers a user-defined authentication listener class.  Under Android, it is recommended to
     * use {@link #registerAuthListener(String, AuthListener, String)} instead to specify the path
     * of the default key store.
     *
     * @param authMechanisms the authentication mechanism(s) to use for peer-to-peer authentication
     * @param listener the authentication listener
     * @return OK if successful
     */
    public Status registerAuthListener(String authMechanisms, AuthListener listener) {
        return registerAuthListener(authMechanisms, listener, null);
    }

    /**
     * Registers a user-defined security violation listener class.
     *
     * @param listener the security violation listener
     */
    public void registerSecurityViolationListener(SecurityViolationListener listener) {
        busAuthListener.setSecurityViolationListener(listener);
    }

    /**
     * Gets the message context of the currently executing method, signal
     * handler, or security violation.
     * The context contains information about the method invoker or signal
     * sender including any authentication that exists with this remote entity.
     * <p>
     * This method can only be called from within the method, signal, or
     * security violation handler itself since the caller's thread information
     * is used to find the appropriate context.
     *
     * @return message context for the currently executing method, signal
     *         handler, security violation, or null if no message can be found
     *         for the calling thread
     */
    public native MessageContext getMessageContext();

    /**
     * Finds instances of a well-known attachment name.  When the name is found,
     * {@link FindNameListener#foundName(String, String, String, String)} is
     * called.  The application can then choose to ignore the name or connect a
     * proxy bus object to it with {@link ProxyBusObject#connect(String)}.
     * <p>
     * This method does the following:
     * <pre>
     * Method foundName = listener.getClass().getMethod("foundName", String.class, String.class, String.class, String.class);
     * busAttachment.registerSignalHandler("org.alljoyn.Bus", "FoundName", listener, foundName);
     *
     * busAttachment.getAllJoynProxyObj().FindName(wellKnownName);</pre>
     *
     * with the addition that further synchronous AllJoyn method calls may be safely
     * made from inside {@code foundName}.
     *
     * @param wellKnownNamePrefix well-known name prefix of the attachment that
     *                            the client is interested in
     * @param listener the result listener
     * @return a status code indicating success or failure
     * @see #cancelFindName(String)
     */
    public Status findName(String wellKnownNamePrefix, FindNameListener listener) {
        if (wellKnownNamePrefix == null) {
            throw new IllegalArgumentException("wellKnownNamePrefix");
        } 
        if (listener == null) {
            throw new IllegalArgumentException("listener");
        }
        try {
            boolean startExecutor;
            synchronized (findNameListeners) {
                if (findNameListeners.get(wellKnownNamePrefix) != null) {
                    return Status.ALREADY_FINDING;
                }
                startExecutor = findNameListeners.isEmpty();
                findNameListeners.put(wellKnownNamePrefix, listener);
            }
            if (startExecutor) {
                foundNameExecutor = Executors.newSingleThreadExecutor();
            }
            
            /* Look for any remote names */
            AllJoynProxyObj.FindNameResult res = getAllJoynProxyObj().FindName(wellKnownNamePrefix);
            if (res == AllJoynProxyObj.FindNameResult.Success) {
                return Status.OK;
            } else if (res == AllJoynProxyObj.FindNameResult.AlreadyDiscovering) {
                return Status.ALREADY_FINDING;
            } else {
                return Status.FAIL;
            }
        } catch (BusException ex) {
            BusException.log(ex);
            return Status.FAIL;
        }
    }

    /**
     * Cancels interest in a well-known attachment name that was previously
     * included in a call to {@link #findName(String, FindNameListener)}.
     * <p>
     * This method does the following:
     * <pre>
     * busAttachment.getAllJoynProxyObj().CancelFindName(wellKnownName);
     *
     * Method foundName = listener.getClass().getMethod("foundName", String.class, String.class, String.class, String.class);
     * busAttachment.deregisterSignalHandler("org.alljoyn.Bus", "FoundName", listener, foundName);</pre>
     * 
     * @param wellKnownNamePrefix well-known name prefix of the attachment that
     *                            client is no longer interested in
     * @return a status code indicating success or failure
     */
    public Status cancelFindName(String wellKnownNamePrefix) {
        if (wellKnownNamePrefix == null) {
            throw new IllegalArgumentException("wellKnownNamePrefix");
        }
        try {
            AllJoynProxyObj.CancelFindNameResult res = 
                getAllJoynProxyObj().CancelFindName(wellKnownNamePrefix);
            if (res == AllJoynProxyObj.CancelFindNameResult.Success) {
                boolean stopExecutor;
                synchronized (findNameListeners) {
                    findNameListeners.remove(wellKnownNamePrefix);
                    stopExecutor = findNameListeners.isEmpty();
                }
                if (stopExecutor) {
                    foundNameExecutor.shutdown();
                }
                return Status.OK;
            } else {
                return Status.FAIL;
            }
        } catch (BusException ex) {
            BusException.log(ex);
            return Status.FAIL;
        }
    }
}
