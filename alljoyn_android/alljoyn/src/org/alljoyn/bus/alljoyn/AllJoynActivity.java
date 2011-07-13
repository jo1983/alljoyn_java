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

import org.alljoyn.bus.alljoyn.AllJoynApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

import android.os.Bundle;
import android.util.Log;

public class AllJoynActivity extends Activity {
    private static final String TAG = "alljoyn.AllJoynActivity";
    

    private static final int DIALOG_DEBUG = 0xdebac1e;

    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");

    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mApp = (AllJoynApp)getApplication();
        if (mApp.running() == false) {
        	showDialog(DIALOG_DEBUG);
        }
    }
    
	public void onDestroy() {
        Log.i(TAG, "onDestroy()");
    	super.onDestroy();
 	}
	
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_DEBUG:
            return new AlertDialog.Builder(this)
            .setTitle("check in?")
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	mApp.checkin();
                }
            })
            .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	mApp.checkin();
                }
            }).create();
        }
        return null;
    }
    
    private AllJoynApp mApp;
}
