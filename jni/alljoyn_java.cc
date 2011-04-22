/******************************************************************************
 * Copyright 2010 - 2011, Qualcomm Innovation Center, Inc.
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
 *
 ******************************************************************************/
#include <jni.h>
#include <stdio.h>
#include <assert.h>
#include <map>
#include <qcc/Debug.h>
#include <qcc/Log.h>
#include <qcc/ManagedObj.h>
#include <qcc/Mutex.h>
#include <qcc/String.h>
#include <qcc/Thread.h>
#include <alljoyn/BusAttachment.h>
#include <alljoyn/DBusStd.h>
#include <MsgArgUtils.h>
#include <SignatureUtils.h>
#include "alljoyn_java.h"

#define QCC_MODULE "ALLJOYN_JAVA"

using namespace std;
using namespace qcc;
using namespace ajn;

// TODO: Cache IDs - not sure if the non java/lang ones are valid all the time

/** The cached JVM pointer, valid across all contexts. */
static JavaVM* jvm = NULL;

/** java/lang cached items - these are guaranteed to be loaded at all times. */
static jclass CLS_Object = NULL;
static jclass CLS_String = NULL;

/** org/alljoyn/bus */
static jclass CLS_BusException = NULL;
static jclass CLS_ErrorReplyBusException = NULL;
static jclass CLS_IntrospectionListener = NULL;
static jclass CLS_BusObjectListener = NULL;
static jclass CLS_MessageContext = NULL;
static jclass CLS_MsgArg = NULL;
static jclass CLS_Signature = NULL;
static jclass CLS_Status = NULL;
static jclass CLS_Variant = NULL;
static jclass CLS_BusAttachment = NULL;
static jclass CLS_SessionOpts = NULL;

static jmethodID MID_Object_equals = NULL;
static jmethodID MID_BusException_log = NULL;
static jmethodID MID_MsgArg_marshal = NULL;
static jmethodID MID_MsgArg_marshal_array = NULL;
static jmethodID MID_MsgArg_unmarshal = NULL;
static jmethodID MID_MsgArg_unmarshal_array = NULL;

/**
 * @return The JNIEnv pointer valid in the calling context.
 */
static JNIEnv* GetEnv(jint* result = 0)
{
    JNIEnv* env;
    jint ret = jvm->GetEnv((void**)&env, JNI_VERSION_1_2);
    if (result) {
        *result = ret;
    }
    if (JNI_EDETACHED == ret) {
#if defined(QCC_OS_ANDROID)
        ret = jvm->AttachCurrentThread(&env, NULL);
#else
        ret = jvm->AttachCurrentThread((void**)&env, NULL);
#endif
    }
    assert(JNI_OK == ret);
    return env;
}

static void DeleteEnv(jint result)
{
    if (JNI_EDETACHED == result) {
        jvm->DetachCurrentThread();
    }
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm,
                                  void* reserved)
{
    QCC_UseOSLogging(true);
    jvm = vm;
    JNIEnv* env;
    if (jvm->GetEnv((void**)&env, JNI_VERSION_1_2)) {
        return JNI_ERR;
    } else {
        jclass clazz;
        clazz = env->FindClass("java/lang/Object");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_Object = (jclass)env->NewGlobalRef(clazz);
        MID_Object_equals = env->GetMethodID(CLS_Object, "equals", "(Ljava/lang/Object;)Z");
        if (!MID_Object_equals) {
            return JNI_ERR;
        }

        clazz = env->FindClass("java/lang/String");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_String = (jclass)env->NewGlobalRef(clazz);

        /** org/alljoyn/bus */
        clazz = env->FindClass("org/alljoyn/bus/BusException");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_BusException = (jclass)env->NewGlobalRef(clazz);
        MID_BusException_log = env->GetStaticMethodID(CLS_BusException, "log", "(Ljava/lang/Throwable;)V");
        if (!MID_BusException_log) {
            return JNI_ERR;
        }
        clazz = env->FindClass("org/alljoyn/bus/ErrorReplyBusException");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_ErrorReplyBusException = (jclass)env->NewGlobalRef(clazz);
        clazz = env->FindClass("org/alljoyn/bus/IntrospectionListener");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_IntrospectionListener = (jclass)env->NewGlobalRef(clazz);
        clazz = env->FindClass("org/alljoyn/bus/BusObjectListener");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_BusObjectListener = (jclass)env->NewGlobalRef(clazz);
        clazz = env->FindClass("org/alljoyn/bus/MsgArg");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_MsgArg = (jclass)env->NewGlobalRef(clazz);
        MID_MsgArg_marshal = env->GetStaticMethodID(CLS_MsgArg, "marshal",
                                                    "(JLjava/lang/String;Ljava/lang/Object;)V");
        if (!MID_MsgArg_marshal) {
            return JNI_ERR;
        }
        MID_MsgArg_marshal_array = env->GetStaticMethodID(CLS_MsgArg, "marshal",
                                                          "(JLjava/lang/String;[Ljava/lang/Object;)V");
        if (!MID_MsgArg_marshal_array) {
            return JNI_ERR;
        }
        MID_MsgArg_unmarshal = env->GetStaticMethodID(CLS_MsgArg, "unmarshal",
                                                      "(JLjava/lang/reflect/Type;)Ljava/lang/Object;");
        if (!MID_MsgArg_unmarshal) {
            return JNI_ERR;
        }
        MID_MsgArg_unmarshal_array = env->GetStaticMethodID(CLS_MsgArg, "unmarshal",
                                                            "(Ljava/lang/reflect/Method;J)[Ljava/lang/Object;");
        if (!MID_MsgArg_unmarshal_array) {
            return JNI_ERR;
        }
        clazz = env->FindClass("org/alljoyn/bus/MessageContext");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_MessageContext = (jclass)env->NewGlobalRef(clazz);
        clazz = env->FindClass("org/alljoyn/bus/Signature");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_Signature = (jclass)env->NewGlobalRef(clazz);
        clazz = env->FindClass("org/alljoyn/bus/Status");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_Status = (jclass)env->NewGlobalRef(clazz);
        clazz = env->FindClass("org/alljoyn/bus/Variant");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_Variant = (jclass)env->NewGlobalRef(clazz);
        clazz = env->FindClass("org/alljoyn/bus/BusAttachment");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_BusAttachment = (jclass)env->NewGlobalRef(clazz);
        clazz = env->FindClass("org/alljoyn/bus/SessionOpts");
        if (!clazz) {
            return JNI_ERR;
        }
        CLS_SessionOpts = (jclass)env->NewGlobalRef(clazz);

        return JNI_VERSION_1_2;
    }
}

/**
 * Wrap local references to ensure proper release.
 */
template <class T> class JLocalRef {
  public:
    JLocalRef() : jobj(NULL) { }
    JLocalRef(const T& obj) : jobj(obj) { }
    ~JLocalRef() { if (jobj) GetEnv()->DeleteLocalRef(jobj); }
    JLocalRef& operator=(T obj)
    {
        if (jobj) GetEnv()->DeleteLocalRef(jobj);
        jobj = obj;
        return *this;
    }
    operator T() { return jobj; }
    T move()
    {
        T ret = jobj;
        jobj = NULL;
        return ret;
    }
  private:
    T jobj;
};

/**
 * Scoped JNIEnv pointer to ensure proper release.
 */
class JScopedEnv {
  public:
    JScopedEnv();
    ~JScopedEnv();
    JNIEnv* operator->() { return env; }
  private:
    JNIEnv* env;
    jint detached;
};

JScopedEnv::JScopedEnv()
    : env(GetEnv(&detached))
{
}

JScopedEnv::~JScopedEnv()
{
    /* Clear any pending exceptions before detaching. */
    {
        JLocalRef<jthrowable> ex = env->ExceptionOccurred();
        if (ex) {
            env->ExceptionClear();
            env->CallStaticVoidMethod(CLS_BusException, MID_BusException_log, (jthrowable)ex);
        }
    }
    DeleteEnv(detached);
}

/**
 * Wrap StringUTFChars to ensure proper release of resource.  NULL is
 * a valid value, so exceptions must be checked for explicitly by the
 * caller after constructing the JString.
 */
class JString {
  public:
    JString(jstring s);
    ~JString();
    const char* c_str() { return str; }
  private:
    jstring jstr;
    const char* str;
};

JString::JString(jstring s)
    : jstr(s), str(jstr ? GetEnv()->GetStringUTFChars(jstr, NULL) : NULL)
{
}

JString::~JString()
{
    if (str) GetEnv()->ReleaseStringUTFChars(jstr, str);
}

static void Throw(const char* name, const char* msg)
{
    JNIEnv* env = GetEnv();
    JLocalRef<jclass> clazz = env->FindClass(name);
    if (clazz) {
        env->ThrowNew(clazz, msg);
    }
}

static void ThrowErrorReplyBusException(const char* name, const char* message)
{
    JNIEnv* env = GetEnv();
    JLocalRef<jstring> jname = env->NewStringUTF(name);
    if (!jname) {
        return;
    }
    JLocalRef<jstring> jmessage = env->NewStringUTF(message);
    if (!jmessage) {
        return;
    }
    jmethodID mid = env->GetMethodID(CLS_ErrorReplyBusException, "<init>",
                                     "(Ljava/lang/String;Ljava/lang/String;)V");
    JLocalRef<jthrowable> jexc = (jthrowable)env->NewObject(CLS_ErrorReplyBusException, mid,
                                                            (jstring)jname, (jstring)jmessage);
    if (jexc) {
        env->Throw(jexc);
    }
}

/**
 * @return The handle value as a pointer.  NULL is a valid value, so
 *         exceptions must be checked for explicitly by the caller.
 */
static void* GetHandle(jobject jobj)
{
    JNIEnv* env = GetEnv();
    if (!jobj) {
        Throw("java/lang/NullPointerException", "failed to get native handle on null object");
        return NULL;
    }
    JLocalRef<jclass> clazz = env->GetObjectClass(jobj);
    jfieldID fid = env->GetFieldID(clazz, "handle", "J");
    void* handle = NULL;
    if (fid) {
        handle = (void*)env->GetLongField(jobj, fid);
    }
    return handle;
}

/**
 * May throw an exception.
 */
static void SetHandle(jobject jobj, void* handle)
{
    JNIEnv* env = GetEnv();
    if (!jobj) {
        Throw("java/lang/NullPointerException", "failed to set native handle on null object");
        return;
    }
    JLocalRef<jclass> clazz = env->GetObjectClass(jobj);
    jfieldID fid = env->GetFieldID(clazz, "handle", "J");
    if (fid) {
        env->SetLongField(jobj, fid, (jlong)handle);
    }
}

/**
 * @return A org.alljoyn.bus.Status enum value from the QStatus.
 */
static jobject JStatus(QStatus status)
{
    JNIEnv* env = GetEnv();
    jmethodID mid = env->GetStaticMethodID(CLS_Status, "create", "(I)Lorg/alljoyn/bus/Status;");
    if (!mid) {
        return NULL;
    }
    return env->CallStaticObjectMethod(CLS_Status, mid, status);
}

/*
 * class org_alljoyn_bus_BusAttachment
 */

class MessageContext {
  public:
    static Message GetMessage();
    MessageContext(const Message& msg);
    ~MessageContext();
  private:
    static map<Thread*, Message> messages;
    Mutex lock;
};

map<Thread*, Message> MessageContext::messages;

Message MessageContext::GetMessage()
{
    map<Thread*, Message>::iterator it = messages.find(Thread::GetThread());
    assert(messages.end() != it);
    return it->second;
}

MessageContext::MessageContext(const Message& msg)
{
    lock.Lock();
    messages.insert(pair<Thread*, Message>(Thread::GetThread(), msg));
    lock.Unlock();
}

MessageContext::~MessageContext()
{
    lock.Lock();
    map<Thread*, Message>::iterator it = messages.find(Thread::GetThread());
    messages.erase(it);
    lock.Unlock();
}

class JKeyStoreListener : public KeyStoreListener {
  public:
    JKeyStoreListener(jobject jlistener);
    ~JKeyStoreListener();
    QStatus LoadRequest(KeyStore& keyStore);
    QStatus StoreRequest(KeyStore& keyStore);
  private:
    jweak jkeyStoreListener;
    jmethodID MID_getKeys;
    jmethodID MID_getPassword;
    jmethodID MID_putKeys;
    jmethodID MID_encode;
};

JKeyStoreListener::JKeyStoreListener(jobject jlistener)
    : jkeyStoreListener(NULL)
{
    JNIEnv* env = GetEnv();
    jkeyStoreListener = (jweak)env->NewGlobalRef(jlistener);
    if (!jkeyStoreListener) {
        return;
    }
    JLocalRef<jclass> clazz = env->GetObjectClass(jkeyStoreListener);
    MID_getKeys = env->GetMethodID(clazz, "getKeys", "()[B");
    if (!MID_getKeys) {
        QCC_DbgPrintf(("JKeyStoreListener::JKeystoreListener(): Can't find getKeys() in jListener\n"));
        return;
    }
    MID_getPassword = env->GetMethodID(clazz, "getPassword", "()[C");
    if (!MID_getPassword) {
        QCC_DbgPrintf(("JKeyStoreListener::JKeystoreListener(): Can't find getPassword() in jListener\n"));
        return;
    }
    MID_putKeys = env->GetMethodID(clazz, "putKeys", "([B)V");
    if (!MID_putKeys) {
        QCC_DbgPrintf(("JKeyStoreListener::JKeystoreListener(): Can't find putKeys() in jListener\n"));
        return;
    }
    MID_encode = env->GetStaticMethodID(CLS_BusAttachment, "encode", "([C)[B");
    if (!MID_encode) {
        QCC_DbgPrintf(("JKeyStoreListener::JKeystoreListener(): Can't find endode() in jListener\n"));
        return;
    }
}

JKeyStoreListener::~JKeyStoreListener()
{
    JNIEnv* env = GetEnv();
    if (jkeyStoreListener) {
        env->DeleteGlobalRef(jkeyStoreListener);
    }
}

QStatus JKeyStoreListener::LoadRequest(KeyStore& keyStore)
{
    JScopedEnv env;
    JLocalRef<jbyteArray> jarray = (jbyteArray)env->CallObjectMethod(jkeyStoreListener, MID_getKeys);
    if (env->ExceptionCheck()) {
        return ER_FAIL;
    }
    String source;
    if (jarray) {
        jsize len = env->GetArrayLength(jarray);
        jbyte* jelements = env->GetByteArrayElements(jarray, NULL);
        if (!jelements) {
            return ER_FAIL;
        }
        source = String((const char*)jelements, len);
        env->ReleaseByteArrayElements(jarray, jelements, JNI_ABORT);
    }
    /*
     * Get the password from the Java listener and load the keys.
     * Some care here is taken to ensure that we erase any in-memory
     * copies of the password as soon as possible after to minimize
     * attack exposure.
     */
    JLocalRef<jcharArray> jpasswordChar = (jcharArray)env->CallObjectMethod(jkeyStoreListener, MID_getPassword);
    if (env->ExceptionCheck() || !jpasswordChar) {
        return ER_FAIL;
    }
    JLocalRef<jbyteArray> jpassword = (jbyteArray)env->CallStaticObjectMethod(CLS_BusAttachment, MID_encode,
                                                                              (jcharArray)jpasswordChar);
    if (env->ExceptionCheck()) {
        return ER_FAIL;
    }
    jchar* passwordChar = env->GetCharArrayElements(jpasswordChar, NULL);
    if (env->ExceptionCheck()) {
        return ER_FAIL;
    }
    memset(passwordChar, 0, env->GetArrayLength(jpasswordChar) * sizeof(jchar));
    env->ReleaseCharArrayElements(jpasswordChar, passwordChar, 0);
    if (!jpassword) {
        return ER_FAIL;
    }
    jbyte* password = env->GetByteArrayElements(jpassword, NULL);
    if (env->ExceptionCheck()) {
        return ER_FAIL;
    }
    QStatus status = LoadKeys(keyStore, source, String((const char*)password, env->GetArrayLength(jpassword)));
    memset(password, 0, env->GetArrayLength(jpassword) * sizeof(jbyte));
    env->ReleaseByteArrayElements(jpassword, password, 0);
    return status;
}

QStatus JKeyStoreListener::StoreRequest(KeyStore& keyStore)
{
    String sink;
    QStatus status = StoreKeys(keyStore, sink);
    if (ER_OK != status) {
        return status;
    }
    JScopedEnv env;
    JLocalRef<jbyteArray> jarray = env->NewByteArray(sink.size());
    if (!jarray) {
        return ER_FAIL;
    }
    env->SetByteArrayRegion(jarray, 0, sink.size(), (jbyte*)sink.data());
    if (env->ExceptionCheck()) {
        return ER_FAIL;
    }
    env->CallVoidMethod(jkeyStoreListener, MID_putKeys, (jbyteArray)jarray);
    if (env->ExceptionCheck()) {
        return ER_FAIL;
    }
    return ER_OK;
}

class JBusListener : public BusListener {
  public:
    JBusListener(jobject jlistener);
    ~JBusListener();

    void ListenerRegistered(BusAttachment* bus) { }
    void ListenerUnregistered() { }
    void FoundAdvertisedName(const char* name, TransportMask transport, const char* namePrefix);
    void LostAdvertisedName(const char* name, TransportMask transport, const char* namePrefix);
    void NameOwnerChanged(const char* busName, const char* previousOwner, const char* newOwner);
    void SessionLost(const SessionId& sessionId);
    bool AcceptSessionJoiner(SessionPort sessionPort, const char* joiner, const SessionOpts& opts);
    void SessionJoined(SessionPort sessionPort, SessionId id, const char* joiner);
    void BusStopping();

  private:
    jweak jbusListener;
    jmethodID MID_foundAdvertisedName;
    jmethodID MID_lostAdvertisedName;
    jmethodID MID_nameOwnerChanged;
    jmethodID MID_sessionLost;
    jmethodID MID_acceptSessionJoiner;
    jmethodID MID_sessionJoined;
    jmethodID MID_busStopping;
};

JBusListener::JBusListener(jobject jlistener)
    : jbusListener(NULL)
{
    QCC_DbgPrintf(("JBusListener::JBusListener()\n"));
    JNIEnv* env = GetEnv();
    jbusListener = (jweak)env->NewGlobalRef(jlistener);
    if (!jbusListener) {
        return;
    }

    JLocalRef<jclass> clazz = env->GetObjectClass(jbusListener);

    MID_foundAdvertisedName = env->GetMethodID(clazz, "foundAdvertisedName", "(Ljava/lang/String;SLjava/lang/String;)V");
    if (!MID_foundAdvertisedName) {
        QCC_DbgPrintf(("JBusListener::JBusListener(): Can't find foundAdvertisedName() in jbusListener\n"));
    }

    MID_lostAdvertisedName = env->GetMethodID(clazz, "lostAdvertisedName", "(Ljava/lang/String;SLjava/lang/String;)V");
    if (!MID_lostAdvertisedName) {
        QCC_DbgPrintf(("JBusListener::JBusListener(): Can't find lostAdvertisedName() in jbusListener\n"));
    }

    MID_nameOwnerChanged = env->GetMethodID(clazz, "nameOwnerChanged", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if (!MID_nameOwnerChanged) {
        QCC_DbgPrintf(("JBusListener::JBusListener(): Can't find nameOwnerChanged() in jbusListener\n"));
    }

    MID_sessionLost = env->GetMethodID(clazz, "sessionLost", "(I)V");
    if (!MID_sessionLost) {
        QCC_DbgPrintf(("JBusListener::JBusListener(): Can't find sessionLost() in jbusListener\n"));
    }

    MID_acceptSessionJoiner = env->GetMethodID(clazz, "acceptSessionJoiner", "(SLjava/lang/String;Lorg/alljoyn/bus/SessionOpts;)Z");
    if (!MID_acceptSessionJoiner) {
        QCC_DbgPrintf(("JBusListener::JBusListener(): Can't find acceptSessionJoiner() in jbusListener\n"));
    }

    MID_sessionJoined = env->GetMethodID(clazz, "sessionJoined", "(SILjava/lang/String;)V");
    if (!MID_sessionJoined) {
        QCC_DbgPrintf(("JBusListener::JBusListener(): Can't find sessionJoined() in jbusListener\n"));
    }

    MID_busStopping = env->GetMethodID(clazz, "busStopping", "()V");
    if (!MID_busStopping) {
        QCC_DbgPrintf(("JBusListener::JBusListener(): Can't find busStopping() in jbusListener\n"));
    }
}

JBusListener::~JBusListener()
{
    JNIEnv* env = GetEnv();
    if (jbusListener) {
        env->DeleteGlobalRef(jbusListener);
    }
}

void JBusListener::FoundAdvertisedName(const char* name, TransportMask transport, const char* namePrefix)
{
    QCC_DbgPrintf(("JBusListener::FoundAdvertisedName()\n"));
    JScopedEnv env;

    //
    // Translate the C++ formal parameters into their JNI counterparts.
    //
    JLocalRef<jstring> jname = env->NewStringUTF(name);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::FoundAdvertisedName(): Exception\n"));
        return;
    }

    jshort jtransport = transport;

    JLocalRef<jstring> jnamePrefix = env->NewStringUTF(namePrefix);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::FoundAdvertisedName(): Exception\n"));
        return;
    }

    QCC_DbgPrintf(("JBusListener::FoundAdvertisedName(): Call out to listener object and method\n"));
    env->CallVoidMethod(jbusListener, MID_foundAdvertisedName, (jstring)jname, jtransport, (jstring)jnamePrefix);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::FoundAdvertisedName(): Exception\n"));
        return;
    }

    QCC_DbgPrintf(("JBusListener::FoundAdvertisedName(): Return\n"));
}

void JBusListener::LostAdvertisedName(const char* name, TransportMask transport, const char* namePrefix)
{
    QCC_DbgPrintf(("JBusListener::LostAdvertisedName()\n"));
    JScopedEnv env;

    //
    // Translate the C++ formal parameters into their JNI counterparts.
    //
    JLocalRef<jstring> jname = env->NewStringUTF(name);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::LostAdvertisedName(): Exception\n"));
        return;
    }

    jshort jtransport = transport;

    JLocalRef<jstring> jnamePrefix = env->NewStringUTF(namePrefix);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::LostAdvertisedName(): Exception\n"));
        return;
    }

    QCC_DbgPrintf(("JBusListener::LostAdvertisedName(): Call out to listener object and method\n"));
    env->CallVoidMethod(jbusListener, MID_lostAdvertisedName, (jstring)jname, jtransport, (jstring)jnamePrefix);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::LostAdvertisedName(): Exception\n"));
        return;
    }

    QCC_DbgPrintf(("JBusListener::LostAdvertisedName(): Return\n"));
}

void JBusListener::NameOwnerChanged(const char* busName, const char* previousOwner, const char* newOwner)
{
    QCC_DbgPrintf(("JBusListener::NameOwnerChanged()\n"));
    JScopedEnv env;

    //
    // Translate the C++ formal parameters into their JNI counterparts.
    //
    JLocalRef<jstring> jbusName = env->NewStringUTF(busName);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::NameOwnerChanged(): Exception\n"));
        return;
    }

    JLocalRef<jstring> jpreviousOwner = env->NewStringUTF(previousOwner);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::NameOwnerChanged(): Exception\n"));
        return;
    }

    JLocalRef<jstring> jnewOwner = env->NewStringUTF(newOwner);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::NameOwnerChanged(): Exception\n"));
        return;
    }

    QCC_DbgPrintf(("JBusListener::NameOwnerChanged(): Call out to listener object and method\n"));
    env->CallVoidMethod(jbusListener, MID_nameOwnerChanged, (jstring)jbusName, (jstring)jpreviousOwner, (jstring)jnewOwner);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::NameOwnerChanged(): Exception\n"));
        return;
    }

    QCC_DbgPrintf(("JBusListener::NameOwnerChanged(): Return\n"));
}

void JBusListener::SessionLost(const SessionId& sessionId)
{
    QCC_DbgPrintf(("JBusListener::SessionLost()\n"));
    JScopedEnv env;

    //
    // Translate the C++ formal parameters into their JNI counterparts.
    //
    jint jsessionId = sessionId;

    QCC_DbgPrintf(("JBusListener::SessionLost(): Call out to listener object and method\n"));
    env->CallVoidMethod(jbusListener, MID_sessionLost, jsessionId);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::SessionLost(): Exception\n"));
        return;
    }

    QCC_DbgPrintf(("JBusListener::SessionLost(): Return\n"));
}

bool JBusListener::AcceptSessionJoiner(SessionPort sessionPort, const char* joiner, const SessionOpts& opts)
{
    QCC_DbgPrintf(("JBusListener::AcceptSessionJoiner()\n"));
    JScopedEnv env;

    JLocalRef<jstring> jjoiner = env->NewStringUTF(joiner);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::AcceptSessionJoiner(): Exception\n"));
        return false;
    }

    jmethodID mid = env->GetMethodID(CLS_SessionOpts, "<init>", "()V");
    if (!mid) {
        QCC_DbgPrintf(("JBusListener::AcceptSessionJoiner(): Can't find SessionOpts.<init>\n"));
        return false;
    }

    QCC_DbgPrintf(("JBusListener::AcceptSessionJoiner(): Create new SessionOpts\n"));
    JLocalRef<jobject> jsessionopts = env->NewObject(CLS_SessionOpts, mid);

    QCC_DbgPrintf(("JBusListener::AcceptSessionJoiner(): Load SessionOpts\n"));
    jfieldID fid = env->GetFieldID(CLS_SessionOpts, "traffic", "B");
    env->SetByteField(CLS_SessionOpts, fid, opts.traffic);

    fid = env->GetFieldID(CLS_SessionOpts, "isMultipoint", "Z");
    env->SetBooleanField(CLS_SessionOpts, fid, opts.isMultipoint);

    fid = env->GetFieldID(CLS_SessionOpts, "proximity", "B");
    env->SetByteField(CLS_SessionOpts, fid, opts.proximity);

    fid = env->GetFieldID(CLS_SessionOpts, "transports", "S");
    env->SetShortField(CLS_SessionOpts, fid, opts.transports);

    QCC_DbgPrintf(("JBusListener::AcceptSessionJoiner(): Call out to listener object and method\n"));
    bool result = env->CallBooleanMethod(jbusListener, MID_acceptSessionJoiner,
                                         sessionPort, (jstring)jjoiner, (jobject)jsessionopts);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::AcceptSessionJoiner(): Exception\n"));
        return false;
    }

    QCC_DbgPrintf(("JBusListener::AcceptSessionJoiner(): Return result %d\n", result));
    return result;
}

void JBusListener::SessionJoined(SessionPort sessionPort, SessionId id, const char* joiner)
{
    QCC_DbgPrintf(("JBusListener::SessionJoined()\n"));
    JScopedEnv env;

    JLocalRef<jstring> jjoiner = env->NewStringUTF(joiner);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::SessionJoined(): Exception\n"));
    }

    QCC_DbgPrintf(("JBusListener::AcceptSessionJoiner(): Call out to listener object and method\n"));
    env->CallVoidMethod(jbusListener, MID_sessionJoined, sessionPort, id, (jstring)jjoiner);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::SessionJoined(): Exception\n"));
        return;
    }
}

void JBusListener::BusStopping(void)
{
    QCC_DbgPrintf(("JBusListener::BusStopping()\n"));
    JScopedEnv env;

    QCC_DbgPrintf(("JBusListener::BusStopping(): Call out to listener object and method\n"));
    env->CallVoidMethod(jbusListener, MID_sessionLost);
    if (env->ExceptionCheck()) {
        QCC_DbgPrintf(("JBusListener::BusStopping(): Exception\n"));
        return;
    }

    QCC_DbgPrintf(("JBusListener::BusStopping(): Return\n"));
}

class JAuthListener : public AuthListener {
  public:
    JAuthListener(jobject jlistener);
    ~JAuthListener();
    bool RequestCredentials(const char* authMechanism, const char* authPeer, uint16_t authCount,
                            const char* userName, uint16_t credMask, Credentials& credentials);
    bool VerifyCredentials(const char* authMechanism, const char* peerName, const Credentials& credentials);
    void SecurityViolation(QStatus status, const Message& msg);
    void AuthenticationComplete(const char* authMechanism, const char* peerName, bool success);
  private:
    jweak jauthListener;
    jmethodID MID_requestCredentials;
    jmethodID MID_verifyCredentials;
    jmethodID MID_securityViolation;
    jmethodID MID_authenticationComplete;
};

JAuthListener::JAuthListener(jobject jlistener)
    : jauthListener(NULL)
{
    JNIEnv* env = GetEnv();
    jauthListener = (jweak)env->NewGlobalRef(jlistener);
    if (!jauthListener) {
        return;
    }
    JLocalRef<jclass> clazz = env->GetObjectClass(jauthListener);
    MID_requestCredentials = env->GetMethodID(clazz, "requestCredentials",
                                              "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;I)Lorg/alljoyn/bus/AuthListener$Credentials;");
    if (!MID_requestCredentials) {
        QCC_DbgPrintf(("JAuthListener::JAuthListener(): Can't find requestCredentials() in jListener\n"));
        return;
    }
    MID_verifyCredentials = env->GetMethodID(clazz, "verifyCredentials", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z");
    if (!MID_verifyCredentials) {
        QCC_DbgPrintf(("JAuthListener::JAuthListener(): Can't find verifyCredentials() in jListener\n"));
        return;
    }
    MID_securityViolation = env->GetMethodID(clazz, "securityViolation", "(Lorg/alljoyn/bus/Status;)V");
    if (!MID_securityViolation) {
        QCC_DbgPrintf(("JAuthListener::JAuthListener(): Can't find securityViolation() in jListener\n"));
        return;
    }
    MID_authenticationComplete = env->GetMethodID(clazz, "authenticationComplete", "(Ljava/lang/String;Ljava/lang/String;Z)V");
    if (!MID_authenticationComplete) {
        QCC_DbgPrintf(("JAuthListener::JAuthListener(): Can't find authenticationComplete() in jListener\n"));
        return;
    }
}

JAuthListener::~JAuthListener()
{
    JNIEnv* env = GetEnv();
    if (jauthListener) {
        env->DeleteGlobalRef(jauthListener);
    }
}

bool JAuthListener::RequestCredentials(const char* authMechanism, const char* authPeer, uint16_t authCount,
                                       const char* userName, uint16_t credMask, Credentials& credentials)
{
    JScopedEnv env;
    JLocalRef<jstring> jauthMechanism = env->NewStringUTF(authMechanism);
    if (env->ExceptionCheck()) {
        return false;
    }
    JLocalRef<jstring> jauthPeer = env->NewStringUTF(authPeer);
    if (env->ExceptionCheck()) {
        return false;
    }
    JLocalRef<jstring> juserName = env->NewStringUTF(userName);
    if (env->ExceptionCheck()) {
        return false;
    }
    JLocalRef<jobject> jcredentials = env->CallObjectMethod(jauthListener, MID_requestCredentials,
                                                            (jstring)jauthMechanism,
                                                            (jstring)jauthPeer,
                                                            authCount,
                                                            (jstring)juserName,
                                                            credMask);
    if (env->ExceptionCheck()) {
        return false;
    }
    if (!jcredentials) {
        return false;
    }
    JLocalRef<jclass> clazz = env->GetObjectClass(jcredentials);

    jfieldID fid = env->GetFieldID(clazz, "password", "[B");
    if (!fid) {
        return false;
    }
    JLocalRef<jbyteArray> jpassword = (jbyteArray)env->GetObjectField(jcredentials, fid);
    if (env->ExceptionCheck()) {
        return false;
    }
    if (jpassword) {
        jbyte* password = env->GetByteArrayElements(jpassword, NULL);
        if (env->ExceptionCheck()) {
            return false;
        }
        credentials.SetPassword(String((const char*)password, env->GetArrayLength(jpassword)));
        memset(password, 0, env->GetArrayLength(jpassword) * sizeof(jbyte));
        env->ReleaseByteArrayElements(jpassword, password, 0);
    }

    fid = env->GetFieldID(clazz, "userName", "Ljava/lang/String;");
    if (!fid) {
        return false;
    }
    juserName = (jstring)env->GetObjectField(jcredentials, fid);
    if (env->ExceptionCheck()) {
        return false;
    }
    if (juserName) {
        JString userName(juserName);
        credentials.SetUserName(userName.c_str());
    }
    fid = env->GetFieldID(clazz, "certificateChain", "Ljava/lang/String;");
    if (!fid) {
        return false;
    }
    JLocalRef<jstring> jcertificate = (jstring)env->GetObjectField(jcredentials, fid);
    if (env->ExceptionCheck()) {
        return false;
    }
    if (jcertificate) {
        JString certificate(jcertificate);
        credentials.SetCertChain(certificate.c_str());
    }
    fid = env->GetFieldID(clazz, "privateKey", "Ljava/lang/String;");
    if (!fid) {
        return false;
    }
    JLocalRef<jstring> jprivateKey = (jstring)env->GetObjectField(jcredentials, fid);
    if (env->ExceptionCheck()) {
        return false;
    }
    if (jprivateKey) {
        JString privateKey(jprivateKey);
        credentials.SetPrivateKey(privateKey.c_str());
    }

    fid = env->GetFieldID(clazz, "logonEntry", "[B");
    if (!fid) {
        return false;
    }
    JLocalRef<jbyteArray> jlogonEntry = (jbyteArray)env->GetObjectField(jcredentials, fid);
    if (env->ExceptionCheck()) {
        return false;
    }
    if (jlogonEntry) {
        jbyte* logonEntry = env->GetByteArrayElements(jlogonEntry, NULL);
        if (env->ExceptionCheck()) {
            return false;
        }
        credentials.SetLogonEntry(String((const char*)logonEntry, env->GetArrayLength(jlogonEntry)));
        memset(logonEntry, 0, env->GetArrayLength(jlogonEntry) * sizeof(jbyte));
        env->ReleaseByteArrayElements(jlogonEntry, logonEntry, 0);
    }

    if (env->ExceptionCheck()) {
        return false;
    }
    return true;
}

bool JAuthListener::VerifyCredentials(const char* authMechanism, const char* authPeer, const Credentials& credentials)
{
    JScopedEnv env;
    JLocalRef<jstring> jauthMechanism = env->NewStringUTF(authMechanism);
    if (env->ExceptionCheck()) {
        return false;
    }
    JLocalRef<jstring> jauthPeer = env->NewStringUTF(authPeer);
    if (env->ExceptionCheck()) {
        return false;
    }
    JLocalRef<jstring> juserName = credentials.IsSet(AuthListener::CRED_USER_NAME) ?
                                   env->NewStringUTF(credentials.GetUserName().c_str()) : NULL;
    if (env->ExceptionCheck()) {
        return false;
    }
    JLocalRef<jstring> jcert = credentials.IsSet(AuthListener::CRED_CERT_CHAIN) ?
                               env->NewStringUTF(credentials.GetCertChain().c_str()) : NULL;
    if (env->ExceptionCheck()) {
        return false;
    }
    jboolean acceptable = env->CallBooleanMethod(jauthListener, MID_verifyCredentials, (jstring)jauthMechanism,
                                                 (jstring)jauthPeer, (jstring)juserName, (jstring)jcert);
    if (env->ExceptionCheck()) {
        return false;
    }
    return acceptable;
}

void JAuthListener::SecurityViolation(QStatus status, const Message& msg)
{
    JScopedEnv env;
    MessageContext context(msg);
    JLocalRef<jobject> jstatus = JStatus(status);
    if (env->ExceptionCheck()) {
        return;
    }
    env->CallVoidMethod(jauthListener, MID_securityViolation, (jobject)jstatus);
}

void JAuthListener::AuthenticationComplete(const char* authMechanism, const char* authPeer,  bool success)
{
    JScopedEnv env;
    JLocalRef<jstring> jauthMechanism = env->NewStringUTF(authMechanism);
    if (env->ExceptionCheck()) {
        return;
    }
    JLocalRef<jstring> jauthPeer = env->NewStringUTF(authPeer);
    if (env->ExceptionCheck()) {
        return;
    }
    env->CallVoidMethod(jauthListener, MID_authenticationComplete, (jstring)jauthMechanism, (jstring)jauthPeer, success);
}

class JBusObject : public BusObject {
  public:
    JBusObject(BusAttachment& bus, const char* path, jobject jobj);
    ~JBusObject();
    bool IsSameObject(jobject jobj);
    QStatus AddInterfaces(jobjectArray jbusInterfaces);
    void MethodHandler(const InterfaceDescription::Member* member, Message& msg);
    QStatus MethodReply(const InterfaceDescription::Member* member, Message& msg, QStatus status);
    QStatus MethodReply(const InterfaceDescription::Member* member, Message& msg, jobject jreply);
    QStatus Signal(const char* destination, SessionId sessionId, const char* ifaceName, const char* signalName,
                   const MsgArg* args, size_t numArgs, uint32_t timeToLive, uint8_t flags);
    QStatus Get(const char* ifcName, const char* propName, MsgArg& val);
    QStatus Set(const char* ifcName, const char* propName, MsgArg& val);
    String GenerateIntrospection(bool deep = false, size_t indent = 0) const;
    void ObjectRegistered();
    void ObjectUnregistered();
  private:
    struct Property {
        String signature;
        jobject jget;
        jobject jset;
    };
    typedef map<String, jobject> JMethod;
    typedef map<String, Property> JProperty;
    jweak jbusObj;
    jmethodID MID_generateIntrospection;
    jmethodID MID_registered;
    jmethodID MID_unregistered;
    JMethod methods;
    JProperty properties;
};

JBusObject::JBusObject(BusAttachment& bus, const char* path, jobject jobj)
    : BusObject(bus, path), jbusObj(NULL), MID_generateIntrospection(NULL), MID_registered(NULL), MID_unregistered(NULL)
{
    JNIEnv* env = GetEnv();
    jbusObj = (jweak)env->NewGlobalRef(jobj);
    if (!jbusObj) {
        return;
    }
    if (env->IsInstanceOf(jbusObj, CLS_IntrospectionListener)) {
        JLocalRef<jclass> clazz = env->GetObjectClass(jbusObj);
        MID_generateIntrospection = env->GetMethodID(clazz, "generateIntrospection", "(ZI)Ljava/lang/String;");
        if (!MID_generateIntrospection) {
            return;
        }
    }
    if (env->IsInstanceOf(jbusObj, CLS_BusObjectListener)) {
        JLocalRef<jclass> clazz = env->GetObjectClass(jbusObj);
        MID_registered = env->GetMethodID(clazz, "registered", "()V");
        if (!MID_registered) {
            return;
        }
        MID_unregistered = env->GetMethodID(clazz, "unregistered", "()V");
        if (!MID_unregistered) {
            return;
        }
    }
}

JBusObject::~JBusObject()
{
    JNIEnv* env = GetEnv();
    for (JMethod::const_iterator method = methods.begin(); method != methods.end(); ++method) {
        env->DeleteGlobalRef(method->second);
    }
    for (JProperty::const_iterator property = properties.begin(); property != properties.end(); ++property) {
        env->DeleteGlobalRef(property->second.jget);
        env->DeleteGlobalRef(property->second.jset);
    }
    if (jbusObj) {
        env->DeleteGlobalRef(jbusObj);
    }
}

bool JBusObject::IsSameObject(jobject jobj)
{
    return GetEnv()->IsSameObject(jbusObj, jobj);
}

QStatus JBusObject::AddInterfaces(jobjectArray jbusInterfaces)
{
    QStatus status;

    JNIEnv* env = GetEnv();
    jsize len = env->GetArrayLength(jbusInterfaces);
    for (jsize i = 0; i < len; ++i) {
        JLocalRef<jobject> jbusInterface = env->GetObjectArrayElement(jbusInterfaces, i);
        if (env->ExceptionCheck()) {
            return ER_FAIL;
        }
        const InterfaceDescription* intf = (const InterfaceDescription*)GetHandle(jbusInterface);
        if (env->ExceptionCheck()) {
            return ER_FAIL;
        }
        assert(intf);
        status = AddInterface(*intf);
        if (ER_OK != status) {
            return status;
        }

        size_t numMembs = intf->GetMembers(NULL);
        const InterfaceDescription::Member** membs = new const InterfaceDescription::Member *[numMembs];
        if (!membs) {
            return ER_OUT_OF_MEMORY;
        }
        intf->GetMembers(membs, numMembs);
        for (size_t m = 0; m < numMembs; ++m) {
            if (MESSAGE_METHOD_CALL == membs[m]->memberType) {
                status = AddMethodHandler(membs[m],
                                          static_cast<MessageReceiver::MethodHandler>(&JBusObject::MethodHandler));
                if (ER_OK != status) {
                    break;
                }
                JLocalRef<jstring> jname = env->NewStringUTF(membs[m]->name.c_str());
                if (!jname) {
                    status = ER_FAIL;
                    break;
                }
                JLocalRef<jclass> clazz = env->GetObjectClass(jbusInterface);
                jmethodID mid = env->GetMethodID(clazz, "getMember",
                                                 "(Ljava/lang/String;)Ljava/lang/reflect/Method;");
                if (!mid) {
                    status = ER_FAIL;
                    break;
                }
                JLocalRef<jobject> jmethod = env->CallObjectMethod(jbusInterface, mid, (jstring)jname);
                if (env->ExceptionCheck()) {
                    status = ER_FAIL;
                    break;
                }
                if (!jmethod) {
                    status = ER_BUS_INTERFACE_NO_SUCH_MEMBER;
                    break;
                }
                jobject jref = env->NewGlobalRef(jmethod);
                if (!jref) {
                    status = ER_FAIL;
                    break;
                }
                String key = intf->GetName() + membs[m]->name;
                methods.insert(pair<String, jobject>(key, jref));
            }
        }
        delete [] membs;
        membs = NULL;
        if (ER_OK != status) {
            return status;
        }

        size_t numProps = intf->GetProperties(NULL);
        const InterfaceDescription::Property** props = new const InterfaceDescription::Property *[numProps];
        if (!props) {
            return ER_OUT_OF_MEMORY;
        }
        intf->GetProperties(props, numProps);
        for (size_t p = 0; p < numProps; ++p) {
            Property property;
            property.signature = props[p]->signature;

            JLocalRef<jstring> jname = env->NewStringUTF(props[p]->name.c_str());
            if (!jname) {
                status = ER_FAIL;
                break;
            }
            JLocalRef<jclass> clazz = env->GetObjectClass(jbusInterface);
            jmethodID mid = env->GetMethodID(clazz, "getProperty",
                                             "(Ljava/lang/String;)[Ljava/lang/reflect/Method;");
            if (!mid) {
                status = ER_FAIL;
                break;
            }
            JLocalRef<jobjectArray> jmethods = (jobjectArray)env->CallObjectMethod(jbusInterface, mid,
                                                                                   (jstring)jname);
            if (env->ExceptionCheck()) {
                status = ER_FAIL;
                break;
            }
            if (!jmethods) {
                status = ER_BUS_NO_SUCH_PROPERTY;
                break;
            }
            JLocalRef<jobject> jget = env->GetObjectArrayElement(jmethods, 0);
            if (env->ExceptionCheck()) {
                status = ER_FAIL;
                break;
            }
            if (jget) {
                property.jget = env->NewGlobalRef(jget);
                if (!property.jget) {
                    status = ER_FAIL;
                    break;
                }
            } else {
                property.jget = NULL;
            }
            JLocalRef<jobject> jset = env->GetObjectArrayElement(jmethods, 1);
            if (env->ExceptionCheck()) {
                status = ER_FAIL;
                break;
            }
            if (jset) {
                property.jset = env->NewGlobalRef(jset);
                if (!property.jset) {
                    status = ER_FAIL;
                    break;
                }
            } else {
                property.jset = NULL;
            }

            String key = intf->GetName() + props[p]->name;
            properties.insert(pair<String, Property>(key, property));
        }
        delete [] props;
        props = NULL;
        if (ER_OK != status) {
            return status;
        }
    }

    return ER_OK;
}

/**
 * Marshal an Object into a MsgArg.
 *
 * @param[in] signature the signature of the Object
 * @param[in] jarg the Object
 * @param[in] arg the MsgArg to marshal into
 * @return the marshalled MsgArg or NULL if the marshalling failed.  This will
 *         be the same as @param arg if marshalling succeeded.
 */
static MsgArg* Marshal(const char* signature, jobject jarg, MsgArg* arg)
{
    JNIEnv* env = GetEnv();
    JLocalRef<jstring> jsignature = env->NewStringUTF(signature);
    if (!jsignature) {
        return NULL;
    }
    env->CallStaticVoidMethod(CLS_MsgArg, MID_MsgArg_marshal, (jlong)arg, (jstring)jsignature, jarg);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    return arg;
}

/**
 * Marshal an Object[] into MsgArgs.  The arguments are marshalled into an
 * ALLJOYN_STRUCT with the members set to the marshalled Object[] elements.
 *
 * @param[in] signature the signature of the Object[]
 * @param[in] jargs the Object[]
 * @param[in] arg the MsgArg to marshal into
 * @return an ALLJOYN_STRUCT containing the marshalled MsgArgs or NULL if the
 *         marshalling failed.  This will be the same as @param arg if
 *         marshalling succeeded.
 */
static MsgArg* Marshal(const char* signature, jobjectArray jargs, MsgArg* arg)
{
    JNIEnv* env = GetEnv();
    JLocalRef<jstring> jsignature = env->NewStringUTF(signature);
    if (!jsignature) {
        return NULL;
    }
    env->CallStaticVoidMethod(CLS_MsgArg, MID_MsgArg_marshal_array, (jlong)arg, (jstring)jsignature, jargs);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    return arg;
}

/**
 * Unmarshal a single MsgArg into an Object.
 *
 * @param[in] arg the MsgArg
 * @param[in] jtype the Type of the Object to unmarshal into
 * @return the unmarshalled Java Object
 */
static jobject Unmarshal(const MsgArg* arg, jobject jtype)
{
    JNIEnv* env = GetEnv();
    jobject jarg = env->CallStaticObjectMethod(CLS_MsgArg, MID_MsgArg_unmarshal, (jlong)arg, jtype);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    return jarg;
}

/**
 * Unmarshal MsgArgs into an Object[].
 *
 * @param[in] args the MsgArgs
 * @param[in] numArgs the number of MsgArgs
 * @param[in] jmethod the Method that will be invoked with the returned Object[]
 * @param[out] junmarshalled the unmarshalled Java Object[]
 */
static QStatus Unmarshal(const MsgArg* args, size_t numArgs, jobject jmethod,
                         JLocalRef<jobjectArray>& junmarshalled)
{
    MsgArg arg(ALLJOYN_STRUCT);
    arg.v_struct.members = (MsgArg*)args;
    arg.v_struct.numMembers = numArgs;
    JNIEnv* env = GetEnv();
    junmarshalled = (jobjectArray)env->CallStaticObjectMethod(CLS_MsgArg, MID_MsgArg_unmarshal_array,
                                                              jmethod, (jlong) & arg);
    if (env->ExceptionCheck()) {
        return ER_FAIL;
    }
    return ER_OK;
}

/**
 * Unmarshal an AllJoyn message into an Object[].
 *
 * @param[in] msg the AllJoyn message received
 * @param[in] jmethod the Method that will be invoked with the returned Object[]
 * @param[out] junmarshalled the unmarshalled Java Objects
 */
static QStatus Unmarshal(Message& msg, jobject jmethod, JLocalRef<jobjectArray>& junmarshalled)
{
    const MsgArg* args;
    size_t numArgs;
    msg->GetArgs(numArgs, args);
    return Unmarshal(args, numArgs, jmethod, junmarshalled);
}

void JBusObject::MethodHandler(const InterfaceDescription::Member* member, Message& msg)
{
    JScopedEnv env;
    MessageContext context(msg);
    /*
     * The Java method is called via invoke() on the
     * java.lang.reflect.Method object.  This allows us to package up
     * all the message args into an Object[], saving us from having to
     * figure out the signature of each method to lookup.
     */
    String key = member->iface->GetName() + member->name;
    JMethod::const_iterator method = methods.find(key);
    if (methods.end() == method) {
        MethodReply(member, msg, ER_BUS_OBJECT_NO_SUCH_MEMBER);
        return;
    }

    JLocalRef<jobjectArray> jargs;
    QStatus status = Unmarshal(msg, method->second, jargs);
    if (ER_OK != status) {
        MethodReply(member, msg, status);
        return;
    }

    JLocalRef<jclass> clazz = env->GetObjectClass(method->second);
    jmethodID mid = env->GetMethodID(clazz, "invoke",
                                     "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
    if (!mid) {
        MethodReply(member, msg, ER_FAIL);
        return;
    }
    JLocalRef<jobject> jreply = env->CallObjectMethod(method->second, mid, jbusObj, (jobjectArray)jargs);
    if (env->ExceptionCheck()) {
        MethodReply(member, msg, ER_FAIL);
        return;
    }
    MethodReply(member, msg, jreply);
}

QStatus JBusObject::MethodReply(const InterfaceDescription::Member* member, Message& msg, QStatus status)
{
    if (member->annotation & MEMBER_ANNOTATE_NO_REPLY) {
        return ER_OK;
    } else {
        return BusObject::MethodReply(msg, status);
    }
}

QStatus JBusObject::MethodReply(const InterfaceDescription::Member* member, Message& msg, jobject jreply)
{
    if (member->annotation & MEMBER_ANNOTATE_NO_REPLY) {
        if (!jreply) {
            return ER_OK;
        } else {
            QCC_LogError(ER_BUS_BAD_HDR_FLAGS,
                         ("Method %s is annotated as 'no reply' but value returned, replying anyway",
                          member->name.c_str()));
        }
    }
    JNIEnv* env = GetEnv();
    MsgArg replyArgs;
    QStatus status;
    uint8_t completeTypes = SignatureUtils::CountCompleteTypes(member->returnSignature.c_str());
    if (jreply) {
        JLocalRef<jobjectArray> jreplyArgs;
        if (completeTypes > 1) {
            jmethodID mid = env->GetStaticMethodID(CLS_Signature, "structArgs",
                                                   "(Ljava/lang/Object;)[Ljava/lang/Object;");
            if (!mid) {
                return MethodReply(member, msg, ER_FAIL);
            }
            jreplyArgs = (jobjectArray)env->CallStaticObjectMethod(CLS_Signature, mid, (jobject)jreply);
            if (env->ExceptionCheck()) {
                return MethodReply(member, msg, ER_FAIL);
            }
        } else {
            /*
             * Create Object[] out of the invoke() return value to reuse
             * marshalling code in Marshal() for the reply message.
             */
            jreplyArgs = env->NewObjectArray(1, CLS_Object, NULL);
            if (!jreplyArgs) {
                return MethodReply(member, msg, ER_FAIL);
            }
            env->SetObjectArrayElement(jreplyArgs, 0, jreply);
            if (env->ExceptionCheck()) {
                return MethodReply(member, msg, ER_FAIL);
            }
        }
        if (!Marshal(member->returnSignature.c_str(), jreplyArgs, &replyArgs)) {
            return MethodReply(member, msg, ER_FAIL);
        }
        status = BusObject::MethodReply(msg, replyArgs.v_struct.members, replyArgs.v_struct.numMembers);
    } else if (completeTypes) {
        String errorMessage(member->iface->GetName());
        errorMessage += "." + member->name + " returned null";
        QCC_LogError(ER_BUS_BAD_VALUE, (errorMessage.c_str()));
        status = BusObject::MethodReply(msg, "org.alljoyn.bus.BusException", errorMessage.c_str());
    } else {
        status = BusObject::MethodReply(msg, (MsgArg*)NULL, 0);
    }
    if (ER_OK != status) {
        env->ThrowNew(CLS_BusException, QCC_StatusText(status));
    }
    return status;
}

QStatus JBusObject::Signal(const char* destination, SessionId sessionId, const char* ifaceName, const char* signalName,
                           const MsgArg* args, size_t numArgs, uint32_t timeToLive, uint8_t flags)
{
    const InterfaceDescription* intf = bus.GetInterface(ifaceName);
    if (!intf) {
        return ER_BUS_OBJECT_NO_SUCH_INTERFACE;
    }
    const InterfaceDescription::Member* signal = intf->GetMember(signalName);
    if (!signal) {
        return ER_BUS_OBJECT_NO_SUCH_MEMBER;
    }
    return BusObject::Signal(destination, sessionId, *signal, args, numArgs, timeToLive, flags);
}

QStatus JBusObject::Get(const char* ifcName, const char* propName, MsgArg& val)
{
    JScopedEnv env;
    String key = String(ifcName) + propName;
    JProperty::const_iterator property = properties.find(key);
    if (properties.end() == property) {
        return ER_BUS_NO_SUCH_PROPERTY;
    }
    if (!property->second.jget) {
        return ER_BUS_PROPERTY_ACCESS_DENIED;
    }

    JLocalRef<jclass> clazz = env->GetObjectClass(property->second.jget);
    jmethodID mid = env->GetMethodID(clazz, "invoke",
                                     "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
    if (!mid) {
        return ER_FAIL;
    }
    JLocalRef<jobject> jvalue = env->CallObjectMethod(property->second.jget, mid, jbusObj, NULL);
    if (env->ExceptionCheck()) {
        return ER_FAIL;
    }
    if (!Marshal(property->second.signature.c_str(), (jobject)jvalue, &val)) {
        return ER_FAIL;
    }

    return ER_OK;
}

QStatus JBusObject::Set(const char* ifcName, const char* propName, MsgArg& val)
{
    JScopedEnv env;
    String key = String(ifcName) + propName;
    JProperty::const_iterator property = properties.find(key);
    if (properties.end() == property) {
        return ER_BUS_NO_SUCH_PROPERTY;
    }
    if (!property->second.jset) {
        return ER_BUS_PROPERTY_ACCESS_DENIED;
    }

    JLocalRef<jobjectArray> jvalue;
    QStatus status = Unmarshal(&val, 1, property->second.jset, jvalue);
    if (ER_OK != status) {
        return status;
    }

    JLocalRef<jclass> clazz = env->GetObjectClass(property->second.jset);
    jmethodID mid = env->GetMethodID(clazz, "invoke",
                                     "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
    if (!mid) {
        return ER_FAIL;
    }
    env->CallObjectMethod(property->second.jset, mid, jbusObj, (jobjectArray)jvalue);
    if (env->ExceptionCheck()) {
        return ER_FAIL;
    }

    return ER_OK;
}

String JBusObject::GenerateIntrospection(bool deep, size_t indent) const
{
    if (NULL != MID_generateIntrospection) {
        JScopedEnv env;
        JLocalRef<jstring> jintrospection = (jstring)env->CallObjectMethod(jbusObj, MID_generateIntrospection, deep, indent);
        if (env->ExceptionCheck()) {
            return BusObject::GenerateIntrospection(deep, indent);
        }
        JString introspection(jintrospection);
        if (env->ExceptionCheck()) {
            return BusObject::GenerateIntrospection(deep, indent);
        }
        return String(introspection.c_str());
    }
    return BusObject::GenerateIntrospection(deep, indent);
}

void JBusObject::ObjectRegistered()
{
    BusObject::ObjectRegistered();
    if (NULL != MID_registered) {
        JScopedEnv env;
        env->CallVoidMethod(jbusObj, MID_registered);
    }
}

void JBusObject::ObjectUnregistered()
{
    BusObject::ObjectUnregistered();
    if (NULL != MID_registered) {
        JScopedEnv env;
        env->CallVoidMethod(jbusObj, MID_unregistered);
    }
}

class JSignalHandler : public MessageReceiver {
  public:
    JSignalHandler(jobject jobj, jobject jmethod);
    ~JSignalHandler();
    bool IsSameObject(jobject jobj, jobject jmethod);
    QStatus Register(BusAttachment& bus, const char* ifaceName, const char* signalName, const char* srcPath);
    void Unregister(BusAttachment& bus);
    void SignalHandler(const InterfaceDescription::Member* member, const char* sourcePath, Message& msg);
  private:
    jweak jsignalHandler;
    jobject jmethod;
    const InterfaceDescription::Member* member;
    String source;
    String rule;
};

JSignalHandler::JSignalHandler(jobject jobj, jobject jmeth)
    : jsignalHandler(NULL), jmethod(NULL), member(NULL)
{
    JNIEnv* env = GetEnv();
    jsignalHandler = (jweak)env->NewGlobalRef(jobj);
    jmethod = env->NewGlobalRef(jmeth);
}

JSignalHandler::~JSignalHandler()
{
    JNIEnv* env = GetEnv();
    if (jmethod) {
        env->DeleteGlobalRef(jmethod);
    }
    if (jsignalHandler) {
        env->DeleteGlobalRef(jsignalHandler);
    }
}

bool JSignalHandler::IsSameObject(jobject jobj, jobject jmeth)
{
    JNIEnv* env = GetEnv();
    return env->IsSameObject(jsignalHandler, jobj) && env->CallBooleanMethod(jmethod, MID_Object_equals, jmeth);
}

QStatus JSignalHandler::Register(BusAttachment& bus, const char* ifaceName, const char* signalName,
                                 const char* srcPath)
{
    const InterfaceDescription* intf = bus.GetInterface(ifaceName);
    if (!intf) {
        return ER_BUS_NO_SUCH_INTERFACE;
    }
    member = intf->GetMember(signalName);
    if (!member) {
        return ER_BUS_INTERFACE_NO_SUCH_MEMBER;
    }
    source = srcPath;
    QStatus status = bus.RegisterSignalHandler(this,
                                               static_cast<MessageReceiver::SignalHandler>(&JSignalHandler::SignalHandler),
                                               member,
                                               source.c_str());
    if (ER_OK == status) {
        rule = "type='signal',interface='" + String(ifaceName) + "',member='" + String(signalName) + "'";
        if (!source.empty()) {
            rule += ",path='" + source + "'";
        }
        MsgArg arg("s", rule.c_str());
        Message reply(bus);
        const ProxyBusObject& dbusObj = bus.GetDBusProxyObj();
        status = dbusObj.MethodCall(ajn::org::freedesktop::DBus::InterfaceName, "AddMatch", &arg, 1, reply);
    }
    return status;
}

void JSignalHandler::Unregister(BusAttachment& bus)
{
    if (member) {
        MsgArg arg("s", rule.c_str());
        Message reply(bus);
        const ProxyBusObject& dbusObj = bus.GetDBusProxyObj();
        dbusObj.MethodCall(ajn::org::freedesktop::DBus::InterfaceName, "RemoveMatch", &arg, 1, reply);

        bus.UnregisterSignalHandler(this,
                                    static_cast<MessageReceiver::SignalHandler>(&JSignalHandler::SignalHandler),
                                    member,
                                    source.c_str());
    }
}

void JSignalHandler::SignalHandler(const InterfaceDescription::Member* member,
                                   const char* sourcePath,
                                   Message& msg)
{
    JScopedEnv env;
    MessageContext context(msg);

    JLocalRef<jobjectArray> jargs;
    QStatus status = Unmarshal(msg, jmethod, jargs);
    if (ER_OK != status) {
        return;
    }

    JLocalRef<jclass> clazz = env->GetObjectClass(jmethod);
    jmethodID mid = env->GetMethodID(clazz, "invoke",
                                     "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
    if (!mid) {
        return;
    }
    env->CallObjectMethod(jmethod, mid, jsignalHandler, (jobjectArray)jargs);
}

class _Bus : public BusAttachment {
  public:
    static JBusObject* GetBusObject(jobject jbusObject);

    _Bus(const char* applicationName, bool allowRemoteMessages);
    QStatus Connect(const char* connectArgs, jobject jkeyStoreListener, const char* authMechanisms,
                    jobject jauthListener, const char* keyStoreFileName);
    void Disconnect(const char* connectArgs);
    QStatus EnablePeerSecurity(const char* authMechanisms, jobject jauthListener, const char* keyStoreFileName);
    QStatus RegisterBusObject(const char* objPath, jobject jbusObject, jobjectArray jbusInterfaces);
    void UnregisterBusObject(jobject jbusObject);
    QStatus RegisterSignalHandler(const char* ifaceName, const char* signalName,
                                  jobject jsignalHandler, jobject jmethod, const char* srcPath);
    void UnregisterSignalHandler(jobject jsignalHandler, jobject jmethod);

  private:
    static vector<JBusObject*> busObjs;

    JKeyStoreListener* keyStoreListener;
    JAuthListener* authListener;
    vector<JSignalHandler*> signalHandlers;
};

vector<JBusObject*> _Bus::busObjs;

_Bus::_Bus(const char* applicationName, bool allowRemoteMessages)
    : BusAttachment(applicationName, allowRemoteMessages), keyStoreListener(NULL), authListener(NULL)
{
}

JBusObject* _Bus::GetBusObject(jobject jbusObject)
{
    for (vector<JBusObject*>::iterator it = busObjs.begin(); it != busObjs.end(); ++it) {
        if ((*it)->IsSameObject(jbusObject)) {
            return (*it);
        }
    }
    return NULL;
}

QStatus _Bus::Connect(const char* connectArgs, jobject jkeyStoreListener, const char* authMechanisms,
                      jobject jauthListener, const char* keyStoreFileName)
{
    JNIEnv* env = GetEnv();
    QStatus status = Start();
    if (ER_OK != status) {
        goto exit;
    }

    if (jkeyStoreListener) {
        keyStoreListener = new JKeyStoreListener(jkeyStoreListener);
        if (!keyStoreListener) {
            Throw("java/lang/OutOfMemoryError", NULL);
        }
        if (env->ExceptionCheck()) {
            status = ER_FAIL;
            goto exit;
        }
        RegisterKeyStoreListener(*keyStoreListener);
    }

    status = EnablePeerSecurity(authMechanisms, jauthListener, keyStoreFileName);
    if (ER_OK != status) {
        goto exit;
    }

    status = BusAttachment::Connect(connectArgs);

    exit :
    if (ER_OK != status) {
        Disconnect(connectArgs);
    }
    return status;
}

void _Bus::Disconnect(const char* connectArgs)
{
    if (IsConnected()) {
        QStatus status = BusAttachment::Disconnect(connectArgs);
        if (ER_OK != status) {
            QCC_LogError(status, ("Disconnect failed"));
        }
    }
    // TODO: DisablePeerSecurity
    // TODO: UnregisterKeyStoreListener
    if (IsStarted()) {
        QStatus status = Stop();
        if (ER_OK != status) {
            QCC_LogError(status, ("Stop failed"));
        }
    }
    delete authListener;
    authListener = NULL;
    delete keyStoreListener;
    keyStoreListener = NULL;
}

QStatus _Bus::EnablePeerSecurity(const char* authMechanisms, jobject jauthListener, const char* keyStoreFileName)
{
    JNIEnv* env = GetEnv();
    if (!authMechanisms || !IsStarted()) {
        return ER_OK;
    }
    authListener = new JAuthListener(jauthListener);
    if (!authListener) {
        Throw("java/lang/OutOfMemoryError", NULL);
    }
    if (env->ExceptionCheck()) {
        return ER_FAIL;
    }
    QStatus status = BusAttachment::EnablePeerSecurity(authMechanisms, authListener, keyStoreFileName);
    if (ER_OK != status) {
        delete authListener;
        authListener = NULL;
    }
    return status;
}

QStatus _Bus::RegisterBusObject(const char* objPath, jobject jbusObject, jobjectArray jbusInterfaces)
{
    JNIEnv* env = GetEnv();
    if (GetBusObject(jbusObject)) {
        return ER_BUS_OBJ_ALREADY_EXISTS;
    }
    JBusObject* busObj = new JBusObject(*this, objPath, jbusObject);
    QStatus status = busObj->AddInterfaces(jbusInterfaces);
    if (env->ExceptionCheck()) {
        status = ER_FAIL;
    }
    if (ER_OK == status) {
        status = BusAttachment::RegisterBusObject(*busObj);
    }
    if (ER_OK == status) {
        busObjs.push_back(busObj);
    } else {
        delete busObj;
    }
    return status;
}

void _Bus::UnregisterBusObject(jobject jbusObject)
{
    JBusObject* busObj = GetBusObject(jbusObject);
    if (busObj) {
        BusAttachment::UnregisterBusObject(*busObj);
        for (vector<JBusObject*>::iterator it = busObjs.begin(); it != busObjs.end(); ++it) {
            if ((*it)->IsSameObject(jbusObject)) {
                delete (*it);
                busObjs.erase(it);
                break;
            }
        }
    }
}

QStatus _Bus::RegisterSignalHandler(const char* ifaceName, const char* signalName,
                                    jobject jsignalHandler, jobject jmethod, const char* srcPath)
{
    JSignalHandler* signalHandler = new JSignalHandler(jsignalHandler, jmethod);
    QStatus status = signalHandler->Register(*this, ifaceName, signalName, srcPath);
    if (ER_OK == status) {
        signalHandlers.push_back(signalHandler);
    } else {
        delete signalHandler;
    }
    return status;
}

void _Bus::UnregisterSignalHandler(jobject jsignalHandler, jobject jmethod)
{
    for (vector<JSignalHandler*>::iterator it = signalHandlers.begin(); it != signalHandlers.end(); ++it) {
        if ((*it)->IsSameObject(jsignalHandler, jmethod)) {
            (*it)->Unregister(*this);
            delete (*it);
            signalHandlers.erase(it);
            break;
        }
    }
}

/*
 * Java garbage collector may call finalizers in any order, so ensure that
 * BusAttachment stays around until attached ProxyBusObjects deleted.
 */
typedef ManagedObj<_Bus> Bus;

/*
 * class org_alljoyn_bus_BusAttachment
 */

JNIEXPORT void JNICALL Java_org_alljoyn_bus_BusAttachment_create(JNIEnv* env,
                                                                 jobject thiz,
                                                                 jstring japplicationName,
                                                                 jboolean allowRemoteMessages)
{
    JString applicationName(japplicationName);
    if (env->ExceptionCheck()) {
        return;
    }
    const char* name = applicationName.c_str();
    Bus* bus = new Bus(name, allowRemoteMessages);
    if (!bus) {
        Throw("java/lang/OutOfMemoryError", NULL);
    }
    if (!env->ExceptionCheck()) {
        SetHandle(thiz, bus);
    }
    if (env->ExceptionCheck()) {
        delete bus;
    }
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_BusAttachment_destroy(JNIEnv* env,
                                                                  jobject thiz)
{
    Bus* bus = (Bus*)GetHandle(thiz);
    if (!bus) {
        return;
    }
    delete bus;
    SetHandle(thiz, NULL);
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_BusAttachment_registerBusListener(JNIEnv* env, jobject thiz, jobject jlistener)
{
    QCC_DbgPrintf(("BusAttachment_registerBusListener()\n"));

    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_registerBusListener(): Exception\n"));
        return;
    }
    assert(bus);

    //
    // TODO:  Memory leak
    //
    QCC_DbgPrintf(("BusAttachment_registerBusListener(): Creating JBusListener\n"));
    JBusListener* busListener = new JBusListener(jlistener);

    QCC_DbgPrintf(("BusAttachment_registerBusListener(): Call RegisterBusListener()\n"));
    (*bus)->RegisterBusListener(*busListener);
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_BusAttachment_unregisterBusListener(JNIEnv* env, jobject thiz, jobject jbusListener)
{
    QCC_DbgPrintf(("BusAttachment_unregisterBusListener()\n"));

    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_unregisterBusListener(): Exception\n"));
        return;
    }

    //
    // TODO:  Plug memory leak of bus listeners
    //
    // assert(bus);
    // (*bus)->UnregisterBusListener(jbuslistener);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_requestName(JNIEnv*env, jobject thiz,
                                                                         jstring jname, jint jflags)
{
    QCC_DbgPrintf(("BusAttachment_requestName()\n"));

    //
    // Load the C++ well-known name with the Java well-known name.
    //
    JString name(jname);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_requestName(): Exception\n"));
        return NULL;
    }

    //
    // Get a copy of the pointer to the BusAttachment (via a managed object)
    //
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_requestName(): Exception\n"));
        return NULL;
    }
    assert(bus);

    //
    // Make the AllJoyn call.
    //
    QCC_DbgPrintf(("BusAttachment_requestName(): Call RequestName(%s, 0x%08x)\n",
                   name.c_str(), jflags));

    QStatus status = (*bus)->RequestName(name.c_str(), jflags);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_requestName(): Exception\n"));
        return NULL;
    }

    QCC_DbgPrintf(("BusAttachment_requestName(): Back from RequestName(%s, 0x%08x)\n",
                   name.c_str(), jflags));

    if (status != ER_OK) {
        QCC_LogError(status, ("BusAttachment_requestName(): RequestName() fails\n"));
    }

    return JStatus(status);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_releaseName(JNIEnv*env, jobject thiz,
                                                                         jstring jname)
{
    QCC_DbgPrintf(("BusAttachment_releaseName()\n"));

    //
    // Load the C++ well-known name with the Java well-known name.
    //
    JString name(jname);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_releaseName(): Exception\n"));
        return NULL;
    }

    //
    // Get a copy of the pointer to the BusAttachment (via a managed object)
    //
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_releaseName(): Exception\n"));
        return NULL;
    }
    assert(bus);

    //
    // Make the AllJoyn call.
    //

    QCC_DbgPrintf(("BusAttachment_releaseName(): Call ReleaseName(%s)\n",
                   name.c_str()));

    QStatus status = (*bus)->ReleaseName(name.c_str());
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_releaseName(): Exception\n"));
        return NULL;
    }

    QCC_DbgPrintf(("BusAttachment_releaseName(): Back from ReleaseName(%s)\n",
                   name.c_str()));

    if (status != ER_OK) {
        QCC_LogError(status, ("BusAttachment_releaseName(): ReleaseName() fails\n"));
    }

    return JStatus(status);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_addMatch(JNIEnv*env, jobject thiz,
                                                                      jstring jrule)
{
    QCC_DbgPrintf(("BusAttachment_addMatch()\n"));

    //
    // Load the C++ well-known name with the Java well-known name.
    //
    JString rule(jrule);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_addMatch(): Exception\n"));
        return NULL;
    }

    //
    // Get a copy of the pointer to the BusAttachment (via a managed object)
    //
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_addMatch(): Exception\n"));
        return NULL;
    }
    assert(bus);

    //
    // Make the AllJoyn call.
    //
    QCC_DbgPrintf(("BusAttachment_addMatch(): Call AddMatch(%s)\n",
                   rule.c_str()));

    QStatus status = (*bus)->AddMatch(rule.c_str());
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_addMatch(): Exception\n"));
        return NULL;
    }

    QCC_DbgPrintf(("BusAttachment_addMatch(): Back from AddMatch(%s)\n",
                   rule.c_str()));

    if (status != ER_OK) {
        QCC_LogError(status, ("BusAttachment_addMatch(): AddMatch() fails\n"));
    }

    return JStatus(status);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_advertiseName(JNIEnv* env, jobject thiz,
                                                                           jstring jname, jshort jtransports)
{
    QCC_DbgPrintf(("BusAttachment_advertiseName()\n"));

    //
    // Load the C++ well-known name with the Java well-known name.
    //
    JString name(jname);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_advertiseName(): Exception\n"));
        return NULL;
    }

    //
    // Get a copy of the pointer to the BusAttachment (via a managed object)
    //
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_advertiseName(): Exception\n"));
        return NULL;
    }
    assert(bus);

    //
    // Make the AllJoyn call.
    //
    QCC_DbgPrintf(("BusAttachment_advertiseName(): Call AdvertiseName(%s, 0x%04x)\n",
                   name.c_str(), jtransports));

    QStatus status = (*bus)->AdvertiseName(name.c_str(), jtransports);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_advertiseName(): Exception\n"));
        return NULL;
    }

    QCC_DbgPrintf(("BusAttachment_advertiseName(): Back from AdvertiseName(%s)\n",
                   name.c_str()));

    if (status != ER_OK) {
        QCC_LogError(status, ("BusAttachment_advertiseName(): AdvertiseName() fails\n"));
    }

    return JStatus(status);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_cancelAdvertiseName(JNIEnv* env, jobject thiz,
                                                                                 jstring jname, jshort jtransports)
{
    QCC_DbgPrintf(("BusAttachment_cancelAdvertiseName()\n"));

    //
    // Load the C++ well-known name Java well-known name.
    //
    JString name(jname);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_cancelAdvertiseName(): Exception\n"));
        return NULL;
    }

    //
    // Get a copy of the pointer to the BusAttachment (via a managed object)
    //
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_cancelAdvertiseName(): Exception\n"));
        return NULL;
    }
    assert(bus);

    //
    // Make the AllJoyn call.
    //
    QCC_DbgPrintf(("BusAttachment_cancelAdvertiseName(): Call CancelAdvertiseName(%s, 0x%04x)\n",
                   name.c_str()));

    QStatus status = (*bus)->CancelAdvertiseName(name.c_str(), jtransports);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_cancelAdvertiseName(): Exception\n"));
        return NULL;
    }

    QCC_DbgPrintf(("BusAttachment_cancelAdvertiseName(): Back from CancelAdvertiseName(%s)\n",
                   name.c_str()));

    if (status != ER_OK) {
        QCC_LogError(status, ("BusAttachment_cancelAdvertiseName(): CancelAdvertiseName() fails\n"));
    }

    return JStatus(status);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_findAdvertisedName(JNIEnv* env, jobject thiz,
                                                                                jstring jname)
{
    QCC_DbgPrintf(("BusAttachment_findAdvertisedName()\n"));

    //
    // Load the C++ well-known name Java well-known name.
    //
    JString name(jname);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_findAdvertisedName(): Exception\n"));
        return NULL;
    }

    //
    // Get a copy of the pointer to the BusAttachment (via a managed object)
    //
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_findAdvertisedName(): Exception\n"));
        return NULL;
    }
    assert(bus);

    //
    // Make the AllJoyn call.
    //
    QCC_DbgPrintf(("BusAttachment_findAdvertisedName(): Call FindAdvertisedName(%s)\n",
                   name.c_str()));

    QStatus status = (*bus)->FindAdvertisedName(name.c_str());
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_findAdvertisedName(): Exception\n"));
        return NULL;
    }

    QCC_DbgPrintf(("BusAttachment_findAdvertisedName(): Back from FindAdvertisedName(%s)\n",
                   name.c_str()));

    if (status != ER_OK) {
        QCC_LogError(status, ("BusAttachment_findAdvertsiedName(): FindAdvertisedName() fails\n"));
    }

    return JStatus(status);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_cancelFindAdvertisedName(JNIEnv* env, jobject thiz,
                                                                                      jstring jname)
{
    QCC_DbgPrintf(("BusAttachment_cancelFindAdvertisedName()\n"));

    //
    // Load the C++ well-known name Java well-known name.
    //
    JString name(jname);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_cancelFindAdvertisedName(): Exception\n"));
        return NULL;
    }

    //
    // Get a copy of the pointer to the BusAttachment (via a managed object)
    //
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_cancelFindAdvertisedName(): Exception\n"));
        return NULL;
    }
    assert(bus);

    //
    // Make the AllJoyn call.
    //
    QCC_DbgPrintf(("BusAttachment_cancelFindAdvertisedName(): Call CancelFindAdvertisedName(%s)\n",
                   name.c_str()));

    QStatus status = (*bus)->CancelFindAdvertisedName(name.c_str());
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_cancelFindAdvertisedName(): Exception\n"));
        return NULL;
    }

    QCC_DbgPrintf(("BusAttachment_cancelFindAdvertisedName(): Back from CancelFindAdvertisedName(%s)\n",
                   name.c_str()));

    if (status != ER_OK) {
        QCC_LogError(status, ("BusAttachment_cancelfindAdvertisedName(): CancelFindAdvertisedName() fails\n"));
    }

    return JStatus(status);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_bindSessionPort(JNIEnv* env, jobject thiz,
                                                                             jobject jsessionPort, jobject jsessionOpts)
{
    QCC_DbgPrintf(("BusAttachment_bindSessionPort()\n"));

    //
    // Load the C++ session port from the Java session port.
    //
    SessionPort sessionPort;
    JLocalRef<jclass> clazz = env->GetObjectClass(jsessionPort);
    jfieldID spValueFid = env->GetFieldID(clazz, "value", "S");
    assert(spValueFid);
    sessionPort = env->GetShortField(jsessionPort, spValueFid);

    //
    // Load the C++ session options from the Java session options.
    //
    SessionOpts sessionOpts;
    clazz = env->GetObjectClass(jsessionOpts);

    jfieldID fid = env->GetFieldID(clazz, "traffic", "B");
    assert(fid);
    sessionOpts.traffic = static_cast<SessionOpts::TrafficType>(env->GetByteField(jsessionOpts, fid));

    fid = env->GetFieldID(clazz, "isMultipoint", "Z");
    assert(fid);
    sessionOpts.isMultipoint = env->GetBooleanField(jsessionOpts, fid);

    fid = env->GetFieldID(clazz, "proximity", "B");
    assert(fid);
    sessionOpts.proximity = env->GetByteField(jsessionOpts, fid);

    fid = env->GetFieldID(clazz, "transports", "S");
    assert(fid);
    sessionOpts.transports = env->GetShortField(jsessionOpts, fid);

    //
    // Get a copy of the pointer to the BusAttachment (via a managed object)
    //
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_bindSessionPort(): Exception\n"));
        return NULL;
    }
    assert(bus);

    //
    // Make the AllJoyn call.
    //
    QCC_DbgPrintf(("BusAttachment_bindSessionPort(): Call BindSessionPort(%d, <0x%02x, %d, 0x%02x, 0x%04x>)\n",
                   sessionPort, sessionOpts.traffic, sessionOpts.isMultipoint, sessionOpts.proximity, sessionOpts.transports));

    QStatus status = (*bus)->BindSessionPort(sessionPort, sessionOpts);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_bindSessionPort(): Exception\n"));
        return NULL;
    }

    QCC_DbgPrintf(("BusAttachment_bindSessionPort(): Back from BindSessionPort(%d, <0x%02x, %d, 0x%02x, 0x%04x>)\n",
                   sessionPort, sessionOpts.traffic, sessionOpts.isMultipoint, sessionOpts.proximity, sessionOpts.transports));

    if (status != ER_OK) {
        QCC_LogError(status, ("BusAttachment_bindSessionPort(): BindSessionPort() fails\n"));
    }

    //
    // Store the actual session port back in the session port out parameter
    //
    env->SetShortField(jsessionPort, spValueFid, sessionPort);

    return JStatus(status);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_joinSession(JNIEnv* env, jobject thiz,
                                                                         jstring jsessionHost,
                                                                         jshort jsessionPort,
                                                                         jobject jsessionId,
                                                                         jobject jsessionOpts)
{
    QCC_DbgPrintf(("BusAttachment_joinSession()\n"));

    //
    // Load the C++ session host string from the java parameter
    //
    JString sessionHost(jsessionHost);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_joinSession(): Exception\n"));
        return NULL;
    }

    //
    // Get a copy of the pointer to the BusAttachment (via a managed object)
    //
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_joinSession(): Exception\n"));
        return NULL;
    }
    assert(bus);

    //
    // Make the AllJoyn call.
    //
    SessionId sessionId = 0;
    SessionOpts sessionOpts;

    QCC_DbgPrintf(("BusAttachment_joinSession(): Call JoinSession(%s, %d, %d,  <0x%02x, %d, 0x%02x, 0x%04x>)\n",
                   sessionHost.c_str(), jsessionPort, sessionId, sessionOpts.traffic, sessionOpts.isMultipoint,
                   sessionOpts.proximity, sessionOpts.transports));

    QStatus status = (*bus)->JoinSession(sessionHost.c_str(), jsessionPort, sessionId, sessionOpts);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_joinSession(): Exception\n"));
        return NULL;
    }

    QCC_DbgPrintf(("BusAttachment_joinSession(): Back from JoinSession(%s, %d, %d,  <0x%02x, %d, 0x%02x, 0x%04x>)\n",
                   sessionHost.c_str(), jsessionPort, sessionId, sessionOpts.traffic, sessionOpts.isMultipoint,
                   sessionOpts.proximity, sessionOpts.transports));

    if (status != ER_OK) {
        QCC_LogError(status, ("BusAttachment_joinSession(): JoinSession() fails\n"));
    }

    //
    // Store the session ID back in its out parameter.
    //
    JLocalRef<jclass> clazz = env->GetObjectClass(jsessionId);
    jfieldID fid = env->GetFieldID(clazz, "value", "I");
    assert(fid);
    env->SetIntField(jsessionId, fid, sessionId);

    //
    // Store the Java session options from the returned C++ session options.
    //
    clazz = env->GetObjectClass(jsessionOpts);

    fid = env->GetFieldID(clazz, "traffic", "B");
    assert(fid);
    env->SetByteField(jsessionOpts, fid, sessionOpts.traffic);

    fid = env->GetFieldID(clazz, "isMultipoint", "Z");
    assert(fid);
    env->SetBooleanField(jsessionOpts, fid, sessionOpts.isMultipoint);

    fid = env->GetFieldID(clazz, "proximity", "B");
    assert(fid);
    env->SetByteField(jsessionOpts, fid, sessionOpts.proximity);

    fid = env->GetFieldID(clazz, "transports", "S");
    assert(fid);
    env->SetShortField(jsessionOpts, fid, sessionOpts.transports);

    return JStatus(status);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_leaveSession(JNIEnv* env, jobject thiz,
                                                                          jint jsessionId)
{
    QCC_DbgPrintf(("BusAttachment_leaveSession()\n"));

    //
    // Get a copy of the pointer to the BusAttachment (via a managed object)
    //
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    assert(bus);

    //
    // Make the AllJoyn call.
    //
    QCC_DbgPrintf(("BusAttachment_leaveSession(): Call LeaveSession(%d)\n",
                   jsessionId));

    QStatus status = (*bus)->LeaveSession(jsessionId);
    if (env->ExceptionCheck()) {
        return NULL;
    }

    QCC_DbgPrintf(("BusAttachment_leaveSession(): back from LeaveSession(%d)\n",
                   jsessionId));

    if (status != ER_OK) {
        QCC_LogError(status, ("BusAttachment_leaveSession(): LeaveSession() fails\n"));
    }

    return JStatus(status);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_getSessionFd(JNIEnv* env, jobject thiz,
                                                                          jint jsessionId,
                                                                          jobject jsockfd)
{
    QCC_DbgPrintf(("BusAttachment_getSessionFd()\n"));

    //
    // Get a copy of the pointer to the BusAttachment (via a managed object)
    //
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_joinSession(): Exception\n"));
        return NULL;
    }
    assert(bus);

    //
    // Make the AllJoyn call.
    //
    qcc::SocketFd sockfd = -1;

    QCC_DbgPrintf(("BusAttachment_getSessionFd(): Call GetSessionFd(%d, %d)\n",
                   jsessionId, sockfd));

    QStatus status = (*bus)->GetSessionFd(jsessionId, sockfd);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_getSessionFd(): Exception\n"));
        return NULL;
    }

    QCC_DbgPrintf(("BusAttachment_getSessionFd(): Back from GetSessionFd(%d, %d)\n",
                   jsessionId, sockfd));


    if (status != ER_OK) {
        QCC_LogError(status, ("BusAttachment_getSessionFd(): GetSessionFd() fails\n"));
    }

    //
    // Store the sockFd in its corresponding out parameter.
    //
    JLocalRef<jclass> clazz = env->GetObjectClass(jsockfd);
    jfieldID fid = env->GetFieldID(clazz, "value", "I");
    assert(fid);
    env->SetIntField(jsockfd, fid, sockfd);

    return JStatus(status);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_setDaemonDebug(JNIEnv*env, jobject thiz,
                                                                            jstring jmodule, jint jlevel)
{
    QCC_DbgPrintf(("BusAttachment_setDaemonDebug()\n"));

    //
    // Load the C++ module name with the Java module name.
    //
    JString module(jmodule);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_setDaemonDebug(): Exception\n"));
        return NULL;
    }

    //
    // Get a copy of the pointer to the BusAttachment (via a managed object)
    //
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_setDaemonDebug(): Exception\n"));
        return NULL;
    }
    assert(bus);

    //
    // Make the AllJoyn call.
    //
    QCC_DbgPrintf(("BusAttachment_setDaemonDebug(): Call SetDaemonDebug(%s, %d)\n",
                   module.c_str(), jlevel));

    QStatus status = (*bus)->SetDaemonDebug(module.c_str(), jlevel);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_setDaemonDebug(): Exception\n"));
        return NULL;
    }

    QCC_DbgPrintf(("BusAttachment_setDaemonDebug(): Back from SetDaemonDebug(%s, %d)\n",
                   module.c_str(), jlevel));

    if (status != ER_OK) {
        QCC_LogError(status, ("BusAttachment_setDaemonDebug(): SetDaemonDebug() fails\n"));
    }

    return JStatus(status);
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_BusAttachment_setLogLevels(JNIEnv*env, jobject thiz,
                                                                       jstring jlogEnv)
{
    QCC_DbgPrintf(("BusAttachment_setLogLevels()\n"));

    //
    // Load the C++ environment string with the Java environment string.
    //
    JString logEnv(jlogEnv);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_setLogLevels(): Exception\n"));
        return;
    }

    //
    // Make the AllJoyn call.
    //
    QCC_DbgPrintf(("QCC_SetLogLevels(%s)\n", logEnv.c_str()));
    QCC_SetLogLevels(logEnv.c_str());
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_BusAttachment_setDebugLevel(JNIEnv*env, jobject thiz,
                                                                        jstring jmodule, jint jlevel)
{
    QCC_DbgPrintf(("BusAttachment_setDebugLevel()\n"));

    //
    // Load the C++ module string with the Java module string.
    //
    JString module(jmodule);
    if (env->ExceptionCheck()) {
        QCC_LogError(ER_FAIL, ("BusAttachment_setDebugLevel(): Exception\n"));
        return;
    }

    //
    // Make the AllJoyn call.
    //
    QCC_DbgPrintf(("QCC_SetDebugLevel(%s, %d)\n", module.c_str(), jlevel));
    QCC_SetDebugLevel(module.c_str(), jlevel);
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_BusAttachment_useOSLogging(JNIEnv*env, jobject thiz,
                                                                       jboolean juseOSLog)
{
    QCC_DbgPrintf(("BusAttachment_useOSLogging()\n"));

    //
    // Make the AllJoyn call.
    //
    QCC_DbgPrintf(("QCC_UseOSLogging(%d)\n", juseOSLog));
    QCC_UseOSLogging(juseOSLog);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_connect(JNIEnv* env,
                                                                     jobject thiz,
                                                                     jstring jconnectArgs,
                                                                     jobject jkeyStoreListener,
                                                                     jstring jauthMechanisms,
                                                                     jobject jauthListener,
                                                                     jstring jkeyStoreFileName)
{
    JString connectArgs(jconnectArgs);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    JString authMechanisms(jauthMechanisms);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    JString keyStoreFileName(jkeyStoreFileName);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    assert(bus);

    QStatus status = (*bus)->Connect(connectArgs.c_str(), jkeyStoreListener, authMechanisms.c_str(),
                                     jauthListener, keyStoreFileName.c_str());
    if (env->ExceptionCheck()) {
        return NULL;
    } else {
        return JStatus(status);
    }
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_BusAttachment_disconnect(JNIEnv* env,
                                                                     jobject thiz,
                                                                     jstring jconnectArgs)
{
    JString connectArgs(jconnectArgs);
    if (env->ExceptionCheck()) {
        return;
    }
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return;
    }
    assert(bus);
    (*bus)->Disconnect(connectArgs.c_str());
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_enablePeerSecurity(JNIEnv* env,
                                                                                jobject thiz,
                                                                                jstring jauthMechanisms,
                                                                                jobject jauthListener,
                                                                                jstring jkeyStoreFileName)
{
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    assert(bus);
    JString authMechanisms(jauthMechanisms);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    JString keyStoreFileName(jkeyStoreFileName);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    QStatus status = (*bus)->EnablePeerSecurity(authMechanisms.c_str(), jauthListener, keyStoreFileName.c_str());
    if (env->ExceptionCheck()) {
        return NULL;
    } else {
        return JStatus(status);
    }
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_registerBusObject(JNIEnv* env,
                                                                               jobject thiz,
                                                                               jstring jobjPath,
                                                                               jobject jbusObject,
                                                                               jobjectArray jbusInterfaces)
{
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    assert(bus);
    JString objPath(jobjPath);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    QStatus status = (*bus)->RegisterBusObject(objPath.c_str(), jbusObject, jbusInterfaces);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    return JStatus(status);
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_BusAttachment_unregisterBusObject(JNIEnv* env,
                                                                              jobject thiz,
                                                                              jobject jbusObject)
{
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return;
    }
    assert(bus);
    (*bus)->UnregisterBusObject(jbusObject);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_registerNativeSignalHandler(JNIEnv* env,
                                                                                         jobject thiz,
                                                                                         jstring jifaceName,
                                                                                         jstring jsignalName,
                                                                                         jobject jsignalHandler,
                                                                                         jobject jmethod,
                                                                                         jstring jsource)
{
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    assert(bus);
    JString ifaceName(jifaceName);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    JString signalName(jsignalName);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    JString source(jsource);
    if (env->ExceptionCheck()) {
        return NULL;
    }

    const char* srcPath = NULL;
    if (source.c_str() && source.c_str()[0]) {
        srcPath = source.c_str();
    }
    QStatus status = (*bus)->RegisterSignalHandler(ifaceName.c_str(), signalName.c_str(),
                                                   jsignalHandler, jmethod, srcPath);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    return JStatus(status);
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_BusAttachment_unregisterSignalHandler(JNIEnv* env,
                                                                                  jobject thiz,
                                                                                  jobject jsignalHandler,
                                                                                  jobject jmethod)
{
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return;
    }
    assert(bus);
    (*bus)->UnregisterSignalHandler(jsignalHandler, jmethod);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_getUniqueName(JNIEnv* env,
                                                                           jobject thiz)
{
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    assert(bus);
    return env->NewStringUTF((*bus)->GetUniqueName().c_str());
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_BusAttachment_clearKeyStore(JNIEnv* env,
                                                                        jobject thiz)
{
    Bus* bus = (Bus*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return;
    }
    assert(bus);
    (*bus)->ClearKeyStore();
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_BusAttachment_getMessageContext(JNIEnv* env,
                                                                               jobject thiz)
{
    Message msg = MessageContext::GetMessage();
    JLocalRef<jstring> jobjectPath = env->NewStringUTF(msg->GetObjectPath());
    if (!jobjectPath) {
        return NULL;
    }
    JLocalRef<jstring> jinterfaceName = env->NewStringUTF(msg->GetInterface());
    if (!jinterfaceName) {
        return NULL;
    }
    JLocalRef<jstring> jmemberName = env->NewStringUTF(msg->GetMemberName());
    if (!jmemberName) {
        return NULL;
    }
    JLocalRef<jstring> jdestination = env->NewStringUTF(msg->GetDestination());
    if (!jdestination) {
        return NULL;
    }
    JLocalRef<jstring> jsender = env->NewStringUTF(msg->GetSender());
    if (!jsender) {
        return NULL;
    }
    JLocalRef<jstring> jsignature = env->NewStringUTF(msg->GetSignature());
    if (!jsignature) {
        return NULL;
    }
    JLocalRef<jstring> jauthMechanism = env->NewStringUTF(msg->GetAuthMechanism().c_str());
    if (!jauthMechanism) {
        return NULL;
    }

    jmethodID mid = env->GetMethodID(CLS_MessageContext, "<init>", "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if (!mid) {
        return NULL;
    }
    return env->NewObject(CLS_MessageContext, mid, msg->IsUnreliable(), (jstring)jobjectPath,
                          (jstring)jinterfaceName, (jstring)jmemberName, (jstring)jdestination,
                          (jstring)jsender, (jstring)jsignature, (jstring)jauthMechanism);
}

/*
 * class org_alljoyn_bus_InterfaceDescription
 */

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_InterfaceDescription_create(JNIEnv* env,
                                                                           jobject thiz,
                                                                           jobject jbus,
                                                                           jstring jname,
                                                                           jboolean secure)
{
    Bus* bus = (Bus*)GetHandle(jbus);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    assert(bus);
    JString name(jname);
    if (env->ExceptionCheck()) {
        return NULL;
    }

    InterfaceDescription* intf;
    QStatus status = (*bus)->CreateInterface(name.c_str(), intf, secure);
    if (ER_BUS_IFACE_ALREADY_EXISTS == status) {
        intf = (InterfaceDescription*)(*bus)->GetInterface(name.c_str());
        assert(intf);
        status = ER_OK;
    }
    if (ER_OK == status) {
        SetHandle(thiz, intf);
    }
    if (env->ExceptionCheck()) {
        return NULL;
    } else {
        return JStatus(status);
    }
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_InterfaceDescription_addMember(JNIEnv* env,
                                                                              jobject thiz,
                                                                              jint type,
                                                                              jstring jname,
                                                                              jstring jinputSig,
                                                                              jstring joutSig,
                                                                              jint annotation)
{
    InterfaceDescription* intf = (InterfaceDescription*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    assert(intf);
    JString name(jname);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    JString inputSig(jinputSig);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    JString outSig(joutSig);
    if (env->ExceptionCheck()) {
        return NULL;
    }

    QStatus status = intf->AddMember((AllJoynMessageType)type, name.c_str(),
                                     inputSig.c_str(), outSig.c_str(), NULL, annotation);
    if (ER_BUS_MEMBER_ALREADY_EXISTS == status) {
        status = ER_OK;
    }
    return JStatus(status);
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_InterfaceDescription_addProperty(JNIEnv* env,
                                                                                jobject thiz,
                                                                                jstring jname,
                                                                                jstring jsignature,
                                                                                jint access)
{
    InterfaceDescription* intf = (InterfaceDescription*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    assert(intf);
    JString name(jname);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    JString signature(jsignature);
    if (env->ExceptionCheck()) {
        return NULL;
    }

    QStatus status = intf->AddProperty(name.c_str(), signature.c_str(), access);
    if (ER_BUS_PROPERTY_ALREADY_EXISTS == status) {
        status = ER_OK;
    }
    return JStatus(status);
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_InterfaceDescription_activate(JNIEnv* env,
                                                                          jobject thiz)
{
    InterfaceDescription* intf = (InterfaceDescription*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return;
    }
    assert(intf);
    intf->Activate();
}

/*
 * class org_alljoyn_bus_ProxyBusObject
 */

class JProxyBusObject : public ProxyBusObject {
  public:
    JProxyBusObject(Bus& bus, const char* endpoint, const char* path, SessionId sessionId);
    Bus bus;
};

JProxyBusObject::JProxyBusObject(Bus& b, const char* endpoint, const char* path, SessionId sessionId)
    : ProxyBusObject(*b, endpoint, path, sessionId), bus(b)
{
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_ProxyBusObject_create(JNIEnv* env, jobject thiz,
                                                                  jobject jbus,
                                                                  jstring jbusName,
                                                                  jstring jobjPath,
                                                                  jint sessionId)
{
    Bus* bus = (Bus*)GetHandle(jbus);
    if (env->ExceptionCheck()) {
        return;
    }
    assert(bus);
    JString busName(jbusName);
    if (env->ExceptionCheck()) {
        return;
    }
    JString objPath(jobjPath);
    if (env->ExceptionCheck()) {
        return;
    }
    JProxyBusObject* proxyBusObj = new JProxyBusObject(*bus, busName.c_str(), objPath.c_str(), sessionId);
    if (!proxyBusObj) {
        Throw("java/lang/OutOfMemoryError", NULL);
    }
    if (!env->ExceptionCheck()) {
        SetHandle(thiz, proxyBusObj);
    }
    if (env->ExceptionCheck()) {
        Bus tmp = proxyBusObj->bus;
        delete proxyBusObj;
    }
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_ProxyBusObject_destroy(JNIEnv* env,
                                                                   jobject thiz)
{
    JProxyBusObject* proxyBusObj = (JProxyBusObject*)GetHandle(thiz);
    if (!proxyBusObj) {
        return;
    }
    Bus tmp = proxyBusObj->bus;
    delete proxyBusObj;
    SetHandle(thiz, NULL);
}

static void AddInterface(jobject thiz,
                         jobject jbus,
                         jstring jinterfaceName)
{
    JNIEnv* env = GetEnv();
    Bus* bus = (Bus*)GetHandle(jbus);
    if (env->ExceptionCheck()) {
        return;
    }
    assert(bus);
    JProxyBusObject* proxyBusObj = (JProxyBusObject*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return;
    }
    assert(proxyBusObj);
    JString interfaceName(jinterfaceName);
    if (env->ExceptionCheck()) {
        return;
    }

    JLocalRef<jclass> clazz = env->GetObjectClass(thiz);
    jmethodID mid = env->GetMethodID(clazz, "addInterface", "(Ljava/lang/String;)I");
    if (!mid) {
        return;
    }
    QStatus status = (QStatus)env->CallIntMethod(thiz, mid, jinterfaceName);
    if (env->ExceptionCheck()) {
        /* AnnotationBusException */
        return;
    }
    if (ER_OK != status) {
        env->ThrowNew(CLS_BusException, QCC_StatusText(status));
        return;
    }
    const InterfaceDescription* intf = (*bus)->GetInterface(interfaceName.c_str());
    assert(intf);
    status = proxyBusObj->AddInterface(*intf);
    if (ER_OK != status) {
        env->ThrowNew(CLS_BusException, QCC_StatusText(status));
    }
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_ProxyBusObject_methodCall(JNIEnv* env,
                                                                         jobject thiz,
                                                                         jobject jbus,
                                                                         jstring jinterfaceName,
                                                                         jstring jmethodName,
                                                                         jstring jinputSig,
                                                                         jobject joutType,
                                                                         jobjectArray jargs,
                                                                         jint replyTimeoutMsecs,
                                                                         jint flags)
{
    Bus* bus = (Bus*)GetHandle(jbus);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    assert(bus);
    JProxyBusObject* proxyBusObj = (JProxyBusObject*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    assert(proxyBusObj);
    JString interfaceName(jinterfaceName);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    JString methodName(jmethodName);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    JString inputSig(jinputSig);
    if (env->ExceptionCheck()) {
        return NULL;
    }

    QStatus status;
    MsgArg args;
    Message replyMsg(**bus);
    const MsgArg* replyArgs;
    size_t numReplyArgs;
    jobject jreplyArg = NULL;
    const InterfaceDescription* intf;
    const InterfaceDescription::Member* member;

    intf = proxyBusObj->GetInterface(interfaceName.c_str());
    if (!intf) {
        AddInterface(thiz, jbus, jinterfaceName);
        if (env->ExceptionCheck()) {
            goto exit;
        }
        intf = proxyBusObj->GetInterface(interfaceName.c_str());
        assert(intf);
    }
    member = intf->GetMember(methodName.c_str());
    if (!member) {
        env->ThrowNew(CLS_BusException, QCC_StatusText(ER_BUS_INTERFACE_NO_SUCH_MEMBER));
        goto exit;
    }

    if (!Marshal(inputSig.c_str(), jargs, &args)) {
        goto exit;
    }

    if (member->annotation & MEMBER_ANNOTATE_NO_REPLY) {
        status = proxyBusObj->MethodCallAsync(*member, NULL, NULL, args.v_struct.members,
                                              args.v_struct.numMembers, NULL, replyTimeoutMsecs, flags);
        if (ER_OK != status) {
            env->ThrowNew(CLS_BusException, QCC_StatusText(status));
        }
    } else {
        status = proxyBusObj->MethodCall(*member, args.v_struct.members, args.v_struct.numMembers,
                                         replyMsg, replyTimeoutMsecs, flags);
        if (ER_OK == status) {
            replyMsg->GetArgs(numReplyArgs, replyArgs);
            if (numReplyArgs > 1) {
                MsgArg structArg(ALLJOYN_STRUCT);
                structArg.v_struct.numMembers = numReplyArgs;
                structArg.v_struct.members = new MsgArg[numReplyArgs];
                for (size_t i = 0; i < numReplyArgs; ++i) {
                    structArg.v_struct.members[i] = replyArgs[i];
                }
                structArg.SetOwnershipFlags(MsgArg::OwnsArgs);
                jreplyArg = Unmarshal(&structArg, joutType);
            } else if (numReplyArgs > 0) {
                jreplyArg = Unmarshal(&replyArgs[0], joutType);
            }
        } else if (ER_BUS_REPLY_IS_ERROR_MESSAGE == status) {
            String errorMessage;
            const char* errorName = replyMsg->GetErrorName(&errorMessage);
            if (errorName) {
                if (!strcmp("org.alljoyn.bus.BusException", errorName)) {
                    env->ThrowNew(CLS_BusException, errorMessage.c_str());
                } else {
                    ThrowErrorReplyBusException(errorName, errorMessage.c_str());
                }
            } else {
                env->ThrowNew(CLS_BusException, QCC_StatusText(status));
            }
        } else {
            env->ThrowNew(CLS_BusException, QCC_StatusText(status));
        }
    }

exit:
    if (env->ExceptionCheck()) {
        return NULL;
    } else {
        return jreplyArg;
    }
}

JNIEXPORT jobject JNICALL Java_org_alljoyn_bus_ProxyBusObject_getProperty(JNIEnv* env,
                                                                          jobject thiz,
                                                                          jobject jbus,
                                                                          jstring jinterfaceName,
                                                                          jstring jpropertyName)
{
    JProxyBusObject* proxyBusObj = (JProxyBusObject*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    assert(proxyBusObj);
    JString interfaceName(jinterfaceName);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    JString propertyName(jpropertyName);
    if (env->ExceptionCheck()) {
        return NULL;
    }

    if (!proxyBusObj->ImplementsInterface(interfaceName.c_str())) {
        AddInterface(thiz, jbus, jinterfaceName);
        if (env->ExceptionCheck()) {
            return NULL;
        }
    }

    MsgArg value;
    QStatus status = proxyBusObj->GetProperty(interfaceName.c_str(), propertyName.c_str(), value);
    if (ER_OK == status) {
        return Unmarshal(&value, CLS_Variant);
    } else {
        env->ThrowNew(CLS_BusException, QCC_StatusText(status));
        return NULL;
    }
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_ProxyBusObject_setProperty(JNIEnv* env,
                                                                       jobject thiz,
                                                                       jobject jbus,
                                                                       jstring jinterfaceName,
                                                                       jstring jpropertyName,
                                                                       jstring jsignature,
                                                                       jobject jvalue)
{
    JProxyBusObject* proxyBusObj = (JProxyBusObject*)GetHandle(thiz);
    if (env->ExceptionCheck()) {
        return;
    }
    assert(proxyBusObj);
    JString interfaceName(jinterfaceName);
    if (env->ExceptionCheck()) {
        return;
    }
    JString propertyName(jpropertyName);
    if (env->ExceptionCheck()) {
        return;
    }
    JString signature(jsignature);
    if (env->ExceptionCheck()) {
        return;
    }

    if (!proxyBusObj->ImplementsInterface(interfaceName.c_str())) {
        AddInterface(thiz, jbus, jinterfaceName);
        if (env->ExceptionCheck()) {
            return;
        }
    }

    MsgArg value;
    QStatus status;
    if (Marshal(signature.c_str(), jvalue, &value)) {
        status = proxyBusObj->SetProperty(interfaceName.c_str(), propertyName.c_str(), value);
    } else {
        status = ER_FAIL;
    }
    if (ER_OK != status) {
        env->ThrowNew(CLS_BusException, QCC_StatusText(status));
    }
}

/*
 * class org_alljoyn_bus_SignalEmitter
 */

JNIEXPORT void JNICALL Java_org_alljoyn_bus_SignalEmitter_signal(JNIEnv* env,
                                                                 jobject thiz,
                                                                 jobject jbusObj,
                                                                 jstring jdestination,
                                                                 jint sessionId,
                                                                 jstring jifaceName,
                                                                 jstring jsignalName,
                                                                 jstring jinputSig,
                                                                 jobjectArray jargs,
                                                                 jint timeToLive,
                                                                 jint flags)
{
    JBusObject* busObj = _Bus::GetBusObject(jbusObj);
    if (!busObj) {
        env->ThrowNew(CLS_BusException, QCC_StatusText(ER_BUS_NO_SUCH_OBJECT));
        return;
    }
    JString destination(jdestination);
    if (env->ExceptionCheck()) {
        return;
    }
    JString ifaceName(jifaceName);
    if (env->ExceptionCheck()) {
        return;
    }
    JString signalName(jsignalName);
    if (env->ExceptionCheck()) {
        return;
    }
    JString inputSig(jinputSig);
    if (env->ExceptionCheck()) {
        return;
    }

    MsgArg args;
    if (!Marshal(inputSig.c_str(), jargs, &args)) {
        return;
    }
    QStatus status = busObj->Signal(destination.c_str(), sessionId, ifaceName.c_str(), signalName.c_str(),
                                    args.v_struct.members, args.v_struct.numMembers, timeToLive, flags);
    if (ER_OK != status) {
        env->ThrowNew(CLS_BusException, QCC_StatusText(status));
    }
}

/*
 * class org_alljoyn_bus_Signature
 */

JNIEXPORT jobjectArray JNICALL Java_org_alljoyn_bus_Signature_split(JNIEnv* env,
                                                                    jclass clazz,
                                                                    jstring jsignature)
{
    JString signature(jsignature);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    const char* next = signature.c_str();
    if (next) {
        uint8_t count = SignatureUtils::CountCompleteTypes(next);
        JLocalRef<jobjectArray> jsignatures = env->NewObjectArray(count, CLS_String, NULL);
        if (!jsignatures) {
            return NULL;
        }
        const char* prev = next;
        for (jsize i = 0; *next; ++i, prev = next) {
            QStatus status = SignatureUtils::ParseCompleteType(next);
            if (ER_OK != status) {
                return NULL;
            }
            assert(i < count);

            ptrdiff_t len = next - prev;
            String type(prev, len);

            JLocalRef<jstring> jtype = env->NewStringUTF(type.c_str());
            if (!jtype) {
                return NULL;
            }
            env->SetObjectArrayElement(jsignatures, i, jtype);
            if (env->ExceptionCheck()) {
                return NULL;
            }
        }
        return jsignatures.move();
    } else {
        return NULL;
    }
}

/*
 * class org_alljoyn_bus_Variant
 */

JNIEXPORT void JNICALL Java_org_alljoyn_bus_Variant_destroy(JNIEnv* env,
                                                            jobject thiz)
{
    MsgArg* arg = (MsgArg*)GetHandle(thiz);
    if (!arg) {
        return;
    }
    delete arg;
    SetHandle(thiz, NULL);
}

JNIEXPORT void JNICALL Java_org_alljoyn_bus_Variant_setMsgArg(JNIEnv* env,
                                                              jobject thiz,
                                                              jlong jmsgArg)
{
    MsgArg* arg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_VARIANT == arg->typeId);
    MsgArg* argCopy = new MsgArg(*arg->v_variant.val);
    if (!argCopy) {
        Throw("java/lang/OutOfMemoryError", NULL);
        return;
    }
    SetHandle(thiz, argCopy);
    if (env->ExceptionCheck()) {
        delete argCopy;
    }
}

/*
 * class org_alljoyn_bus_BusException
 */

JNIEXPORT void JNICALL Java_org_alljoyn_bus_BusException_logln(JNIEnv* env,
                                                               jclass clazz,
                                                               jstring jline)
{
    JString line(jline);
    if (env->ExceptionCheck()) {
        return;
    }
    _QCC_DbgPrint(DBG_LOCAL_ERROR, ("%s", line.c_str()));
}

/*
 * class org_alljoyn_bus_MsgArg
 */

JNIEXPORT jint JNICALL Java_org_alljoyn_bus_MsgArg_getNumElements(JNIEnv* env,
                                                                  jclass clazz,
                                                                  jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_ARRAY == msgArg->typeId);
    return msgArg->v_array.GetNumElements();
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_getElement(JNIEnv* env,
                                                               jclass clazz,
                                                               jlong jmsgArg,
                                                               jint index)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_ARRAY == msgArg->typeId);
    assert(index < (jint)msgArg->v_array.GetNumElements());
    return (jlong) & msgArg->v_array.GetElements()[index];
}

JNIEXPORT jstring JNICALL Java_org_alljoyn_bus_MsgArg_getElemSig(JNIEnv* env,
                                                                 jclass clazz,
                                                                 jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_ARRAY == msgArg->typeId);
    return env->NewStringUTF(msgArg->v_array.GetElemSig());
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_getVal(JNIEnv* env,
                                                           jclass clazz,
                                                           jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    switch (msgArg->typeId) {
    case ALLJOYN_VARIANT: return (jlong)msgArg->v_variant.val;

    case ALLJOYN_DICT_ENTRY: return (jlong)msgArg->v_dictEntry.val;

    default: assert(0); return 0;
    }
}

JNIEXPORT jint JNICALL Java_org_alljoyn_bus_MsgArg_getNumMembers(JNIEnv* env,
                                                                 jclass clazz,
                                                                 jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_STRUCT == msgArg->typeId);
    return msgArg->v_struct.numMembers;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_getMember(JNIEnv* env,
                                                              jclass clazz,
                                                              jlong jmsgArg,
                                                              jint index)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_STRUCT == msgArg->typeId);
    assert(index < (jint)msgArg->v_struct.numMembers);
    return (jlong) & msgArg->v_struct.members[index];
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_getKey(JNIEnv* env,
                                                           jclass clazz,
                                                           jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_DICT_ENTRY == msgArg->typeId);
    return (jlong)msgArg->v_dictEntry.key;
}

JNIEXPORT jbyteArray JNICALL Java_org_alljoyn_bus_MsgArg_getByteArray(JNIEnv* env,
                                                                      jclass clazz,
                                                                      jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_BYTE_ARRAY == msgArg->typeId);
    jbyteArray jarray = env->NewByteArray(msgArg->v_scalarArray.numElements);
    if (!jarray) {
        return NULL;
    }
    jbyte* jelements = env->GetByteArrayElements(jarray, NULL);
    for (size_t i = 0; i < msgArg->v_scalarArray.numElements; ++i) {
        jelements[i] = msgArg->v_scalarArray.v_byte[i];
    }
    env->ReleaseByteArrayElements(jarray, jelements, 0);
    return jarray;
}

JNIEXPORT jshortArray JNICALL Java_org_alljoyn_bus_MsgArg_getInt16Array(JNIEnv* env,
                                                                        jclass clazz,
                                                                        jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_INT16_ARRAY == msgArg->typeId);
    jshortArray jarray = env->NewShortArray(msgArg->v_scalarArray.numElements);
    if (!jarray) {
        return NULL;
    }
    jshort* jelements = env->GetShortArrayElements(jarray, NULL);
    for (size_t i = 0; i < msgArg->v_scalarArray.numElements; ++i) {
        jelements[i] = msgArg->v_scalarArray.v_int16[i];
    }
    env->ReleaseShortArrayElements(jarray, jelements, 0);
    return jarray;
}

JNIEXPORT jshortArray JNICALL Java_org_alljoyn_bus_MsgArg_getUint16Array(JNIEnv* env,
                                                                         jclass clazz,
                                                                         jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_UINT16_ARRAY == msgArg->typeId);
    jshortArray jarray = env->NewShortArray(msgArg->v_scalarArray.numElements);
    if (!jarray) {
        return NULL;
    }
    jshort* jelements = env->GetShortArrayElements(jarray, NULL);
    for (size_t i = 0; i < msgArg->v_scalarArray.numElements; ++i) {
        jelements[i] = msgArg->v_scalarArray.v_uint16[i];
    }
    env->ReleaseShortArrayElements(jarray, jelements, 0);
    return jarray;
}

JNIEXPORT jbooleanArray JNICALL Java_org_alljoyn_bus_MsgArg_getBoolArray(JNIEnv* env,
                                                                         jclass clazz,
                                                                         jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_BOOLEAN_ARRAY == msgArg->typeId);
    jbooleanArray jarray = env->NewBooleanArray(msgArg->v_scalarArray.numElements);
    if (!jarray) {
        return NULL;
    }
    jboolean* jelements = env->GetBooleanArrayElements(jarray, NULL);
    for (size_t i = 0; i < msgArg->v_scalarArray.numElements; ++i) {
        jelements[i] = msgArg->v_scalarArray.v_bool[i];
    }
    env->ReleaseBooleanArrayElements(jarray, jelements, 0);
    return jarray;
}

JNIEXPORT jintArray JNICALL Java_org_alljoyn_bus_MsgArg_getUint32Array(JNIEnv* env,
                                                                       jclass clazz,
                                                                       jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_UINT32_ARRAY == msgArg->typeId);
    jintArray jarray = env->NewIntArray(msgArg->v_scalarArray.numElements);
    if (!jarray) {
        return NULL;
    }
    jint* jelements = env->GetIntArrayElements(jarray, NULL);
    for (size_t i = 0; i < msgArg->v_scalarArray.numElements; ++i) {
        jelements[i] = msgArg->v_scalarArray.v_uint32[i];
    }
    env->ReleaseIntArrayElements(jarray, jelements, 0);
    return jarray;
}

JNIEXPORT jintArray JNICALL Java_org_alljoyn_bus_MsgArg_getInt32Array(JNIEnv* env,
                                                                      jclass clazz,
                                                                      jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_INT32_ARRAY == msgArg->typeId);
    jintArray jarray = env->NewIntArray(msgArg->v_scalarArray.numElements);
    if (!jarray) {
        return NULL;
    }
    jint* jelements = env->GetIntArrayElements(jarray, NULL);
    for (size_t i = 0; i < msgArg->v_scalarArray.numElements; ++i) {
        jelements[i] = msgArg->v_scalarArray.v_int32[i];
    }
    env->ReleaseIntArrayElements(jarray, jelements, 0);
    return jarray;
}

JNIEXPORT jlongArray JNICALL Java_org_alljoyn_bus_MsgArg_getInt64Array(JNIEnv* env,
                                                                       jclass clazz,
                                                                       jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_INT64_ARRAY == msgArg->typeId);
    jlongArray jarray = env->NewLongArray(msgArg->v_scalarArray.numElements);
    if (!jarray) {
        return NULL;
    }
    jlong* jelements = env->GetLongArrayElements(jarray, NULL);
    for (size_t i = 0; i < msgArg->v_scalarArray.numElements; ++i) {
        jelements[i] = msgArg->v_scalarArray.v_int64[i];
    }
    env->ReleaseLongArrayElements(jarray, jelements, 0);
    return jarray;
}

JNIEXPORT jlongArray JNICALL Java_org_alljoyn_bus_MsgArg_getUint64Array(JNIEnv* env,
                                                                        jclass clazz,
                                                                        jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_UINT64_ARRAY == msgArg->typeId);
    jlongArray jarray = env->NewLongArray(msgArg->v_scalarArray.numElements);
    if (!jarray) {
        return NULL;
    }
    jlong* jelements = env->GetLongArrayElements(jarray, NULL);
    for (size_t i = 0; i < msgArg->v_scalarArray.numElements; ++i) {
        jelements[i] = msgArg->v_scalarArray.v_uint64[i];
    }
    env->ReleaseLongArrayElements(jarray, jelements, 0);
    return jarray;
}

JNIEXPORT jdoubleArray JNICALL Java_org_alljoyn_bus_MsgArg_getDoubleArray(JNIEnv* env,
                                                                          jclass clazz,
                                                                          jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_DOUBLE_ARRAY == msgArg->typeId);
    jdoubleArray jarray = env->NewDoubleArray(msgArg->v_scalarArray.numElements);
    if (!jarray) {
        return NULL;
    }
    jdouble* jelements = env->GetDoubleArrayElements(jarray, NULL);
    for (size_t i = 0; i < msgArg->v_scalarArray.numElements; ++i) {
        jelements[i] = msgArg->v_scalarArray.v_double[i];
    }
    env->ReleaseDoubleArrayElements(jarray, jelements, 0);
    return jarray;
}

JNIEXPORT jint JNICALL Java_org_alljoyn_bus_MsgArg_getTypeId(JNIEnv* env,
                                                             jclass clazz,
                                                             jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    return msgArg->typeId;
}

JNIEXPORT jbyte JNICALL Java_org_alljoyn_bus_MsgArg_getByte(JNIEnv* env,
                                                            jclass clazz,
                                                            jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_BYTE == msgArg->typeId);
    return msgArg->v_byte;
}

JNIEXPORT jshort JNICALL Java_org_alljoyn_bus_MsgArg_getInt16(JNIEnv* env,
                                                              jclass clazz,
                                                              jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_INT16 == msgArg->typeId);
    return msgArg->v_int16;
}

JNIEXPORT jshort JNICALL Java_org_alljoyn_bus_MsgArg_getUint16(JNIEnv* env,
                                                               jclass clazz,
                                                               jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_UINT16 == msgArg->typeId);
    return msgArg->v_uint16;
}

JNIEXPORT jboolean JNICALL Java_org_alljoyn_bus_MsgArg_getBool(JNIEnv* env,
                                                               jclass clazz,
                                                               jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_BOOLEAN == msgArg->typeId);
    return msgArg->v_bool;
}

JNIEXPORT jint JNICALL Java_org_alljoyn_bus_MsgArg_getUint32(JNIEnv* env,
                                                             jclass clazz,
                                                             jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_UINT32 == msgArg->typeId);
    return msgArg->v_uint32;
}

JNIEXPORT jint JNICALL Java_org_alljoyn_bus_MsgArg_getInt32(JNIEnv* env,
                                                            jclass clazz,
                                                            jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_INT32 == msgArg->typeId);
    return msgArg->v_int32;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_getInt64(JNIEnv* env,
                                                             jclass clazz,
                                                             jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_INT64 == msgArg->typeId);
    return msgArg->v_int64;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_getUint64(JNIEnv* env,
                                                              jclass clazz,
                                                              jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_UINT64 == msgArg->typeId);
    return msgArg->v_uint64;
}

JNIEXPORT jdouble JNICALL Java_org_alljoyn_bus_MsgArg_getDouble(JNIEnv* env,
                                                                jclass clazz,
                                                                jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_DOUBLE == msgArg->typeId);
    return msgArg->v_double;
}

JNIEXPORT jstring JNICALL Java_org_alljoyn_bus_MsgArg_getString(JNIEnv* env,
                                                                jclass clazz,
                                                                jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_STRING == msgArg->typeId);
    char* str = new char[msgArg->v_string.len + 1];
    if (!str) {
        Throw("java/lang/OutOfMemoryError", NULL);
        return NULL;
    }
    memcpy(str, msgArg->v_string.str, msgArg->v_string.len);
    str[msgArg->v_string.len] = 0;
    jstring jstr = env->NewStringUTF(str);
    delete [] str;
    return jstr;
}

JNIEXPORT jstring JNICALL Java_org_alljoyn_bus_MsgArg_getObjPath(JNIEnv* env,
                                                                 jclass clazz,
                                                                 jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_OBJECT_PATH == msgArg->typeId);
    char* str = new char[msgArg->v_objPath.len + 1];
    if (!str) {
        Throw("java/lang/OutOfMemoryError", NULL);
        return NULL;
    }
    memcpy(str, msgArg->v_objPath.str, msgArg->v_objPath.len);
    str[msgArg->v_objPath.len] = 0;
    jstring jstr = env->NewStringUTF(str);
    delete [] str;
    return jstr;
}

JNIEXPORT jstring JNICALL Java_org_alljoyn_bus_MsgArg_getSignature__J(JNIEnv* env,
                                                                      jclass clazz,
                                                                      jlong jmsgArg)
{
    MsgArg* msgArg = (MsgArg*)jmsgArg;
    assert(ALLJOYN_SIGNATURE == msgArg->typeId);
    char* str = new char[msgArg->v_signature.len + 1];
    if (!str) {
        Throw("java/lang/OutOfMemoryError", NULL);
        return NULL;
    }
    memcpy(str, msgArg->v_signature.sig, msgArg->v_signature.len);
    str[msgArg->v_signature.len] = 0;
    jstring jstr = env->NewStringUTF(str);
    delete [] str;
    return jstr;
}

JNIEXPORT jstring JNICALL Java_org_alljoyn_bus_MsgArg_getSignature___3J(JNIEnv* env,
                                                                        jclass clazz,
                                                                        jlongArray jarray)
{
    size_t numValues = jarray ? env->GetArrayLength(jarray) : 0;
    MsgArg* values = NULL;
    if (numValues) {
        values = new MsgArg[numValues];
        if (!values) {
            Throw("java/lang/OutOfMemoryError", NULL);
            return NULL;
        }
        jlong* jvalues = env->GetLongArrayElements(jarray, NULL);
        for (size_t i = 0; i < numValues; ++i) {
            values[i] = *(MsgArg*)(jvalues[i]);
        }
        env->ReleaseLongArrayElements(jarray, jvalues, JNI_ABORT);
    }
    jstring signature = env->NewStringUTF(MsgArg::Signature(values, numValues).c_str());
    delete [] values;
    return signature;
}

/**
 * Calls MsgArgUtils::SetV() to set the values of a MsgArg.
 *
 * @param arg the arg to set
 * @param jsignature the signature of the arg
 * @param ... the values to set
 * @return the @param arg passed in or NULL if an error occurred
 * @throws BusException if an error occurs
 */
static MsgArg* Set(JNIEnv* env,
                   MsgArg* arg,
                   jstring jsignature,
                   ...)
{
    JString signature(jsignature);
    if (env->ExceptionCheck()) {
        return NULL;
    }
    va_list argp;
    va_start(argp, jsignature);
    size_t one = 1;
    QStatus status = MsgArgUtils::SetV(arg, one, signature.c_str(), &argp);
    va_end(argp);
    if (ER_OK != status) {
        env->ThrowNew(CLS_BusException, QCC_StatusText(status));
        return NULL;
    }
    return arg;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2B(JNIEnv* env,
                                                                               jclass clazz,
                                                                               jlong jmsgArg,
                                                                               jstring jsignature,
                                                                               jbyte value)
{
    return (jlong)Set(env, (MsgArg*)jmsgArg, jsignature, value);
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2Z(JNIEnv* env,
                                                                               jclass clazz,
                                                                               jlong jmsgArg,
                                                                               jstring jsignature,
                                                                               jboolean value)
{
    return (jlong)Set(env, (MsgArg*)jmsgArg, jsignature, value);
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2S(JNIEnv* env,
                                                                               jclass clazz,
                                                                               jlong jmsgArg,
                                                                               jstring jsignature,
                                                                               jshort value)
{
    return (jlong)Set(env, (MsgArg*)jmsgArg, jsignature, value);
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2I(JNIEnv* env,
                                                                               jclass clazz,
                                                                               jlong jmsgArg,
                                                                               jstring jsignature,
                                                                               jint value)
{
    return (jlong)Set(env, (MsgArg*)jmsgArg, jsignature, value);
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2J(JNIEnv* env,
                                                                               jclass clazz,
                                                                               jlong jmsgArg,
                                                                               jstring jsignature,
                                                                               jlong value)
{
    return (jlong)Set(env, (MsgArg*)jmsgArg, jsignature, value);
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2D(JNIEnv* env,
                                                                               jclass clazz,
                                                                               jlong jmsgArg,
                                                                               jstring jsignature,
                                                                               jdouble value)
{
    return (jlong)Set(env, (MsgArg*)jmsgArg, jsignature, value);
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2Ljava_lang_String_2(JNIEnv* env,
                                                                                                 jclass clazz,
                                                                                                 jlong jmsgArg,
                                                                                                 jstring jsignature,
                                                                                                 jstring jvalue)
{
    JString value(jvalue);
    if (env->ExceptionCheck()) {
        return 0;
    }
    MsgArg* arg = Set(env, (MsgArg*)jmsgArg, jsignature, value.c_str());
    if (arg) {
        arg->Stabilize();
    }
    return (jlong)arg;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2_3B(JNIEnv* env,
                                                                                 jclass clazz,
                                                                                 jlong jmsgArg,
                                                                                 jstring jsignature,
                                                                                 jbyteArray jarray)
{
    jbyte* jelements = env->GetByteArrayElements(jarray, NULL);
    MsgArg* arg = Set(env, (MsgArg*)jmsgArg, jsignature, env->GetArrayLength(jarray), jelements);
    if (arg) {
        arg->Stabilize();
    }
    env->ReleaseByteArrayElements(jarray, jelements, JNI_ABORT);
    return (jlong)arg;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2_3Z(JNIEnv* env,
                                                                                 jclass clazz,
                                                                                 jlong jmsgArg,
                                                                                 jstring jsignature,
                                                                                 jbooleanArray jarray)
{
    /* Booleans are different sizes in Java and MsgArg, so can't just do a straight copy. */
    jboolean* jelements = env->GetBooleanArrayElements(jarray, NULL);
    size_t numElements = env->GetArrayLength(jarray);
    bool* v_bool = new bool[numElements];
    if (!v_bool) {
        Throw("java/lang/OutOfMemoryError", NULL);
        return 0;
    }
    for (size_t i = 0; i < numElements; ++i) {
        v_bool[i] = jelements[i];
    }
    MsgArg* arg = Set(env, (MsgArg*)jmsgArg, jsignature, numElements, v_bool);
    if (arg) {
        arg->SetOwnershipFlags(MsgArg::OwnsData);
    } else {
        delete [] v_bool;
    }
    env->ReleaseBooleanArrayElements(jarray, jelements, JNI_ABORT);
    return (jlong)arg;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2_3S(JNIEnv* env,
                                                                                 jclass clazz,
                                                                                 jlong jmsgArg,
                                                                                 jstring jsignature,
                                                                                 jshortArray jarray)
{
    jshort* jelements = env->GetShortArrayElements(jarray, NULL);
    MsgArg* arg = Set(env, (MsgArg*)jmsgArg, jsignature, env->GetArrayLength(jarray), jelements);
    if (arg) {
        arg->Stabilize();
    }
    env->ReleaseShortArrayElements(jarray, jelements, JNI_ABORT);
    return (jlong)arg;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2_3I(JNIEnv* env,
                                                                                 jclass clazz,
                                                                                 jlong jmsgArg,
                                                                                 jstring jsignature,
                                                                                 jintArray jarray)
{
    jint* jelements = env->GetIntArrayElements(jarray, NULL);
    MsgArg* arg = Set(env, (MsgArg*)jmsgArg, jsignature, env->GetArrayLength(jarray), jelements);
    if (arg) {
        arg->Stabilize();
    }
    env->ReleaseIntArrayElements(jarray, jelements, JNI_ABORT);
    return (jlong)arg;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2_3J(JNIEnv* env,
                                                                                 jclass clazz,
                                                                                 jlong jmsgArg,
                                                                                 jstring jsignature,
                                                                                 jlongArray jarray)
{
    jlong* jelements = env->GetLongArrayElements(jarray, NULL);
    MsgArg* arg = Set(env, (MsgArg*)jmsgArg, jsignature, env->GetArrayLength(jarray), jelements);
    if (arg) {
        arg->Stabilize();
    }
    env->ReleaseLongArrayElements(jarray, jelements, JNI_ABORT);
    return (jlong)arg;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_set__JLjava_lang_String_2_3D(JNIEnv* env,
                                                                                 jclass clazz,
                                                                                 jlong jmsgArg,
                                                                                 jstring jsignature,
                                                                                 jdoubleArray jarray)
{
    jdouble* jelements = env->GetDoubleArrayElements(jarray, NULL);
    MsgArg* arg = Set(env, (MsgArg*)jmsgArg, jsignature, env->GetArrayLength(jarray), jelements);
    if (arg) {
        arg->Stabilize();
    }
    env->ReleaseDoubleArrayElements(jarray, jelements, JNI_ABORT);
    return (jlong)arg;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_setArray(JNIEnv* env,
                                                             jclass clazz,
                                                             jlong jmsgArg,
                                                             jstring jelemSig,
                                                             jint numElements)
{
    JString elemSig(jelemSig);
    if (env->ExceptionCheck()) {
        return 0;
    }
    MsgArg* arg = (MsgArg*)jmsgArg;
    MsgArg* elements = new MsgArg[numElements];
    if (!elements) {
        Throw("java/lang/OutOfMemoryError", NULL);
        return 0;
    }
    QStatus status = arg->v_array.SetElements(elemSig.c_str(), numElements, elements);
    if (ER_OK != status) {
        delete [] elements;
        env->ThrowNew(CLS_BusException, QCC_StatusText(status));
        return 0;
    }
    arg->SetOwnershipFlags(MsgArg::OwnsArgs);
    arg->typeId = ALLJOYN_ARRAY;
    return (jlong)arg;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_setStruct(JNIEnv* env,
                                                              jclass clazz,
                                                              jlong jmsgArg,
                                                              jint numMembers)
{
    MsgArg* arg = (MsgArg*)jmsgArg;
    MsgArg* members = new MsgArg[numMembers];
    if (!members) {
        Throw("java/lang/OutOfMemoryError", NULL);
        return 0;
    }
    arg->v_struct.numMembers = numMembers;
    arg->v_struct.members = members;
    arg->SetOwnershipFlags(MsgArg::OwnsArgs);
    arg->typeId = ALLJOYN_STRUCT;
    return (jlong)arg;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_setDictEntry(JNIEnv* env,
                                                                 jclass clazz,
                                                                 jlong jmsgArg)
{
    MsgArg* arg = (MsgArg*)jmsgArg;
    MsgArg* key = new MsgArg;
    MsgArg* val = new MsgArg;
    if (!key || !val) {
        delete val;
        delete key;
        Throw("java/lang/OutOfMemoryError", NULL);
        return 0;
    }
    arg->v_dictEntry.key = key;
    arg->v_dictEntry.val = val;
    arg->SetOwnershipFlags(MsgArg::OwnsArgs);
    arg->typeId = ALLJOYN_DICT_ENTRY;
    return (jlong)arg;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_setVariant__JLjava_lang_String_2J(JNIEnv* env,
                                                                                      jclass clazz,
                                                                                      jlong jmsgArg,
                                                                                      jstring jsignature,
                                                                                      jlong jvalue)
{
    MsgArg* value = new MsgArg(*(MsgArg*)jvalue);
    if (!value) {
        Throw("java/lang/OutOfMemoryError", NULL);
        return 0;
    }
    MsgArg* arg = Set(env, (MsgArg*)jmsgArg, jsignature, value);
    if (arg) {
        arg->SetOwnershipFlags(MsgArg::OwnsArgs);
    }
    return (jlong)arg;
}

JNIEXPORT jlong JNICALL Java_org_alljoyn_bus_MsgArg_setVariant__J(JNIEnv* env,
                                                                  jclass clazz,
                                                                  jlong jmsgArg)
{
    MsgArg* arg = (MsgArg*)jmsgArg;
    MsgArg* val = new MsgArg;
    if (!val) {
        delete val;
        Throw("java/lang/OutOfMemoryError", NULL);
        return 0;
    }
    arg->v_variant.val = val;
    arg->SetOwnershipFlags(MsgArg::OwnsArgs);
    arg->typeId = ALLJOYN_VARIANT;
    return (jlong)arg;
}
