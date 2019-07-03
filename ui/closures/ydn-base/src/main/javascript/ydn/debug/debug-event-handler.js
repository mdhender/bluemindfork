/**
 * @fileoverview About this file
 */


goog.provide('ydn.debug.EventHandler');
goog.require('goog.events.EventHandler');



/**
 * Used only in debug
 * @param {SCOPE=} opt_scope Object in whose scope to call the listeners.
 * @constructor
 * @extends {goog.events.EventHandler}
 * @template SCOPE
 */
ydn.debug.EventHandler = function(opt_scope) {
  goog.base(this, opt_scope);
};
goog.inherits(ydn.debug.EventHandler, goog.events.EventHandler);


/**
 * @inheritDoc
 */
ydn.debug.EventHandler.prototype.handleEvent = function(e) {
  window.console.error('Unhandled event ' + e.type + ' from ' + e.target);
  window.console.log(e);
};


