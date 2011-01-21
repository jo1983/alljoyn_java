/*
 * Copyright 2009-2011, Qualcomm Innovation Center, Inc.
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

package org.alljoyn.bus;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public class AllJoynDaemon {
    private String daemonAddress;
    private Process daemon;
    private String pid;
    private Thread errorReader;
    private Thread inputReader;

    private class StreamReader extends Thread {
        private BufferedReader br;
            
        public StreamReader(InputStream stream) {
            br = new BufferedReader(new InputStreamReader(stream));
        }

        public void run() {
            try {
                String line = null;
                while ((line = br.readLine()) != null) {
                    //System.err.println(line);
                    String[] split = line.split(" *= *");
                    if (split.length == 2 && split[0].matches(".*PID")) {
                        pid = split[1];
                    }
                }
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }

    public AllJoynDaemon() {
        daemonAddress = "unix:abstract=AllJoynDaemonTest";
        if ("The Android Project".equals(System.getProperty("java.vendor"))) {
            return;
        }
        try {
            String address = "BUS_SERVER_ADDRESSES=" + daemonAddress + ";tcp:addr=0.0.0.0,port=5343";
            daemon = Runtime.getRuntime().exec("bbdaemon", new String[] { address });
            errorReader = new StreamReader(daemon.getErrorStream());
            inputReader = new StreamReader(daemon.getInputStream());
            errorReader.start();
            inputReader.start();
            /* Wait a bit for bbdaemon to get initialized */
            while (pid == null) {
                Thread.currentThread().sleep(100);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (daemon != null) {
                Runtime.getRuntime().exec("kill -s SIGINT " + pid);
                daemon.waitFor();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String address() {
        return daemonAddress;
    }
}
