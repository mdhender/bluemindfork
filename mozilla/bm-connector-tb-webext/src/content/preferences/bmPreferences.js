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


var { Services } = ChromeUtils.import("resource://gre/modules/Services.jsm");

var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");
var { bmService, BMSyncObserver } = ChromeUtils.import("chrome://bm/content/modules/bmService.jsm");
var { BMAuthService } = ChromeUtils.import("chrome://bm/content/modules/core2/BMAuthService.jsm");

var gBMPreferences = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMPreferences: "),
    init: function() {
        console.trace("coucou");
    },
    promptUsernameAndPassword: function() {
        let server = document.getElementById("bmserver").value;
        if (!server || server.trim() == "") return;
        let ww = Components.classes["@mozilla.org/embedcomp/window-watcher;1"].getService(Components.interfaces.nsIWindowWatcher);
        let authPrompt = ww.getNewAuthPrompter(window);
        let user = {};
        let pwd = {};
        bmUtils.getCredentialStored(server, user, pwd);
        if (!user.value || !pwd.value) {
            user.value = bmUtils.session.user;
            pwd.value = bmUtils.session.password;
        }
        let oldUser = user.value;
        authPrompt.promptUsernameAndPassword(bmUtils.getLocalizedString("dialogs.title"),
                                             bmUtils.getLocalizedString("authprompt.message") + " " + server,
                                             server,
                                             Components.interfaces.nsIAuthPrompt.SAVE_PASSWORD_PERMANENTLY,
                                             user, pwd);
        if (!user.value || !pwd.value) return;
        //remove old credentials stored
        bmUtils.removeCredentialStored(server, user.value);
        bmUtils.session.user = user.value;
        bmUtils.session.password = pwd.value;
        //reset if login changed
        if (oldUser && user.value != oldUser) {
            BMAuthService.invalidateSession();
            this.reset();
        }
    },
    reset: function() {
        this._logger.info("RESET");
        bmService.reset();
        let observerService = Components.classes["@mozilla.org/observer-service;1"]
                        .getService(Components.interfaces.nsIObserverService);
        observerService.notifyObservers(null, "reload-bm-tabs", "please");
    },
    showLog: function() {
        let file = Components.classes["@mozilla.org/file/directory_service;1"]
                            .getService(Components.interfaces.nsIProperties)
                            .get("TmpD", Components.interfaces.nsIFile);
        file.append("BlueMind");
        if( !file.exists() || !file.isDirectory() ) {
            this._logger.error("debug directory not exist");
            return;
        }
        let profDir = Components.classes["@mozilla.org/file/directory_service;1"]
                             .getService(Components.interfaces.nsIProperties)
                             .get("ProfD", Components.interfaces.nsIFile);
        let profDirName = profDir.leafName;
        let fileName = "bm-connector-tb-log-" + profDirName + ".txt";
        file.append(fileName);
        if (!file.exists()) {
            this._logger.error("debug file not exist");
            return;
        }
        try {
            file.launch();
        } catch(e) {
            this._logger.info(e);
            try {
                let uri = Components.classes["@mozilla.org/network/io-service;1"]
                    .getService(Components.interfaces.nsIIOService)
                    .newFileURI(file);
                let protocolSvc = Components.classes["@mozilla.org/uriloader/external-protocol-service;1"]
                 .getService(Components.interfaces.nsIExternalProtocolService);
                protocolSvc.loadURI(uri);
            } catch(e) {
                this._logger.error(e);
            }
        }
    },
    loadBmPrefs: function() {
        let bmserver = document.getElementById("bmserver");
        bmserver.value = bmUtils.getCharPref("extensions.bm.server", "https://xxxx");
        let checkbox = document.getElementById("bm-checkbox-debug");
        if (checkbox) {
            let debug = bmUtils.getBoolPref("extensions.bm.log.debug", false);
            checkbox.setAttribute("checked", "" + debug);
        }
    },
    saveBmPrefs: function() {
        let oldServer = bmUtils.getCharPref("extensions.bm.server", "https://xxxx");
        let bmserver = document.getElementById("bmserver");
        let newServer = bmserver.value;
        gBMPreferences._logger.debug("Save BM sever pref, old:" + oldServer + ", new:" + newServer);
        if (newServer && newServer != oldServer && newServer != "https://xxxx") {
            bmUtils.setCharPref("extensions.bm.server", newServer);
            BMAuthService.invalidateSession();
            this.reset();
        }
    },
    openSettingsTab: function() {
        gBMPreferences.saveBmPrefs();
        let observerService = Components.classes["@mozilla.org/observer-service;1"]
                        .getService(Components.interfaces.nsIObserverService);
        observerService.notifyObservers(null, "open-bm-settings", "please");
        window.close();
    },
    importCollected: function() {
        if (!bmUtils.promptService.confirm(null, bmUtils.getLocalizedString("dialogs.title"),
                                           bmUtils.getLocalizedString("import.confirm")))
            return;
        gBMPreferences.saveBmPrefs();
        let abManager = Components.classes["@mozilla.org/abmanager;1"]
                .getService().QueryInterface(Components.interfaces.nsIAbManager);
        let directory = abManager.getDirectory("moz-abmdbdirectory://history.mab");
        let it = directory.childCards;
        let count = 0;
        while (it.hasMoreElements()) {
            let card = it.getNext().QueryInterface(Components.interfaces.nsIAbCard);
            let id = card.getProperty("bm-id", null);
            if (!id) {
                let uid = bmUtils.randomUUID();
                card.setProperty("bm-id", uid);
                card.setProperty("bm-added", "true");
                card.setProperty("bm-local", "false");
                card.setProperty("bm-ab-uri", null);
                directory.modifyCard(card);
                this._logger.debug("=> card marked as added");
                count++;
            }
        }
        this._logger.info("Import " + count + " collected cards");
        bmUtils.promptService.alert(null, bmUtils.getLocalizedString("dialogs.title"),
                        "" + count + " " + bmUtils.getLocalizedString("import.willbeimported"));
        bmService.doSync();
    },
    addXMPPAccount: function() {
        let user = {};
        let pwd = {};
        let srv = {};
        if (bmUtils.getSettings(user, pwd, srv, true)) {
            let hostName = srv.value.replace("https://", "");
            let xmppLogin = bmUtils.getEmailOfImapAccount(srv.value);
            if (!xmppLogin) {
                return;
            }
            let hasBmXmpp = false;
            let imAccounts = Services.accounts.getAccounts();
            while (imAccounts.hasMoreElements()) {
                let acc = imAccounts.getNext();
                this._logger.debug("im account id:" + acc.id);
                let prefs = Services.prefs.getBranch("messenger.account." + acc.id + ".options.");
                try {
                    let imServer = prefs.getCharPref("server");
                    this._logger.debug("im server:" + imServer);
                    if (imServer == hostName) {
                        hasBmXmpp = true;
                    }
                } catch(e) {
                    //pref not exist
                }
            }
            if (!hasBmXmpp) {
                
                Services.obs.addObserver(this, "account-connect-error", false);
                Services.obs.addObserver(this, "account-connected", false);
                
                let acc = Services.accounts.createAccount(xmppLogin, "prpl-jabber");
                acc.password = pwd.value;
                acc.setString("server", hostName);
                acc.autoLogin = true;
                acc.save();
                
                try {
                    acc.connect();
                } catch (e) {
                    //ok
                }
            
                let inServer = MailServices.accounts.createIncomingServer(xmppLogin, "prpl-jabber", "im");
                inServer.wrappedJSObject.imAccount = acc;
                
                let xmpp = MailServices.accounts.createAccount();
                inServer.valid = false;
                xmpp.incomingServer = inServer;
                inServer.valid = true;
                MailServices.accounts.notifyServerLoaded(inServer);
                
                try {
                    acc.connect();
                } catch (e) {
                    //ok
                }
            } else {
                Services.wm.getMostRecentWindow("mail:3pane").showChatTab();
                window.close();
            }
        }
    },
    observe: function(aObject, aTopic, aData) {
        let acc = aObject.QueryInterface(Components.interfaces.imIAccount);
        let prplAccount = acc.prplAccount;
        
        if (aTopic == "account-connect-error") {
            let mailWindow = Services.wm.getMostRecentWindow("mail:3pane");
            let params = {
                exceptionAdded: false,
                sslStatus: prplAccount.sslStatus,
                prefetchCert: true,
                location: prplAccount.connectionTarget
            };
            mailWindow.openDialog("chrome://pippki/content/exceptionDialog.xul", "", "chrome,centerscreen,modal", params);
            if (params.exceptionAdded) {
                try {
                    acc.disconnect();
                    acc.connect();
                    MailServices.accounts.notifyServerLoaded(acc.incomingServer);
                } catch (e) {
                    //ok
                }
            }
            Services.obs.removeObserver(this, "account-connect-error");
        } else {
            bmUtils.promptService.alert(null, bmUtils.getLocalizedString("dialogs.title"),
                                    bmUtils.getLocalizedString("im.accountcreated"));
            Services.wm.getMostRecentWindow("mail:3pane").showChatTab();
            Services.obs.removeObserver(this, "account-connected");
            window.close();
        }
    },
    openVacationDialog: function() {
        gBMPreferences.saveBmPrefs();
        window.openDialog("chrome://bm/content/preferences/bmVacation.xul", "", "modal,centerscreen,resizable=no", null);
    },
    // replace of deprecated XUL <preference> for TB >= 67 
    toggleDebug: function() {
        let debug = bmUtils.getBoolPref("extensions.bm.log.debug", false);
        bmUtils.setBoolPref("extensions.bm.log.debug", !debug);
    }
};

// called on bm pref tab load
// do not register load event on pref window: load event can be already triggered if window was opened on another pref tab
window.setTimeout(function() {
    bmService.disablePeriodicallySync();
    gBMPreferences.loadBmPrefs();
}, 100);

// register unlod on pref window only if bm tab has been displayed
window.addEventListener("unload", function(e) {
    gBMPreferences.saveBmPrefs();
    bmService.enablePeriodicallySync();
}, false);