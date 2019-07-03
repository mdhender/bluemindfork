/**
 * @fileoverview HTTP client.
 */


goog.provide('ydn.client.Client');
goog.require('ydn.client.HttpRequest');
goog.require('ydn.client.HttpRequestData');
goog.require('ydn.client.HttpRespondData');



/**
 * HTTP Request client.
 * @interface
 */
ydn.client.Client = function() {};


/**
 * Create a new HTTP request.
 * If callback is provided in the argument, the request is execute immediately.
 * @param {ydn.client.HttpRequestData} req
 * @return {!ydn.client.HttpRequest} Return request object if callback is not
 * provided in the argument.
 */
ydn.client.Client.prototype.request = function(req) {};


