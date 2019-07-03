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
 * @fileoverview Resize effect for all day event.
 *
 * Resizing effect for all ady event, with multi week effect
 *
 */


goog.provide('bluemind.calendar.fx.AlldayResizer');

goog.require('bluemind.fx.Resizer');
goog.require('goog.dom');
goog.require('goog.math.Size');
goog.require('goog.structs.Map');
goog.require('goog.style');
goog.require('goog.dom.classlist');

/**
 * A class that allows mouseresize of an all day event
 *
 * @param {goog.math.Rect} bounds The element that will be dragged.
 * @param {goog.math.Rect=} opt_limits Object containing left, top, width,
 *     and height.
 * @param {Element=} opt_handle An optional handle to control the drag, if null
 *     the target is used.
 *
 * @extends {bluemind.fx.Resizer}
 * @constructor
 */
bluemind.calendar.fx.AlldayResizer = function(bounds, opt_limits, opt_handle) {
  this.index_ = 0;
  this.container_ = goog.dom.createDom('div');
  goog.dom.appendChild(document.body, this.container_);
  this.children_ = new goog.structs.Map();
  var child = this.createChild_(this.index_, bounds);
  goog.base(this, child, opt_handle, opt_limits);
  this.setGrid(new goog.math.Size(bounds.width, bounds.height));
};
goog.inherits(bluemind.calendar.fx.AlldayResizer, bluemind.fx.Resizer);

/** @inheritDoc */
bluemind.calendar.fx.AlldayResizer.prototype.startDrag = function(e) {
  goog.base(this, 'startDrag', e);
};

/** @inheritDoc */
bluemind.calendar.fx.AlldayResizer.prototype.resizeHeight = function(y) {
  var height = this.originalSize.height + (y - this.originalPosition.y);
  if (height > 0 && height <= this.grid.height) {
    return;
  }
  var index = this.index_;
  var bounds = new goog.math.Rect(
      this.limits.left,
      this.originalPosition.y,
      this.originalSize.width,
      this.originalSize.height);
  var x = this.limits.left;
  if (height <= 0) {
    bounds.top -= this.originalSize.height;
    bounds.left += this.limits.width;
    index--;
  } else {
    x += this.limits.width;
    bounds.top += this.originalSize.height;
    index++;
  }
  if (this.children_.containsKey(index)) {
    var data = this.children_.get(index);
    var div = data.element;
    this.children_.remove(this.index_);
    this.originalSize = data.size;
    this.originalPosition = data.position;
    goog.dom.removeNode(this.target);
  } else {
    this.resizeWidth(this.limitX(x));
    var div = this.createChild_(index, bounds);
    this.originalSize = goog.style.getSize(div);
    this.originalPosition = goog.style.getPosition(div);
  }
  this.index_ = index;
  this.target = div;
};


/**
 * @param {number} index Index where the child should be inserted
 *     and height.
 * @param {goog.math.Rect} bounds Bounds of the child.
 * @return {Element} element.
 * @private
 */
bluemind.calendar.fx.AlldayResizer.prototype.createChild_ =
  function(index, bounds) {
  var element = goog.dom.createDom('div');
  goog.dom.classlist.add(element, goog.getCssName('bm-cal-fx-alr'));
  element.style.top = bounds.top + 'px';
  element.style.left = bounds.left + 'px';
  element.style.width = bounds.width + 'px';
  element.style.height = bounds.height + 'px';
  goog.style.setOpacity(element, 0.5);
  goog.dom.appendChild(this.container_, element);
  this.children_.set(index, {
    element: element,
    size: goog.style.getSize(element),
    position: goog.style.getPosition(element)
  });
  return element;
};

/**
 * @return {Element} container.
 */
bluemind.calendar.fx.AlldayResizer.prototype.getDraw = function() {
  return this.container_;
};


/** @inheritDoc */
bluemind.calendar.fx.AlldayResizer.prototype.disposeInternal = function() {
  var children = goog.dom.getChildren(this.container_);
  for (var i = 0; i < children.length; i++) {
    goog.dom.removeNode(children[i]);
  }
  goog.dom.removeNode(this.container_);
};
