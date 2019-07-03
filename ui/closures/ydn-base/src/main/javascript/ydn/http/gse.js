/**
 * @fileoverview Cross domain transport to Google GData API server (GSE).
 *
 */



goog.provide('ydn.http.NoPreflightTransport');
goog.provide('ydn.http.GseTransport');
goog.require('ydn.http.FilteredTransport');



/**
 * The request is rout to proxy_transport if option request required, otherwise go through direct channels.
 *
 * This is use for send request to GSE server. Currently GSE server do not response option request even though
 * it allow cross origin request from any host. Very strange.
 *
 * @extends {ydn.http.TransportFilter}
 * @param {ydn.http.ITransport} direct_transport
 * @param {ydn.http.ITransport} proxy_transport use when pre-flight request is required
 * @constructor
 */
ydn.http.NoPreflightTransport = function (direct_transport, proxy_transport) {
  /**
   *
   * @param {string} url
   * @param {string} method
   * @param {ydn.http.ITransport.Options=} options
   * @return {boolean}
   */
  var filter = function (url, method, options) {

    method = method || 'GET';

    var guri = new goog.Uri(url);
    var domain = guri.getDomain();
    var scheme = guri.getScheme();
    var port = guri.getPort();

    // here we have to choose which method to use
    var is_cross_post = !((!scheme || scheme == window.location.protocol) &&
        (!port || port == window.location.port) &&
        (!domain || domain == window.location.hostname));

    if (is_cross_post) {
      // only cross post require options request
      var is_simple_request = ydn.http.is_simple_request(method, options.headers);
      if (!is_simple_request) {
        return false;
      }
    }
    return true;
  };

  goog.base(this, filter, direct_transport, proxy_transport);
};
goog.inherits(ydn.http.NoPreflightTransport, ydn.http.TransportFilter);



/**
 * The request is rout to proxy_transport for POST and altom GET request, otherwise go through direct channels.
 *
 * This is use for send request to GSE server. GSE server response from cross origin for non ATOM format.
 *
 * @extends {ydn.http.TransportFilter}
 * @param {ydn.http.ITransport} direct_transport
 * @param {ydn.http.ITransport} proxy_transport use when pre-flight request is required
 * @constructor
 */
ydn.http.GseTransport = function (direct_transport, proxy_transport) {
  /**
   *
   * @param {string} url
   * @param {string} method
   * @param {ydn.http.ITransport.Options=} options
   * @return {boolean}
   */
  var filter = function (url, method, options) {

    /**
     * GSE server only accept cross origin request for alternative format other than 'atom'.
     * Default is 'atom'. So 'alt' must set to enable cross origin 'GET' request.
     * @return {boolean}
     */
    var use_alt = function () {
      if (url.indexOf('alt=json') > 0 || url.indexOf('alt=json-in-script') > 0 ||
          url.indexOf('alt=atom-in-script') > 0) {
        return true;
      }
      for (var key in options.params) {
        if (options.params.hasOwnProperty(key) && key == 'alt' && goog.isString(options.params[key]) &&
          options.params[key].toLowerCase() != 'atom') {
          return true;
        }
      }
      return false;
    };

    return (method == 'GET' && use_alt());
  };

  goog.base(this, filter, direct_transport, proxy_transport);
};
goog.inherits(ydn.http.GseTransport, ydn.http.TransportFilter);
