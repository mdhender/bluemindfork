/**
 * @fileoverview Provide server services.
 *
 * Object using this interface, allow easy exchanging for proxy-base submission,
 * argumenting authentication and mocking.
 */


goog.provide('ydn.http');
goog.provide('ydn.http.Scopes');
goog.require('goog.Uri');
goog.require('goog.log');
goog.require('goog.net.XhrManager');
goog.require('ydn.http.ITransport');
goog.require('ydn.json');


/**
 * Default scopes
 * @enum {string}
 */
ydn.http.Scopes = {
  DEFAULT: 'default',
  AUTH: 'auth', // to API server with specific to this app
  LOGIN: 'login', // to API server
  GSE: 'gse', // for google gdata server
  GOOGLE_CLIENT: 'gc', // gapi
  PROXY: 'proxy',
  BUG_REPORT: 'bug'
};


/**
 * For development this will be: http://localhost:8080,
 * for sandbox server https://yathit-dev.appspot.com
 * "https://yit204.appspot.com"
 * "https://yt-app104.appspot.com";
 * "https://mcmf-test2.appspot.com";
 * "https://yathit-api.appspot.com";
 * "http://ydn-forum.appspot.com";
 * "http://localhost:9999";
 * "http://localhost:8888";
 * @define {string}
 */
ydn.http.API_SERVER_ORIGIN = 'https://yathit-api.appspot.com';


/**
 *  ydn.http.API_SERVER_ORIGIN + '/proxy/';
 * @define {string}
 */
ydn.http.PROXY_URL = 'https://yathit-api.appspot.com/proxy/';


/**
 * "http://pure-mist-4374.herokuapp.com";
 * @define {string}
 */
ydn.http.STORAGE_SERVER_URL = '';


/**
 *
 * @define {string}
 */
ydn.http.STATIC_SERVER_ORIGIN = '';


/**
 *
 * @define {string}
 */
ydn.http.BUG_REPORT_SERVER_ORIGIN = 'https://yathit-dev.appspot.com';


/**
 * @protected
 * @type {goog.debug.Logger}
 */
ydn.http.logger = goog.log.getLogger('ydn.http');


/**
 * Indicate whether current browser support Cross Domain posting
 * @return {boolean}
 */
ydn.http.canCrossPost = function() {
  // need more testing, not done.
  if (goog.userAgent.WEBKIT || goog.userAgent.GECKO) {
    return true;
  }
  if (goog.userAgent.IE && goog.userAgent.isVersionOrHigher('9')) {
    return true;
  }
  return false;
};


/**
 * Indicate whether current browser support Cross Domain posting with credential
 * @return {boolean}
 */
ydn.http.canCrossPostWithCredential = function() {
  if (!goog.isDef(ydn.http.canCrossPostWithCredential_)) {
    if (!ydn.http.canCrossPost()) {
      return false;
    }
    try {
      var xhr = new XMLHttpRequest();
      ydn.http.canCrossPostWithCredential_ = !!xhr &&
          goog.isDef(xhr.withCredentials);
    } catch (e) {
      ydn.http.canCrossPostWithCredential_ = false;
    }
  }
  return /** @type {boolean} */ (ydn.http.canCrossPostWithCredential_);
};


/**
 * @private
 * @type {Object.<ydn.http.ITransport>}
 */
ydn.http.transports = {};


/**
 * @private
 * @type {Array.<function(string=): string?>}
 */
ydn.http.scope_resolvers = [];


/**
 *
 * @param {function(string=): string?} resolver
 */
ydn.http.setScopeResolver = function(resolver) {
  ydn.http.scope_resolvers = [resolver];
};


/**
 *
 * @param {function(string=): string?} resolver Resolver should resolve given
 * feed_uri to scope,
 * or return null when feed_uri is irrelevant to the resolver.
 */
ydn.http.addScopeResolver = function(resolver) {
  if (resolver) {
    ydn.http.scope_resolvers.push(resolver);
  }
};


/**
 * Set whatever proxy
 *
 * @param {ydn.http.ITransport} transport
 * @param {string=} opt_scope
 */
ydn.http.setTransport = function(transport, opt_scope) {
  opt_scope = opt_scope || ydn.http.Scopes.DEFAULT;
  ydn.http.transports[opt_scope] = transport;
};


/**
 * Get whatever proxy transport set previously
 * If given proxy is not available, a default transport will be return instead
 * of null.
 * @see {@link ydn.http.Scopes}
 * @param {string=} uri
 * @return {ydn.http.ITransport} transport.
 */
ydn.http.getTransport = function(uri) {
  if (!goog.isDef(uri)) {
    return ydn.http.transports[ydn.http.Scopes.DEFAULT];
  }
  var scope = uri;
  for (var i = 0; i < ydn.http.scope_resolvers.length; i++) {
    var resolver = ydn.http.scope_resolvers[i];
    var out = resolver(uri);
    if (out) {
      scope = out;
      break;
    }
  }
  goog.log.finest(ydn.http.logger, 'Resolving ' + uri + ' to ' + scope);
  return ydn.http.transports[scope] || null;
};


/**
 * Clear transport
 * @param {string=} opt_scope if not specified, all transports will be cleared.
 */
ydn.http.clearTransport = function(opt_scope) {
  if (goog.isDef(opt_scope)) {
    delete ydn.http.transports[opt_scope];
  } else {
    ydn.http.transports = {};
    ydn.http.scope_resolvers = [];
  }
};


/**
 *
 * @param {Object.<string>} headers
 * @return {boolean} true if headers has custom header starting with x-.
 */
ydn.http.has_custom_header = function(headers) {
  for (var key in headers) {
    if (goog.isString(headers[key]) &&
        goog.string.startsWith(headers[key].toLowerCase(), 'x-')) {
      return true;
    }
  }
  return false;
};


/**
 * Check for simple request.
 * {@links https://developer.mozilla.org/en/http_access_control}
 * @param method
 * @param headers
 */
ydn.http.is_simple_request = function(method, headers) {

  method = method.toUpperCase();
  // A simple cross-site request is one that:
  // 1) Only uses GET or POST.
  if (method != 'GET' || method != 'POST') {
    return false;
  }

  if (method == 'POST') {
    // 2. If POST is used to send data to the server,
    // Does not set custom headers with the HTTP Request
    // (such as X-Modified, etc.)
    if (ydn.http.has_custom_header(headers)) {
      return false;
    }
    // the Content-Type of the data sent to the server with the
    // HTTP POST request is one of application/x-www-form-urlencoded,
    // multipart/form-data, or text/plain.
    var content_type = headers['Content-Type'];
    var allow_content_types = ['application/x-www-form-urlencoded',
      'multipart/form-data', 'text/plain'];
    if (content_type &&
        !goog.array.contains(allow_content_types, content_type)) {
      return false;
    }
  }
  return true;
};


/**
 * @type {goog.net.XhrManager}
 * @private
 */
ydn.http.xhr_manager_;


/**
 * Get default xhr manager.
 * @return {!goog.net.XhrManager}
 */
ydn.http.getXhrManager = function() {
  if (!ydn.http.xhr_manager_) {
    var xhr = new goog.net.XhrManager();
    ydn.http.xhr_manager_ = xhr;
    return xhr;
  } else {
    return ydn.http.xhr_manager_;
  }
};

