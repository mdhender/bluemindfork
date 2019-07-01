/**
 * @fileoverview Mock server for REST resource in JSON format.
 */

goog.provide('ydn.client.JsonMockClient');
goog.require('ydn.client.MockClient');



/**
 * <pre>
 *   client = new ydn.client.JsonMockClient(1);
 *   client.setResources([{
 *     method: 'GET',
 *     path: 'http://localhost/test',
 *     resp: {
 *       body: 'OK'
 *       status: 200
 *     }
 *   }]);
 * </pre>
 * Override getResource method for more custom respond.
 * <pre>
 *   client = new ydn.client.JsonMockClient(1);
 *   var old = client.getResource;
 *   client.getResource = function(req) {
 *     if (req.path == '/') {
 *
 *     } else {
 *       return old.call(client, req);
 *     }
 *   };
 * </pre>
 * Set debug log for tracking bug.
 * <pre>
 *   ydn.debug.log('ydn.client', 'fine');
 * </pre>
 * Mock server for REST resource in JSON format.
 * @param {number=} opt_delay server respond time, default to 0 for
 * synchronous respond.
 * @constructor
 * @struct
 * @extends {ydn.client.MockClient}
 */
ydn.client.JsonMockClient = function(opt_delay) {
  ydn.client.JsonMockClient.base(this, 'constructor', opt_delay);
  /**
   * @type {Array<ydn.client.JsonMockClient.ReqObj>}
   */
  this.resources = [];
};
goog.inherits(ydn.client.JsonMockClient, ydn.client.MockClient);


/**
 * @typedef {{
 *   body: Object,
 *   status: number,
 *   statusText: (string|undefined)
 * }}
 */
ydn.client.JsonMockClient.RespObj;


/**
 * @typedef {{
 *   method: string,
 *   path: string,
 *   resp: ydn.client.JsonMockClient.RespObj
 * }}
 */
ydn.client.JsonMockClient.ReqObj;


/**
 * Set resources to respoind the request.
 * @param {Array<ydn.client.JsonMockClient.ReqObj>} res
 */
ydn.client.JsonMockClient.prototype.setResources = function(res) {
  this.resources = res;
};


/**
 * Set resource stored in JSON file in server.
 * @param {string} path
 */
ydn.client.JsonMockClient.prototype.setResourcesByUrl = function(path) {
  var xhr = new XMLHttpRequest();
  xhr.open('GET', path, false);
  var me = this;
  xhr.onload = function() {
    me.resources = JSON.parse(xhr.responseText);
  };
  xhr.send(null);
};


/**
 * Add a resource
 * @param {string} mth 'GET', 'POST'
 * @param {string} path
 * @param {ydn.client.JsonMockClient.RespObj} resp
 */
ydn.client.JsonMockClient.prototype.addResource = function(mth, path, resp) {
  this.resources.push({
    method: mth,
    path: path,
    resp: resp
  });
};


/**
 * Set or remove a resource
 * @param {string} mth 'GET', 'POST'
 * @param {string} path
 * @param {ydn.client.JsonMockClient.RespObj} resp
 * @return {number} 1 if added, 0 if replace the resource.
 */
ydn.client.JsonMockClient.prototype.setResource = function(mth, path, resp) {
  var idx = this.findResource_({
    method: mth,
    path: path
  });
  if (idx >= 0) {
    if (!resp) {
      goog.array.removeAt(this.resources, idx);
    } else {
      this.resources[idx].resp = resp;
    }

    return 0;
  }
  this.resources.push({
    method: mth,
    path: path,
    resp: resp
  });
  return 1;
};


/**
 * Find resource index.
 * @param req
 * @return {number}
 * @private
 */
ydn.client.JsonMockClient.prototype.findResource_ = function(req) {
  for (var i = 0; i < this.resources.length; i++) {
    var res = this.resources[i];
    if ((!res.method || res.method == req.method) &&
        (!res.path || res.path == req.path) &&
        (!res.body || res.body == req.body)) {
      return i;
    }
  }
  // console.log(req.body);
  return -1;
};


/**
 * Get resource.
 * @param {ydn.client.JsonMockClient.ReqObj} req
 * @return {?ydn.client.JsonMockClient.RespObj}
 */
ydn.client.JsonMockClient.prototype.getResource = function(req) {
  var idx = this.findResource_(req);
  if (idx >= 0) {
    return this.resources[idx];
  } else {
    return null;
  }
};


/**
 * @inheritDoc
 */
ydn.client.JsonMockClient.prototype.request = function(req) {
  var r = goog.base(this, 'request', req);
  var resp = r.response;
  resp.status = 404;
  resp.statusText = req.method + ' ' + req.path + ' Not Found';
  var res = this.getResource(req);
  if (res) {
    resp.status = res.resp.status;
    resp.statusText = res.resp.statusText || '';
    resp.body = res.resp.body;
  }
  return r;
};
