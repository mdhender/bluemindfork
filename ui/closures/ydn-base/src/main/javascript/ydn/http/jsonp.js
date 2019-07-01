/**
 * @fileoverview JSONP transport.
 *
 */



goog.provide('ydn.http.JsonpTransport');
goog.require('goog.Uri');
goog.require('goog.net.Jsonp');



/**
 * Cross domain request by JSONP.
 * @implements {ydn.http.ITransport}
 * @constructor
 */
ydn.http.JsonpTransport = function() {

};


/**
 * @protected
 * @type {goog.debug.Logger}
 */
ydn.http.JsonpTransport.prototype.logger = goog.log.getLogger('ydn.http.JsonpTransport');


/**
 * @inheritDoc
 */
ydn.http.JsonpTransport.prototype.send = function(url, opt_callback, options) {
  var method = options.method || 'GET';

  var callback = function(result) {
    var data = new ydn.http.CallbackResult();
    data.url = url;
    data.status = 200; // assume
    data.text = result;
    opt_callback(data);
  };

  var error_callback = function(result) {
    var data = new ydn.http.CallbackResult();
    data.url = url;
    data.status = 400; // assume
    opt_callback(data);
  };

  if (method == 'GET') {
    var jsonp = new goog.net.Jsonp(url);
    jsonp.send(options.params, callback, error_callback);
  } else {
    goog.log.finest(db.logger, method + ' Http request to ' + url + ' ignored.');
  }
};

