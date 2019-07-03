
goog.provide('ydn.display.api.AccountActivityDisplay');

/**
 * @interface
 */
ydn.display.api.AccountActivityDisplay = function() {};


/**
 * @param {number} time
 */
ydn.display.api.AccountActivityDisplay.prototype.setLastAccessSince = function (time) {};

/**
 * This will call only if the ip is different from this ip.
 * @param {string} ip
 */
ydn.display.api.AccountActivityDisplay.prototype.setLastAccessFrom = function (ip) {};