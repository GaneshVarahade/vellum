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
package cromapp;

import dualcontrol.ExtendedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author evan.summers
 */
public class CromConfig {

    Logger logger = LoggerFactory.getLogger(CromHttpHandler.class);
    ExtendedProperties systemProperties = new ExtendedProperties(System.getProperties());
    String confFileName = systemProperties.getString("crom.conf", "conf/crom.conf");

    public void init() {
    }

    public ExtendedProperties getProperties() {
        ExtendedProperties appProperties = new ExtendedProperties();
        appProperties.put("alertScript", "scripts/alert.sh");
        return appProperties;
    }

    public ExtendedProperties getProperties(String prefix) {
        ExtendedProperties properties = new ExtendedProperties();
        if (prefix.equals("httpsServer")) {
            properties.put("port", 8443);
        } else if (prefix.equals("ssl")) {
            char[] pass = systemProperties.getPassword("crom.ssl.pass");
            properties.put("keyStoreLocation", "keystores/crom.jks");
            properties.put("pass", pass);
        }
        logger.info("getProperties {} {}", prefix, properties);
        return properties;
    }
}