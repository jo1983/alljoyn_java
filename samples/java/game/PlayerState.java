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

package org.alljoyn.bus.samples.game;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusSignal;

/**
 * PlayerState contains the state information of a single player for a fictitional
 * multi-player game. In this game, each player periodially and continuously
 * broadcasts his state to all the other players of the game using signals.
 */
@BusInterface
public interface PlayerState {

    /**
     * Player position information signal. (frequently sent)
     *
     * @param x         X position of character
     * @param y         Y position of character
     * @param rotation  360 rototation of character
     */
    @BusSignal(signature="uuu")
    public void PlayerPosition(int x,
                               int y,
                               int rotation) throws BusException;
}

