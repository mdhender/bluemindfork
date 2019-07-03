/**
 * @fileoverview OAuth client.
 *
 */


goog.provide('ydn.client.ISimpleOAuthProvider');



/**
 * @interface
 */
ydn.client.ISimpleOAuthProvider = function() {};


/**
 * @return {!goog.async.Deferred}
 */
ydn.client.ISimpleOAuthProvider.prototype.getAuthToken = goog.abstractMethod;



/**
 * @return {!goog.async.Deferred}
 */
ydn.client.ISimpleOAuthProvider.prototype.removeToken = goog.abstractMethod;
