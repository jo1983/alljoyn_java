/*
 * Copyright 2011, Qualcomm Innovation Center, Inc.
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

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;

import java.util.ArrayList;

import org.alljoyn.bus.alljoyn.AllJoynService;

public class AllJoynApp extends Application {
    private static final String TAG = "alljoyn.AllJoynApp";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
        Intent intent = new Intent(this, AllJoynService.class);
        mRunningService = startService(intent);
        if (mRunningService == null) {
            Log.i(TAG, "onCreate(): failed to startService()");
        }
	}
    
    ComponentName mRunningService = null;
    
    public void doit() {
        Log.i(TAG, "doit(): Starting thread.");
        mThread.start();
    }
    
    @Override
    public void onLowMemory() {
    	super.onLowMemory();
    }
    
    @Override
    public void onTerminate() {
    	super.onTerminate();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    }

    private static native int runDaemon(Object[] argv, String config);

    private Thread mThread = new Thread() {
        public void run()
        {
            Log.i(TAG, "mThread.run()");
            ArrayList<String> argv = new ArrayList<String>();
            argv.add("alljoyn-daemon-service");  // argv[0] is the name (choose one, but must be there).                                
            argv.add("--internal");              // argv[1] use the internal daemon configuration.                                      
            argv.add("--verbosity=7");           // argv[2] set daemon verbosity
            argv.add("--nofork");                // argv[3] don't fork another process, we rely on running "here"
            argv.add("--no-bt");                 // argv[4] don't even try to run bluetooth
            Log.i(TAG, "mThread.run(): calling runDaemon()");
            runDaemon(argv.toArray(), "");
            Log.i(TAG, "mThread.run(): returned from runDaemon().  Self-immolating.");
            System.exit(0);
        }
    };
      
    static {
        System.loadLibrary("daemon-jni");
    }
}
