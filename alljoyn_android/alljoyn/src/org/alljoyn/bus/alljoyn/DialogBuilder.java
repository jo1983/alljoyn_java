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

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.widget.Button;
import android.util.Log;

public class DialogBuilder {
    private static final String TAG = "alljoyn.Dialogs";
      
    public Dialog createMainExitDialog(Activity activity, final AllJoynApp application) {
       	Log.i(TAG, "createmainExitDialog()");
    	final Dialog dialog = new Dialog(activity);
    	dialog.requestWindowFeature(dialog.getWindow().FEATURE_NO_TITLE);
    	dialog.setContentView(R.layout.mainexitdialog);
	        	       	
    	Button yes = (Button)dialog.findViewById(R.id.mainExitOk);
    	yes.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View view) {
    			dialog.cancel();
    			application.exit();
    		}
    	});
	            
    	Button no = (Button)dialog.findViewById(R.id.mainExitCancel);
    	no.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View view) {
    			dialog.cancel();
    		}
    	});
    	
    	return dialog;
    }
    
    public Dialog createDebugSettleDialog(Activity activity, final AllJoynApp application) {
       	Log.i(TAG, "createmainExitDialog()");
    	final Dialog dialog = new Dialog(activity);
    	dialog.requestWindowFeature(dialog.getWindow().FEATURE_NO_TITLE);
    	dialog.setContentView(R.layout.debugsettledialog);
	        	       	
    	Button yes = (Button)dialog.findViewById(R.id.debugSettleOk);
    	yes.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View view) {
    			application.ensureRunning();
    			dialog.cancel();
    		}
    	});
	            
    	Button no = (Button)dialog.findViewById(R.id.debugSettleCancel);
    	no.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View view) {
    			application.ensureRunning();
    			dialog.cancel();
    		}
    	});
    	
    	return dialog;
    }
}
