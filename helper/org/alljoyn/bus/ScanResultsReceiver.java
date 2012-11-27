/**
 * @file
 */

/******************************************************************************
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
 ******************************************************************************/
package org.alljoyn.bus;

import android.content.BroadcastReceiver;
import org.alljoyn.bus.ScanResultMessage;
import org.alljoyn.bus.AllJoynAndroidExt;

import android.content.Intent;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;

public class ScanResultsReceiver extends BroadcastReceiver{

	AllJoynAndroidExt jniProximity;
	//
	// Need a constructor that will take the instance on Proximity Service
	//
	public ScanResultsReceiver(AllJoynAndroidExt jniProximity){
		super();
		this.jniProximity = jniProximity; 
	}
	
	public ScanResultsReceiver getScanReceiver(){
		return this;
	}
	
	@Override
	public void onReceive(Context c, Intent intent){
		
		if(intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
			Log.v("ScanResultsReceiver", "SCAN_RESULTS_AVAILABLE_ACTION received");
			
			List<ScanResult> scanResults = jniProximity.wifiMgr.getScanResults();
			
			if(scanResults.size() == 0){
				Log.v("ScanResultsReceiver", "Result size = 0");
				jniProximity.scanResultsObtained = true;
				return;
			}
			else{
				Log.v("ScanResultsReceiver", "Result size  = " + Integer.toString(scanResults.size()));
			}
			
			jniProximity.lastScanTime = System.currentTimeMillis();
			Log.v("ScanResultsReceiver","lastScanTime =-=-=-=-=-=-=-=-="+jniProximity.lastScanTime);
			
			jniProximity.scanResultMessage = new ScanResultMessage[scanResults.size()];
						
			String currentBSSID = jniProximity.wifiMgr.getConnectionInfo().getBSSID();
			int currentBSSIDIndex = 0;
			for (ScanResult result : scanResults){

				jniProximity.scanResultMessage[currentBSSIDIndex] = new ScanResultMessage();
				jniProximity.scanResultMessage[currentBSSIDIndex].bssid = result.BSSID;
				jniProximity.scanResultMessage[currentBSSIDIndex].ssid = result.SSID;
				if(currentBSSID !=null && currentBSSID.equals(result.BSSID)){
					jniProximity.scanResultMessage[currentBSSIDIndex].attached = true;
				}
				currentBSSIDIndex++;
				
			}
			currentBSSIDIndex = 0;
			jniProximity.scanResultsObtained = true;
	}

}
}
