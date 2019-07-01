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
 * @fileoverview Drag Utilities.
 * @suppress {accessControls}
 * Provides extensible functionality for drag & drop behaviour.
 * 
 */
goog.provide('bluemind.fx.Dragger');
goog.provide('bluemind.fx.Dragger.EventType');

goog.require('goog.fx.Dragger');
goog.require('goog.math.Coordinate');
goog.require('goog.math.Rect');
goog.require('goog.math.Size');
goog.require('goog.style');

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
 * @extends {goog.fx.Dragger}
 * @constructor
 * 
 */
bluemind.fx.Dragger = function(target, opt_handle, opt_limits) {
  goog.base(this, target, opt_handle, opt_limits);
  this.grid = new goog.math.Rect(0, 0, 1, 1);
};
goog.inherits(bluemind.fx.Dragger, goog.fx.Dragger);

/**
 * Rect of the grid to snap to.
 * @protected
 * @type {goog.math.Rect}
 */
bluemind.fx.Dragger.prototype.grid;

/**
 * The position of the target when the first mousedown or touchstart occurred.
 * @protected
 * @type {goog.math.Coordinate}
 */
bluemind.fx.Dragger.prototype.originalPosition;

/**
 * The relative coordinate of the target lastime it moved.
 * @private
 * @type {goog.math.Coordinate}
 */
bluemind.fx.Dragger.prototype.currentDelta_;

/**
 * Limit as setted by the user.
 * @protected
 * @type {goog.math.Rect}
 */
bluemind.fx.Dragger.prototype.limits_;

/**
 * Scroll position.
 * @private
 * @type {goog.math.Coordinate}
 */
bluemind.fx.Dragger.prototype.targetScroll_;

/**
 * Constants for event names.
 * @enum {string}
 */
bluemind.fx.Dragger.EventType = {
  // button before reaching the hysteresis distance.
  BEFORE_START: 'beforestart'
};

/**
 * Sets the distance the user has to drag the element before it move
 * (snap to grid).
 * @param {goog.math.Size} grid Size of the grid to snap to.
 */
bluemind.fx.Dragger.prototype.setGrid = function(grid) {
  this.grid.width = grid.width;
  this.grid.height = grid.height;
};

/** @inheritDoc */
bluemind.fx.Dragger.prototype.startDrag = function(e) {
  this.dispatchEvent(bluemind.fx.Dragger.EventType.BEFORE_START);
  goog.base(this, 'startDrag', e);
  this.currentDelta_ = new goog.math.Coordinate(this.deltaX, this.deltaY);
  this.originalPosition = goog.style.getPosition(this.target);
  var client = goog.style.getClientPosition(this.target);
  this.grid.left = this.deltaX - (this.startX - client.x) % this.grid.width;
  this.grid.top = this.deltaY - (this.startY - client.y) % this.grid.height;
  if (this.scrollTarget_) {
    this.targetScroll_ = new goog.math.Coordinate(this.scrollTarget_.scrollLeft,
      this.scrollTarget_.scrollTop);
  }
  this.limits_ = this.limits.clone();
  this.adjustLimit();
};

/**
 * Reset All dragging data
 * @param {goog.events.BrowserEvent} e Event object.
 */
bluemind.fx.Dragger.prototype.resetDrag = function(e) {
  this.clientX = this.startX = e.clientX;
  this.clientY = this.startY = e.clientY;
  this.screenX = e.screenX;
  this.screenY = e.screenY;
  this.deltaX = this.target.offsetLeft;
  this.deltaY = this.target.offsetTop;
  this.currentDelta_ = new goog.math.Coordinate(this.deltaX, this.deltaY);
  this.originalPosition = goog.style.getPosition(this.target);
  var client = goog.style.getClientPosition(this.target);
  this.grid.left = this.deltaX - (this.startX - client.x) % this.grid.width;
  this.grid.top = this.deltaY - (this.startY - client.y) % this.grid.height;
  if (this.scrollTarget_) {
    this.targetScroll_ = new goog.math.Coordinate(this.scrollTarget_.scrollLeft,
      this.scrollTarget_.scrollTop);
  }
  this.limits = this.limits_.clone();
  this.adjustLimit();
};

/**
 * Make drag limit compatible with the grid
 * @protected
 */
bluemind.fx.Dragger.prototype.adjustLimit = function() {
  if (!isNaN(this.limits.top)) {
    this.limits.top += (this.deltaY - this.limits.top) % this.grid.height;
  }
  if (!isNaN(this.limits.left)) {
    this.limits.left += (this.deltaX - this.limits.left) % this.grid.width;
  }
  if (!isNaN(this.limits.height)) {
    this.limits.height -=
      (this.limits.top + this.limits.height - this.deltaY) % this.grid.height;
  }
  if (!isNaN(this.limits.width)) {
    this.limits.width -=
      (this.limits.left + this.limits.width - this.deltaX) % this.grid.width;
  }
};

/** @inheritDoc */
bluemind.fx.Dragger.prototype.endDrag = function(e, opt_dragCanceled) {
  goog.base(this, 'endDrag', e, opt_dragCanceled);
  this.limits = this.limits_;
};

/**
 * Returns the 'real' x after limits (allows for some limits to be undefined)
 * and snapping are applied.
 * @param {number} x X-coordinate to limit.
 * @return {number} The 'real' X-coordinate after limits are applied.
 */
bluemind.fx.Dragger.prototype.limitX = function(x) {
  x = goog.base(this, 'limitX', x);
  if (x < this.grid.left) {
    x -= this.currentDelta_.x;
    x = Math.floor(x / this.grid.width) * this.grid.width;
    x += this.currentDelta_.x;
  } else if (x > (this.grid.left + this.grid.width)) {
    x -= this.currentDelta_.x;
    x = Math.ceil(x / this.grid.width) * this.grid.width;
    x += this.currentDelta_.x;
  } else {
    x = this.currentDelta_.x;
  }
  return x;
};

/**
 * Returns the 'real' y after limits (allows for some limits to be undefined)
 * and snapping are applied.
 * @param {number} y Y-coordinate to limit.
 * @return {number} The 'real' Y-coordinate after limits are applied.
 */
bluemind.fx.Dragger.prototype.limitY = function(y) {
  y = goog.base(this, 'limitY', y);
  if (y < this.grid.top) {
    y -= this.currentDelta_.y;
    y = Math.floor(y / this.grid.height) * this.grid.height;
    y += this.currentDelta_.y;
  } else if (y > (this.grid.top + this.grid.height)) {
    y -= this.currentDelta_.y;
    y = Math.ceil(y / this.grid.height) * this.grid.height;
    y += this.currentDelta_.y;
  } else {
    y = this.currentDelta_.y;
  }
  return y;
};

/** @inheritDoc */
bluemind.fx.Dragger.prototype.doDrag = function(e, x, y, dragFromScroll) {
  if (dragFromScroll) {
    //FIXME : Access to private method / properties of the parent
    //TODO : This is not optimized this should be done before the first
    // calculatePosition_ call
    var dx = this.scrollTarget_.scrollLeft - this.targetScroll_.x;
    var dy = this.scrollTarget_.scrollTop - this.targetScroll_.y;
    this.targetScroll_.x = this.scrollTarget_.scrollLeft;
    this.targetScroll_.y = this.scrollTarget_.scrollTop;
    var coords = this.calculatePosition_(dx, dy);
    x = coords.x;
    y = coords.y;
  }
  if (this.currentDelta_.y != y || this.currentDelta_.x != x) {
    this.grid.top = y + this.grid.top - this.currentDelta_.y;
    this.currentDelta_.y = y;
    this.grid.left = x + this.grid.left - this.currentDelta_.x;
    this.currentDelta_.x = x;
    goog.base(this, 'doDrag', e, x, y, dragFromScroll);
  }
};
