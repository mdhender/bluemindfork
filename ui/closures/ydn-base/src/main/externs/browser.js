/**
 * @fileoverview About this file.
 * User: mbikt
 * Date: 4/24/12
 * @externs
 */


/**
* Socket IO
* @constructor
*/
function Socket() {}

/**
* @param {string} channel
* @param {Function} listener
*/
Socket.prototype.on = function(channel, listener) {};

/**
* @param {string} channel
* @param {Object} obj
*/
Socket.prototype.emit = function(channel, obj) {};

/**
* Socket IO service
* @constructor
*/
function io() {}

/**
* @param {string} server
* @return {Socket}
*/
io.prototype.connect = function(server) {};



/**
 * @final
 * @type {boolean}
 */
IDBIndex.prototype.multiEntry;



/**
 * @final
 * @type {boolean}
 */
IDBObjectStore.prototype.autoIncrement;



/**
 * @param {*=} key Key identifying the record to be retrieved.
 * @return {!IDBRequest} The IDBRequest object.
 */
IDBIndex.prototype.count = function(key) {};


/**
 *
 * @param {*} first
 * @param {*} second
 * @return {number}
 */
IDBFactory.prototype.cmp = function(first, second) {};


/**
 * @type {!DOMError}
 * @const
 */
IDBRequest.prototype.error;


/**
 * @const
 * @type {Object}
 */
chrome.declarativeContent = {};



/**
 * @param {Object} details
 * @constructor
 */
chrome.declarativeContent.PageStateMatcher = function(details) {};



/**
 * @constructor
 */
chrome.declarativeContent.ShowPageAction = function() {};



/**
 * @param {Object} details
 * @constructor
 */
chrome.declarativeContent.RequestContentScript = function(details) {};


/**
 * @const
 * @param {Object} details
 */
chrome.declarativeContent.onPageChanged = {};


/**
 * @typedef {{
 *   id: string,
 *  conditions: Array,
 *  actions: Array
 * }}
 */
chrome.declarativeContent.RuleOptions;



/**
 * @param {Array.<chrome.declarativeContent.RuleOptions>|undefined} rules
 * @param {function(Object)} callback
 * @see http://developer.chrome.com/apps/app.runtime.html#event-onLaunched
 */
chrome.declarativeContent.onPageChanged.removeRules = function(rules, callback) {};


/**
 * @param {Array.<chrome.declarativeContent.RuleOptions>} rules
 */
chrome.declarativeContent.onPageChanged.addRules = function(rules) {};


/**
 * @const
 * @type {Object}
 */
chrome.instanceID = {};


/**
 * @param {function(string)} cb
 */
chrome.instanceID.getID = function(cb) {};
