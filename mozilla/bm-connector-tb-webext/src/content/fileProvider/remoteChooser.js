/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

var { Services } = ChromeUtils.import("resource://gre/modules/Services.jsm");
var { percentEncode } = ChromeUtils.import("resource://gre/modules/Http.jsm");

var { bmUtils } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");

var gBMRemoteChooser = {
  _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMRemoteChooser: "),
  _callback: null,
  _baseUri: null,
  _notify: {},
  loadWindow: function () {
    console.trace("loadWindow");
    Services.scriptloader.loadSubScript("chrome://bm/content/notifyTools.js", this._notify, "UTF-8");
    let user = {};
    let pwd = {};
    let srv = {};
    if (bmUtils.getSettings(user, pwd, srv, true, window)) {
      if (!Services.io.offline) {
        this._auth(srv.value, user.value, pwd.value);
        this._baseUri = srv.value;
      }
    } else {
      this._logger.info("not enouth settings");
    }
  },
  _auth: function (aServer, aLogin, aPassword) {
    let host = aServer.replace("https://", "");
    let cookies = Services.cookies.getCookiesFromHost(host, {});
    for (let cookie of cookies) {
      Services.cookies.remove(cookie.host, cookie.name, cookie.path, {});
    }
    let url = aServer + "/login/native";
    let self = this;
    let rpcClient = Components.classes["@blue-mind.net/rpcclient;1"].getService().wrappedJSObject;
    let cmd = rpcClient.newCommand(url, "GET");
    cmd.onSuccess = function (xhr) {
      self._logger.debug("Response headers:" + xhr.getAllResponseHeaders());
      let cookies = Services.cookies.getCookiesFromHost(host, {});
      let hpsSession = null;
      for (let cookie of cookies) {
        if (cookie.name == "HPSSESSION") {
          hpsSession = cookie.value;
        }
      }
      self._login(aServer, aLogin, aPassword, hpsSession);
    };
    cmd.onFailure = function (xhr) {
      let resp = xhr.responseText;
      self._logger.error("Fail to login: " + resp);
    };
    rpcClient.execute(cmd);
  },
  _login: function (aServer, aLogin, aPassword, aHpsSession) {
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
    cmd.getData = function () {
      return postData.map(p => p[0] + "=" + percentEncode(p[1])).join("&");
    };
    cmd.onSuccess = function (xhr) {
      self._onAuth(xhr.responseText, xhr, data);
    };
    cmd.onFailure = function (xhr) {
      let resp = xhr.responseText;
      self._logger.error("Fail to login: " + resp);
    };
    rpcClient.execute(cmd);
  },
  _onAuth: function (aResponseText, aRequest, aData) {
    if (aResponseText.indexOf("<meta name=\"X-BM-Page\" content=\"maintenance\"/>") != -1) {
      this._logger.info("BM server is in maintenance");
      //FIXME what ?
    } else {
      this._logger.debug("Response headers:" + aRequest.getAllResponseHeaders());
      this._logger.debug("cookie host:" + aData.cookiesHost);
      let cookies = Services.cookies.getCookiesFromHost(aData.cookiesHost, {});
      let ssoCookie = null;
      for (let cookie of cookies) {
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
  _openWebPage: function () {
    this._notify.notifyTools.notifyBackground({command: "openPopup", url: this._baseUri + "/chooser/#"});
  },
}
