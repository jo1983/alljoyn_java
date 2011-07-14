/*
 * Copyright 2010-2011, Qualcomm Innovation Center, Inc.
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
package org.alljoyn.bus.alljoyn;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.app.PendingIntent;
import android.os.IBinder;
import android.util.Log;

public class AllJoynService extends Service {
	private static final String TAG = "alljoyn.AllJoynService";

	public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return null;
	}
	
	public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
        
        CharSequence title = "AllJoyn";
        CharSequence message = "Service started.";
        Intent intent = new Intent(this, AllJoynActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new Notification(R.drawable.icon, null, System.currentTimeMillis());
        notification.setLatestEventInfo(this, title, message, pendingIntent);
        notification.flags |= Notification.DEFAULT_SOUND | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

        Log.i(TAG, "onCreate(): startForeground()");
        startForeground(NOTIFICATION_ID, notification);
 	}

	public void onDestroy() {
        super.onDestroy();
		Log.i(TAG, "onDestroy()");
 	}
    
	public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
		Log.i(TAG, "onStartCommand()");
        return START_STICKY;
	}
	
    private static final int NOTIFICATION_ID = 0xdefaced;
}