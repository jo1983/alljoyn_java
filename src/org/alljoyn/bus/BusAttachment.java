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

import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.BusListener;
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

    /**
     * Request a well-known name.
     * This method is a shortcut/helper that issues an org.freedesktop.DBus.RequestName method call to the local daemon
     * and interprets the response.
     *
     * @param requestedName  Well-known name being requested.
     * @param flags          Bitmask name flag (see DBusStd.h)
     * 						<ul>
     * 						<li>ALLJOYN_NAME_FLAG_ALLOW_REPLACEMENT</li>
     * 						<li>ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING</li>
     * 						<li>ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE</li>
     * 						</ul>
     * 							
     *
     * @return
     * <ul>
     * <li>OK if request completed successfully and primary ownership was granted.</li>
     * <li>BUS_NOT_CONNECTED if a connection has not been made with a local bus</li>
     * <li>other error status codes indicating a failure.</li>
     * <ul>
     */
    public native Status requestName(String name, int flags);

    /**
     * Value for requestName flags bit corresponding to allowing another bus
     * attachment to take ownership of this name.
     */
    public static final int ALLJOYN_NAME_FLAG_ALLOW_REPLACEMENT = 0x01;

    /**
     * Value for requestName flags bit corresponding to a request to take
     * ownership of the name in question if it is already taken.
     */
    public static final int ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING = 0x02;

    /**
     * Value for requestName flags bit corresponding to a request to 
     * fail if the name in question cannot be immediately obtained.
     */
    public static final int ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE = 0x04;

    /**
     * Release a previously requeted well-known name.
     * This method is a shortcut/helper that issues an org.freedesktop.DBus.ReleaseName method call to the local daemon
     * and interprets the response.
     *
     * @param name  Well-known name being released.
     *
     * @return 
     * <ul>
     * <li>OK if the name was released.</li>
     * <li>BUS_NOT_CONNECTED if a connection has not been made with a local bus</li>
     * <li>other error status codes indicating a failure.</li>
     * </ul>
     */
    public native Status releaseName(String name);

    /**
     * Add a DBus match rule.
     * This method is a shortcut/helper that issues an org.freedesktop.DBus.AddMatch method call to the local daemon.
     *
     * @param rule  Match rule to be added (see the DBus specification for the
     *              format of this string).
     *
     * @return 
     * <ul>
     * <li>OK if the match rule was added.</li>  
     * <li>BUS_NOT_CONNECTED if a connection has not been made with a local bus</li> 
     * <li>other error status codes indicating a failure</li>
     * </ul>
     */
    public native Status addMatch(String rule);

    /**
     * Remove a DBus match rule.
     * This method is a shortcut/helper that issues an org.freedesktop.DBus.RemoveMatch method call to the local daemon.
     *
     * @param rule  Match rule to be removed (see the DBus specification for the
     *              format of this string).
     *
     * @return 
     * <ul>
     * <li>OK if the match rule was removed.</li>  
     * <li>BUS_NOT_CONNECTED if a connection has not been made with a local bus</li> 
     * <li>other error status codes indicating a failure</li>
     * </ul>
     */
    public native Status removeMatch(String rule);

    /**
     * Advertise the existence of a well-known name to other (possibly disconnected) AllJoyn daemons.
     *
     * This method is a shortcut/helper that issues an org.codeauora.AllJoyn.Bus.AdvertisedName method call to the local daemon
     * and interprets the response.
     *
     * @param name        The well-known name to advertise. (Must be owned by the caller via RequestName).
     * @param transports  Set of transports to use for sending advertisment.
     *
     * @return
     * <ul> 
     * <li>OK if the name was advertised.</li>
     * <li>BUS_NOT_CONNECTED if a connection has not been made with a local bus</li>
     * <li>other error status codes indicating a failure </li>
     * </ul>
     */
    public native Status advertiseName(String name, short transports);

    /**
     * Stop advertising the existence of a well-known name to other AllJoyn daemons.
     *
     * This method is a shortcut/helper that issues an
     * org.codeauora.AllJoyn.Bus.CancelAdvertiseName method call to the local
     * daemon and interprets the response.
     *
     * @param name        A well-known name that was previously advertised via AdvertiseName.
     * @param transports  Set of transports whose name advertisment will be cancelled.
     *
     * @return 
     * <ul>
     * <li>OK if the name advertisements were stopped.</li>  
     * <li>BUS_NOT_CONNECTED if a connection has not been made with a local bus</li>
     * <li>other error status codes indicating a failure.</li>
     * </ul>
     */
    public native Status cancelAdvertiseName(String name, short transports);

    /**
     * Register interest in a well-known name prefix for the purpose of discovery.
     * This method is a shortcut/helper that issues an org.codeauora.AllJoyn.Bus.FindAdvertisedName method call to the local daemon
     * and interprets the response.
     *
     * @param namePrefix  Well-known name prefix that application is interested in receiving BusListener::FoundAdvertisedName
     *                    notifications about.
     *
     * @return 
     * <ul>
     * <li>OK if discovery was started.</li>  
     * <li>BUS_NOT_CONNECTED if a connection has not been made with a local bus</li>
     * <li>other error status codes indicating a failure.</li>
     * </ul>
     */
    public native Status findAdvertisedName(String namePrefix);

    /**
     * Cancel interest in a well-known name prefix that was previously
     * registered with FindAdvertisedName.  This method is a shortcut/helper
     * that issues an org.codeauora.AllJoyn.Bus.CancelFindAdvertisedName method
     * call to the local daemon and interprets the response.
     *
     * @param namePrefix  Well-known name prefix that application is no longer interested in receiving
     *                    BusListener::FoundAdvertisedName notifications about.
     *
     * @return 
     * <ul>
     * <li>OK if discovery was cancelled.</li>
     * <li>BUS_NOT_CONNECTED if a connection has not been made with a local bus</li>
     * <li>other error status codes indicating a general failure condition.</li>
     * </ul>
     */
    public native Status cancelFindAdvertisedName(String namePrefix);

    /**
     * Make a SessionPort available for external BusAttachments to join.
     *
     * Each BusAttachment binds its own set of SessionPorts. Session joiners use
     * the bound session port along with the name of the attachement to create a
     * persistent logical connection (called a Session) with the original
     * BusAttachment.
     *
     * A SessionPort and bus name form a unique identifier that BusAttachments
     * use when joining a session.  SessionPort values can be pre-arranged
     * between AllJoyn services and their clients (well-known SessionPorts).
     *
     * Once a session is joined using one of the service's well-known
     * SessionPorts, the service may bind additional SessionPorts (dyanamically)
     * and share these SessionPorts with the joiner over the original
     * session. The joiner can then create additional sessions with the service
     * by calling JoinSession with these dynamic SessionPort ids.
     *
     * @param sessionPort SessionPort value to bind or SESSION_PORT_ANY to allow
     *                    this method to choose an available port. On successful
     *                    return, this value contains the chosen SessionPort.
     *
     * @param opts        Session options that joiners must agree to in order to                                          
     *                    successfully join the session.
     *
     * @param listener    SessionPortListener that will be notified via callback
     *                    when a join attempt is made on the bound session port.
     *
     * @return 
     * <ul>
     * <li>OK if the new session port was bound.</li>
     * <li>BUS_NOT_CONNECTED if a connection has not been made with a local bus</li>
     * <li>other error status codes indicating a failure.</li>
     * <ul>
     */
    public native Status bindSessionPort(Mutable.ShortValue sessionPort,
                                         SessionOpts sessionOpts,
                                         SessionPortListener listener);

    /** 
     * When passed to BindSessionPort as the requested port, the system will
     * assign an ephemeral session port
     */
    public static final short SESSION_PORT_ANY = 0;

    /**
     * When passed to ProxyBusObject, the system will use any available connection.
     */
    public static final int SESSION_ID_ANY = 0;

    /**
     * Cancel an existing port binding.
     *
     * @param   sessionPort    Existing session port to be un-bound.
     *
     * @return
     * <ul>
     * <li>OK if the session port was unbound.</li>
     * <li>BUS_NOT_CONNECTED if connection has not been made with the local daemon.</li>
     * <li>other error status codes indicating a failure</li> 
     */
    public native Status unbindSessionPort(Mutable.ShortValue sessionPort);

    /**
     * Join a session.
     *
     * This method is a shortcut/helper that issues an
     * org.codeauora.AllJoyn.Bus.JoinSession method call to the local daemon and
     * interprets the response.
     *
     * @param sessionHost   Bus name of attachment that is hosting the session to be joined.
     * @param sessionPort   SessionPort of sessionHost to be joined.                                                           
     * @param sessionId     Set to the unique identifier for session.
     * @param opts          Set to the actual session options of the joined session.
     * @param listener      Listener to be called when session related asynchronous 
     *                      events occur.
     *
     * @return
     * <ul> 
     * <li>OK if the session was joined.</li>
     * <li>BUS_NOT_CONNECTED if a connection has not been made with a local bus</li>
     * <li>other error status codes indicating a failure.</li>
     * </ul>                                                                                 
     */
    public native Status joinSession(String sessionHost,
                                     short sessionPort,
                                     Mutable.IntegerValue sessionId,
                                     SessionOpts opts,
                                     SessionListener listener);

    /**
     * Leave an existing session.
     *
     * This method is a shortcut/helper that issues an
     * org.codeauora.AllJoyn.Bus.LeaveSession method call to the local daemon
     * and interprets the response.
     *
     * @param sessionId     Session id.
     *
     * @return 
     * <ul>
     * <li>OK if daemon response was left.</li>  
     * <li>BUS_NOT_CONNECTED if a connection has not been made with a local bus</li>
     * <li>other error status codes indicating failures.</li>
     * </ul>                                                                        
     */
    public native Status leaveSession(int sessionId);

    /**
     * Set the SessionListener for an existing session.
     *
     * Calling this method will override (replace) the listener set by a previoius call to
     * setSessionListener or a listener specified in joinSession.
     *
     * @param sessionId    The session id of an existing session.
     * @param listener     The SessionListener to associate with the session. May be null to clear previous listener.
     * @return  ER_OK if successful.
     */
    public native Status setSessionListener(int sessionId, SessionListener listener);
    
    /**
     * Get the file descriptor for a raw (non-message based) session.
     *
     * @param sessionId  Id of an existing streamming session.
     * @param sockFd     Socket file descriptor for session.
     *
     * @return Status.OK if the socket FD was returned.  ER_BUS_NOT_CONNECTED if a
     *         connection has not been made with a local bus; other error status
     *         codes indicating a failure.                                                                        
     */
    public native Status getSessionFd(int sessionId, Mutable.IntegerValue sockFd);

    /**
     * This sets the debug level of the local AllJoyn daemon if that daemon
     * was built in debug mode.
     *
     * The debug level can be set for individual subsystems or for "ALL"
     * subsystems.  Common subsystems are "ALLJOYN" for core AllJoyn code,
     * "ALLJOYN_OBJ" for the sessions management code, "ALLJOYN_BT" for the
     * Bluetooth subsystem, "ALLJOYN_BTC" for the Bluetooth topology manager,
     * and "ALLJOYN_NS" for the TCP name services.  Debug levels for specific
     * subsystems override the setting for "ALL" subsystems.  For example if
     * "ALL" is set to 7, but "ALLJOYN_OBJ" is set to 1, then detailed debug
     * output will be generated for all subsystems expcept for "ALLJOYN_OBJ"
     * which will only generate high level debug output.  "ALL" defaults to 0
     * which is off, or no debug output.
     *
     * The debug output levels are actually a bit field that controls what
     * output is generated.  Those bit fields are described below:
     *<ul>
     *     <li>0x1: High level debug prints (these debug prints are not common)</li>
     *     <li>0x2: Normal debug prints (these debug prints are common)</li>
     *     <li>0x4: Function call tracing (these debug prints are used
     *            sporadically)</li>
     *     <li>0x8: Data dump (really only used in the "SOCKET" module - can
     *            generate a <strong>lot</strong> of output)</li>
     * </ul>
     *
     * Typically, when enabling debug for a subsystem, the level would be set
     * to 7 which enables High level debug, normal debug, and function call
     * tracing.  Setting the level 0, forces debug output to be off for the
     * specified subsystem.
     *
     * @param module  the name of the module to generate debug output from.
     * @param level   the debug level to set for the module.
     *
     * @return 
     * <ul>
     * <li>OK if debug request was successfully sent to the AllJoyn daemon</li> 
     * <li>BUS_NO_SUCH_OBJECT if daemon was not built in debug mode.</li>
     * </ul>
     */
    public native Status setDaemonDebug(String module, int level);

    /**
     * Set AllJoyn logging levels.
     *
     * @param logEnv    A semicolon separated list of KEY=VALUE entries used
     *                  to set the log levels for internal AllJoyn modules.
     *                  (i.e. ALLJOYN=7;ALL=1)
     */
    public native void setLogLevels(String logEnv);

    /**
     * Set AllJoyn debug levels.
     *
     * @param module    name of the module to generate debug output
     * @param level     debug level to set for the module
     */
    public native void setDebugLevel(String module, int level);

   /**
    * Indicate whether AllJoyn logging goes to OS logger or stdout
    *
    * @param  useOSLog   true iff OS specific logging should be used rather than print for AllJoyn debug messages.
    */
    public native void useOSLogging(boolean useOSLog);

    /**
     * Register an object that will receive bus event notifications.
     *
     * @param listener  Object instance that will receive bus event notifications.
     */
    public native void registerBusListener(BusListener listener);

    /**
     * unregister an object that was previously registered with RegisterBusListener.
     *
     * @param listener  Object instance to un-register as a listener.
     */
    public native void unregisterBusListener(BusListener listener);

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

        public Credentials requestCredentials(String authMechanism, String authPeer, int authCount,
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

            if (authListener.requested(authMechanism, authPeer, authCount, userName, 
                                       requests.toArray(new AuthRequest[0]))) {
                return credentials;
            }
            return null;
        }

        public boolean verifyCredentials(String authMechanism, String peerName, String userName, 
                                         String cert) throws BusException {
            if (authListener == null) {
                throw new BusException("No registered application AuthListener");
            }
            /*
             * authCount is set to 0 here since it can't be cached from
             * requestCredentials, and it's assumed that the application will
             * not immediately reject a request with an authCount of 0.
             */
            return authListener.requested(authMechanism, peerName, 0, userName == null ? "" : userName,
                                          new AuthRequest[] { new VerifyRequest(cert) });
        }

        public void securityViolation(Status status) {
            if (violationListener != null) {
                violationListener.violated(status);
            }
        }

        public void authenticationComplete(String authMechanism, String peerName,  boolean success) {
            if (authListener != null) {
                authListener.completed(authMechanism, peerName, success);
            }
        }
    }

    private AuthListenerInternal busAuthListener;

    /** End-to-end authentication mechanisms. */
    private String authMechanisms;

    /** Default key store file name. */
    private String keyStoreFileName;

    private Map<String, FindAdvertisedNameListener> findAdvertisedNameListeners;

    private Method foundAdvertisedName;

    private ExecutorService executor;

    private Method lostAdvertisedName;

    private DBusProxyObj dbus;

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
        findAdvertisedNameListeners = Collections.synchronizedMap(new HashMap<String, FindAdvertisedNameListener>());
        try {
            foundAdvertisedName = getClass().getDeclaredMethod(
                "foundAdvertisedName", String.class, Short.class, String.class);
            foundAdvertisedName.setAccessible(true);
            lostAdvertisedName = getClass().getDeclaredMethod(
                "lostAdvertisedName", String.class, Short.class, String.class);
            lostAdvertisedName.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            /* This will not happen */
        }
        create(applicationName, allowRemoteMessages);
        dbus = new ProxyBusObject(this, "org.freedesktop.DBus", "/org/freedesktop/DBus", SESSION_ID_ANY,
                                  new Class[] { DBusProxyObj.class }).getInterface(DBusProxyObj.class);        
        executor = Executors.newSingleThreadExecutor();
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
     * Used to defer listener methods called on a signal handler to
     * another thread.  This allows the listener method to make
     * blocking calls back into the library.
     */
    void execute(Runnable runnable) {
        executor.execute(runnable);
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
            return connect(address, keyStoreListener, authMechanisms, busAuthListener, keyStoreFileName);
        } else {
            return Status.INVALID_CONNECT_ARGS;
        }
    }

    /**
     * Disconnects from the local daemon and stops the message bus.
     */
    public void disconnect() {
        if (address != null) {
            unregisterSignalHandler(this, foundAdvertisedName);
            unregisterSignalHandler(this, lostAdvertisedName);
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
     * Unregisters a bus object.
     *
     * @param obj the BusObject to unregister
     */
    public native void unregisterBusObject(BusObject obj);

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
     * @param sessionId the session corresponding to the connection to the the object
     * @param busInterfaces an array of BusInterfaces that this proxy should respond to
     * @return a ProxyBusObject for an object that implements all interfaces listed in busInterfaces
     * @see org.alljoyn.bus.annotation.BusMethod
     */
    public ProxyBusObject getProxyBusObject(String busName,
                                            String objPath,
                                            int sessionId,
                                            Class[] busInterfaces) {
        return new ProxyBusObject(this, busName, objPath, sessionId, busInterfaces);
    }

    /**
     * Gets the DBusProxyObj interface of the org.freedesktop.DBus proxy object.
     * The DBusProxyObj interface is provided for backwards compatibility with
     * the DBus protocol.
     *
     * @return the DBusProxyObj interface
     */
    public DBusProxyObj getDBusProxyObj() {
        return dbus;
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
                    ifaceName = InterfaceDescription.getName(iface);
                    try {
                        Method signal = iface.getMethod(signalName, handlerMethod.getParameterTypes());
                        signalName = InterfaceDescription.getName(signal);
                    } catch (NoSuchMethodException ex) {
                        // Ignore, use signalName parameter provided 
                    }
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
     * @return <ul>
     *         <li>OK if the register is succesful
     *         <li>BUS_NO_SUCH_INTERFACE if the interface and signal
     *         specified in any {@code @BusSignalHandler} annotations
     *         of {@code obj} are unknown to this BusAttachment.  See
     *         {@link org.alljoyn.bus.annotation.BusSignalHandler} for
     *         a discussion of how to annotate signal handlers.
     *         </ul>
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
     * Unregisters a signal handler.
     *
     * @param obj the object receiving the signal
     * @param handlerMethod the signal handler method
     */
    public native void unregisterSignalHandler(Object obj, Method handlerMethod);

    /**
     * Unregisters all public methods annotated as signal handlers.
     *
     * @param obj object with previously annotated signal handlers that have
     *            been registered
     * @see org.alljoyn.bus.annotation.BusSignalHandler
     */
    public void unregisterSignalHandlers(Object obj) {
        for (Method m : obj.getClass().getMethods()) {
            BusSignalHandler a = m.getAnnotation(BusSignalHandler.class);
            if (a != null) {
                unregisterSignalHandler(obj, m);
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

}
