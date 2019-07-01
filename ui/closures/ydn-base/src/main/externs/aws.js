/**
 * @fileoverview AWS JS SDK.
 *
 * http://docs.aws.amazon.com/AWSJavaScriptSDK/latest/frames.html
 * @externs
 */


/**
 * @type {Object}
 * @const
 */
var AWS = {};


/**
 * Represents your AWS security credentials.
 * @constructor
 */
AWS.Credentials = function() {};


/**
 * Amazon Mobile Analytics Manager
 * @param {Object} options
 * @constructor
 * @extends {AWS.Credentials}
 */
AWS.CognitoIdentityCredentials = function(options) {};


/**
 * @type {string}
 */
AWS.config.region = '';


/**
 * @type {AWS.Credentials}
 */
AWS.config.credentials = null;



/**
 * @param {{RoleArn: string, WebIdentityToken: string}} params parameters.
 * @extends {AWS.Credentials}
 * @constructor
 */
AWS.WebIdentityCredentials = function(params) {};


/**
 * @type {string}
 */
AWS.WebIdentityCredentials.prototype.WebIdentityToken = '';



/**
 * This class encapsulates the the response information from a service request
 * operation sent through AWS.Request.
 * @constructor
 */
AWS.Response = function() {

};


/**
 * @type {Object}
 */
AWS.Response.prototype.data = null;


/**
 * @type {Object}
 */
AWS.Response.prototype.error = null;


/**
 * @type {{
 *    body: string,
 *    headers: Object.<string>,
 *    statusCode: number
 * }}
 */
AWS.Response.prototype.httpResponse;



/**
 * All requests made through the SDK are asynchronous and use a callback interface.
 * @constructor
 */
AWS.Request = function() {

};


/**
 * Listen event.
 * @param {string} event success, error or complete
 * @param {function(AWS.Response)} callback
 */
AWS.Request.prototype.on = function(event, callback) {};


/**
 * Sends the request object.
 * {Error} err the error object returned from the request.
 * Set to null if the request is successful.
 * {Object} data  the de-serialized data returned from the request.
 * Set to null if a request error occurs.
 * @param {function(Error, Object)=} opt_callback request callback.
 */
AWS.Request.prototype.send = function(opt_callback) {};



/**
 * Constructs S3 service interface object.
 * @param {{params: {
 *   region: (string|undefined), Bucket: (string|undefined)}
 * }} params
 * @constructor
 */
AWS.S3 = function(params) {};



/**
 * @constructor
 */
AWS.S3.Object = function() {};


/**
 * @type {string}
 */
AWS.S3.Object.prototype.ETag = '';


/**
 * @type {string}
 */
AWS.S3.Object.prototype.Generation = '';


/**
 * @type {string}
 */
AWS.S3.Object.prototype.Key = '';


/**
 * @type {string}
 */
AWS.S3.Object.prototype.LastModified = '';


/**
 * @type {string}
 */
AWS.S3.Object.prototype.MetaGeneration = '';


/**
 * @type {string}
 */
AWS.S3.Object.prototype.Size = '';



/**
 * @constructor
 */
AWS.S3.ListBucketResult = function() {};


/**
 * @type {!Array.<!AWS.S3.Object>}
 */
AWS.S3.ListBucketResult.prototype.Contents;


/**
 * @type {string}
 */
AWS.S3.ListBucketResult.prototype.IsTruncated = '';


/**
 * @type {string}
 */
AWS.S3.ListBucketResult.prototype.Marker = '';


/**
 * @type {string}
 */
AWS.S3.ListBucketResult.prototype.Name = '';


/**
 * @type {string}
 */
AWS.S3.ListBucketResult.prototype.Prefix = '';


/**
 * @typedef {{
 *   ACL: (string|undefined),
 *   Body: (Object|string),
 *   CacheControl: (string|undefined),
 *   ContentDisposition:(string|undefined),
 *   Expires:(Date|undefined),
 *   Key: string,
 *   ContentType:(string|undefined),
 *   Metadata:(Object.<string>|undefined)
 * }} put object req parameter.
 */
AWS.S3.ParamPutObj;


/**
 * Adds an object to a bucket.
 * @param {AWS.S3.ParamPutObj} params
 * @param {function(boolean, Object)=} callback
 * @return {AWS.Request}
 */
AWS.S3.prototype.putObject = function(params, callback) {};


/**
 * @typedef {{
 *   IfMatch: (string|undefined),
 *   IfModifiedSince: (Date|undefined),
 *   IfNoneMatch:(string|undefined),
 *   IfUnmodifiedSince:(Date|undefined),
 *   Range:(string|undefined),
 *   VersionId:(string|undefined),
 *   Key: string
 * }}
 */
AWS.S3.ObjectReqParam;


/**
 * Retrieves objects from Amazon S3.
 * @param {AWS.S3.ObjectReqParam} params
 * @param {function(boolean, Object)=} callback
 * @return {AWS.Request}
 */
AWS.S3.prototype.getObject = function(params, callback) {};


/**
 * Returns some or all (up to 1000) of the objects in a bucket.
 * @param {{
 *   Bucket: (string|undefined),
 *   Delimiter: (string|undefined),
 *   Marker:(string|undefined),
 *   MaxKeys:(number|undefined),
 *   Prefix:(string|undefined)
 * }} params
 * @param {function(boolean, AWS.S3.ListBucketResult)=} callback result
 * callback.
 * @return {AWS.Request}
 */
AWS.S3.prototype.listObjects = function(params, callback) {};


/**
 * @const
 * @type {Object}
 */
var AMA = {};



/**
 * Amazon Mobile Analytics Manager
 * @param {Object} options
 * @constructor
 * @link https://aws.github.io/aws-sdk-mobile-analytics-js/doc/AMA.Manager.html
 */
AMA.Manager = function(options) {};


/**
 * <pre>
 *   mobileAnalyticsClient.recordEvent('CUSTOM EVENT NAME', {
 *      'ATTRIBUTE_1_NAME': 'ATTRIBUTE_1_VALUE',
 *      }, {
 *       'METRIC_1_NAME': 1,
 *   });
 * </pre>
 * @param name
 * @param attributes
 * @param matric
 */
AMA.Manager.recordEvent = function(name, attributes, matric) {};
