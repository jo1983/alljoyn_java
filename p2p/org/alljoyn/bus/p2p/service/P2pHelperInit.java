/*
 * Copyright 2012, Qualcomm Innovation Center, Inc.
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

package org.alljoyn.bus.p2p.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Process;

import org.alljoyn.bus.p2p.service.P2pHelperService;
import android.net.wifi.WifiManager;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * This class prepares the P2pHelperService singleton
 * Applications using the bundled daemon must invoke PrepareP2pHelper() to enable
 * use of Wi-Fi Direct transport.
 */
public class P2pHelperInit {
    
    private final static String TAG = "P2pHelperService";
    private static Context sContext;
    public static P2pHelperService sP2pHelper;
    
    public static Context getContext(){
    	return sContext;
    }
    
    /**
     * Initialize P2pHelperService
     * @param context The application context
     */
    public static void PrepareP2pHelper(Context context) {
        sContext = context.getApplicationContext();
        Log.v(TAG,"Saved Application Context");
        
        // Instantiate and start the P2pHelperService
        sP2pHelper = new P2pHelperService(sContext, "null:");
        sP2pHelper.startup();
    }
    
}
