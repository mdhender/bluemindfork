goog.provide('ydn.api.display.UserDisplay');



/**
 * @interface
 */
ydn.api.display.UserDisplay = function() {};



/**
 *
 * @param {string} email
 * @param {boolean=} un_confirmed nickname is not yet confirmed from server, but extracted from the session.
 */
ydn.api.display.UserDisplay.prototype.setNickname = function (email, un_confirmed) {};
//
///**
// *
// * @param {string} label
// */
//ydn.api.display.UserDisplay.prototype.setLoginLabel = function (label) {};


/**
 * Click login or logout link.
 * @return {EventTarget}
 */
ydn.api.display.UserDisplay.prototype.getLoginClick = function() {};


/**
* @param {string} url
*/
ydn.api.display.UserDisplay.prototype.setLoginUrl = function (url) {};

/**
*
* @param {string} url
*/
ydn.api.display.UserDisplay.prototype.setLogoutUrl = function (url) {};


