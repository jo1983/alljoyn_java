/*
 * Copyright 2010-2011, 2013, Qualcomm Innovation Center, Inc.
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
package org.alljoyn.bus.samples;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;

public class Client {
	static { 
		System.loadLibrary("alljoyn_java");
	}
	private static final short CONTACT_PORT=42;
	static BusAttachment mBus;
	
	public static class SampleSignalHandler  {
		@BusSignalHandler(iface="org.alljoyn.bus.samples.SampleInterface", signal="buttonClicked")
		public void buttonClicked(int id) {
			switch(id) {
			case 0:
				startNewGame();
				break;
			case 1:
				continueGame();
				break;
			case 2:
				quitGame();
				break;
			default:
				break;
			}
		}

		@BusSignalHandler(iface="org.alljoyn.bus.samples.SampleInterface", signal="playerPosition")
		public void playerPosition(int x, int y, int z) {
		    updatePlayerPosition(x,y,z);
		}
		
		public void startNewGame() {
			System.out.println("Starting a new Game");
		}
		
		public void continueGame() {
			System.out.println("Continuing Game");
		}
		
		public void quitGame() {
			System.out.println("Quiting Game");
		}
		
		public void updatePlayerPosition(int x, int y, int z) {
			System.out.println(String.format("Players position is %d, %d, %d.", x, y, z));
		}
	}
	
	public static void main(String[] args) {
	    Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                mBus.release();
            }
        });
	    
		class MyBusListener extends BusListener {
			public void foundAdvertisedName(String name, short transport, String namePrefix) {
				System.out.println(String.format("BusListener.foundAdvertisedName(%s, %d, %s)", name, transport, namePrefix));
				short contactPort = CONTACT_PORT;
				SessionOpts sessionOpts = new SessionOpts();
				sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
				sessionOpts.isMultipoint = false;
				sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
				sessionOpts.transports = SessionOpts.TRANSPORT_ANY;
				
				Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

				mBus.enableConcurrentCallbacks();
				
				Status status = mBus.joinSession(name, contactPort, sessionId, sessionOpts,	new SessionListener());
				if (status != Status.OK) {
					System.exit(0);
				}
				System.out.println(String.format("BusAttachement.joinSession successful sessionId = %d", sessionId.value));
			}
			public void nameOwnerChanged(String busName, String previousOwner, String newOwner){
				if ("com.my.well.known.name".equals(busName)) {
					System.out.println("BusAttachement.nameOwnerChagned(" + busName + ", " + previousOwner + ", " + newOwner);
				}
			}
			
		}
		
		mBus = new BusAttachment("AppName", BusAttachment.RemoteMessage.Receive);
		
		BusListener listener = new MyBusListener();
		mBus.registerBusListener(listener);
		
		Status status = mBus.connect();
		if (status != Status.OK) {
			System.exit(0);
		}
		System.out.println("BusAttachment.connect successful");
		
		SampleSignalHandler mySignalHandlers = new SampleSignalHandler();
		
		status = mBus.registerSignalHandlers(mySignalHandlers);
		if (status != Status.OK) {
			System.exit(0);
		}
		System.out.println("BusAttachment.registerSignalHandlers successful");

		status = mBus.addMatch("type='signal',iface='org.alljoyn.bus.samples.SampleInterface',member='playerPosition'");
		if (status != Status.OK) {
			System.exit(0);
		}
		System.out.println("BusAttachment.addMatch successful");
		
		status = mBus.findAdvertisedName("com.my.well.known.name");
		if (status != Status.OK) {
			System.exit(0);
		}
		System.out.println("BusAttachment.findAdvertisedName successful " + "com.my.well.known.name");
		while(true) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				System.out.println("Program interupted");
			}
		}
	}
}