/**
 * @fileoverview Extern declaration for JsTestDriver that is not in closure testing suite.
 * User: mbikt
 * Date: 4/18/12
 * @see js_test_driver
 * @externs
 */

/**
 *
 * @param {string} name
 */
var TestCase = function(name) {};

/**
 *
 * @param {string} name
 */
var AsyncTestCase = function(name) {};


var assertException = function (msg, callback, error) { // in Closure test, it is assertThrows
};

var jstestdriver = {};


/**
 * http://code.google.com/p/google-api-javascript-client/wiki/ReferenceDocs
 * @constructor
 */
var GapiRequest = function() {};


/**
 * @type {string} The URL to handle the request.
 */
GapiRequest.prototype.path;

/**
 * @type {string} The HTTP request method to use. Default is 'GET'.
 */
GapiRequest.prototype.method;

/**
 * @type {Object} URL params in key-value pair form.
 */
GapiRequest.prototype.params;

/**
 * @type {Object}  Additional HTTP request headers.
 */
GapiRequest.prototype.headers;

/**
 * @type {string} The HTTP request body (applies to PUT or POST).
 */
GapiRequest.prototype.body;


/**
 * @const
 * @type {Object}
 */
var CryptoJS = {};


/**
 * @param {string} s
 * @return {string}
 */
CryptoJS.MD5 = function(s) {};
