/**
 * @fileoverview OAuth client.
 *
 */


goog.provide('ydn.client.OAuthClient');
goog.require('goog.async.Deferred');
goog.require('ydn.client.Client');
goog.require('ydn.client.SimpleHttpRequest');



/**
 * OAuth client.
 * @param {ydn.client.IOAuthProvider} provider
 * @param {goog.net.XhrManager} xm xhr manager.
 * @param {boolean=} opt_use_header user authroization header, otherwise use
 * token in parameter.
 * @constructor
 * @implements {ydn.client.Client}
 * @struct
 */
ydn.client.OAuthClient = function(provider, xm, opt_use_header) {
  /**
   * @protected
   * @type {ydn.client.IOAuthProvider}
   */
  this.provider = provider;
  /**
   * @protected
   * @type {goog.net.XhrManager}
   */
  this.xm = xm;
  /**
   * @protected
   * @type {YdnApiToken}
   */
  this.token = null;
  /**
   * @final
   * @type {boolean}
   * @protected
   */
  this.use_header_authorization = goog.isDef(opt_use_header) ? opt_use_header :
      ydn.client.OAuthClient.USE_AUTHORIZATION_HEADER;
};


/**
 * @inheritDoc
 */
ydn.client.OAuthClient.prototype.request = function(req) {
  return new ydn.client.OAuthClient.Request(req, this);
};



/**
 * Request data.
 * @param {ydn.client.HttpRequestData} args
 * @param {ydn.client.OAuthClient} parent xhr manager.
 * @constructor
 * @extends {ydn.client.SimpleHttpRequest}
 * @struct
 */
ydn.client.OAuthClient.Request = function(args, parent) {
  goog.base(this, args, parent.xm);
  /**
   * @final
   * @type {ydn.client.OAuthClient}
   */
  this.parent = parent;

};
goog.inherits(ydn.client.OAuthClient.Request, ydn.client.SimpleHttpRequest);


/**
 * @define {boolean} debug flag.
 */
ydn.client.OAuthClient.DEBUG = false;


/**
 * @const
 * @type {boolean}
 */
ydn.client.OAuthClient.USE_AUTHORIZATION_HEADER = true;


/**
 * @const
 * @type {ydn.client.HttpRespondData}
 */
ydn.client.OAuthClient.Request.ERR_RESP =
    new ydn.client.HttpRespondData(0, null, null, 'No access token');


/**
 * @return {boolean}
 */
ydn.client.OAuthClient.Request.prototype.hasValidToken = function() {
  return !!this.parent.token && this.parent.token.expires > goog.now();
};


/**
 * @private
 */
ydn.client.OAuthClient.Request.prototype.insertHeader_ = function() {
  if (this.parent.use_header_authorization) {
    this.req_data.headers['Authorization'] = 'Bearer ' + this.parent.token.access_token;
  } else {
    this.req_data.params['access_token'] = this.parent.token.access_token;
  }
};


/**
 * Execute the request.
 * @param {function(this: T, Object, ydn.client.HttpRespondData)?} cb
 * @param {T=} opt_scope scope.
 * @template T
 */
ydn.client.OAuthClient.Request.prototype.execute = function(cb, opt_scope) {

  var me = this;

  // start
  if (this.hasValidToken()) {
    this.insertHeader_();
    goog.base(this, 'execute', function(data, raw) {
      if (raw.getStatus() == 401) { // (Unauthorized)
        if (ydn.client.OAuthClient.DEBUG) {
          window.console.log(me.parent.token);
          window.console.log('Although not expires, refreshing token fail due to ' +
              raw.getStatus() + ' ' + raw.getStatusText());
        }
        cb.call(opt_scope, null, raw);
      } else {
        cb.call(opt_scope, data, raw);
      }
    }, this);

  } else {
    this.parent.provider.getOAuthToken().addCallbacks(function(x) {
      this.parent.token = /** @type {YdnApiToken} */ (x);
      this.insertHeader_();
      ydn.client.OAuthClient.Request.superClass_.execute.call(this, cb, opt_scope);
      // goog.base(this, 'execute', cb, opt_scope);
    }, function(e) {
      this.parent.token = null;
      cb.call(opt_scope, null, ydn.client.OAuthClient.Request.ERR_RESP);
    }, this);
  }
};



