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

/* Sync service loaded once */
this.EXPORTED_SYMBOLS = ["bmService", "BMSyncObserver"];

var { Services } = ChromeUtils.import("resource://gre/modules/Services.jsm");
var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");

function BMSyncObserver() {}
BMSyncObserver.prototype = {
    observe: function(subject, topic, data) {
        let msg;
        if (subject != null) {
            msg = subject.wrappedJSObject;
        }
        switch(data) {
            case "start":
                this._setStatusText(bmUtils.getLocalizedString("status.start"));
                break;
            case "stop":
                let text = bmUtils.getLocalizedString("status.stop");
                if (msg.res != Components.results.NS_OK) {
                    text += " " + bmUtils.getLocalizedString("status.error") + ": " + msg.message;
                }
                this._setStatusText(text);
                break;
            case "error" :
                this._setStatusText(bmUtils.getLocalizedString("dialogs.title") + ": " + msg.message);
                break;
        }
    },
    _setStatusText: function(aText) {
        let text = "" + aText;
        if (aText.length > 150) {
            text = text.substring(0, 150) + "...";
        }
        this._status.setAttribute("value", text);
    },
    register: function(aWindow) {
        this._status = aWindow.document.getElementById("bm-sync-status");
        let obs = Components.classes["@mozilla.org/observer-service;1"]
                            .getService(Components.interfaces.nsIObserverService);
        obs.addObserver(this, "bm-sync-progress", false);
    },
    unregister: function() {
        let obs = Components.classes["@mozilla.org/observer-service;1"]
                            .getService(Components.interfaces.nsIObserverService);
        obs.removeObserver(this, "bm-sync-progress");
    }
}

let bmService = {
    monitor: null,
    sync: null,
    lockSync : false,
    listAddLocalMemberAckEnabled: true,
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("bmSyncService: "),
    _timer: Components.classes["@mozilla.org/timer;1"].createInstance(Components.interfaces.nsITimer),
    _maxScriptRunTime: 10,
    _maxChromeScriptRunTime: 20,
    _disableMaxScriptRunTime: function() {
        this._maxScriptRunTime = bmUtils.getIntPref("dom.max_script_run_time", 10);
        this._maxChromeScriptRunTime = bmUtils.getIntPref("dom.max_chrome_script_run_time", 20);
        bmUtils.setIntPref("dom.max_script_run_time", 0);
        bmUtils.setIntPref("dom.max_chrome_script_run_time", 0);
    },
    _restoreMaxScriptRunTime: function() {
        bmUtils.setIntPref("dom.max_script_run_time", this._maxScriptRunTime);
        bmUtils.setIntPref("dom.max_chrome_script_run_time", this._maxChromeScriptRunTime);
    },
    init: async function() {
        let loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"]
                .getService(Components.interfaces.mozIJSSubScriptLoader);
        loader.loadSubScript("chrome://bm/content/monitor.js");
        loader.loadSubScript("chrome://messenger/content/sanitize.js");
        this.monitor = gBMMonitor;
        this.monitor.startListening();
        loader.loadSubScript("chrome://bm/content/contactSync.js");
        this.sync = gBMContactSync;
        this.sync.init();
        let versions = await bmUtils.getVersionString().catch(function(e) {
            console.error(e);
        });
        let banner = "===========================================================================================";
        this._logger.info(banner);
        this._logger.info("BlueMind Connector Started || " + versions);
        this._logger.info(banner);
        this._delay = bmUtils.getIntPref("extensions.bm.refresh.delay", 0);
        let debugReset = bmUtils.getBoolPref("extensions.bm.dev.resetOnStart", false);
        if (debugReset) {
            this._logger.info("/!\\ RESET ON START ENABLED /!\\");
            this.reset();
        }
    },
    enablePeriodicallySync: function() {
        this._logger.debug("enablePeriodicallySync");
        if (this._delay > 0) {
            this._timer.init(this, this._delay * 60 * 1000, Components.interfaces.nsITimer.TYPE_REPEATING_SLACK);
        }
    },
    disablePeriodicallySync: function() {
        this._logger.debug("disablePeriodicallySync");
        if (this._delay > 0) {
            this._timer.cancel();
        }
    },
    onShutdown: function() {
        this.disablePeriodicallySync();
    },
    doSync: function(isBackgroundSync, aWindow) {
        if (this.lockSync){
            this._logger.info("*Sync is locked*");
            return;
        }
        let self = this;
        this._logger.info("*BEGIN SYNC*");
        this.monitor.stopListening();
        this.lockSync = true;
        this._disableMaxScriptRunTime();
        this.sync.doSync(function() {self._onDoSync()}, isBackgroundSync, aWindow);
    },
    _onDoSync: function() {
        this.monitor.startListening();
        this._logger.info("*END SYNC*");
        this.lockSync = false;
        this._restoreMaxScriptRunTime();
    },
    reset: function() {
        if (this.lockSync) {
            this._logger.info("*Sync in progress*");
            return;
        }
        this._logger.info("*Reset*");
        this.monitor.stopListening();
        bmUtils.deleteAllBmDirectories();
        bmUtils.setCharPref("extensions.bm.contacts.lastSync", "0");
        bmUtils.setCharPref("extensions.bm.folders.lastSync", "0");
        bmUtils.setCharPref("extensions.bm.folders.deleted", "");
        bmUtils.setCharPref("extensions.bm.lists.lastSync", "0");
        this._clearCacheAndCookies();
        this._clearIndexedDB();
        this.monitor.startListening();
        this._logger.info("*Reset done*");
    },
    _clearCacheAndCookies: function() {
        this._logger.info("Clear cookies and cache");
        try {
            Services.prefs.setBoolPref("privacy.cpd.cache", true);
            Services.prefs.setBoolPref("privacy.cpd.cookies", true);
            Services.prefs.setBoolPref("privacy.cpd.offlineApps", true);
            let s = new Sanitizer();
            s.prefDomain = "privacy.cpd.";
            s.range = Sanitizer.getClearRange(Sanitizer.TIMESPAN_EVERYTHING);
            s.ignoreTimespan = !s.range;
            s.sanitize();
        } catch(e) {
            this._logger.error(e);
        }
    },
    _clearIndexedDB: function() {
		try {
			//clear indexedDB
			let serverUrl = bmUtils.getCharPref("extensions.bm.server", null);
			if (serverUrl) {
				this._logger.info("Clear indexedDB for uri:" + serverUrl);
				let baseURI = Services.io.newURI(serverUrl, null, null);
				let principal = Services.scriptSecurityManager.createContentPrincipal(baseURI, {});
                Services.qms.clearStoragesForPrincipal(principal);
			}
		} catch(e) {
			this._logger.error(e);
		}
	},
    observe: function(aSubject, aTopic, aData) {
        if (aTopic == 'timer-callback' && !Services.io.offline) {
            this.doSync(true);
        }
    }
}
