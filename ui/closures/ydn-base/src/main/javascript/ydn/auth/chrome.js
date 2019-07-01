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
 * @fileoverview Chrome authentication.
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */


goog.provide('ydn.auth.Chrome');
goog.require('goog.Timer');



/**
 * Create Google client authentication app.
 * @param {Function} on_ready on ready callback.
 * @param {Array.<string>=} opt_lib Get list of gapi client library to load.
 * Eg: ['plus', 'v1', 'storage', 'v1beta2']
 * @constructor
 * @struct
 */
ydn.auth.Chrome = function(on_ready, opt_lib) {
  /**
   * @type {Function}
   */
  this.on_ready = on_ready;
  /**
   * @type {Array.<string>}
   * @private
   */
  this.lib_ = opt_lib || [];
  /**
   * @private
   */
  this.auth_result_ = null;
};


/**
 * @define {boolean} log process steps.
 */
ydn.auth.Chrome.LOG = true;


/**
 * Log if enable.
 * @param {string} msg
 */
ydn.auth.Chrome.prototype.log = function(msg) {
  if (ydn.auth.Chrome.LOG) {
    window.console.log('GapiAuth:' + msg);
  }
};


/**
 * Start authenticate to Google.
 */
ydn.auth.Chrome.prototype.init = function() {
  this.log('init');
  goog.Timer.callOnce(this.checkAuth, 1, this);
};


/**
 * @protected
 */
ydn.auth.Chrome.prototype.checkAuth = function() {
  this.log('checkAuth');
  chrome.identity.getAuthToken({
    'interactive': false
  }, goog.bind(this.handleAuthResult, this));
};


/**
 * @param {string=} authResult
 * @protected
 */
ydn.auth.Chrome.prototype.handleAuthResult = function(authResult) {
  this.log('handleAuthResult');
  var login_link = document.getElementById('user-login');
  if (ydn.auth.Chrome.LOG) {
    window.console.log(authResult);
  }
  this.auth_result_ = authResult;
  if (authResult) {
    login_link.textContent = 'logout';
    login_link.onclick = null;
    login_link.href = 'https://accounts.google.com/logout';
    login_link.style.display = 'none'; // don't show logout link
    gapi.auth.setToken(authResult);
    this.makeApiCall();
  } else {
    login_link.style.display = '';
    login_link.href = '#';
    login_link.onclick = goog.bind(this.handleAuthClick, this);
    login_link.textContent = 'login';
    this.on_ready();
    this.on_ready = null;
  }
};


/**
 * @protected
 * @param {Event} event
 * @return {boolean}
 */
ydn.auth.Chrome.prototype.handleAuthClick = function(event) {
  this.log('handleAuthClick');
  chrome.identity.getAuthToken({
    'interactive': true
  }, goog.bind(this.handleAuthResult, this));
  return false;
};


/**
 * Load require library.
 * @param {function(this:ydn.auth.Chrome)} cb
 */
ydn.auth.Chrome.prototype.loadLibrary = function(cb) {
  this.log('loadLibrary');
  if (this.lib_.length == 0) {
    cb.call(this);
  } else {
    var lib = this.lib_.shift();
    var ver = this.lib_.shift();
    var me = this;
    gapi.client.load(lib, ver, function() {
      if (me.lib_.length == 0) {
        cb.call(me);
      } else {
        me.loadLibrary(cb);
      }
    });
  }
};


/**
 * @protected
 */
ydn.auth.Chrome.prototype.makeApiCall = function() {
  this.log('makeApiCall');
  var me = this;
  this.loadLibrary(function() {
    // console.log('lib loaded')
    gapi.client.request({
      'path': 'oauth2/v3/userinfo',
      'callback': function(data) {
        var ele_name = document.getElementById('user-name');
        ele_name.textContent = data['email']; // .replace(/@.+/, '');
        ele_name.style.display = '';
        me.on_ready();
        me.on_ready = null;
      }
    });
  });
};


