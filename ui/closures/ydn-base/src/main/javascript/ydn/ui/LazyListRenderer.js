/**
 * @fileoverview Lazy list data model.
 */


goog.provide('ydn.ui.LazyListRenderer');
goog.require('ydn.ui.ILazyListRenderer');


/**
 * Lazy list data model.
 * @param {number=} opt_height item height.
 * @constructor
 * @implements {ydn.ui.ILazyListRenderer}
 * @struct
 */
ydn.ui.LazyListRenderer = function(opt_height) {
  /**
   * @protected
   * @type {number}
   */
  this.height = opt_height || 60;
};


/**
 * @return {number}
 */
ydn.ui.LazyListRenderer.prototype.getHeight = function() {
  return this.height;
};


/**
 * Render on given element.
 * @param {Object} data
 */
ydn.ui.LazyListRenderer.prototype.render = function(data) {
  var el = document.createElement('DIV');
  el.textContent = data['id'];
  el.setAttribute('data-id', data['id']);
  goog.style.setHeight(el, this.height);
  setTimeout(function() {
    el.textContent = data['id'] + '. ' + data['name'];
  }, Math.random() * 1000);
  return el;
};

