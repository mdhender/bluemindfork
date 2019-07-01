// The MIT License (MIT)
// Copyright © 2013 Mechanobiology Institute, National University of Singapore.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the “Software”), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.


/**
 * @fileoverview Oauth authentication.
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */


goog.provide('ydn.auth.Oauth');
goog.require('goog.Timer');



/**
 * Create Google client authentication app.
 * @constructor
 * @struct
 */
ydn.auth.Oauth = function() {
  /**
   * @type {Array.<string>}
   * @private
   */
  this.lib_ = this.getLib();
  var lbl = this.getApiKey() + ' ' + this.getClientId();
  this.log('oauth ' + lbl);
  /**
   * @type {ChromeExOAuth}
   * @private
   */
  this.oauth_ = null;
};


/**
 * @define {boolean} log process steps.
 */
ydn.auth.Oauth.LOG = false;


/**
 * Application name.
 * @return {string}
 */
ydn.auth.Oauth.prototype.getAppId = function() {
  throw new Error();
};


/**
 * Log if enable.
 * @param {string} msg
 */
ydn.auth.Oauth.prototype.log = function(msg) {
  if (ydn.auth.Oauth.LOG) {
    window.console.log('GapiAuth:' + msg);
  }
};


/**
 * Return scope.
 * @return {string}
 */
ydn.auth.Oauth.prototype.getScope = function() {
  return 'email';
};


/**
 * Get list of gapi client library to load.
 * Eg: ['plus', 'v1', 'storage', 'v1beta2']
 * @return {Array.<string>}
 */
ydn.auth.Oauth.prototype.getLib = function() {
  return [];
};


/**
 * @protected
 * @return {string}
 */
ydn.auth.Oauth.prototype.getApiKey = function() {
  throw new Error();
};


/**
 * @protected
 * @return {string}
 */
ydn.auth.Oauth.prototype.getClientId = function() {
  throw new Error();
};


/**
 * Start authenticate to Google.
 */
ydn.auth.Oauth.prototype.init = function() {
  this.log('init');
  this.oauth_ = ChromeExOAuth.initBackgroundPage({
    'request_url': 'https://www.google.com/accounts/OAuthGetRequestToken',
    'authorize_url': 'https://www.google.com/accounts/OAuthAuthorizeToken',
    'access_url': 'https://www.google.com/accounts/OAuthGetAccessToken',
    'consumer_key': 'anonymous',
    'consumer_secret': 'anonymous',
    'scope': this.getScope(),
    'app_name': this.getAppId()
  });
  goog.Timer.callOnce(this.checkAuth, 1, this);
};


/**
 * @protected
 */
ydn.auth.Oauth.prototype.checkAuth = function() {
  this.log('checkAuth');
  this.oauth_.authorize(goog.bind(this.handleAuthResult, this));
};


/**
 * @param {GapiAuthResult} authResult
 * @protected
 */
ydn.auth.Oauth.prototype.handleAuthResult = function(authResult) {
  this.log('handleAuthResult');
  var login_link = document.getElementById('user-login');
  if (ydn.auth.Oauth.LOG) {
    window.console.log(authResult);
  }
  this.auth_result_handled_ = true;
  if (authResult && !authResult.error) {
    login_link.textContent = 'logout';
    login_link.onclick = null;
    login_link.href = 'https://accounts.google.com/logout';
    login_link.style.display = 'none'; // don't show logout link
    this.makeApiCall();
  } else {
    login_link.style.display = '';
    login_link.href = '#';
    login_link.onclick = goog.bind(this.handleAuthClick, this);
    login_link.textContent = 'login';
    this.run();
  }
};


/**
 * @protected
 * @param {Event} event
 * @return {boolean}
 */
ydn.auth.Oauth.prototype.handleAuthClick = function(event) {
  this.log('handleAuthClick');
  this.oauth_.authorize(goog.bind(this.handleAuthResult, this));
  return false;
};


/**
 * @type {boolean}
 * @private
 */
ydn.auth.Oauth.prototype.runing_ = false;


/**
 * Run app after, authorization done.
 */
ydn.auth.Oauth.prototype.run = function() {
  if (this.runing_) {
    throw new Error('Already run');
  }
  this.runing_ = true;
};




/**
 * @protected
 */
ydn.auth.Oauth.prototype.makeApiCall = function() {
  this.log('makeApiCall');

};



