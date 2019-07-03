/**
 * @fileoverview OAuth client.
 *
 */


goog.provide('ydn.client.SimpleOAuthClient');
goog.require('goog.async.Deferred');
goog.require('ydn.client.Client');
goog.require('ydn.client.SimpleHttpRequest');
goog.require('ydn.client.ISimpleOAuthProvider');



/**
 * OAuth client.
 * @param {ydn.client.ISimpleOAuthProvider} provider
 * @param {goog.net.XhrManager} xm xhr manager.
 * @param {boolean=} opt_use_header user authroization header, otherwise use
 * token in parameter.
 * @constructor
 * @implements {ydn.client.Client}
 * @struct
 */
ydn.client.SimpleOAuthClient = function(provider, xm, opt_use_header) {
  /**
   * @protected
   * @type {ydn.client.ISimpleOAuthProvider}
   */
  this.provider = provider;
  /**
   * @protected
   * @type {goog.net.XhrManager}
   */
  this.xm = xm;
  /**
   * @final
   * @type {boolean}
   * @protected
   */
  this.use_header_authorization = goog.isDef(opt_use_header) ? opt_use_header :
      ydn.client.SimpleOAuthClient.USE_AUTHORIZATION_HEADER;
};


/**
 * @inheritDoc
 */
ydn.client.SimpleOAuthClient.prototype.request = function(req) {
  return new ydn.client.SimpleOAuthClient.Request(req, this);
};



/**
 * Request data.
 * @param {ydn.client.HttpRequestData} args
 * @param {ydn.client.SimpleOAuthClient} parent xhr manager.
 * @constructor
 * @extends {ydn.client.SimpleHttpRequest}
 * @struct
 */
ydn.client.SimpleOAuthClient.Request = function(args, parent) {
  goog.base(this, args, parent.xm);
  /**
   * @final
   * @type {ydn.client.SimpleOAuthClient}
   */
  this.parent = parent;

};
goog.inherits(ydn.client.SimpleOAuthClient.Request, ydn.client.SimpleHttpRequest);


/**
 * @define {boolean} debug flag.
 */
ydn.client.SimpleOAuthClient.DEBUG = false;


/**
 * @const
 * @type {boolean}
 */
ydn.client.SimpleOAuthClient.USE_AUTHORIZATION_HEADER = true;


/**
 * @const
 * @type {ydn.client.HttpRespondData}
 */
ydn.client.SimpleOAuthClient.Request.ERR_RESP =
    new ydn.client.HttpRespondData(0, null, null, 'No access token');


/**
 * @param {string} token
 * @private
 */
ydn.client.SimpleOAuthClient.Request.prototype.insertHeader_ = function(token) {
  if (this.parent.use_header_authorization) {
    this.req_data.headers['Authorization'] = 'Bearer ' + token;
  } else {
    this.req_data.params['access_token'] = token;
  }
};


/**
 * Execute the request.
 * @param {function(this: T, Object, ydn.client.HttpRespondData)?} cb
 * @param {T=} opt_scope scope.
 * @template T
 */
ydn.client.SimpleOAuthClient.Request.prototype.execute = function(cb, opt_scope) {

  this.parent.provider.getAuthToken().addCallbacks(function(token) {
    if (token) {
      this.insertHeader_(token);
      ydn.client.SimpleOAuthClient.Request.superClass_.execute.call(this, cb, opt_scope);
    } else {
      cb.call(opt_scope, null, ydn.client.SimpleOAuthClient.Request.ERR_RESP);
    }
  }, function(e) {
    cb.call(opt_scope, null, ydn.client.SimpleOAuthClient.Request.ERR_RESP);
  }, this);
};



