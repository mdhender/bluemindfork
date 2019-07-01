/**
 * @fileoverview HTTP client.
 */


goog.provide('ydn.client.HttpRespondData');



/**
 * Create HTTP Raw Response data.
 * This class exists so that it is easier to interpolate with GAPI library and
 * provide strong type, when use internally.
 * @param {number|string} status status or raw.
 * @param {*=} opt_body
 * @param {Object.<string>=} opt_headers
 * @param {string=} opt_status_text
 * @constructor
 * @struct
 */
ydn.client.HttpRespondData = function(status, opt_body, opt_headers,
                                      opt_status_text) {
  if (goog.isString(status)) {
    this.raw = status;
  } else {
    this.raw = null;
    this.status = status;
    this.body = opt_body;
    this.headers = opt_headers || {};
    this.statusText = opt_status_text;
  }
};


/**
 * Unparsed raw result.
 * @type {string?}
 * @protected
 */
ydn.client.HttpRespondData.prototype.raw;


/**
 * @type {*}
 * @protected
 */
ydn.client.HttpRespondData.prototype.body;


/**
 * @type {!Object.<string>}
 * @protected
 */
ydn.client.HttpRespondData.prototype.headers;


/**
 * @type {number}
 * @protected
 */
ydn.client.HttpRespondData.prototype.status;


/**
 * @type {string|undefined}
 * @protected
 */
ydn.client.HttpRespondData.prototype.statusText;


/**
 * @protected
 */
ydn.client.HttpRespondData.prototype.ensureParse = function() {
  if (this.raw) {
    var json = ydn.json.parse(this.raw);
    if (json['gapiRequest']) {
      json = json['gapiRequest']['data'];
    }
    this.status = json['status'];
    this.statusText = json['statusText'];
    this.headers = json['headers'];
    this.body = json['body'];
    this.raw = null;
  }
};


/**
 * @return {number}
 */
ydn.client.HttpRespondData.prototype.getStatus = function() {
  this.ensureParse();
  return this.status;
};


/**
 * Return true if status code is equal or more than 200 and less then 400.
 * @return {boolean}
 */
ydn.client.HttpRespondData.prototype.isSuccess = function() {
  this.ensureParse();
  return this.status >= 200 && this.status < 400;
};


/**
 * @return {string}
 */
ydn.client.HttpRespondData.prototype.getStatusText = function() {
  this.ensureParse();
  return this.statusText || '';
};


/**
 * @return {*}
 */
ydn.client.HttpRespondData.prototype.getBody = function() {
  this.ensureParse();
  return this.body;
};


/**
 * @param {string} header Header name.
 * @return {string|undefined}
 */
ydn.client.HttpRespondData.prototype.getHeader = function(header) {
  this.ensureParse();
  return this.headers[header];
};


/**
 * @return {!Object} Get all headers. Treat as read-only object.
 */
ydn.client.HttpRespondData.prototype.getHeaders = function() {
  this.ensureParse();
  return this.headers; // should we clone?
};


/**
 * Check or sniff respond data is JSON or not.
 * @return {boolean}
 */
ydn.client.HttpRespondData.prototype.isJson = function() {
  this.ensureParse();
  var content_type = this.getHeader('content-type');
  if (content_type && content_type.toLocaleLowerCase()
          .indexOf('application/json') >= 0) {
    return true;
  } else {
    var text = this.body;
    if (goog.isString(text) && !goog.string.isEmpty(text)) {
      return (/^[\],:{}\s]*$/.test(text.replace(/\\["\\\/bfnrtu]/g, '@').
          replace(/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g, ']').
          replace(/(?:^|:|,)(?:\s*\[)+/g, '')));
    } else {
      return false;
    }
  }
};


/**
 * @return {Object}
 */
ydn.client.HttpRespondData.prototype.getJson = function() {

  if (this.isJson() && goog.isString(this.body)) {
    return /** @type {Object} */ (JSON.parse(this.body));
  } else {
    return /** @type {Object} */ (this.body);
  }
};


/**
 * @param {gapi.client.RawResp|ydn.client.HttpRespondData|string} args
 * @return {!ydn.client.HttpRespondData}
 */
ydn.client.HttpRespondData.wrap = function(args) {
  return args instanceof ydn.client.HttpRespondData ? args :
      goog.isString(args) ? new ydn.client.HttpRespondData(args) :
      new ydn.client.HttpRespondData(args['status'], args['body'],
          args['headers'], args['statusText']);
};

