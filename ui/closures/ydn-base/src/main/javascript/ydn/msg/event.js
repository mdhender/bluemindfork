/**
 * @fileoverview About this file
 */


goog.provide('ydn.msg.Event');
goog.require('goog.events.Event');



/**
 * Post message event.
 * @param {exYdn.PostMessage} msg
 * @constructor
 * @extends {goog.events.Event}
 */
ydn.msg.Event = function(msg) {
  var type = msg.req || 'event';
  goog.base(this, type);
  /**
   * @final
   * @type {exYdn.PostMessage}
   */
  this.mesage = msg;
};
goog.inherits(ydn.msg.Event, goog.events.Event);


/**
 * @return {exYdn.PostMessage}
 */
ydn.msg.Event.prototype.getMessage = function() {
  return this.mesage;
};


/**
 * @return {*}
 */
ydn.msg.Event.prototype.getData = function() {
  return this.mesage['data'];
};

