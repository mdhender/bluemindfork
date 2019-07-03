/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
*/

/* Start observer loaded on tb start*/

Components.utils.import("resource://gre/modules/XPCOMUtils.jsm");
Components.utils.import("resource://gre/modules/FileUtils.jsm");
Components.utils.import("resource://gre/modules/Services.jsm");
Components.utils.import("resource://bm/bmUtils.jsm");
Components.utils.import("resource://bm/bmService.jsm");

function StartObserver() {
    if (this._observerAdded) {
        return;
    }
    let observerService = Components.classes["@mozilla.org/observer-service;1"]
                                    .getService(Components.interfaces.nsIObserverService);
    if (ChromeUtils.generateQI) {
        // TB >= 65
        observerService.addObserver(this, "profile-after-change", false);
    } else {
        observerService.addObserver(this, "final-ui-startup", false);
    }
    observerService.addObserver(this, "quit-application", false);
    this._observerAdded = true;
}

StartObserver.prototype = {
    classDescription: "StartObserver XPCOM Component",
    classID:          Components.ID("{96392759-b5d1-451f-9373-e82bd03daf93}"),
    contractID:       "@blue-mind.net/startobserver;1",
    /* Needed for XPCOMUtils NSGetModule */
    QueryInterface:  ChromeUtils.generateQI ?
        ChromeUtils.generateQI([Components.interfaces.nsISupports, Components.interfaces.nsIObserver])
        : XPCOMUtils.generateQI([Components.interfaces.nsISupports, Components.interfaces.nsIObserver]),
    _ioService: Components.classes["@mozilla.org/network/io-service;1"].getService(Components.interfaces.nsIIOService),
    _startReceived: false,
    observe: function(aSubject, aTopic, aData) {
        switch (aTopic) {
            case "final-ui-startup":
            case "profile-after-change":
                if (!this._startReceived) {
                    this._startReceived = true;
                    this._startup();
                }
                break;
            case "quit-application":
                this._shutdown();
                break;
            default:
                break;
        }
    },
    _consoleListener: null,
    _startup: function() {
        //everything logged in js console is logged in file
        let consoleService = Components.classes["@mozilla.org/consoleservice;1"].getService(Components.interfaces.nsIConsoleService);
        this._consoleListener = {
            logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject
                                .getLogger("Console: "),
            QueryInterface: function(aIID) {
                if (!aIID.equals(Components.interfaces.nsISupports)
                    && !aIID.equals(Components.interfaces.nsIConsoleListener)) {
                    throw Components.results.NS_ERROR_NO_INTERFACE;
                }
                return this;
            },
            observe: function(aMsg) {
                let message = aMsg.message;
                if (!message.indexOf("[JavaScript Warning:") == 0)
                    this.logger.info(message);
            }
        }
        consoleService.registerListener(this._consoleListener);
        
        //install certificates
        this._installCertificates();
        
        //content policy accept websocket tbird >= esr-45
        if (Components.interfaces.nsIMsgContentPolicy) {
            let policy = Components.classes["@mozilla.org/messenger/content-policy;1"].getService(Components.interfaces.nsIMsgContentPolicy);
            policy.addExposedProtocol("wss");
        }

        //init bm service
        bmService.init();
    },
    _shutdown: function() {
        this._consoleListener.logger.debug("Shutdown");
        bmService.monitor.stopListening();
        let consoleService = Components.classes["@mozilla.org/consoleservice;1"].getService(Components.interfaces.nsIConsoleService);
        consoleService.unregisterListener(this._consoleListener);
        bmService.onShutdown();
    },
    _installCertificates: function() {
        let cert = "cacert.pem";
        this._addCertificate(cert);
    },
    _addCertificate: function(CertName) {
        try {
            let channel;
            let input;
            try {
                if (this._ioService.newChannel2) {
                    channel = this._ioService.newChannel2("chrome://bm/content/certs/" + CertName,
                                                             null, //aOriginCharset
                                                             null, //aBaseURI
                                                             null, //aLoadingNode
                                                             Services.scriptSecurityManager.getSystemPrincipal(),
                                                             null, //aTriggeringPrincipal
                                                             Components.interfaces.nsILoadInfo.SEC_ALLOW_CROSS_ORIGIN_DATA_IS_NULL,
                                                             Components.interfaces.nsIContentPolicy.TYPE_OTHER);
                } else {
                    channel = this._ioService.newChannel("chrome://bm/content/certs/" + CertName, null, null);
                }
                input = channel.open();
            } catch (e) {
                //file not found
                Components.utils.reportError(e);
                return;
            }
            let certDB = Components.classes["@mozilla.org/security/x509certdb;1"].getService(Components.interfaces.nsIX509CertDB);
            let scriptableStream = Components.classes["@mozilla.org/scriptableinputstream;1"].createInstance(Components.interfaces.nsIScriptableInputStream);
            let fileUri = channel.URI;
            scriptableStream.init(input);
            let certfile = scriptableStream.read(input.available());
            scriptableStream.close();
            input.close();
            let beginCert = "-----BEGIN CERTIFICATE-----";
            let endCert = "-----END CERTIFICATE-----";
        
            certfile = certfile.replace(/[\r\n]/g, "");
            let begin = certfile.indexOf(beginCert);
            let end = certfile.indexOf(endCert);
            let cert = certfile.substring(begin + beginCert.length, end);
            
            // load cert in memory to get is fingerPrint
            let daCert = certDB.constructX509FromBase64(cert);
            let fingerPrint = (daCert.sha1Fingerprint != null ? daCert.sha1Fingerprint : daCert.md5Fingerprint);
                    
            //search cert by fingerPrint
            let newKey = this._getCertDbKeyByFingerPrint(fingerPrint);
            
            if (!newKey) {
                let file = null;
                try {
                    file = fileUri.QueryInterface(Components.interfaces.nsIFileURL).file;
                } catch (e) {
                    //TB >= 60
                }
                if (file) {
                    //import server cert
                    try {
                        certDB.importCertsFromFile(file, Components.interfaces.nsIX509Cert.CA_CERT);
                    } catch(e) {
                        // TB <= 45
                        certDB.importCertsFromFile(null, file, Components.interfaces.nsIX509Cert.CA_CERT);
                    }
                    newKey = this._getCertDbKeyByFingerPrint(fingerPrint);
                     //trust
                    let newCert = certDB.findCertByDBKey(newKey, null);
                    certDB.setCertTrust(newCert, Components.interfaces.nsIX509Cert.CA_CERT, Components.interfaces.nsIX509CertDB.TRUSTED_SSL);
                } else {
                    //TB >= 60
                    certDB.addCertFromBase64(cert, "C,C,C", "");
                }
            }
        } catch (e) {
            Components.utils.reportError(e);
        }
    },
    _getCertDbKeyByFingerPrint: function(aFingerPrint) {
        let certDB;
        try {
            // nsIX509CertDB2 was collapsed into nsIX509CertDB API
            certDB = Components.classes["@mozilla.org/security/x509certdb;1"].getService(Components.interfaces.nsIX509CertDB2);
        } catch(e) {
            certDB = Components.classes["@mozilla.org/security/x509certdb;1"].getService(Components.interfaces.nsIX509CertDB);
        }
        let it = certDB.getCerts().getEnumerator();
        while(it.hasMoreElements()) {
            let crt = it.getNext().QueryInterface(Components.interfaces.nsIX509Cert);
            if (crt.sha1Fingerprint == aFingerPrint || crt.md5Fingerprint == aFingerPrint) {
                return crt.dbKey;
            }
        }
        return null;
    },
    _getCaCertForEntityCert: function(cert) {
        let nextCertInChain = cert;
        let lastSubjectName = "";
        while(true) {
            if (nextCertInChain == null) {
                return null;
            }
            if ((nextCertInChain.type == Components.interfaces.nsIX509Cert.CA_CERT) || 
                (nextCertInChain.subjectName == lastSubjectName)) {
                break;
            }
            lastSubjectName = nextCertInChain.subjectName;
            nextCertInChain = nextCertInChain.issuer;
        }
        return nextCertInChain;
    }
};

/**
 * XPCOMUtils.generateNSGetFactory was introduced in Mozilla 2 (Firefox 4).
 * XPCOMUtils.generateNSGetModule is for Mozilla 1.9.0 (Firefox 3.0).
 */
if (XPCOMUtils.generateNSGetFactory)
    var NSGetFactory = XPCOMUtils.generateNSGetFactory([StartObserver]);
else
    var NSGetModule = XPCOMUtils.generateNSGetModule([StartObserver]);