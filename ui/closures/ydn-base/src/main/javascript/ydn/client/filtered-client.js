/**
 * @fileoverview Filtered client.
 *
 * Filtered client allow optional pass through if certain criteria are met.
 */


goog.provide('ydn.client.FilteredClient');



/**
 * Filtered client allow optional pass through if certain criteria are met.
 * @param {function(ydn.client.HttpRequestData)} filter
 * @param {ydn.client.Client} pass_client
 * @param {ydn.client.Client} fail_client
 * @constructor
 * @struct
 * @implements {ydn.client.Client}
 */
ydn.client.FilteredClient = function(filter, pass_client, fail_client) {
  this.filter_ = filter;
  this.pass_client_ = pass_client;
  this.fail_client_ = fail_client;
};


/**
 * @inheritDoc
 */
ydn.client.FilteredClient.prototype.request = function(req) {
  if (this.filter_(req)) {
    return this.pass_client_.request(req);
  } else {
    return this.fail_client_.request(req);
  }
};
