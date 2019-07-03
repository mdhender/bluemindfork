/**
 * @fileoverview Simple mock server responding to a query.
 */


goog.provide('ydn.http.SimpleQueryMockServer');
goog.require('ydn.http.StaticMockServer');


/**
 * @extends {ydn.http.StaticMockServer}
 * @param domain
 * @param {boolean=} opt_sync
 * @param {number=} opt_delay default to 50 (ms)
 * @constructor
 */
ydn.http.SimpleQueryMockServer = function(domain, opt_sync, opt_delay) {
  goog.base(this, domain, opt_sync, opt_delay);
  this.apps = {};
};
goog.inherits(ydn.http.SimpleQueryMockServer, ydn.http.StaticMockServer);


/**
 * Response data is hash by encoded query string without '?'.
 * @typedef {{
 *  body: string,
 *  status: number
 * }}
 */
ydn.http.SimpleQueryMockServer.ResponseData;


/**
 * Response data is hash by encoded query string.
 * @param {string} app_path url to response
 * @param {Array.<Object.<string>>} data
 */
ydn.http.SimpleQueryMockServer.prototype.loadAppData = function(app_path, data) {
  this.apps[app_path] = data;
};


/**
 * @override
 */
ydn.http.SimpleQueryMockServer.prototype.send = function(url, callback, options) {
  var guri = new goog.Uri(url);
  var app = this.apps[guri.getPath()];
  if (this.isDomainValid(guri) && app) {
    var data = app[guri.getQuery()];
    if (data) {
      this.response(url, 200, data, callback);
    } else {
      this.response(url, 404, '', callback);
    }
  } else {
    goog.base(this, 'send', url, callback, options);
  }
};


