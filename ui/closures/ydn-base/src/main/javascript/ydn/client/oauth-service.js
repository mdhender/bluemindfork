/**
 * @fileoverview OAuth provider.
 *
 */


goog.provide('ydn.client.OAuthService');
goog.require('ydn.client.IOAuthProvider');



/**
 * @param {string} redirect_url redirect URL.
 * @param {Object=} opt_params optional parameter.
 * @constructor
 * @struct
 * @implements {ydn.client.IOAuthProvider}
 */
ydn.client.OAuthService = function(redirect_url, opt_params) {
  /**
   * @private
   * @type {string}
   */
  this.redirect_url_ = redirect_url;

  /**
   * @type {YdnApiToken}
   * @private
   */
  this.gdata_token_ = null;
  /**
   * @type {goog.async.Deferred}
   * @private
   */
  this.df_gdata_token_ = null;
  /**
   * @type {Object}
   * @private
   */
  this.params_ = opt_params || null;

  var cache = goog.global.localStorage.getItem(this.getStorageKey());
  if (cache) {
    this.gdata_token_ = /** @type {YdnApiToken} */(JSON.parse(cache));
  }
};


/**
 * @protected
 * @return {string}
 */
ydn.client.OAuthService.prototype.getStorageKey = function() {
  return 'oauth-service' + this.redirect_url_;
};


/**
 * Get GData token.
 * @param {string=} opt_redirect redirect url.
 * @return {!goog.async.Deferred}
 */
ydn.client.OAuthService.prototype.getOAuthToken = function(opt_redirect) {
  if (this.df_gdata_token_) {
    return this.df_gdata_token_;
  } else if (this.gdata_token_ && this.gdata_token_['expires'] > goog.now()) {
    return goog.async.Deferred.succeed(this.gdata_token_);
  }

  this.df_gdata_token_ = new goog.async.Deferred();
  var redirect = opt_redirect || this.redirect_url_;
  var params = {
    'url': redirect
  };
  if (this.params_) {
    goog.object.extend(params, this.params_);
  }

  var data = new ydn.client.HttpRequestData('/token', 'GET', params);
  var client = ydn.client.getClient(ydn.http.Scopes.AUTH);
  if (!client) {
    return goog.async.Deferred.fail(new Error('No auth client'));
  }
  var req = client.request(data);
  req.execute(function(x, raw) {
    if (raw.isSuccess()) {
      var token = /** @type {YdnApiToken} */ (x['Tokens'][0]);
      if (!token.has_token) {
        this.df_gdata_token_.errback(token);
      } else {
        token['expires'] = (token.expires_in * 1000) + goog.now();
        this.gdata_token_ = token;
        goog.global.localStorage.setItem(this.getStorageKey(), JSON.stringify(this.gdata_token_));
        this.df_gdata_token_.callback(token);
      }
    } else {
      this.gdata_token_ = null;
      this.df_gdata_token_.errback(raw);
    }
    this.df_gdata_token_ = null;
  }, this);
  return this.df_gdata_token_;
};


/**
 * Reset.
 */
ydn.client.OAuthService.prototype.reset = function() {
  this.gdata_token_ = null;
  this.df_gdata_token_ = null;
  goog.global.localStorage.removeItem(this.getStorageKey());
};


/**
 * Revoke Google gdata refresh token in the server.
 * @return {!goog.async.Deferred}
 */
ydn.client.OAuthService.prototype.revokeGDataToken = function() {
  var df = new goog.async.Deferred();
  var client = ydn.client.getClient(ydn.http.Scopes.AUTH);
  var req = client.request(new ydn.client.HttpRequestData('/token', 'DELETE'));
  req.execute(function(json, raw) {
    if (raw.isSuccess() || raw.getStatus() == 404)  {
      this.reset();
      df.callback(null);
    } else {
      df.errback('error ' + raw.getStatusText());
    }
  }, this);
  return df;
};

