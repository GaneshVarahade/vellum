/*
 * Source https://code.google.com/p/vellum by @evanxsummers

       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements. See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.  
 */
package crumapp;

import dualcontrol.ExtendedProperties;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import localca.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vellum.crypto.rsa.RsaKeyStores;
import vellum.httpserver.VellumHttpsServer;
import vellum.type.ComparableTuple;
import vellum.util.Streams;

/**
 *
 * @author evan.summers
 */
public class CrumApp {

    Logger logger = LoggerFactory.getLogger(getClass());
    CrumConfig config = new CrumConfig();
    CrumStorage storage = new CrumStorage();
    ExtendedProperties properties; 
    String scriptName;
    Thread serverThread;
    VellumHttpsServer httpsServer;
    Map<ComparableTuple, StatusRecord> recordMap = new HashMap();
    Map<ComparableTuple, AlertRecord> alertMap = new HashMap();
    
    public void init() throws Exception {
        config.init();
        properties = config.getProperties();
        scriptName = properties.getString("scriptName", null);
        storage.init();
        httpsServer = new VellumHttpsServer(config.getProperties("httpsServer"));
        char[] keyPassword = Long.toString(new SecureRandom().nextLong() & 
                System.currentTimeMillis()).toCharArray();
        KeyStore keyStore = RsaKeyStores.createKeyStore("JKS", "crum", keyPassword, 365);
        SSLContext sslContext = SSLContexts.create(keyStore, keyPassword, 
                new CrumTrustManager(this));
        httpsServer.init(sslContext);        
        logger.info("initialized");
    }

    public void start() throws Exception {
        if (httpsServer != null) {
            httpsServer.start();
            httpsServer.createContext("/", new CrumHttpHandler(this));
            logger.info("HTTPS server started");
        }
        logger.info("started");
        if (config.systemProperties.getBoolean("crum.test")) {
            test();
        }
    }
    
    public void test() throws Exception {
        String pattern = "From: [a-z]+ \\(Cron Daemon\\)";
        logger.info("matches {}", "From: root (Cron Daemon)".matches(pattern));
        
    }

    public void stop() throws Exception {
        if (httpsServer != null) {
            httpsServer.stop();
        }
    }

    public CrumStorage getStorage() {
        return storage;
    }
    
    synchronized void add(StatusRecord statusRecord) {
        StatusRecord previous = recordMap.put(statusRecord.getKey(), statusRecord);
        if (previous == null) {
            alertMap.put(statusRecord.getKey(), new AlertRecord(statusRecord));
        } else {
            AlertRecord alertRecord = alertMap.get(statusRecord.getKey());
            logger.info("add {}", Arrays.toString(new Object[] {
                    alertRecord.getStatusRecord().getStatusType(), 
                    previous.getStatusType(), statusRecord.getStatusType()}));
            if (statusRecord.isAlertable(previous, alertRecord)) {
                alert(statusRecord, previous, alertRecord);
                alertMap.put(statusRecord.getKey(), new AlertRecord(statusRecord));
            }
        }
    }
    
    synchronized void alert(StatusRecord statusRecord, StatusRecord previous,
            AlertRecord previousAlert) {
        logger.info("ALERT {}", statusRecord.toString());
        if (scriptName != null) {
            try {
                exec(scriptName, "CRUM_STATUS=" + statusRecord.getStatusType());
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }
    
    
    public void exec(String command, String ... envp) throws Exception {
        Process process = Runtime.getRuntime().exec(command, envp);
        logger.info("process started: " + command);
        int exitCode = process.waitFor();
        logger.info("process completed {}", exitCode);
        logger.info("output\n {}\n", Streams.readString(process.getInputStream()));
    }
    
    public static void main(String[] args) throws Exception {
        try {
            CrumApp app = new CrumApp();
            app.init();
            app.start();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
