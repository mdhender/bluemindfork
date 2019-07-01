/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
*/

/**
 * @fileoverview Resize Utilities.
 *
 * Provides extensible functionality for resizing behaviour.
 *
 */

goog.provide('bluemind.fx.Resizer');

goog.require('bluemind.fx.Dragger');
goog.require('goog.events.BrowserEvent');
goog.require('goog.math.Coordinate');
goog.require('goog.math.Rect');
goog.require('goog.math.Size');
goog.require('goog.style');

/**
 * A class that allows mouse or touch-based resize (moving) of an element
 *
 * @param {Element} target The element that will be dragged.
 * @param {Element=} opt_handle An optional handle to control the drag, if null
 *     the target is used.
 * @param {goog.math.Rect=} opt_limits Object containing left, top, width,
 *     and height.
 *
 * @extends {bluemind.fx.Dragger}
 * @constructor
 */
bluemind.fx.Resizer = function(target, opt_handle, opt_limits) {
  goog.base(this, target, opt_handle, opt_limits);
};
goog.inherits(bluemind.fx.Resizer, bluemind.fx.Dragger);

/**
 * The size of the target when the first mousedown or touchstart occurred.
 * @protected
 * @type {goog.math.Size}
 */
bluemind.fx.Resizer.prototype.originalSize;


/** @inheritDoc */
bluemind.fx.Resizer.prototype.startDrag = function(e) {
  this.originalSize = goog.style.getSize(this.target);
  var borderBox = goog.style.getBorderBox(this.target);
  goog.base(this, 'startDrag', e);
};

/** @inheritDoc */
bluemind.fx.Resizer.prototype.resetDrag = function(e) {
  this.originalSize = goog.style.getSize(this.target);
  var borderBox = goog.style.getBorderBox(this.target);
  goog.base(this, 'resetDrag', e);
};

/** @inheritDoc */
bluemind.fx.Resizer.prototype.adjustLimit = function() {
  if (!isNaN(this.limits.top)) {
    this.limits.top -= this.originalSize.height - this.grid.height + 1;
  }
  if (!isNaN(this.limits.height)) {
    this.limits.height -= this.grid.height;
  }
  if (!isNaN(this.limits.left)) {
    this.limits.left -= this.originalSize.width - this.grid.width;
  }
  if (!isNaN(this.limits.width)) {
    this.limits.width -= this.grid.width;
  }
  goog.base(this, 'adjustLimit');
};

/** @inheritDoc */
bluemind.fx.Resizer.prototype.defaultAction = function(x, y) {
  this.resizeHeight(y);
  this.resizeWidth(x);
};

/**
 * Perform width resize
 * @protected
 * @param {number} x X-coordinate for target element.
 */
bluemind.fx.Resizer.prototype.resizeWidth = function(x) {
  var width = this.originalSize.width + (x - this.originalPosition.x);
  var dx = 0;
  if (width < this.grid.width) {
    width = Math.abs(width) + this.grid.width;
    dx = width;
    width += this.originalSize.width;
  }
  this.target.style.left = (this.originalPosition.x - dx) + 'px';
  this.target.style.width = (width) + 'px';
};

/**
 * Perform height resize
 * @protected
 * @param {number} y Y-coordinate for target element.
 */
bluemind.fx.Resizer.prototype.resizeHeight = function(y) {
  var height = this.originalSize.height + (y - this.originalPosition.y) + 1;
  var dy = 0;
  if (height < this.grid.height) {
    height = Math.abs(height) + this.grid.height;
    dy = height;
    height += this.originalSize.height;
  }
  this.target.style.top = (this.originalPosition.y - dy) + 'px';
  this.target.style.height = (height) + 'px';
};
