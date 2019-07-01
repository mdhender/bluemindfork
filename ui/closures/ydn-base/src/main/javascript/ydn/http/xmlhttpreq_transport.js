/**
 * @fileoverview XMLHttpRequest transport.
 */


goog.provide('ydn.http.XMLHttpRequest');
goog.require('goog.Uri');
goog.require('goog.object');
goog.require('ydn.debug.error.ArgumentException');
goog.require('ydn.http');
goog.require('ydn.http.ITransport');
goog.require('ydn.http.Transport');


/**
 * proxy_url must end with '/'
 * @typedef {{
 *    sync: (boolean|undefined),
 *    withCredentials: (boolean|undefined),
 *    proxy_url: (string|undefined),
 *    origin: (string|undefined),
 *    headers: (Object.<string>|undefined),
 *    params: (Object.<string>|undefined),
 *    http_method_override: (boolean|undefined),
 *    content_length: (boolean|undefined)
 * }}
 */
ydn.http.XMLHttpRequestTransportOptions;



/**
 * content_length: include 'Content-Length' header
 * @param {ydn.http.XMLHttpRequestTransportOptions} options
 * @constructor
 * @extends {ydn.http.Transport}
 */
ydn.http.XMLHttpRequest = function(options) {
  goog.base(this);
  options = options || {};

  this.proxy_url = options.proxy_url;
  if (this.proxy_url && !goog.string.endsWith(this.proxy_url, '/')) {
    throw new ydn.debug.error.ArgumentException('proxy url must end with a backslash.');
  }

  /** @final  */
  this.sync = !!options.sync;
  /** @final  */
  this.withCredentials = !!options.withCredentials;
  /** @final  */
  this.default_origin = goog.isString(options.origin) ?
      ydn.http.XMLHttpRequest.parseOrigin_(options.origin) : null;
  /** @final  */
  this.default_headers = options.headers || {};
  /** @final  */
  this.default_params = options.params || {};
  /** @final  */
  this.http_method_override = !!options.http_method_override;
  /** @final  */
  this.content_length = !!options.content_length;
};
goog.inherits(ydn.http.XMLHttpRequest, ydn.http.Transport);


/**
 * @protected
 * @type {boolean}
 */
ydn.http.XMLHttpRequest.prototype.sync = false;


/**
 * @protected
 * @type {boolean}
 */
ydn.http.XMLHttpRequest.prototype.withCredentials = false;


/**
 * @type {string|undefined}
 * @protected
 */
ydn.http.XMLHttpRequest.prototype.proxy_url;


/**
 * @protected
 * @type {Object}
 */
ydn.http.XMLHttpRequest.prototype.default_origin = null;


/**
 * @protected
 * @type {Object.<string>}
 */
ydn.http.XMLHttpRequest.prototype.default_headers = null;

/**
 * @protected
 * @type {Object.<string>}
 */
ydn.http.XMLHttpRequest.prototype.default_params = null;

/**
 * @protected
 * @type {boolean}
 */
ydn.http.XMLHttpRequest.prototype.http_method_override = false;

/**
 * @protected
 * @type {boolean}
 */
ydn.http.XMLHttpRequest.prototype.content_length = false;


/**
 *
 * @param {string} url
 * @return {{scheme: string, domain: string, port: ?number}}
 * @private
 */
ydn.http.XMLHttpRequest.parseOrigin_ = function(url) {
  var guri = new goog.Uri(url);
  return {
    scheme: guri.getScheme(),
    domain: guri.getDomain(),
    port: guri.getPort()
  };
};

/**
 * @inheritDoc
 */
ydn.http.XMLHttpRequest.prototype.send = function(url, callback, options) {

  options = ydn.http.getDefaultOptions(options);
  var method = options.method;
  var body = options.body;
  var header = options.headers;
  var uri_params = options.params;

  method = method || 'GET';
  method = method.toUpperCase();

  for (var h in this.default_headers) {
    if (!goog.isDefAndNotNull(header[h])) {
      header[h] = this.default_headers[h];
    }
  }
  for (var h in this.default_params) {
    if (!goog.isDefAndNotNull(uri_params[h])) {
      uri_params[h] = this.default_params[h];
    }
  }

  var gurl;
  if (!goog.object.isEmpty(/** @type {Object} */ (uri_params))) {
    gurl = new goog.Uri(url);
    for (var x in uri_params) {
      gurl.setParameterValues(x, uri_params[x]);
    }
    url = gurl.toString();
  }

  if (this.default_origin) {
    gurl = gurl || new goog.Uri(url);
    gurl.setDomain(this.default_origin.domain);
    if (this.default_origin.scheme) {
      gurl.setScheme(this.default_origin.scheme);
    }
    if (this.default_origin.port) {
      gurl.setPort(this.default_origin.port);
    }
    url = gurl.toString();
  }

  if (this.proxy_url) {
    if (goog.string.startsWith(url, '/')) {
      url = url.substr(1);
    }
    url = this.proxy_url + url;
  }

  if (this.http_method_override && !(method == 'GET' || method == 'POST')) {
    method = 'POST';
    header['x-http-method-override'] = method;
  }

  if (this.content_length && !(method == 'GET')) {
    header['Content-Length'] = goog.isString(body) ? body.length + '' : '0';
  }

  var httpRequest = new XMLHttpRequest();
  httpRequest.open(method, url, !this.sync);
  for (var h in header) {
    if (header.hasOwnProperty(h) && goog.isDefAndNotNull(header[h])) {
      httpRequest.setRequestHeader(h, header[h]);
    }
  }
  if (this.withCredentials) {
    httpRequest.withCredentials = true;
  }
  if (callback) {
    httpRequest.onreadystatechange = function(e) {
      if (httpRequest.readyState === 4) {
        // httpRequest.responseType ? should we be reading from header?
        var content_type = httpRequest.getResponseHeader('Content-Type');
        var data = new ydn.http.CallbackResult(content_type,
          httpRequest.responseText, url, httpRequest.status, httpRequest.response);
        callback(data);
        callback = undefined; // release reference
        httpRequest.onreadystatechange = null;
      }
    };
  }
  httpRequest.send(body);
};
