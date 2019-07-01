goog.provide('ydn.api.display.TokenDisplay');



/**
 * @interface
 */
ydn.api.display.TokenDisplay = function() {};


/**
 * Set authorize url and show it. If null, hide it.
 * @param {string?} url
 */
ydn.api.display.TokenDisplay.prototype.setAuthorizeUrl = function (url) {};


/**
 * @return {EventTarget}
 */
ydn.api.display.TokenDisplay.prototype.getAuthorizeUrlClick = function() {};



