goog.provide('net.bluemind.api.VertxBlueMindClient');
goog.require('goog.object');

goog.require('relief.rpc.Command');
goog.require('relief.rpc.RPCService');
goog.require('restClient');
goog.require('relief.cache.Cache');
goog.require('goog.async.Deferred');
goog.require('goog.crypt.base64');

/**
 * @param {relief.rpc.RPCService} rpc RPC Service
 * @param {string} base RPC base path
 * @constructor
 */
net.bluemind.api.VertxBlueMindClient = function(sessionId) {
  this.sessionId = sessionId;
};

/**
 * @type {relief.rpc.RPCService} rpc
 * @protected
 */
net.bluemind.api.VertxBlueMindClient.prototype.rpc;

/**
 * @type {string} base
 * @protected
 */
net.bluemind.api.VertxBlueMindClient.prototype.base;

/**
 * 
 * @param {relief.rpc.Command} cmd Command to execute
 * @param {Object} data Post data
 * @returns {goog.Thenable}
 */
net.bluemind.api.VertxBlueMindClient.prototype.send_ = function(headers, verb, path, params, body) {
  var result = new goog.async.Deferred();
  var rc = {
    'headers' : headers,
    'path' : '/api' + path,
    'method' : verb,
    'params' : params,
    'body' : null
  };

  if (body) {
    rc.body = goog.crypt.base64.encodeString(goog.json.serialize(body));
  }
  if (!rc.headers['X-BM-ApiKey']) {
    rc.headers['X-BM-ApiKey'] = goog.global['bmcSessionInfos']['sid']
  }

  restClient.sendMessage(rc, function(resp) {
    if (resp["statusCode"] < 400) {
      if (resp["data"]) {
        var ret = goog.json.parse(goog.crypt.base64.decodeString(resp.data));
        result.callback(ret);
      } else {
        result.callback();
      }
    } else {
      result.error(goog.crypt.base64.decodeString(resp.data));
    }
  });
  return result;
}
