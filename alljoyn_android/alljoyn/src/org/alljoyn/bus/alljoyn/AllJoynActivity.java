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
import org.alljoyn.bus.alljoyn.DialogBuilder;

import android.app.Activity;
import android.app.Dialog;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class AllJoynActivity extends Activity {
    private static final String TAG = "alljoyn.AllJoynActivity";

    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mExitButton = (Button)findViewById(R.id.mainExit);
        mExitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_EXIT_ID);
        	}
        });
        
        mApp = (AllJoynApp)getApplication();
        
        if (mApp.running() == false) {
            boolean waitForDebuggerToSettle = true;
            if (waitForDebuggerToSettle) {
            	showDialog(DIALOG_DEBUG_ID);
            } else {
            	mApp.ensureRunning();
            }
        }
    }
    
	public void onDestroy() {
        Log.i(TAG, "onDestroy()");
    	super.onDestroy();
 	}
	
    public static final int DIALOG_EXIT_ID = 0;
    private static final int DIALOG_DEBUG_ID = 1;

    protected Dialog onCreateDialog(int id) {
    	Log.i(TAG, "onCreateDialog()");
        switch(id) {
        case DIALOG_EXIT_ID:
	        { 
	        	DialogBuilder builder = new DialogBuilder();
	        	return builder.createMainExitDialog(this, mApp);
	        }        	
        case DIALOG_DEBUG_ID:
            {
	        	DialogBuilder builder = new DialogBuilder();
	        	return builder.createDebugSettleDialog(this, mApp);
            }
        }
        assert(false);
        return null;
    }
	   
    private AllJoynApp mApp;
    private Button mExitButton;
}
