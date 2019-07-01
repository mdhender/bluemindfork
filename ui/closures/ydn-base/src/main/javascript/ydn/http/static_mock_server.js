/**
 * @fileoverview Mock server for static content.
 */


goog.provide('ydn.http.StaticMockServer');
goog.require('ydn.http.ITransport');
goog.require('ydn.http.CallbackResult');
goog.require('goog.log');
goog.require('ydn.utils');
goog.require('ydn.json');

/**
 * @implements {ydn.http.ITransport}
 * @constructor
 * @param {(string|Array.<string>)=} opt_domains
 * @param {boolean=} opt_sync
 * @param {number=} opt_delay default to 50 (ms)
 */
ydn.http.StaticMockServer = function(opt_domains, opt_sync, opt_delay) {

  /**
   *
   * @type {!Array.<string>}
   */
  this.domains = goog.isArray(opt_domains) ?
      opt_domains : goog.isString(opt_domains) ?
      [opt_domains] : [];

  /**
   * @private
   * @type {boolean}
   */
  this.async = !opt_sync;

  /**
   * @type {number}
   */
  this.delay = goog.isDef(opt_delay) ? opt_delay : 50;

  /**
   * Store HTML body content for each url key.
   * @type {Object.<string>}
   */
  this.database = {};
};


/**
 * @protected
 * @type {goog.debug.Logger}
 */
ydn.http.StaticMockServer.prototype.logger = goog.log.getLogger('ydn.http.StaticMockServer');


/**
 * Load GET request response data.
 * @param {string} url
 * @param {string} html body HTML to response on GET {@code url} request.
 */
ydn.http.StaticMockServer.prototype.load = function(url, html) {
  var guri = new goog.Uri(url);
  var path = guri.getPath();
  var domain = guri.getDomain();
  if (domain && !goog.array.contains(this.domains, domain)) {
    this.domains.push(domain);
  }
  this.database[path] = html;
};


/**
 * @protected
 * @param {string} url
 * @param {number} status
 * @param {string} content
 * @param {function(!ydn.http.CallbackResult)=} callback
 */
ydn.http.StaticMockServer.prototype.response = function(url, status, content, callback) {

  var content_type = 'text/plain';

  var response = new ydn.http.CallbackResult(content_type, content, url, status);
  goog.log.finest(db.logger, 'Responding ' + status + ' to ' + url);
  if (callback) {
    callback(response);
  }
};


/**
 *
 * @param {goog.Uri} guri
 * @return {boolean}
 */
ydn.http.StaticMockServer.prototype.isDomainValid = function(guri) {
  var domain = guri.getDomain();
  return !domain || goog.array.contains(this.domains, domain);
};


/**
 * @inheritDoc
 */
ydn.http.StaticMockServer.prototype.send = function(url, callback, options) {

  options = ydn.http.getDefaultOptions(options);
  goog.log.finest(db.logger, 'Receiving ' + options.method + ' request: ' + url);

  var guri = new goog.Uri(url);
  var path = guri.getPath();
  if (!this.isDomainValid(guri)) {
    throw Error('Receiving ' + guri.getDomain() + ', but we server only ' + ydn.json.stringify(this.domains));
  }

  var text = this.database[url];
  var status = 404; // not found
  if (goog.isString(text)) {
    status = 200;
  } else {
    text = '';
  }

  if (options.method != 'GET') {
    status = 405; // Method Not Allowed
    text = options.method + ' method not supported.';
    this.response(url, status, text, callback);
  }

  if (this.async) {
    goog.Timer.callOnce(function(e) {
      this.response(url, status, text, callback);
    }, this.delay, this);
  } else {
    this.response(url, status, text, callback);
  }
};
