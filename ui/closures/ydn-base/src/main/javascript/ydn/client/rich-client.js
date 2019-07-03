/**
 * @fileoverview Default HTTP client.
 */


goog.provide('ydn.client.RichClient');
goog.require('ydn.client');
goog.require('ydn.client.SimpleHttpRequest');
goog.require('ydn.client.base');



/**
 * Singleton simple client.
 * @param {goog.net.XhrManager=} opt_xm xhr manager.
 * @param {Object=} opt_headers optional headers to send with each request.
 * @param {string=} opt_proxy proxy url.
 * @param {number=} opt_retry
 * @param {boolean=} opt_credentials invoke with credentials
 * @constructor
 * @implements {ydn.client.Client}
 * @struct
 */
ydn.client.RichClient = function(opt_xm, opt_headers, opt_proxy, opt_retry, opt_credentials) {
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
  /**
   * @type {number}
   * @private
   */
  this.retry_ = opt_retry || 0;
  /**
   * @type {boolean}
   * @private
   */
  this.with_credentials_ = !!opt_credentials;
};


/**
 * @type {goog.net.XhrManager}
 * @private
 */
ydn.client.RichClient.prototype.xm_ = null;


/**
 * Set header to the client.
 * @param {string} name
 * @param {?string} value
 */
ydn.client.RichClient.prototype.setHeader = function(name, value) {
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
ydn.client.RichClient.prototype.request = function(args) {
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
  return new ydn.client.SimpleHttpRequest(args, this.xm_, this.retry_, this.with_credentials_);
};


/**
 * Get singleton simple client.
 * @return {!ydn.client.RichClient}
 */
ydn.client.RichClient.getInstance = function() {
  if (!ydn.client.RichClient.instance_) {
    ydn.client.RichClient.instance_ = new ydn.client.RichClient();
  }
  return ydn.client.RichClient.instance_;
};


