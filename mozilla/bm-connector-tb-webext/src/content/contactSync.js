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

/*Sync folder and contacts*/

const Cc = Components.classes;
const Ci = Components.interfaces;
const Cu = Components.utils;
const Cr = Components.results;
var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");
var { BMAuthService } = ChromeUtils.import("chrome://bm/content/modules/core2/BMAuthService.jsm");

var gBMContactSync = {
    _logger: Cc["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMContactSync: "),
    _proxy: null,
    _directoriesToSync: new HashMap(),
    _abManager: Cc["@mozilla.org/abmanager;1"].getService().QueryInterface(Ci.nsIAbManager),
    _observerService : Cc["@mozilla.org/observer-service;1"].getService(Ci.nsIObserverService),
    init: function() {
        let loader = Cc["@mozilla.org/moz/jssubscript-loader;1"].getService(Ci.mozIJSSubScriptLoader);
        loader.loadSubScript("chrome://bm/content/core2/ContainerService.js");
    },
    doSync: function(aFinalCallback, isBackgroundSync, aWindow) {
        this._finalCallback = aFinalCallback;
        let user = {};
        let pwd = {};
        let srv = {};
        if (bmUtils.getSettings(user, pwd, srv, !isBackgroundSync, aWindow)) {
            this._observerService.notifyObservers(null, "bm-sync-progress", "start");
            bmUtils.session.baseUrl = srv.value;
            this._login(srv.value, user.value, pwd.value);
        } else {
            this._logger.debug("not enouth settings");
            let msg = new BMXPComObject();
            msg.res = Cr.NS_ERROR_FAILURE;
            this._observerService.notifyObservers(msg, "bm-sync-progress", "stop");
            this._finalCallback();
        }
    },
    _login: function(aSrv, aLogin, aPassword) {
        let auth = BMAuthService.login(aSrv, aLogin, aPassword);
        let self = this;
        auth.then(function(logged) {
            self._sync(aSrv, logged.authKey, logged.authUser);
        }, function(aRejectReason) {
            self._logger.error(aRejectReason);
            let msg = new BMXPComObject();
            if (aRejectReason instanceof BMError) {
                msg.message = bmUtils.getLocalizedString(aRejectReason.message);
                if (aRejectReason.message == "errors.credentials"
                    || aRejectReason.message == "errors.role") {
                    bmUtils.promptService.alert(null, bmUtils.getLocalizedString("dialogs.title"),
                            msg.message);
                }
            }
            msg.res = Cr.NS_ERROR_FAILURE;
            self._observerService.notifyObservers(msg, "bm-sync-progress", "stop");
            self._finalCallback();
        }).catch(function(err) {
            self._logger.error(err);
        });
    },
    _sync: function(aSrv, aBmAuthKey, aUser) {
        let container = new BMContainerService(aSrv, aBmAuthKey, aUser);
        let result = container.sync();
        let self = this;
        result.then(function(aSuccessReason) {
            self._logger.info("END SYNC");
            let msg = new BMXPComObject();
            msg.res = Cr.NS_OK;
            aSuccessReason.forEach(function(result) {
                self._logger.info("RESULT [" + result.fname + "] success: " + result.success + ", " + result.value);
                if (!result.success) {
                    msg.res = Cr.NS_ERROR_FAILURE;
                    msg.message = result.value;
                }
            });
            self._observerService.notifyObservers(msg, "bm-sync-progress", "stop");
            self._finalCallback();        
        },
        function(aRejectReason) {
            self._logger.error(aRejectReason);
            let msg = new BMXPComObject();
            msg.res = Cr.NS_ERROR_FAILURE;
            self._observerService.notifyObservers(msg, "bm-sync-progress", "stop");
            self._finalCallback();
        }).catch(function(err) {
            self._logger.error(err);
        });
    }
}
