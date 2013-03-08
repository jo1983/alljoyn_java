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
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;

public class Client {
	static { 
		System.loadLibrary("alljoyn_java");
	}
	private static final short CONTACT_PORT=42;
	static BusAttachment mBus;
	
	private static ProxyBusObject mProxyObj;
	private static SampleInterface mSampleInterface;
	
	private static boolean isJoined = false;
	
	static class MyBusListener extends BusListener {
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
			
			mProxyObj =  mBus.getProxyBusObject("com.my.well.known.name",
												"/myService",
												sessionId.value,
												new Class<?>[] { SampleInterface.class});

			mSampleInterface = mProxyObj.getInterface(SampleInterface.class);
			isJoined = true;
			
		}
		public void nameOwnerChanged(String busName, String previousOwner, String newOwner){
			if ("com.my.well.known.name".equals(busName)) {
				System.out.println("BusAttachement.nameOwnerChagned(" + busName + ", " + previousOwner + ", " + newOwner);
			}
		}
		
	}

    private static class MyRunnable implements Runnable {
        private int mThreadNumber;

        MyRunnable(int n) {
            mThreadNumber = n;
        }

		public void run() {
            try {
                System.out.println("Thread " + mThreadNumber + ": Starting callculate P1");
                System.out.println("Thread " + mThreadNumber + ": Pi(1000000000) = " + mSampleInterface.Pi(1000000000));
            } catch (BusException e1) {
                e1.printStackTrace();
            }
        }
    }

	public static void main(String[] args) {
		mBus = new BusAttachment("AppName", BusAttachment.RemoteMessage.Receive);
		
		BusListener listener = new MyBusListener();
		mBus.registerBusListener(listener);
		
		Status status = mBus.connect();
		if (status != Status.OK) {
			System.exit(0);
		}
		
        System.out.println("BusAttachment.connect successful on " + System.getProperty("org.alljoyn.bus.address"));
		
		status = mBus.findAdvertisedName("com.my.well.known.name");
		if (status != Status.OK) {
			System.exit(0);
		}
		System.out.println("BusAttachment.findAdvertisedName successful " + "com.my.well.known.name");
		
		while(!isJoined) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				System.out.println("Program interupted");
			}
		}
		
		try {
			System.out.println("Ping : " + mSampleInterface.Ping("Hello World"));
			System.out.println("Concatenate : " + mSampleInterface.Concatenate("The Eagle ", "has landed!"));
			System.out.println("Fibonacci(4) : " + mSampleInterface.Fibonacci(4));
		} catch (BusException e1) {
			e1.printStackTrace();
		}

        Thread thread1 = new Thread(new MyRunnable(1));
        Thread thread2 = new Thread(new MyRunnable(2));

        thread1.start();
        thread2.start();

        try {
            thread2.join();
            thread1.join();
        } catch (InterruptedException ex) {
        }
        mBus.release();
	}
}