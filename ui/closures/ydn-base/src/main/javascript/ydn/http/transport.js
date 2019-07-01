/**
 * @fileoverview HTTP Transport.
 *
 * Provide HTTP request.
 */

goog.provide('ydn.http.ITransport');
goog.provide('ydn.http.Transport');
goog.provide('ydn.http.CallbackResult');



/**
 *
 * @constructor
 * @param {string} content_type
 * @param {string} text
 * @param {string} url
 * @param {number} status
 * @param {*=} response
 * @param {Object.<string>=} headers response headers
 */
ydn.http.CallbackResult = function (content_type, text, url, status, response,
                                    headers) {
  /** @final */
  this.status = status;
  /** @final */
  this.contentType = content_type;
  /**
   * @final
   * @type {string}
   */
  this.responseText = text || '';
  /** @final */
  this.url = url;
  /* final */
  this.response = response || null;
  /* final */
  this.headers = headers || {};
};


/**
 * @type {number}
 */
ydn.http.CallbackResult.prototype.status;

/**
 * @type {string}
 */
ydn.http.CallbackResult.prototype.contentType;

/**
 * @type {string}
 */
ydn.http.CallbackResult.prototype.responseText;

/**
 * @type {string}
 */
ydn.http.CallbackResult.prototype.url;

/**
 * @type {*}
 */
ydn.http.CallbackResult.prototype.response;


/**
 * @type {Object.<string>}
 */
ydn.http.CallbackResult.prototype.headers;


/**
 *
 * @param {!Object} result_json
 * @return {ydn.http.CallbackResult}
 */
ydn.http.CallbackResult.wrap = function (result_json) {
  if (result_json instanceof ydn.http.CallbackResult) {
    return result_json;
  } else {
    return new ydn.http.CallbackResult(
        result_json['contentType'],
        result_json['responseText'],
        result_json['url'],
        result_json['status'],
        result_json['response'],
        result_json['headers']);
  }
};


/**
 *
 * @param {string} header the header
 * @return {string|undefined} header value
 */
ydn.http.CallbackResult.prototype.getHeader = function(header) {
  var v = this.headers[header];
  if (v) {
    return v;
  }
  header = header.toLowerCase();
  for (var key in this.headers) {
    if (key.toLowerCase() == header) {
      return this.headers[key];
    }
  }
  return undefined;
};


/**
 *
 * @return {!Object}
 */
ydn.http.CallbackResult.prototype.getResponseJson = function() {
  var is_content_json = this.contentType == 'json' ||
    (goog.isString(this.contentType) &&
      goog.string.startsWith(this.contentType, 'application/json'));
  if (is_content_json &&
      this.response != null && typeof this.response == 'object') {
    return /** @type {!Object} */ (this.response);
  } else {
    return ydn.json.parse(this.responseText);
  }
};


/**
 *
 * @return {number}
 */
ydn.http.CallbackResult.prototype.getStatus = function() {
  return this.status;
};


/**
 *
 * @return {string}
 */
ydn.http.CallbackResult.prototype.getUrl = function() {
  return this.url;
};

/**
 *
 * @return {boolean}
 */
ydn.http.CallbackResult.prototype.isSuccessStatusCode = function() {
  return this.status >= 200 && this.status < 400;
};


/**
 * Return status and shortened text from the result.
 * @return {string}
 */
ydn.http.CallbackResult.prototype.getMessage = function() {
  var txt = goog.isString(this.responseText) ?
    this.responseText.substring(0, 200) : '';
  return this.status + ': ' + txt;
};


/**
 *
 * @return {boolean} if content_type is json
 */
ydn.http.CallbackResult.prototype.isContentJson = function() {
  return /application\/json/.test(this.contentType);
};

/**
 *
 * @return {boolean} if content_type is javascript
 */
ydn.http.CallbackResult.prototype.isContentJavascript = function() {
  return /text\/javascript/.test(this.contentType);
};



/**
 * @return {string}
 */
ydn.http.CallbackResult.prototype.toString = function() {
  if (goog.DEBUG) {
    var msg = this.message ? this.message : this.responseText;
    return this.status + ' ' + this.url + ' ' + msg;
  } else {
    return goog.base(this, 'toString');
  }
};



/**
 *
 * @interface
 */
ydn.http.ITransport = function() {};


/**
 * @typedef {{
 * method: (string|undefined),
 * body: (string|undefined),
 * headers: (Object.<string>|undefined),
 * params: (Object.<string>|undefined)
 * }}
 */
ydn.http.ITransport.Options;


/**
 *
 * @param {Object=} options given option
 * @return {!ydn.http.ITransport.Options} return given options if not
 * default options. input argument is not modified.
 */
ydn.http.getDefaultOptions = function(options) {
  var default_options = {
    method: 'GET',
    body: undefined,
    headers: {},
    params: {}

  };
  if (goog.isDefAndNotNull(options)) {
    return {
      method: options.method || default_options.method,
      body: options.body,
      headers: options.headers || {},
      params: options.params || {}
    };
  } else {
    return default_options;
  }
};


/**
 * Submit HTTP request.
 *
 * @param {string} url
 * @param {function(!ydn.http.CallbackResult)=} opt_callback
 * @param {ydn.http.ITransport.Options=} opt_options
 * @return {goog.async.Deferred|undefined} if not provided, callback result
 * is return in the deferred function.
 */
ydn.http.ITransport.prototype.send = function(url, opt_callback, opt_options) {

};



/**
 * Define HTTP transport object.
 * @constructor
 * @implements {ydn.http.ITransport}
 */
ydn.http.Transport = function() {};


/**
 * @inheritDoc
 */
ydn.http.Transport.prototype.send = goog.abstractMethod;



