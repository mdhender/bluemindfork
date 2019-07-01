// Copyright 2012 YDN Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview Google Appengine authentication.
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */


goog.provide('ydn.auth.GaeAuth');
goog.require('goog.Timer');
goog.require('goog.async.Delay');
goog.require('ydn.client.AdaptorClient');
goog.require('ydn.client.FilteredClient');
goog.require('ydn.client.IOAuthProvider');



/**
 * Create Google Appengine authentication app.
 * @constructor
 * @struct
 * @implements {ydn.client.IOAuthProvider}
 */
ydn.auth.GaeAuth = function() {

  /**
   * Auth result.
   * @type {YdnApiUser}
   * @private
   */
  this.auth_result_ = null;

  /**
   * @type {boolean}
   * @private
   */
  this.auth_ = false;

  /**
   * @type {boolean}
   * @private
   */
  this.runing_ = false;
  /**
   * @type {YdnApiToken}
   * @private
   */
  this.gdata_token_ = null;
  /**
   * @type {goog.async.Deferred}
   * @private
   */
  this.df_gdata_token_ = null;

  var me = this;
  if (chrome && chrome.runtime) {
    chrome.runtime.onMessageExternal.addListener(
        function(request, sender, sendResponse) {
          window.console.log(sender['url'] + ' ' + request);
          if (goog.string.startsWith(sender['url'], mbi.app.base.LOGIN_ORIGIN)) {
            sendResponse('close');
            me.doLogin_();
          }
        });
  }
};


/**
 * @define {boolean} log process steps.
 */
ydn.auth.GaeAuth.LOG = false;


/**
 * Log if enable.
 * @param {string|*} msg
 */
ydn.auth.GaeAuth.prototype.log = function(msg) {
  if (ydn.auth.GaeAuth.LOG) {
    if (goog.isString(msg)) {
      window.console.log('GaeAuth:' + msg);
    } else {
      window.console.log(msg);
    }
  }
};


/**
 * @protected
 * @type {string}
 */
ydn.auth.GaeAuth.prototype.app_id = 'wiki'; // 'test';


/**
 * @define {string} login server domain
 */
ydn.auth.GaeAuth.GAE_HOSTNAME = 'www.yathit.com';


/**
 * @const
 * @type {string}
 */
ydn.auth.GaeAuth.LOGIN_ORIGIN = 'https://' + ydn.auth.GaeAuth.GAE_HOSTNAME;


/**
 * Login.
 * @param {string} url
 * @param {function(this: T, *)} cb
 * @param {T=} opt_scope
 * @template T
 */
ydn.auth.GaeAuth.prototype.request = function(url, cb, opt_scope) {
  var xhr = new XMLHttpRequest();
  url = 'https://' + ydn.auth.GaeAuth.GAE_HOSTNAME + url;
  xhr.open('GET', url, true);
  xhr.withCredentials = true;
  xhr.onload = function(e) {
    var json = null;
    if (xhr.status < 400) {
      json = JSON.parse(xhr.responseText);
    }

    cb.call(opt_scope, json);

  };
  xhr.send();
};


/**
 * Login.
 * @param {function(this: T, YdnApiToken)} cb
 * @param {T=} opt_scope
 * @template T
 */
ydn.auth.GaeAuth.prototype.authorize = function(cb, opt_scope) {

  var redirect = ydn.auth.GaeAuth.LOGIN_ORIGIN + '/pm.html?pm=' + location.href;
  var url = '/a/' + this.app_id + '/token?url=' + encodeURIComponent(redirect);

  this.request(url, function(json) {
    var a = document.getElementById('user-login');
    var name = document.getElementById('user-name');
    this.log(json);
    var tokens = json['Tokens'];
    var token = /** @type {YdnApiToken} */ (tokens[0]);
    if (token.has_token) {
      this.gdata_token_ = token;
      token.expires = goog.now() + token.expires_in;
    } else {
      this.gdata_token_ = null;
      a.textContent = 'authorize';
      a.href = token.authorize_url;
      a.style.display = '';
    }
    cb.call(opt_scope, token);
  }, this);

};


/**
 * Login.
 * @param {function(this: T, YdnApiUser)} cb
 * @param {T=} opt_scope
 * @template T
 */
ydn.auth.GaeAuth.prototype.login = function(cb, opt_scope) {

  var url = '/rpc_login?pm=' + location.href;

  this.request(url, function(json) {
    var a = document.getElementById('user-login');
    var name = document.getElementById('user-name');
    this.log(json);
    var user = /** @type {YdnApiUser} */ (json['User']);
    if (user.is_login) {
      name.textContent = user.email;
      a.href = user.logout_url;
      a.style.display = 'none';
      a.textContent = 'logout';
      name.style.display = '';
    } else {
      a.href = user.login_url;
      a.textContent = 'login';
      a.style.display = '';
      name.style.display = 'none';
    }
    this.auth_result_ = user;
    if (cb) {
      cb.call(opt_scope, user);
    }
  }, this);

};


/**
 * Setup clients after login.
 * @param {GapiToken} token
 */
ydn.auth.GaeAuth.prototype.setupClients = function(token) {
  if (!token.token_type) {
    token.token_type = 'Bearer';
  }
  gapi.auth.setToken(token);
  var xhr = mbi.app.shared.getXhrManager();
  var gdata_client = new ydn.client.OAuthClient(this);
  ydn.client.setClient(gdata_client, ydn.http.Scopes.GOOGLE_CLIENT);
  var proxy_url = ydn.auth.GaeAuth.LOGIN_ORIGIN + '/a/' + this.app_id + '/proxy/';
  var proxy = new ydn.client.SimpleClient(
      xhr, undefined, proxy_url);
  var gdata_proxy = new ydn.client.AdaptorClient(gdata_client, function(args) {
    var req = /** @type {ydn.client.HttpRequestData} */ (args);
    req.path = proxy_url + req.path;
    return req;
  });
  ydn.client.setClient(proxy, ydn.http.Scopes.PROXY);
  var gse = new ydn.client.FilteredClient(function(req) {
    return req.method == 'GET';
  }, gdata_client, gdata_proxy);
  ydn.client.setClient(gse, ydn.http.Scopes.GSE);
};


/**
 * @private
 */
ydn.auth.GaeAuth.prototype.doLogin_ = function() {
  this.login(function(user) {
    if (user && user.is_login) {
      var client = new ydn.client.SimpleClient();
      ydn.client.setClient(client, ydn.http.Scopes.DEFAULT);
      ydn.client.setClient(client, ydn.http.Scopes.LOGIN);
    }
  }, this);
};


/**
 * Start authenticate to Google.
 * Login.
 */
ydn.auth.GaeAuth.prototype.auth = function() {
  this.log('auth');
  goog.asserts.assert(!this.auth_, 'already auth');
  var login_link = document.getElementById('user-login');
  login_link.addEventListener('click', function(e) {
    e.preventDefault();
    var w = 600;
    var h = 400;
    var left = (screen.width / 2) - (w / 2);
    var top = (screen.height / 2) - (h / 2);
    var url = e.target.href;
    window.open(url, undefined, 'toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=no, resizable=no, copyhistory=no, width=' + w + ', height=' + h + ', top=' + top + ', left=' + left);
    return true;
  }, true);
  var me = this;

  this.doLogin_();
  this.auth_ = true;
};


/**
 * @return {YdnApiUser}
 */
ydn.auth.GaeAuth.prototype.getAuthResult = function() {
  return this.auth_result_;
};


/**
 * Run app after, authorization done.
 * @param {YdnApiToken} token
 */
ydn.auth.GaeAuth.prototype.run = function(token) {
  if (this.runing_) {
    throw new Error('Already run');
  }
  this.runing_ = true;
};


goog.exportProperty(ydn.auth.GaeAuth.prototype, 'auth',
    ydn.auth.GaeAuth.prototype.auth);


