/**
 * @fileoverview Default HTTP client.
 */


goog.provide('ydn.client.AdaptorClient');
goog.require('ydn.client');
goog.require('ydn.client.SimpleHttpRequest');



/**
 * Singleton simple client.
 * @param {ydn.client.Client} client parent client.
 * @param {function(this: T, ydn.client.HttpRequestData): ydn.client.HttpRequestData} adaptor
 * to modifiy request data.
 * @param {T=} opt_scope optional scope.
 * @template T
 * @constructor
 * @implements {ydn.client.Client}
 * @const
 */
ydn.client.AdaptorClient = function(client, adaptor, opt_scope) {
  /**
   * @final
   * @type {ydn.client.Client}
   */
  this.client = client;
  /**
   * @final
   * @protected
   * @type {function(this:T, ydn.client.HttpRequestData): ydn.client.HttpRequestData}
   */
  this.adaptor = adaptor;
  /**
   * @protected
   * @final
   * @type {*}
   */
  this.scope = opt_scope;
};


/**
 * @inheritDoc
 */
ydn.client.AdaptorClient.prototype.request = function(args) {
  args = this.adaptor.call(this.scope, args);
  return this.client.request(args);
};



