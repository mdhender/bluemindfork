/**
 * @fileoverview Mock server for REST resource in JSON format.
 */


goog.provide('ydn.client.JsonMockServer');

goog.require('goog.Timer');
goog.require('goog.log');
goog.require('ydn.client.Resource');
goog.require('ydn.http.CallbackResult');
goog.require('ydn.object');
goog.require('ydn.structs.Buffer');
goog.require('ydn.utils');



/**
 * @param {number=} opt_delay server respond time, default to 0 for
 * synchronous respond.
 * @constructor
 * @implements {ydn.client.Client}
 * @struct
 */
ydn.client.JsonMockServer = function(opt_delay) {

  this.delay = opt_delay || 0;
  this.resources = new ydn.structs.Buffer(ydn.client.Resource.cmp);
  this.hit_counts = {
    'GET': 0,
    'POST': 0,
    'PUT': 0,
    'DELETE': 0,
    'HEAD': 0
  };
};


/**
 * @define {boolean} debug flag.
 */
ydn.client.JsonMockServer.DEBUG = false;


/**
 * @protected
 * @type {goog.debug.Logger}
 */
ydn.client.JsonMockServer.prototype.logger =
    goog.log.getLogger('ydn.client.JsonMockServer');


/**
 * Response data is hash by encoded query string without '?'.
 * @typedef {{
 *  body: string,
 *  status: number
 * }}
 */
ydn.client.JsonMockServer.ResponseData;


/**
 * @type {ydn.structs.Buffer}
 */
ydn.client.JsonMockServer.prototype.resources;


/**
 *
 * @type {number}
 */
ydn.client.JsonMockServer.prototype.delay = 0;


/**
 * @type {Object.<number>} hit count.
 * @protected
 */
ydn.client.JsonMockServer.prototype.hit_counts;


/**
 * Response data is hash by encoded query string.
 * @param {string} app_path url path to feed.
 * @param {Object} data response feed object with entry.
 * @return {ydn.client.Resource} data added to the database.
 */
ydn.client.JsonMockServer.prototype.loadData = function(app_path, data) {
  var key = window.decodeURIComponent(app_path);
  goog.asserts.assertObject(data);
  var res = new ydn.client.Resource(key, data);
  this.resources.add(res);
  return res;
};


/**
 * Get HTTP query count.
 * @param {string=} opt_mth HTTP method. By default return all queries count.
 * @return {number} number of query.
 */
ydn.client.JsonMockServer.prototype.getHitCount = function(opt_mth) {
  if (opt_mth) {
    return this.hit_counts[opt_mth];
  } else {
    var n = 0;
    for (var key in this.hit_counts) {
      n += this.hit_counts[key];
    }
    return n;
  }
};


/**
 * Extract document for the uri.
 * @param {!goog.Uri} uri
 * @return {ydn.client.Resource}
 * @protected
 */
ydn.client.JsonMockServer.prototype.getResource = function(uri) {
  var res = null;
  this.resources.traverse(function(obj) {
    res = obj.value;
    return true; // break
  }, new ydn.client.Resource(uri.getPath()));
  return /** @type {ydn.client.Resource} */ (res);
};


/**
 * Extract document for the uri.
 * @param {!goog.Uri} uri
 * @return {Object|undefined}
 * @protected
 */
ydn.client.JsonMockServer.prototype.getContent = function(uri) {
  var res = this.getResource(uri);
  if (res) {
    return res.body;
  } else {
    return null;
  }
};


/**
 * Prepare response. Sort or filter as necessary.
 * @param {ydn.client.HttpRequestData} data
 * @param {string} content_type
 * @param {number} status
 * @param {ydn.client.Resource} res
 * @return {!ydn.client.HttpRespondData} HTTP respond result.
 */
ydn.client.JsonMockServer.prototype.prepareResult = function(data,
    content_type, status, res) {

  var headers = {};
  if (status >= 200 && status < 400 && res) {
    headers = {
      'etag': res.etag,
      'updated': res.updated};
  }

  if ((headers.etag &&
      headers.etag == options.headers['if-none-match']) ||
      (headers.updated &&
          headers.updated == options.headers['if-modified-since'])) {
    return new ydn.client.HttpRespondData(304, null, headers);
  } else {
    var json = null;
    if (res) {
      json = res.body;
    }
    return new ydn.client.HttpRespondData(status, json, headers);
  }

};


/**
 * @type {ydn.http.CallbackResult}
 * @private
 */
ydn.client.JsonMockServer.prototype.last_response_ = null;


/**
 * @return {ydn.http.CallbackResult}
 */
ydn.client.JsonMockServer.prototype.getLastResponse = function() {
  return this.last_response_;
};


/**
 * @inheritDoc
 */
ydn.client.JsonMockServer.prototype.request = function(req) {

  var method = req.method ? req.method.toUpperCase() : 'GET';
  var content_type = 'application/json';
  var status = 404;
  var res = null;

  var uri = new goog.Uri(req.getUri());
  this.hit_counts[method]++;

  if (method == 'GET') {
    res = this.getResource(uri);
    if (res) {
      status = 200;
    }
  } else if (method == 'PUT' || method == 'POST') {
    res = this.getResource(uri);
    if (res) {
      status = 200;
    } else {
      status = 201;
    }
    var load = goog.isString(req.body) ? ydn.json.parse(req.body) :
        req.body || {};
    res = this.loadData(uri.getPath(), load);
    if (status == 201) {
      // leave it
    } else {
      res = null;
    }
  } else if (method == 'DELETE') {
    res = this.getResource(uri);
    if (res) {
      status = 200;
      this.resources.remove(res);
    } else {
      status = 404;
    }
  } else {
    throw new ydn.error.NotImplementedException(method);
  }

  var response = this.prepareResult(req, content_type, status, res);

  goog.log.finest(this.logger, 'Responding ' + response.status + ' to ' + method +
      ' ' + url);

  var reply = function() {
    this.last_response_ = response;
    if (req.callback) {
      req.callback()
    }
  }
  if (this.delay <= 0) {
    this.last_response_ = response;
    callback(response);
    callback = undefined;
  } else {
    goog.Timer.callOnce(function() {
      this.last_response_ = response;
      callback(response);
      callback = undefined;
    }, this.delay, this);
  }
};


