/**
 * @fileoverview Wrap Google Javascript Client.
 * @link https://developers.google.com/api-client-library/javascript/
 */


goog.provide('ydn.client.GapiClient');
goog.require('ydn.client');
goog.require('ydn.client.SimpleClient');



/**
 * Wrap Google Javascript Client.
 * @param {*} client gapi.client.
 * @param {boolean=} opt_immediately a client that executes request immediate.
 * @constructor
 * @implements {ydn.client.Client}
 */
ydn.client.GapiClient = function(client, opt_immediately) {
  goog.asserts.assert(client && 'request' in client, 'HTTP client must have' +
      ' a request method');
  this.client_ = client;
  this.immediately = !!opt_immediately;
};


/**
 * @type {*}
 */
ydn.client.GapiClient.prototype.client_;


/**
 * @inheritDoc
 */
ydn.client.GapiClient.prototype.request = function(req_data) {
  var args = {
    'path': req_data.path,
    'method': req_data.method,
    'headers': req_data.headers,
    'params': req_data.params,
    'body': req_data.body
  };
  if (this.immediately) {
    args['callback'] = function(json, raw) {
      req.callback(json, raw);
      args['callback'] = null;
    };
  }
  var gapi_req = this.client_['request'](args);
  var req = new ydn.client.GapiClient.Request(gapi_req);
  return req;
};



/**
 * @param {gapi.client.HttpRequest} req
 * @constructor
 * @implements {ydn.client.HttpRequest}
 */
ydn.client.GapiClient.Request = function(req) {
  this.req_ = req || null;
};


/**
 * @type {gapi.client.HttpRequest}
 * @private
 */
ydn.client.GapiClient.Request.prototype.req_;


/**
 * @private
 */
ydn.client.GapiClient.Request.prototype.cb_;


/**
 * @private
 */
ydn.client.GapiClient.Request.prototype.result_;


/**
 * @param {Object} json
 * @param {gapi.client.RawResp} raw
 */
ydn.client.GapiClient.Request.prototype.callback = function(json, raw) {
  if (this.cb_) {
    this.cb_(json, ydn.client.HttpRespondData.wrap(raw));
    this.cb_ = null;
  }
  this.result_ = {
    json: json,
    raw: raw
  };
};


/**
 * @inheritDoc
 */
ydn.client.GapiClient.Request.prototype.execute = function(cb) {
  if (this.req_) {
    this.req_.execute(function(json, raw) {
      cb(json, ydn.client.HttpRespondData.wrap(raw));
    });
    this.req_ = null;
  } else {
    if (this.result_) {
      cb(this.result_.json, ydn.client.HttpRespondData.wrap(this.result_.raw));
      this.result_ = null;
    } else {
      this.cb_ = cb;
    }
  }
};


/**
 * Wrap GAPI client.
 * @param {*} client GAPI client, SimpleClient or null.
 * @return {ydn.client.Client}
 */
ydn.client.GapiClient.wrap = function(client) {
  if (client) {
    if (client instanceof ydn.client.SimpleClient) {
      return client;
    } else if (goog.isFunction(client)) {
      var request = client;
      var mock_client = {
        /**
         * @param {gapi.client.ReqData} args
         */
        'request': function(args) {
          request(args);
        }
      };
      return new ydn.client.GapiClient(mock_client, true);
    } else if ('request' in client) {
      return new ydn.client.GapiClient(client);
    } else {
      throw new ydn.debug.error.ArgumentException(client +
          ' is not a valid HTTP Client');
    }
  } else {
    return null;
  }
};

