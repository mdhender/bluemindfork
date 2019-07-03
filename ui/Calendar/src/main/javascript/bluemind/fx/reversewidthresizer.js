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
 * @fileoverview Resize Utilities for width resizing.
 *
 */

goog.provide('bluemind.fx.ReverseWidthResizer');

goog.require('bluemind.fx.Resizer');

/**
 * A class that allows mouse or touch-based width resize (moving) of an element
 *
 * @param {Element} target The element that will be dragged.
 * @param {Element=} opt_handle An optional handle to control the drag, if null
 *     the target is used.
 * @param {goog.math.Rect=} opt_limits Object containing left, top, width,
 *     and width.
 *
 * @extends {bluemind.fx.Dragger}
 * @constructor
 */
bluemind.fx.ReverseWidthResizer = function(target, opt_handle, opt_limits) {
  goog.base(this, target, opt_handle, opt_limits);
};
goog.inherits(bluemind.fx.ReverseWidthResizer, bluemind.fx.Resizer);

/** @inheritDoc */
bluemind.fx.ReverseWidthResizer.prototype.resizeHeight = function(y) {};

/** @inheritDoc */
bluemind.fx.ReverseWidthResizer.prototype.limitY = function(y) {};

/** @inheritDoc */
bluemind.fx.ReverseWidthResizer.prototype.resizeWidth = function(x) {
  var dx = (this.originalPosition.x - x);
  this.target.style.left = (this.originalPosition.x - dx) + 'px';
  this.target.style.width = (this.originalSize.width + dx) + 'px';
};
