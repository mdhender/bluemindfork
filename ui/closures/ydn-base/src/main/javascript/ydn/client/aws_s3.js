/**
 * @fileoverview AWS S3 client.
 *
 * http://docs.aws.amazon.com/AWSJavaScriptSDK/
 */


goog.provide('ydn.client.aws.S3');
goog.require('ydn.client.Client');



/**
 * Create AWS S3 client.
 * @param {string=} opt_bucket bucket name.
 * @param {string=} opt_region bucket name.
 * @constructor
 * @implements {ydn.client.Client}
 * @struct
 */
ydn.client.aws.S3 = function(opt_bucket, opt_region) {
  if (!goog.isDef(goog.global['AWS'])) {
    throw new ydn.debug.error.InvalidOperationException(
        'AWS JS SDK not loaded');
  }
  /**
   * Default bucket
   * @type {string|undefined}
   * @protected
   */
  this.default_bucket_name = opt_bucket;
  /**
   * Default region
   * @type {string|undefined}
   * @protected
   */
  this.region = opt_region;
  /**
   * Default bucket
   * @type {AWS.S3}
   * @protected
   */
  this.default_bucket = null;
  /**
   * Cached bucket
   * @type {AWS.S3}
   * @protected
   */
  this.cached_bucket = null;
  /**
   * Cached bucket
   * @type {string|undefined}
   * @protected
   */
  this.cached_bucket_name = opt_bucket;
};


/**
 * @define {boolean} debug flag.
 */
ydn.client.aws.DEBUG = false;


/**
 * @inheritDoc
 */
ydn.client.aws.S3.prototype.request = function(req) {
  return new ydn.client.aws.HttpRequest(this, req);
};


/**
 * Get bucket or default.
 * @param {string=} opt_name
 * @return {AWS.S3}
 */
ydn.client.aws.S3.prototype.getBucket = function(opt_name) {
  var options = {
    'params': {
      'Bucket': this.default_bucket_name,
      'region': this.region
    }
  };
  if (!opt_name || opt_name == this.default_bucket_name) {
    if (!this.default_bucket_name) {
      throw new ydn.debug.error.ArgumentException('bucket name required');
    }
    if (!this.default_bucket) {
      this.default_bucket = new AWS.S3(options);
    }
    return this.default_bucket;
  } else if (this.cached_bucket && this.cached_bucket_name == opt_name) {
    return this.cached_bucket;
  } else {
    options.params['Bucket'] = opt_name;
    this.cached_bucket = new AWS.S3(options);
    this.cached_bucket_name = opt_name;
    return this.cached_bucket;
  }
};



/**
 * @implements {ydn.client.HttpRequest}
 * @param {ydn.client.aws.S3} parent
 * @param {ydn.client.HttpRequestData} params
 * @constructor
 * @struct
 */
ydn.client.aws.HttpRequest = function(parent, params) {
  /**
   * @protected
   * @type {ydn.client.aws.S3}
   */
  this.parent = parent;
  /**
   * @final
   * @private
   * @type {ydn.client.HttpRequestData}
   */
  this.req_data_ = params;
};


/**
 * Execute and callback.
 * @param {string} mth method.
 * @param {Object} params
 * @param {function(this: T, Object, ydn.client.HttpRespondData)?} cb
 * @param {T=} opt_scope scope.
 * @template T
 * @private
 */
ydn.client.aws.HttpRequest.prototype.execute_ = function(mth, params, cb, opt_scope) {
  var status = NaN;
  var status_text = '';
  var resp_headers = {};
  if (ydn.client.aws.DEBUG) {
    window.console.log(mth, params);
  }
  var req = this.parent.getBucket()[mth](
      /** @type {AWS.S3.ObjectReqParam} */ (params));
  req.on('complete', function(resp) {
    if (ydn.client.aws.DEBUG) {
      window.console.log(resp);
    }
    if (resp['httpResponse']) {
      status = resp['httpResponse']['statusCode'];
      if (goog.isString(status)) {
        status = parseInt(status, 10);
      }
      resp_headers = resp['httpResponse']['headers'];
    }
    if (resp['error']) {
      // in case of error, statusCode is still undefined.
      status_text = resp['error']['message'];
      if (!status) {
        status = 400;
      }
    }
  });
  req.send(function(err, data) {
    if (ydn.client.aws.DEBUG) {
      window.console.log(mth, params, err, data);
    }
    var resp = new ydn.client.HttpRespondData(status, data, resp_headers,
        status_text);
    if (err) {
      cb.call(opt_scope, null, resp);
    } else {
      cb.call(opt_scope, data, resp);
    }
  });
};


/**
 * @inheritDoc
 */
ydn.client.aws.HttpRequest.prototype.execute = function(cb, opt_scope) {
  var uri = new goog.Uri(this.req_data_.getUri());
  var path = uri.getPath();
  var key = path.charAt(0) == '/' ? path.substr(1) : path;
  var params = this.req_data_.headers || {};
  if (key) {
    params['Key'] = key;
  }

  if (!key) {
    // Bucket request
  } else {
    // Object request
    if (this.req_data_.method == 'GET') {
      this.execute_('getObject', params, cb, opt_scope);
    } else if (this.req_data_.method == 'PUT') {
      this.execute_('putObject', params, cb, opt_scope);
    } else {
      throw new ydn.debug.error.NotSupportedException('HTTP method ' +
          this.req_data_.method);
    }
  }
};



