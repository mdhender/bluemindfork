goog.provide('net.bluemind.api.BlueMindClient');
goog.require('goog.object');

goog.require('relief.rpc.Command');
goog.require('relief.rpc.RPCService');
goog.require('relief.cache.Cache');
goog.require('goog.async.Deferred');

/**
 * @param {relief.rpc.RPCService} rpc RPC Service
 * @param {string} base RPC base path
 * @constructor
 */
net.bluemind.api.BlueMindClient = function(rpc, base) {
  this.rpc = rpc;
  this.base = base + '/api';
};

/**
 * @type {relief.rpc.RPCService} rpc
 * @protected
 */
net.bluemind.api.BlueMindClient.prototype.rpc;

/**
 * @type {string} base
 * @protected
 */
net.bluemind.api.BlueMindClient.prototype.base;

/**
 * 
 * @param {relief.rpc.Command} cmd Command to execute
 * @param {Object} data Post data
 * @returns {goog.Thenable}
 */
net.bluemind.api.BlueMindClient.prototype.execute = function(cmd, data) {
  var result = new goog.async.Deferred();

  if (data != null) {
    cmd.getData = function() {
      var s = new goog.json.Serializer();
      return s.serialize(data);
    };
  }
  cmd.onSuccess = function(event) {
    var xhr = event.target;
    var resp = xhr.getResponseText();
    if (resp != null && resp.length > 0) {
      var value = JSON.parse(xhr.getResponseText());
      if (value['errorType']) {
        var e = new Error(value['message']);
        e.name = value['errorType'];
        result.errback(e);
      } else {
        result.callback(value);
      }
    } else {
      result.callback([]);
    }
  };

  cmd.onFailure = function(event) {

    var xhr = event.target;
    if (xhr.isComplete() && xhr.getLastErrorCode() == 6 && !xhr.getStatus()) {
      result.errback("Offline");
      return;
    }
    var resp = xhr.getResponseText();
    if (resp != null && resp.length > 0) {
      try {
        var error = JSON.parse(xhr.getResponseText());
        var e = new Error(error['message']);
        e.errorType = error['errorType'];
        e.errorCode = error['errorCode'];
        result.errback(e);
      } catch (e) {
        result.errback(xhr.getStatus());
      }
    } else {
      result.errback(xhr.getLastErrorCode());
    }
  };
  try {
    this.rpc.execute(cmd);
  } catch (e) {
    result.errback(e);
  }

  return result;
}
