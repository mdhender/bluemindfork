/**
 * @fileoverview About this file
 */


goog.provide('ydn.client.Resource');



/**
 * Resource
 * @param {string} path
 * @param {Object=} opt_body
 * @param {number=} opt_updated
 * @param {string=} opt_etag
 * @constructor
 */
ydn.client.Resource = function(path, opt_body, opt_updated, opt_etag) {
  this.path = path;
  this.body = null;
  if (opt_body) {
    this.body = ydn.object.clone(opt_body);
    this.updated = opt_updated || goog.now();
    this.etag = opt_etag || ydn.utils.getEtag(this.body);
  }
};


/**
 * comparator
 * @param {ydn.client.Resource} a
 * @param {ydn.client.Resource} b
 * @return {number}
 */
ydn.client.Resource.cmp = function(a, b) {
  return a.path == b.path ? 0 : a.path > b.path ? 1 : -1;
};
