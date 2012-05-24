package org.alljoyn.bus;

import org.alljoyn.bus.annotation.BusAnnotation;
import org.alljoyn.bus.annotation.BusAnnotations;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusSignal;

@BusInterface(name="org.alljoyn.bus.InterfaceWithAnnotations")
@BusAnnotations({@BusAnnotation(name="org.freedesktop.DBus.Deprecated", value="true")})
public interface InterfaceWithAnnotations {

    @BusMethod(signature="s", replySignature="s")
    @BusAnnotations({@BusAnnotation(name="name", value="value"), @BusAnnotation(name="name2", value="value2")})
    public String Ping(String inStr) throws BusException;

    @BusMethod(signature="s", replySignature="s")
    public String Pong(String inStr) throws BusException;


    @BusMethod(signature="s")
    @BusAnnotations({@BusAnnotation(name="org.freedesktop.DBus.Deprecated", value="true"),
        @BusAnnotation(name="org.freedesktop.DBus.Method.NoReply", value="true")})
    public void Pong2(String inStr) throws BusException;


    @BusSignal()
    @BusAnnotations({@BusAnnotation(name="org.freedesktop.DBus.Deprecated", value="true")})
    public void Signal() throws BusException;
}
