/**
 * @fileoverview OAuth client.
 *
 */


goog.provide('ydn.client.IOAuthProvider');



/**
 * @interface
 */
ydn.client.IOAuthProvider = function() {};


/**
 * @return {!goog.async.Deferred}
 */
ydn.client.IOAuthProvider.prototype.getOAuthToken = goog.abstractMethod;


