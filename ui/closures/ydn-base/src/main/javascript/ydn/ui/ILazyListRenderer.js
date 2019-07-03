/**
 * @fileoverview Lazy list item renderer.
 */


goog.provide('ydn.ui.ILazyListRenderer');


/**
 * @interface
 */
ydn.ui.ILazyListRenderer = function() {};


/**
 * @return {number}
 */
ydn.ui.ILazyListRenderer.prototype.getHeight = function() {};


/**
 * Render on given element.
 * @param {Object} data
 * @return {Element}
 */
ydn.ui.ILazyListRenderer.prototype.render = function(data) {};

