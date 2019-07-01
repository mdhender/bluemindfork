/**
 * @fileoverview Default HTTP client.
 */


goog.provide('ydn.client.SimpleClient');
goog.require('ydn.client.Client');
goog.require('ydn.client.SimpleHttpRequest');
goog.require('ydn.client.base');



/**
 * Singleton simple client.
 * @param {goog.net.XhrManager=} opt_xm xhr manager.
 * @param {Object=} opt_headers optional headers to send with each request.
 * @param {string=} opt_proxy proxy url.
 * @constructor
 * @implements {ydn.client.Client}
 * @struct
 */
ydn.client.SimpleClient = function(opt_xm, opt_headers, opt_proxy) {
  this.xm_ = goog.isDef(opt_xm) ? // use default only if not null.
      opt_xm : ydn.client.base.getXhrManager();
  /**
   * @type {Object|undefined}
   * @private
   */
  this.header_ = opt_headers;
  /**
   * @type {string|undefined}
   * @private
   */
  this.proxy_url_ = opt_proxy;
};


/**
 * @type {goog.net.XhrManager}
 * @private
 */
ydn.client.SimpleClient.prototype.xm_;


/**
 * Set header to the client.
 * @param {string} name
 * @param {?string} value
 */
ydn.client.SimpleClient.prototype.setHeader = function(name, value) {
  if (!this.header_) {
    this.header_ = {};
  }
  if (value) {
    this.header_[name] = value;
  } else {
    delete this.header_[name];
  }
};


/**
 * @inheritDoc
 */
ydn.client.SimpleClient.prototype.request = function(args) {
  if (this.header_) {
    if (!args.headers) {
      args.headers = {};
    }
    for (var key in this.header_) {
      args.headers[key] = this.header_[key];
    }
  }
  if (this.proxy_url_) {
    args.path = this.proxy_url_ + args.path;
  }
  return new ydn.client.SimpleHttpRequest(args, this.xm_);
};


/**
 * Get singleton simple client.
 * @return {!ydn.client.SimpleClient}
 */
ydn.client.SimpleClient.getInstance = function() {
  if (!ydn.client.SimpleClient.instance_) {
    ydn.client.SimpleClient.instance_ = new ydn.client.SimpleClient();
  }
  return ydn.client.SimpleClient.instance_;
};


