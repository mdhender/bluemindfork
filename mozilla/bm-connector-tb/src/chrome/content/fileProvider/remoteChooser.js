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

/* Remote file chooser */

const Cc = Components.classes;
const Ci = Components.interfaces;
const Cu = Components.utils;
const Cr = Components.results;

Cu.import("resource://gre/modules/Http.jsm");
Cu.import("resource://gre/modules/Services.jsm");
Cu.import("resource://bm/bmUtils.jsm");

var gBMRemoteChooser = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMRemoteChooser: "),
    _callback: null,
    _baseUri: null,
    loadWindow: function() {
        this._callback = window.arguments[0];
        let user = {};
        let pwd = {};
        let srv = {};
        if (bmUtils.getSettings(user, pwd, srv, true, window)) {
          if (!Services.io.offline) {
            this._auth(srv.value, user.value, pwd.value);
            this._baseUri=srv.value;
          }
        } else {
            this._logger.info("not enouth settings");
        }
    },
    _auth: function(aServer, aLogin, aPassword) {
		let host = aServer.replace("https://", "");
		let cookies = Services.cookies.getCookiesFromHost(host, {});
		while (cookies.hasMoreElements()) {
			let cookie = cookies.getNext().QueryInterface(Ci.nsICookie2);
			Services.cookies.remove(cookie.host, cookie.name, cookie.path, false, {});
		}
		let url = aServer + "/login/native";
		let self = this;
		let rpcClient = Components.classes["@blue-mind.net/rpcclient;1"].getService().wrappedJSObject;
		let cmd = rpcClient.newCommand(url, "GET");
		cmd.onSuccess = function(xhr) {
            self._logger.debug("Response headers:" + xhr.getAllResponseHeaders());
            let cookies = Services.cookies.getCookiesFromHost(host, {});
            let hpsSession = null;
            while (cookies.hasMoreElements()) {
                let cookie = cookies.getNext().QueryInterface(Ci.nsICookie2);
                if (cookie.name == "HPSSESSION") {
                    hpsSession = cookie.value;
                }
            }
            self._login(aServer, aLogin, aPassword, hpsSession);
		};
		cmd.onFailure = function(xhr) {
            let resp = xhr.responseText;
            self._logger.error("Fail to login: " + resp);
		};
		rpcClient.execute(cmd);
	},
    _login: function(aServer, aLogin, aPassword, aHpsSession) {
        let host = aServer.replace("https://", "");
		let url = aServer + "/login/index.html";
		let postData = [["login", aLogin],
						["password", aPassword],
						["priv", "priv"],
						["askedUri", "/"],
                        ["csrfToken", aHpsSession]];
		let data = {
			server: aServer,
            hpsSession: aHpsSession,
			cookiesHost: host
		};
		let self = this;
		let rpcClient = Components.classes["@blue-mind.net/rpcclient;1"].getService().wrappedJSObject;
		let cmd = rpcClient.newCommand(url, "POST");
		cmd.headers = [["Content-Type", "application/x-www-form-urlencoded; charset=utf-8"]];
		cmd.getData = function() {
			return postData.map(p => p[0] + "=" + percentEncode(p[1])).join("&");
		};
		cmd.onSuccess = function(xhr) {
			self._onAuth(xhr.responseText, xhr, data);
		};
		cmd.onFailure = function(xhr) {
			let resp = xhr.responseText;
			self._logger.error("Fail to login: " + resp);
		};
		rpcClient.execute(cmd);
    },
    _onAuth: function(aResponseText, aRequest, aData) {
		if (aResponseText.indexOf("<meta name=\"X-BM-Page\" content=\"maintenance\"/>") != -1) {
			this._logger.info("BM server is in maintenance");
			//FIXME what ?
		} else {
			this._logger.debug("Response headers:" + aRequest.getAllResponseHeaders());
            this._logger.debug("cookie host:" + aData.cookiesHost);
			let cookies = Services.cookies.getCookiesFromHost(aData.cookiesHost, {});
			let ssoCookie = null;
			while (cookies.hasMoreElements()) {
				let cookie = cookies.getNext().QueryInterface(Ci.nsICookie2);
				if (cookie.name == "BMHPS") {
					ssoCookie = cookie.value;
				}
			}
			this._logger.debug("ssoCookie:" + ssoCookie);
			if (!ssoCookie && aRequest.responseText.indexOf("Login failed for user") != -1) {
                bmUtils.promptService.alert(null, bmUtils.getLocalizedString("dialogs.title"),
                            bmUtils.getLocalizedString("errors.credentials"));
                bmUtils.removeCredentialStored(aData.server);
                bmUtils.session.password = null;
			} else {
				this._openWebPage();
			}
		}
    },
    _openWebPage: function() {
        let rBrowser = document.getElementById("remote-browser");
        let win;
        let self = this;
        rBrowser.addEventListener("DOMContentLoaded", function(e) {
            win = rBrowser.contentWindow.wrappedJSObject;
            let options = {
                success: function(links) {
                    var files = [];
                    for (var i = 0; i < links.length; i++) {
                      var size = 0;
                      var sizeKeys = ['Content-Length', 'size'];
                      for (var j = 0; j < links[i].metadata.length; j++) {
                        if (sizeKeys.indexOf(links[i].metadata[j]['key']) >= 0) {
                          size = links[i].metadata[j]['value'];
                          break;
                        }
                      }
                      files.push({
                        size: size,       
                        path: links[i].path, 
                        name: links[i].name,
                      });
                    }
                    self._callback(files);
                    window.close();
                },
                cancel: function() {
                    window.close();
                },
                multi: true,
                close: false
            };
            let opts = Components.utils.cloneInto(options, win, {cloneFunctions: true});
            win.application.setOptions(opts);
        });
        if (!Components.interfaces.nsIMsgCloudFileProvider) {
          // TB 68 loadURI extra param
          let params = {
            triggeringPrincipal: Services.scriptSecurityManager.getSystemPrincipal()
          };
          rBrowser.loadURI(this._baseUri + "/chooser/#", params);
        } else {
          rBrowser.loadURI(this._baseUri + "/chooser/#");
        }
    },
}