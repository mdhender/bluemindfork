/**
 * @fileoverview Filtered transport.
 *
 * Filtered transport allow optional pass through if certain cretitia are met.
 */


goog.provide('ydn.http.FilteredTransport');
goog.provide('ydn.http.TransportFilter');
goog.require('ydn.http.ITransport');
goog.require('goog.Uri');


/**
 * Filter transport. By default only allow GET request
 * @param {function(string, string, ydn.http.ITransport.Options=): boolean} filter Return
 * true to use {@code pass_transport}, otherwise {@code fail_transport} will be used, if provided.
 * @implements {ydn.http.ITransport}
 * @param {ydn.http.ITransport}  pass_transport
 * @param {ydn.http.ITransport=}  fail_transport
 * .each argument represent uri, method, body, header, uri_params
 * @constructor
 */
ydn.http.TransportFilter = function(filter, pass_transport, fail_transport) {
  this.pass_transport = pass_transport;
  this.fail_transport = fail_transport;
  this.filter = filter;
  this.allow_request = ['GET'];
};


/**
 * @protected
 * @type {goog.debug.Logger}
 */
ydn.http.TransportFilter.prototype.logger = goog.log.getLogger('ydn.http.TransportFilter');


/**
 * @inheritDoc
 */
ydn.http.TransportFilter.prototype.send = function(uri, callback, options) {

  options = ydn.http.getDefaultOptions(options);

  if (this.filter(uri, /** @type {string} */ (options.method), options)) {
    goog.log.finest(this.logger, 'pass transport selected.');
    this.pass_transport.send(uri, callback, options);
  } else if (this.fail_transport) {
    goog.log.finest(this.logger, 'fail transport selected.');
    this.fail_transport.send(uri, callback, options);
  } else {
    goog.log.info(this.logger, 'Drop ' + options.method + ' request: ' + uri);
    if (goog.isString(options.body)) {
      goog.log.finest(this.logger, options.body);
    }
  }
};


/**
 * Simple filtered transport, allowing only GET request. Set {@code allow_request} as necessary.
 * @extends {ydn.http.TransportFilter}
 * @param {ydn.http.ITransport}  transport
 * @param {Array.<string>=}  allow_request default to {@code ['GET']}
 * @constructor
 */
ydn.http.FilteredTransport = function(transport, allow_request) {
  allow_request = allow_request || ['GET'];

  /**
   *
   * @param {string} url
   * @param {string} method
   * @param {ydn.http.ITransport.Options=} options
   * @return {boolean}
   */
  var filter = function (url, method, options) {
    return allow_request.indexOf(method) >= 0;
  };

  goog.base(this, filter, transport);
};
goog.inherits(ydn.http.FilteredTransport, ydn.http.TransportFilter);


