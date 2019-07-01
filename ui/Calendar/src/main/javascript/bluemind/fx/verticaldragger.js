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
 * @fileoverview Dragger Utilities for vertical dragging.
 *
 */

goog.provide('bluemind.fx.VerticalDragger');

goog.require('bluemind.fx.Dragger');

/**
 * A class that allows mouse or touch-based dragging (moving) of an element
 * with snapping
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
bluemind.fx.VerticalDragger = function(target, opt_handle, opt_limits) {
  goog.base(this, target, opt_handle, opt_limits);
};
goog.inherits(bluemind.fx.VerticalDragger, bluemind.fx.Dragger);

/** @inheritDoc */
bluemind.fx.VerticalDragger.prototype.limitX = function(x) {};

/** @inheritDoc */
bluemind.fx.VerticalDragger.prototype.defaultAction = function(x, y) {
  this.target.style.top = y + 'px';
};
