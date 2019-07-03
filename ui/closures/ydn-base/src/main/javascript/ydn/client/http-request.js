/**
 * @fileoverview HTTP client.
 */


goog.provide('ydn.client.HttpRequest');
goog.provide('ydn.client.HttpRequestData');



/**
 * Create HTTP Request data.
 * This class exists so that it is easier to interpolate with GAPI library and
 * provide strong type, when use internally.
 * @param {string} path
 * @param {string=} opt_method
 * @param {Object.<string>=} opt_params
 * @param {Object.<string>=} opt_headers
 * @param {ArrayBuffer|Blob|Document|FormData|string=} opt_body
 * @constructor
 * @struct
 * @implements {ydn.client.HttpRequest}
 */
ydn.client.HttpRequestData = function(path, opt_method,
                                      opt_params, opt_headers, opt_body) {
  /**
   * @type {string}
   */
  this.path = path;
  /**
   * Upper case HTTP method. Default to 'GET'.
   * @type {string}
   */
  this.method = opt_method ? opt_method.toUpperCase() : 'GET';
  /**
   * @type {!Object}
   */
  this.params = opt_params || {};
  /**
   * @type {!Object}
   */
  this.headers = opt_headers || {};
  /**
   * @type {ArrayBuffer|Blob|Document|FormData|string|undefined}
   */
  this.body = opt_body;
};


/**
 * @return {string}
 */
ydn.client.HttpRequestData.prototype.getUri = function() {
  return this.path;
};


/**
 * Return true if the request is write request.
 * @return {boolean}
 */
ydn.client.HttpRequestData.prototype.isWriteRequest = function() {
  return this.method == 'POST' || this.method == 'PUT' ||
      this.method == 'DELETE';
};


/**
 * @inheritDoc
 */
ydn.client.HttpRequestData.prototype.execute = goog.abstractMethod;


/**
 * Wrap request data.
 * Note: callback is not used in ydn.client.HttpRequestData.
 * @param {gapi.client.ReqData|ydn.client.HttpRequestData} args
 * @return {!ydn.client.HttpRequestData}
 */
ydn.client.HttpRequestData.wrap = function(args) {
  return args instanceof ydn.client.HttpRequestData ? args :
      new ydn.client.HttpRequestData(args['path'],
          args['method'], args['params'], args['headers'], args['body']);
};



/**
 * HTTP Request.
 * @interface
 */
ydn.client.HttpRequest = function() {};


/**
 * Execute the request.
 * @param {function(this: T, Object, ydn.client.HttpRespondData)?} cb
 * @param {T=} opt_obj scope.
 * @template T
 */
ydn.client.HttpRequest.prototype.execute = function(cb, opt_obj) {};




