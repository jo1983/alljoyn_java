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

import android.content.SharedPreferences; 
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
        
        /*
         * Update the shared preferences for this app.
         */
        updatePrefs();

        /*
         * Start a service to represent AllJoyn to the Android system.  We
         * aren't interested in getting involved with Service lifecycle since
         * our goal is really to reproduce a system daemon; so we just spin
         * up the daemon when the application is run.  We use the Android 
         * Service to convince the application framework not to kill us.  In
         * particular we set the service priority to foreground.
         */
        Intent intent = new Intent(this, AllJoynService.class);
        mRunningService = startService(intent);
        if (mRunningService == null) {
            Log.i(TAG, "onCreate(): failed to startService()");
        }
	}
    
    ComponentName mRunningService = null;
    
    private void updatePrefs() {
        Log.i(TAG, "updatePrefs()");
        
        SharedPreferences sharedPreferences = getSharedPreferences("AllJoynPreferences", MODE_PRIVATE);

        /*
         * When calling into the daemon, use this as the program name
         */
        mName = sharedPreferences.getString("name", "alljoyn-daemon"); 
        Log.i(TAG, "updatePrefs(): name = " + mName);
        
        /*
         * When calling into the daemon use the --session flag if true
         */
        mSession = sharedPreferences.getBoolean("session", false);
        Log.i(TAG, "updatePrefs(): session = " + mSession);

        /*
         * When calling into the daemon use the --system flag if true
         */
        mSystem = sharedPreferences.getBoolean("system", false);
        Log.i(TAG, "updatePrefs(): system = " + mSystem);
        
        /*
         * When calling into the daemon use the --internal flag if true
         */
        mInternal = sharedPreferences.getBoolean("internal", true);
        Log.i(TAG, "updatePrefs(): internal = " + mInternal);

        /*
         * If not --internal, use this configuration
         */
        mConfig = sharedPreferences.getString("config", 
            "<busconfig>" + 
            "  <type>alljoyn</type>" + 
            "  <listen>unix:abstract=alljoyn</listen>" + 
            "  <listen>tcp:addr=0.0.0.0,port=9955</listen>" +
            "  <policy context=\"default\">" +
            "    <allow send_interface=\"*\"/>" +
            "    <allow receive_interface=\"*\"/>" +
            "    <allow own=\"*\"/>" +
            "    <allow user=\"*\"/>" +
            "    <allow send_requested_reply=\"true\"/>" +
            "    <allow receive_requested_reply=\"true\"/>" +
            "  </policy>" +
            "  <limit name=\"auth_timeout\">32768</limit>" +
            "  <limit name=\"max_incomplete_connections_tcp\">16</limit>" +
            "  <limit name=\"max_completed_connections_tcp\">64</limit>" +
            "  <alljoyn module=\"ipns\">" +
            "    <property interfaces=\"*\"/>" +
            "  </alljoyn>" +
            "</busconfig>");
        Log.i(TAG, "updatePrefs(): config = " + mConfig);
        
        /*
         * When calling into the daemon use the --no-bt flag if true
         */
        mNoBT = sharedPreferences.getBoolean("no-bt", true);
        Log.i(TAG, "updatePrefs(): no-bt = " + mNoBT);
        
        /*
         * When calling into the daemon use the --no-tcp flag if true
         */
        mNoTCP = sharedPreferences.getBoolean("no-tcp", false);
        Log.i(TAG, "updatePrefs(): no-tcp = " + mNoTCP);
        
        /*
         * When calling into the daemon, use this verbosity level
         */
        mVerbosity = sharedPreferences.getString("verbosity", "7"); 
        Log.i(TAG, "updatePrefs(): verbosity = " + mVerbosity);
    }
      
    public boolean running() {
    	return mThread.isAlive();
    }
    
    public void checkin() {
        Log.i(TAG, "checkin()");
        if (mThread.isAlive() == false) {
        	doit();
        }
    }
    
    private void doit() {
        Log.i(TAG, "doit()");
        
        Log.i(TAG, "doit(): Starting thread.");
        mThread.start();
    }
    
    private void undoit() {
        Log.i(TAG, "undoit()");
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

    private static native int runDaemon(Object[] argv, Object[] envNames, Object[] envValues, String config);
    
    private boolean mSession = false;
    private boolean mSystem = false;
    private boolean mInternal = true;
    private String mConfig = null;
    private boolean mNoBT = true;
    private boolean mNoTCP = false;
    private String mVerbosity = "7";
    private String mName = "alljoyn-daemon";

    private Thread mThread = new Thread() {
        public void run()
        {
            Log.i(TAG, "mThread.run()");
            ArrayList<String> argv = new ArrayList<String>();
            argv.add(mName);
            argv.add("--nofork");
            argv.add("--verbosity=" + mVerbosity);
                       
            if (mSystem) {
            	argv.add("--system");
            }
            
            if (mSession) {
            	argv.add("--session");
            }
            
            if (mInternal) {
            	argv.add("--internal");
            }
            
            if (mNoBT) {
                argv.add("--no-bt");            	
            }
            
            if (mNoTCP) {
                argv.add("--no-tcp");            	
            }

            ArrayList<String> envNames = new ArrayList<String>();
            ArrayList<String> envValues = new ArrayList<String>();
            envNames.add("ER_DEBUG_ALL"); envValues.add("7");
            Log.i(TAG, "mThread.run(): calling runDaemon()"); 
            runDaemon(argv.toArray(), envNames.toArray(), envValues.toArray(), mConfig);
            Log.i(TAG, "mThread.run(): returned from runDaemon().  Self-immolating.");
            undoit();
            System.exit(0);
        }
    };
      
    static {
        System.loadLibrary("daemon-jni");
    }
}
