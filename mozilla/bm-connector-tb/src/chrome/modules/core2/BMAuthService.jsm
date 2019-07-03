/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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

/* */

this.EXPORTED_SYMBOLS = ["BMAuthService"];

Components.utils.import("resource://bm/bmUtils.jsm");

let loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"]
                    .getService(Components.interfaces.mozIJSSubScriptLoader);
loader.loadSubScript("chrome://bm/content/core2/client/InstallationClient.js");
loader.loadSubScript("chrome://bm/content/core2/client/AuthenticationClient.js");

function BMSession(aHost, aLogin) {
    this.host = aHost;
    this.login = aLogin;
    this.authKey = null;
    this.authUser = null;
}

let BMAuthService = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService()
                            .wrappedJSObject.getLogger("BMAuthService: "),
    _session: null,
    _installClient: null,
    _authClient: null,
    invalidateSession: function() {
        this._session = null;
    },
    login: function(aHost, aLogin, aPassword) {
        this._installClient = new InstallationClient(aHost);
        this._authClient = new AuthenticationClient(aHost);
        let self = this;
        let prom = new Promise(function(resolve, reject) {
            let ret = self._installClient.getSystemState();
            let session = null;
            ret.then(function(state) {
                if (state != "CORE_STATE_RUNNING") {
                    return Promise.reject(new BMError("errors.CORE_UNAVAILABLE"));
                }
                session = self._session;
                if (session && session.authKey) {
                    return self._validateSession(aHost, aLogin, aPassword, session);
                } else {
                    if (!session) {
                        session = new BMSession(aHost, aLogin);
                        self._session = session;
                    }
                    return self._login(aHost, aLogin, aPassword, session);
                }
            }).then(function(ok) {
                resolve(ok);
            }, function(aReject) {
                self._logger.debug("Fail to validate session or create a new one");
                if (session) {
                    session.authKey = null;
                    session.authUser = null;
                }
                reject(aReject);
            });
        });
        return prom;
    },
    _validateSession: function(aHost, aLogin, aPassword, aSession) {
        this._authClient._rpcClient.setAuthKey(aSession.authKey);
        let self = this;
        let prom = new Promise(function(resolve, reject) {
            let ret = self._authClient.ping();
            ret.then(function() {
                self._logger.debug("Session valid");
                resolve({
                    authUser: aSession.authUser,
                    authKey: aSession.authKey
                });
            }, function(aReject) {
                self._authClient._rpcClient.setAuthKey(null);
                let res = self._login(aHost, aLogin, aPassword, aSession);
                res.then(function(ok) {
                    resolve(ok); 
                });
            });
        });
        return prom;
    },
    _login: function(aHost, aLogin, aPassword, aSession) {
        let authKey;
        let self = this;
        let prom = new Promise(function(resolve, reject) {
            let ret = bmUtils.getOrigin();
            let origin;
            ret.then(function(orig) {
                origin = orig;
                let splitLogin = aLogin.split("@");
                if (splitLogin.length == 1) {
                    return self._getFullLogin(aHost, aLogin);
                } else {
                    return Promise.resolve(aLogin);
                }
            }).then(function(fullLogin) {
                return self._authClient.loginWithParams(fullLogin, aPassword, origin, false);
            }).then(function(loginResp) {
                if (loginResp.status != "Ok") {
                    self._logger.error("Login failed: " + loginResp.message);
                    bmUtils.removeCredentialStored(aHost);
                    bmUtils.session.password = null;
                    return Promise.reject(new BMError("errors.credentials"));
                }
                if (loginResp.authUser.roles.indexOf("hasTbird") == -1) {
                    self._logger.error("User cannot use tbird connector");
                    bmUtils.removeCredentialStored(aHost);
                    bmUtils.session.password = null;
                    return Promise.reject(new BMError("errors.role"));
                }
                authKey = loginResp.authKey;
                self._authClient._rpcClient.setAuthKey(authKey);
                return self._authClient.getCurrentUser();
            }).then(function(user) {
                self._logger.debug("Login success");
                bmUtils.session.password = aPassword;
                aSession.authKey = authKey;
                aSession.authUser = user;
                resolve({
                    authUser: user,
                    authKey: authKey
                });
            }, function(aReject) {
                reject(aReject);
            });
        });
        return prom;
    },
    _getFullLogin: function(aHost, aLogin) {
        this._logger.info("Missing right part in login [" + aLogin + "] : try using default domain of server");
        let self = this;
        let prom = new Promise(function(resolve, reject) {
            let res = self._installClient.getInfos();
            res.then(function(infos) {
                let defDomain = infos.defaultDomain;
                if (defDomain) {
                    let fullLogin = aLogin + "@" + defDomain;
                    bmUtils.updateCredentialsStored(aHost, aLogin, fullLogin);
                    bmUtils.session.user = fullLogin;
                    resolve(fullLogin);
                } else {
                    self._logger.error("Login invalid cannot find right part: " + aLogin);
                    bmUtils.removeCredentialStored(aHost);
                    bmUtils.session.password = null;
                    reject(new BMError("errors.credentials"));
                }
            }, function(aReject) {
                reject(aReject);
            });
        });
        return prom;
    }
}
