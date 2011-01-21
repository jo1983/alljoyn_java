/**
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

package org.alljoyn.bus.samples.game;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.MessageContext;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.bus.ifaces.DBusProxyObj;

import java.lang.reflect.Method;

/**
 * AllJoyn multi-player game sample.
 * This contrived example implements an overly simple multi-player game where each
 * instance of this class shares its PlayerState with all other players in the game.
 */
public class Game implements PlayerState, BusObject {
    static {
        System.loadLibrary("alljoyn_java");
    }

    /** Bus connection */
    private BusAttachment bus = null;

    /**
     * Print usage message.
     */
    public static void Usage() {
        System.out.println("Usage: Game <player_name>");
        return;
    }

    /**
     * Main entry point for org.alljoyn.bus.samples.game.Game
     */
    public static void main(String[] args) {

        /* Check args */
        if (args.length != 1) {
            Usage();
            return;
        }

        try {
            Game game = new Game(args);
        } catch (InterruptedException ex) {
            System.out.println("Interrupted");
        } catch (GameException ex) {
            System.out.println("Game exitied: " + ex.getMessage());
        } catch (BusException ex) {
            System.out.println("Bus Exception: " + ex.toString());
        } catch (NoSuchMethodException ex) {
            System.out.println("Exception: " + ex.toString());
        }
    }

    private Game(String[] args) throws BusException, GameException, InterruptedException,
                                       NoSuchMethodException {

        /* Create a bus connection and connect to the bus */
        bus = new BusAttachment(getClass().getName());
        Status status = bus.connect();
        if (Status.OK != status) {
            throw new GameException("BusAttachment.connect() failed with " + status.toString());
        }

        /* Register the service */
        status = bus.registerBusObject(this, "/game/player/" + args[0]);
        if (Status.OK != status) {
            throw new GameException("BusAttachment.registerBusObject() failed: " +
                                    status.toString());
        }

        /* Request a well-known name */
        DBusProxyObj control = bus.getDBusProxyObj();
        DBusProxyObj.RequestNameResult res = control.RequestName("org.alljoyn.bus.samples.game",
                                                                DBusProxyObj.REQUEST_NAME_NO_FLAGS);
        if (res != DBusProxyObj.RequestNameResult.PrimaryOwner) {
            throw new GameException("Failed to obtain well-known name");
        }

        /* Create a signal emitter so we can send out our position */
        SignalEmitter emitter = new SignalEmitter(this);
        PlayerState playerState = emitter.getInterface(PlayerState.class);

        /* Register a signal handler to receive other players' state */
        status = bus.registerSignalHandlers(this);
        if (Status.OK != status) {
            throw new GameException("Cannot register signal handler");
        }

        /* Periodically broadcast this player's state information */
        Thread thisThread = Thread.currentThread();
        while (true) {
            thisThread.sleep(50);
            playerState.PlayerPosition(100, 200, 180);
        }
    }

    /**
     * Handler that receives player position information from remote players.
     * @param x         X position.
     * @param y         Y position.
     * @param rotation  Character rotation.
     */
    @BusSignalHandler(iface="org.alljoyn.bus.samples.game.PlayerState", signal="PlayerPosition")
    public void PlayerPosition(int x, int y, int rotation) {
        /* Determine which player sent the position message by examing the message context */
        MessageContext ctx = bus.getMessageContext();

        /* Update player position on the screen */
        System.out.println(ctx.objectPath + ": " + x + "," + y + "," + rotation);
    }
}
