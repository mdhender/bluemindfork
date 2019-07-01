/**
 * @fileoverview HTTP client.
 */


goog.provide('ydn.client');
goog.require('ydn.client.Client');
goog.require('ydn.client.SimpleClient');
goog.require('ydn.debug.ILogger');
goog.require('ydn.http.Scopes');


/**
 * @private
 * @type {Object.<ydn.client.Client>}
 */
ydn.client.clients_ = {};


/**
 * @private
 * @type {Array.<function(string=): string?>}
 */
ydn.client.scope_resolvers_ = [];


/**
 *
 * @param {function(string=): string?} resolver
 */
ydn.client.setScopeResolver = function(resolver) {
  ydn.client.scope_resolvers_ = [resolver];
};


/**
 *
 * @param {function(string=): string?} resolver Resolver should resolve given
 * feed_uri to scope,
 * or return null when feed_uri is irrelevant to the resolver.
 */
ydn.client.addScopeResolver = function(resolver) {
  if (resolver) {
    ydn.client.scope_resolvers_.push(resolver);
  }
};


/**
 * Set whatever proxy
 *
 * @param {ydn.client.Client} transport
 * @param {string=} opt_scope
 */
ydn.client.setClient = function(transport, opt_scope) {
  opt_scope = opt_scope || ydn.http.Scopes.DEFAULT;
  ydn.client.clients_[opt_scope] = transport;
};


/**
 * Get whatever proxy transport set previously
 * If given proxy is not available, a default transport will be return instead
 * of null.
 * @param {string=} opt_uri uri or scope {@link ydn.http.Scopes}
 * @return {ydn.client.Client} transport.
 * @see ydn.client.getDefaultClient for getting a default client.
 */
ydn.client.getClient = function(opt_uri) {
  if (!goog.isDef(opt_uri)) {
    return ydn.client.clients_[ydn.http.Scopes.DEFAULT];
  }
  var scope = opt_uri;
  for (var i = 0; i < ydn.client.scope_resolvers_.length; i++) {
    var resolver = ydn.client.scope_resolvers_[i];
    var out = resolver(opt_uri);
    if (out) {
      scope = out;
      break;
    }
  }
  return ydn.client.clients_[scope] || null;
};


/**
 * Get default client. If default client does not exist, a default client
 * will be set.
 * @return {!ydn.client.Client}
 */
ydn.client.getDefaultClient = function() {
  var client = ydn.client.getClient();
  if (!client) {
    client = new ydn.client.SimpleClient();
    ydn.client.setClient(client);
  }
  return client;
};


/**
 * Clear transport
 * @param {string=} opt_scope if not specified, all transports will be cleared.
 */
ydn.client.clearTransport = function(opt_scope) {
  if (goog.isDef(opt_scope)) {
    delete ydn.client.clients_[opt_scope];
  } else {
    ydn.client.clients_ = {};
    ydn.client.scope_resolvers_ = [];
  }
};


/**
 * @type {ydn.debug.ILogger} log error.
 */
ydn.client.error_logger = null;

