/**
 * @fileoverview Lazy list data model interface.
 *
 * When number of items change, this dispatch {@link goog.events.EventType.CHANGE} 
 * and {@link goog.events.EventType.LOAD} events.
 */


goog.provide('ydn.ui.ILazyListModel');


/**
 * Lazy list data model interface.
 * @interface
 * @extends {goog.events.Listenable}
 */
ydn.ui.ILazyListModel = function() {};


/**
 * @return {number}
 */
ydn.ui.ILazyListModel.prototype.getCount = function() {

};


/**
 * @param {number} at
 * @return {Object}
 */
ydn.ui.ILazyListModel.prototype.getItemAt = function(at) {

};
