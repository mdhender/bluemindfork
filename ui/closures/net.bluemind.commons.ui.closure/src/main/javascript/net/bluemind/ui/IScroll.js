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
 * @fileoverview Widget for infinite scrolling list.
 */

goog.provide("net.bluemind.ui.IScroll");

goog.require("goog.Promise");
goog.require("goog.iter");
goog.require("goog.style");
goog.require("goog.async.AnimationDelay");
goog.require("goog.events.Event");
goog.require("goog.events.EventType");
goog.require("goog.events.MouseWheelHandler");
goog.require("goog.events.MouseWheelHandler.EventType");
goog.require("goog.iter.StopIteration");
goog.require("goog.math.Range");
goog.require("goog.ui.Container");
goog.require("goog.ui.Control");
goog.require("goog.ui.ControlRenderer");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.Component.State");
goog.require("net.bluemind.ui.IScrollRenderer");

/**
 * Infinite scroll list.
 * 
 * @param {goog.ui.ControlRenderer=} opt_childRenderer Child constructor.
 *                Default is goog.ui.Control.
 * @param {?goog.ui.Container.Orientation=} opt_orientation Container
 *                orientation; defaults to {@code VERTICAL}.
 * @param {goog.ui.ContainerRenderer=} opt_renderer Renderer used to render or
 *                decorate the container; defaults to
 *                {@link goog.ui.ContainerRenderer}.
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for document
 *                interaction.
 * @extends {goog.ui.Container}
 * @constructor
 */
net.bluemind.ui.IScroll = function(opt_childRenderer, opt_orientation, opt_renderer, opt_domHelper) {
  opt_renderer = opt_renderer || net.bluemind.ui.IScrollRenderer.getInstance();
  goog.base(this, opt_orientation, opt_renderer, opt_domHelper);
  this.childRenderer_ = opt_childRenderer
      || goog.ui.ControlRenderer.getCustomRenderer(goog.ui.ControlRenderer, 'iscroll-item');
  // Create at least on child to have getChildSize working
  this.createItem_();
  this.offset_ = 0;
  this.range_ = new goog.math.Range(0, Infinity);
  this.delay_ = new goog.async.AnimationDelay(this.update_, this.getDomHelper().getWindow(), this);
  this.setModel({});
};
goog.inherits(net.bluemind.ui.IScroll, goog.ui.Container);

/**
 * @type {number}
 * @private
 */
net.bluemind.ui.IScroll.prototype.childSize_;

/**
 * @type {goog.async.AnimationDelay}
 * @private
 */
net.bluemind.ui.IScroll.prototype.delay_;

/**
 * Request asynchronously for data
 * 
 * @type {function(number, number): goog.Thenable}
 * @public
 */
net.bluemind.ui.IScroll.prototype.dataRequest;

/**
 * Request element position
 * 
 * @type {function(Object): goog.Thenable}
 * @public
 */
net.bluemind.ui.IScroll.prototype.positionRequest;

/**
 * Fill control with model data.
 * 
 * @type {function(goog.ui.Control, *, *, number)}
 * @public
 */
net.bluemind.ui.IScroll.prototype.fill;

/**
 * Child render.
 * 
 * @type {goog.ui.ControlRenderer}
 * @public
 */
net.bluemind.ui.IScroll.prototype.childRenderer_;

/**
 * Selection model. Can handle multiple or unique selection. Store selected
 * model uid.
 * 
 * @type {net.bluemind.ui.SelectionModel}
 * @public
 */
net.bluemind.ui.IScroll.prototype.selectionModel_;

/**
 * @type {goog.events.MouseWheelHandler}
 * @private
 */
net.bluemind.ui.IScroll.prototype.mwh_;

/**
 * @type {number}
 * @private
 */
net.bluemind.ui.IScroll.prototype.offset_;

/**
 * @type {number}
 * @private
 */
net.bluemind.ui.IScroll.prototype.limit_;

/**
 * @type {number}
 * @private
 */
net.bluemind.ui.IScroll.prototype.size_;

/**
 * Start is included, end is excluded. So [0, 230] contains exactly 230
 * elements.
 * 
 * @type {goog.math.Range}
 */
net.bluemind.ui.IScroll.prototype.range_;

/**
 * Create a new item
 * 
 * @private
 */
net.bluemind.ui.IScroll.prototype.createItem_ = function() {
  var child = new goog.ui.Control(null, this.childRenderer_);
  child.setVisible(false);
  child.setSupportedState(goog.ui.Component.State.SELECTED, true);
  this.addChild(child, true);
  return child;
}

/**
 * Set a new selection model and sets up an event listener to handle
 * {@link goog.events.EventType.SELECT} events dispatched by it.
 * 
 * @param {net.bluemind.ui.SelectionModel} selectionModel Selection model
 */
net.bluemind.ui.IScroll.prototype.setSelectionModel = function(selectionModel) {
  if (this.selectionModel_) {
    this.getHandler().unlisten(this.selectionModel_, goog.events.EventType.SELECT, this.handleSelectionChange_);
    this.selectionModel_ = null;
    this.renderModel_();
  }
  this.selectionModel_ = selectionModel;
  if (this.selectionModel_) {
    this.getHandler().listen(this.selectionModel_, goog.events.EventType.SELECT, this.handleSelectionChange_);
  }
};

/**
 * Set data range
 * 
 * @param {number} start Data first position
 * @param {number} end Data last position
 */
net.bluemind.ui.IScroll.prototype.setRange = function(start, end) {
  var r = new goog.math.Range(start, end);
  if (!goog.math.Range.equals(this.range_, r)) {
    this.range_ = r;
    this.refreshSizes_();
  }
};

/**
 * @private
 */
net.bluemind.ui.IScroll.prototype.refreshSizes_ = function() {
  if (!this.getElement()) {
    this.createDom();
  }
  var el = this.getElement();
  var content = this.getRenderer().getContentElement(el);
  var size = 0, sw = 0;
  if (this.range_.getLength() != Infinity) {
    // - 2 : -1 because limit is ceiled, -1 to display at least 1 element.
    var size = this.range_.getLength() + this.limit_ - 2;
  }
  this.getRenderer().setScrollSize(this, size);
  if (size > this.limit_) {
    sw = goog.style.getScrollbarWidth() || 16;
  }
  content.style.width = (goog.style.getSize(el).width - sw) + 'px';
};

/**
 * @return {net.bluemind.ui.SelectionModel} Current selection model
 */
net.bluemind.ui.IScroll.prototype.getSelectionModel = function() {
  return this.selectionModel_;
};

/** @override */
net.bluemind.ui.IScroll.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(this, goog.ui.Component.EventType.ACTION, this.handleItemAction_);

  if (!this.mwh_) {
    this.mwh_ = new goog.events.MouseWheelHandler(this.getElement());
    this.registerDisposable(this.mwh_);
  }
  this.getHandler().listen(this.mwh_, goog.events.MouseWheelHandler.EventType.MOUSEWHEEL, this.handleMW_);

  var scroll = this.getRenderer().getScrollElement(this.getElement());
  this.getHandler().listen(scroll, goog.events.EventType.SCROLL, this.handleScroll_);

  this.refresh();
  this.renderModel_();
};

/** @override */
net.bluemind.ui.IScroll.prototype.highlightFirst = function() {
  if (this.range_.start > -Infinity) {
    var start = this.range_.start - 1;
    this.highlightHelper(function(index, range) {
      if (++index >= range.end) {
        index = range.end - 1;
      }
      return index;
    }, start);
  }
};

/** @override */
net.bluemind.ui.IScroll.prototype.highlightLast = function() {
  if (this.range_.end < Infinity) {
    var start = this.range_.end;
    this.highlightHelper(function(index, range) {
      if (--index < range.start) {
        index = range.start;
      }
      return index;
    }, start);
  }
};

/** @override */
net.bluemind.ui.IScroll.prototype.highlightNext = function() {
  var start = this.selectedIndex_;
  try {
    this.highlightHelper(function(index, range) {
      if (++index >= range.end) {
        throw goog.iter.StopIteration;
      }
      return index;
    }, start);
  } catch (ex) {
    if (ex !== goog.iter.StopIteration) {
      throw ex;
    }
  }
};

/** @override */
net.bluemind.ui.IScroll.prototype.highlightPrevious = function() {
  var start = this.selectedIndex_;
  try {
    this.highlightHelper(function(index, range) {
      if (--index < range.start) {
        throw goog.iter.StopIteration;
      }
      return index;
    }, start);
  } catch (ex) {
    if (ex !== goog.iter.StopIteration) {
      throw ex;
    }
  }
};

/** @override */
net.bluemind.ui.IScroll.prototype.highlightHelper = function(fn, startIndex) {
  var position = fn.call(this, startIndex, this.range_);
  var visited = 0;
  while (visited <= this.limit_) {
    var index = this.getChildIndex_(position);
    if (index !== false) {
      var control = this.getChildAt(index);
      if (control && this.canHighlightItem(control)) {
        this.setHighlightedIndexFromKeyEvent(control);
        return true;
      }
      startIndex = position;
      position = fn.call(this, position, this.range_);
    } else {
      // FIXME : If goTo does not display position (bug ?) it will
      // result in an infinite loop.
      this.goTo(position).then(function() {
        this.highlightHelper(fn, startIndex);
      }, null, this);
      return false;
    }
    visited++;
  }
  return false;
};

/** @override */
net.bluemind.ui.IScroll.prototype.setHighlightedIndexFromKeyEvent = function(control) {
  var actionEvent = new goog.events.Event(goog.ui.Component.EventType.ACTION);
  control.dispatchEvent(actionEvent);
};

/**
 * Handles {@link goog.events.EventType.SELECT} events raised by the selection
 * model when the selection changes list if necessary.
 * 
 * @param {goog.events.Event} e Selection event to handle.
 * @private
 */
net.bluemind.ui.IScroll.prototype.handleSelectionChange_ = function(e) {
  this.renderModel_();
};

/**
 * @param {goog.events.MouseWheelEvent} e
 * @private
 */
net.bluemind.ui.IScroll.prototype.handleScroll_ = function(e) {
  var scroll = this.getRenderer().getScrollElement(this.getElement());
  var position = Math.ceil(scroll.scrollTop / this.getChildSize());
  this.offset_ = Math.max(Math.min(position, this.range_.end), this.range_.start);
  var limit = Math.min(this.limit_, (this.range_.end - this.offset_));
  this.load_(this.offset_, limit);
};

/**
 * Handles {@link goog.ui.Component.EventType.ACTION} events dispatched by the
 * item clicked by the user. Updates the selection model.
 * 
 * @param {goog.events.Event} e Action event to handle.
 * @private
 */
net.bluemind.ui.IScroll.prototype.handleItemAction_ = function(e) {
  var control = (/** @type {goog.ui.Control} */
  (e.target));
  var o = control.getModel();
  this.selectionModel_.setSelected(o, true);
};

/**
 * @param {goog.events.MouseWheelEvent} e
 * @private
 */
net.bluemind.ui.IScroll.prototype.handleMW_ = function(e) {
  e.preventDefault();
  this.adjustWheel_(e);
  if (e.deltaY < 0) {
    var rows = Math.floor(e.deltaY / 6);
  } else {
    var rows = Math.ceil(e.deltaY / 6);
  }
  this.goTo(this.offset_ + rows);
};

/**
 * @param {goog.events.MouseWheelEvent} e
 * @private
 */
net.bluemind.ui.IScroll.prototype.adjustWheel_ = function(e) {
  if (goog.userAgent.EDGE) {
    var be = e.getBrowserEvent();
    var deltaY = goog.events.MouseWheelHandler.smartScale_(-be.wheelDelta, 40);
    if (goog.isNumber(this.maxDeltaY_)) {
      deltaY = goog.math.clamp(deltaY, -this.maxDeltaY_, this.maxDeltaY_);
    }
    e.deltaY = deltaY;
  }
}

/**
 * Go to a given offset.
 * 
 * @param {number} position Postion to go.
 * @return {goog.Promise} Goto asynchronous exec
 * @public
 */
net.bluemind.ui.IScroll.prototype.goTo = function(position) {
  this.setOffset_(position);
  var limit = Math.min(this.limit_, (this.range_.end - this.offset_));
  return this.load_(this.offset_, limit);
};

/**
 * Go to a given offset.
 * 
 * @param {number} position Postion to go.
 * @public
 */
net.bluemind.ui.IScroll.prototype.setOffset_ = function(position) {
  position = Math.max(Math.min(position, (this.range_.end - 1)), this.range_.start);
  this.offset_ = position;
  var scroll = this.getRenderer().getScrollElement(this.getElement());
  var h = this.getHandler();
  h.unlisten(scroll, goog.events.EventType.SCROLL, this.handleScroll_);
  h.listenOnce(scroll, goog.events.EventType.SCROLL, function(e) {
    e.stopPropagation();
    h.listen(scroll, goog.events.EventType.SCROLL, this.handleScroll_);
  });
  scroll.scrollTop = this.offset_ * this.getChildSize();
};

/**
 * Is position currently in viewport.
 * 
 * @param {number} position Postion to match.
 * @return {boolean} true if in viewport
 * @public
 */
net.bluemind.ui.IScroll.prototype.isDisplayed = function(position) {
  var index = this.getChildIndex_(position);
  if (index !== false) {
    return this.getChildAt(index).isVisible();
  }
  return false;
};

/**
 * Get control index from a list position.
 * 
 * @param {number} position Postion to search.
 * @return {number | boolean} child index
 * @public
 */
net.bluemind.ui.IScroll.prototype.getChildIndex_ = function(position) {
  if (position >= this.offset_ && position < (this.offset_ + this.limit_)) {
    position -= this.offset_;
    for (var i = 0, index = 0; i < (this.limit_ - 1); i++) {
      var control = this.getChildAt(i);
      if (control.isEnabled()) {
        if (index == position) {
          return i;
        } else {
          index++;
        }
      }
    }
  }
  return false;
};

/**
 * Force a refresh of the displayed data.
 * 
 * @public
 */
net.bluemind.ui.IScroll.prototype.refresh = function() {
  this.limit_ = Math.ceil(goog.style.getSize(this.getElement()).height / this.getChildSize());
  this.setOffset_(this.offset_);
  this.refreshSizes_();
  var limit = Math.min(this.limit_, (this.range_.end - this.offset_));
  this.load_(this.offset_, limit, true);
};

/**
 * Clear iscroll data.
 * 
 * @public
 */
net.bluemind.ui.IScroll.prototype.clear = function() {
  this.selectionModel_.clear();
  this.setModel({});
  this.goTo(0);
};

/**
 * Get widget size / offset FIXME: The offset is get via scrollTop, it should
 * not.
 */

/**
 * @param {number} offset
 * @param {number} limit
 * @param {boolean=} opt_clear Clear cache.
 * @return {goog.Promise} loading async execution
 * @private
 */
net.bluemind.ui.IScroll.prototype.load_ = function(offset, limit, opt_clear) {
  var end = (offset + limit);
  if (!opt_clear) {
    var o = end, l = 0;
    var model = this.getModel() || {};
    for (var i = offset; i < end; i++) {
      if (!goog.isDefAndNotNull(model[i])) {
        o = i;
        break;
      }
    }
    for (i = end; i > o; i--) {
      if (!goog.isDefAndNotNull(model[(i - 1)])) {
        l = i - o;
        break;
      }
    }
    offset = o;
    limit = l;
  }

  var promise = null;
  if (limit > 0 && offset < end) {
    if (this.dataRequest) {
      promise = this.dataRequest(offset, limit).then(function(values) {
        this.addToModel_(values, offset);
        this.renderModel_();
      }, null, this);
    }
  } else {
    promise = goog.Promise.resolve();
  }
  this.renderModel_();
  if (!!opt_clear) {
    this.setModel({});
  }
  return promise;
};

/**
 * @param {Array.<*>} values Model data.
 * @param {number} offset Offset of the values.
 * @private
 */
net.bluemind.ui.IScroll.prototype.addToModel_ = function(values, offset) {
  var model = this.getModel() || {};
  for (var i = 0; i < values.length; i++) {
    if (values[i]) {
      model[offset + i] = values[i];
    } else {
      model[offset + i] = null;
    }
  }
  this.setModel(model);
};

/**
 * Refresh scroll items
 * 
 * @param {number} ts
 * @private
 */
net.bluemind.ui.IScroll.prototype.update_ = function(ts) {
  this.renderModel_();
};

/**
 * Refresh scroll items
 * 
 * @param {number} ts
 * @private
 */
net.bluemind.ui.IScroll.prototype.renderModel_ = function() {
  if (this.fill && this.isInDocument()) {
    var model = this.getModel(), previous, index = 0;
    for (var i = 0; i < this.limit_; i++) {
      var position = this.offset_ + index;
      if (position == this.range_.end) {
        break;
      }
      var data = model[position];
      var child = this.getChildAt(i);
      if (!child) {
        child = this.createItem_();
      }
      child.setModel(data);
      if (this.fill(child, model, previous, position)) {
        index++;
      }
      if (child.isEnabled() && child.isSupportedState(goog.ui.Component.State.SELECTED)) {
        var selected = this.selectionModel_.isSelected(data);
        child.setSelected(selected);
        // FIXME : Select index is not accurate and is unrespectfull of
        // selectModel.
        if (selected)
          this.selectedIndex_ = position;
      }
      previous = position;
    }
    while (i < this.getChildCount()) {
      var child = this.getChildAt(i++);
      if (child) {
        child.setVisible(false);
        child.setModel(null);
      }
    }
  }
};

/**
 * Child size
 * 
 * @return {number} Child height
 */
net.bluemind.ui.IScroll.prototype.getChildSize = function() {
  // FIXME or TODO : can be cached ? Or is it costless (de porc) ?
  if (!this.childSize_) {
    this.childSize_ = goog.style.getSize(this.getChildAt(0).getElement()).height;
  }
  return this.childSize_;
};

/**
 * Get an element position in iscroll list.
 * 
 * @param o
 * @return {goog.Promise} Position asyn execution promise.
 */
net.bluemind.ui.IScroll.prototype.getPosition = function(o) {
  var model = this.getModel();
  // FIXME: keyprovider of the selection model should not be used this way...
  var key = this.selectionModel_.keyProvider(o);
  for ( var pos in model) {
    if (key == this.selectionModel_.keyProvider(model[pos])) {
      return goog.Promise.resolve(pos);
    }
  }
  if (this.positionRequest) {
    return this.positionRequest(o);
  }
  return goog.Promise.reject();
};
