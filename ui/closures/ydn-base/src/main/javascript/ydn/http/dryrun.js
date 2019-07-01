/**
 * @fileoverview Filtered transport.
 *
 * Filtered transport allow optional pass through if certain cretitia are met.
 */


goog.provide('ydn.http.DryrunTransport');
goog.require('ydn.http.ITransport');
goog.require('ydn.http.MockHttpTransport');
goog.require('goog.Uri');
goog.require('ydn.http.TransportFilter');



/**
 * Other than GET request are print to console without sending to the server.
 * @extends {ydn.http.TransportFilter}
 * @param {ydn.http.ITransport}  transport
 * @constructor
 */
ydn.http.DryrunTransport = function(transport) {

  /**
   *
   * @param {string} url
   * @param {string} method
   * @param {ydn.http.ITransport.Options=} options
   * @return {boolean}
   */
  var filter = function (url, method, options) {
    return method == 'GET';
  };

  var drop_transport = new ydn.http.MockHttpTransport();
  drop_transport.print_body = true;

  goog.base(this, filter, transport, drop_transport);
};
goog.inherits(ydn.http.DryrunTransport, ydn.http.TransportFilter);

