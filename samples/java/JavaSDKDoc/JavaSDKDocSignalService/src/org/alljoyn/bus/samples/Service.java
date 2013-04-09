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
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;

public class Service {
	static { 
		System.loadLibrary("alljoyn_java");
	}
	
	private static final short CONTACT_PORT=42;
	private static BusAttachment mBus;
	private static SampleInterface myInterface;
	
	static boolean mSessionEstablished = false;
	static int mSessionId;
	static String mJoinerName;
	
	public static class SignalInterface implements SampleInterface, BusObject {
	    public void buttonClicked(int id) throws BusException{/*No code needed here*/}
	    public void playerPosition(int x, int y, int z) throws BusException{/*No code needed here*/}
	}
		
	private static class MyBusListener extends BusListener {
		public void nameOwnerChanged(String busName, String previousOwner, String newOwner){
			if ("com.my.well.known.name".equals(busName)) {
				System.out.println("BusAttachement.nameOwnerChanged(" + busName + ", " + previousOwner + ", " + newOwner);
			}
		}
	}
	
	public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                mBus.release();
            }
        });

		mBus = new BusAttachment("AppName", BusAttachment.RemoteMessage.Receive);
		
		Status status;
		
		SignalInterface mySignalInterface = new SignalInterface();
		
		status = mBus.registerBusObject(mySignalInterface, "/MyService/Path");
		if (status != Status.OK) {
			System.exit(0);
			return;
		}
		System.out.println("BusAttachment.registerBusObject successful");
		
		BusListener listener = new MyBusListener();
		mBus.registerBusListener(listener);
		
		status = mBus.connect();
		if (status != Status.OK) {
			System.exit(0);
			return;
		}
		System.out.println("BusAttachment.connect successful on " + System.getProperty("org.alljoyn.bus.address"));		
		
		Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
		
		SessionOpts sessionOpts = new SessionOpts();
		sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
		sessionOpts.isMultipoint = false;
		sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
		sessionOpts.transports = SessionOpts.TRANSPORT_ANY;

		status = mBus.bindSessionPort(contactPort, sessionOpts, 
				new SessionPortListener() {
			public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
				System.out.println("SessionPortListener.acceptSessionJoiner called");
				if (sessionPort == CONTACT_PORT) {
					return true;
				} else {
					return false;
				}
			}
			public void sessionJoined(short sessionPort, int id, String joiner) {
				System.out.println(String.format("SessionPortListener.sessionJoined(%d, %d, %s)", sessionPort, id, joiner));
				mSessionId = id;
				mJoinerName = joiner;
				mSessionEstablished = true;
			}
		});
		if (status != Status.OK) {
			System.exit(0);
			return;
		}
		System.out.println("BusAttachment.bindSessionPort successful");
		
		int flags = 0; //do not use any request name flags
		status = mBus.requestName("com.my.well.known.name", flags);
		if (status != Status.OK) {
			System.exit(0);
			return;
		}
		System.out.println("BusAttachment.request 'com.my.well.known.name' successful");
		
		status = mBus.advertiseName("com.my.well.known.name", SessionOpts.TRANSPORT_ANY);
		if (status != Status.OK) {
			System.out.println("Status = " + status);
			mBus.releaseName("com.my.well.known.name");
			System.exit(0);
			return;
		}
		System.out.println("BusAttachment.advertiseName 'com.my.well.known.name' successful");
		
		try {
		while (!mSessionEstablished) {
				Thread.sleep(10);
		}
		
		System.out.println(String.format("SignalEmitter sessionID = %d", mSessionId));

		SignalEmitter emitter = new SignalEmitter(mySignalInterface, mJoinerName, mSessionId, SignalEmitter.GlobalBroadcast.On);

		myInterface = emitter.getInterface(SampleInterface.class);
		 
		
		while (true) {
			myInterface.buttonClicked(0);
			myInterface.playerPosition(100, 50, 45);
			myInterface.buttonClicked(2);
			System.out.println(String.format("sending playerPostion: %d %d %d", 100, 50, 45));
			Thread.sleep(5000);
		}
		} catch (InterruptedException ex) {
			System.out.println("Interrupted");
		} catch (BusException ex) {
			System.out.println("Bus Exception: " + ex.toString());
		}
	}
}