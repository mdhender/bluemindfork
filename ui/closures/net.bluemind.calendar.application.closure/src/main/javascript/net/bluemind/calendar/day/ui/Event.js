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
 * @fileoverview Event object in the day view.
 */

goog.provide("net.bluemind.calendar.day.ui.Event");

goog.require("goog.array");
goog.require("goog.dom");
goog.require("goog.style");
goog.require("goog.date.DateTime");
goog.require("goog.dom.classlist");
goog.require("goog.events.Event");
goog.require("goog.events.EventType");
goog.require("goog.fx.Dragger.EventType");
goog.require("goog.math.Rect");
goog.require("goog.math.Size");
goog.require("goog.ui.Component");
goog.require("goog.ui.Component.EventType");
goog.require("net.bluemind.calendar.day.templates");
goog.require("bluemind.fx.Dragger");
goog.require("bluemind.fx.HeightResizer");
goog.require("bluemind.fx.Dragger.EventType");
/**
 * View class for Calendar days view.
 * 
 * @param {Object} model Event object model
 * @param {function(goog.math.Coordinate): goog.date.DateLike} callback
 * Coordinate callback
 * @param {goog.i18n.DateTimeFormat} format Time Format
 * @param {Array} calendars
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.day.ui.Event = function(model, callback, format, calendars, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.getDateForCoordinate_ = callback;
  this.setModel(model);
  this.setCalendars(calendars);
  this.format = format;
};

goog.inherits(net.bluemind.calendar.day.ui.Event, goog.ui.Component);

/**
 * Calendars
 * 
 * @type {Array}
 */
net.bluemind.calendar.day.ui.Event.prototype.calendars_;

/**
 * time formatter
 * 
 * @type {goog.i18n.DateTimeFormat}
 */
net.bluemind.calendar.day.ui.Event.prototype.format;

/**
 * @type {bluemind.fx.HeightResizer}
 * @private
 */
net.bluemind.calendar.day.ui.Event.prototype.resize_;

/**
 * @type {function(goog.math.Coordinate): goog.date.DateLike}
 * @private
 */
net.bluemind.calendar.day.ui.Event.getDateForCoordinate_;

/** @override */
net.bluemind.calendar.day.ui.Event.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.changed_ = false;

  this.refreshContainer();
  var event = this.getModel();
  var el = this.getElement();

  goog.soy.renderElement(el, net.bluemind.calendar.day.templates.event, event)
  
  if (event.states.updatable) {
    var handle = this.getElementByClass(goog.getCssName('ev-resizer'));
    this.resize_ = new bluemind.fx.HeightResizer(el, handle);
    this.resize_.setHysteresis(2);
    this.resize_.setGrid(new goog.math.Size(1, 21));

    this.registerDisposable(this.resize_);

    this.drag_ = new bluemind.fx.Dragger(el);
    this.drag_.setHysteresis(2);

    // FIXME : ?????
    // goog.object.set(this.resize_, 'scroll', null);
    // goog.object.set(this.drag_, 'scroll', null);
  }
};

/**
 * 
 */
/** @override */
net.bluemind.calendar.day.ui.Event.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.refreshContent();
  if (this.resize_) {
    var el = this.getElementByClass(goog.getCssName('ev-resizer'));
    this.addHandlers_(this.resize_, el);
    var top = parseInt(this.getElement().style.top, 10);
    this.resize_.setLimits(new goog.math.Rect(0, top, 0, 1008 - top));
  }
  if (this.drag_) {
    this.getHandler().listen(this.drag_, bluemind.fx.Dragger.EventType.BEFORE_START, this.setDragLimit_);
    this.addHandlers_(this.drag_, this.getElement());
  } else {
    this.getHandler().listen(this.getElement(), goog.events.EventType.CLICK, function(e) {
      e.stopPropagation();
      this.dispatchEvent(e.type);
    });
    this.getHandler().listen(this.getElement(), goog.events.EventType.MOUSEDOWN, function(e) {
      e.stopPropagation();
      this.dispatchEvent(e.type);
    });
  }

};

/**
 * Add resize handler on event component
 * 
 * @param {bluemind.fx.Dragger} fx
 * @param {Element} el
 * @private
 */
net.bluemind.calendar.day.ui.Event.prototype.addHandlers_ = function(fx, el) {
  var h = this.getHandler();

  h.listen(el, goog.events.EventType.MOUSEDOWN, goog.events.Event.stopPropagation);
  h.listen(fx, bluemind.fx.Dragger.EventType.BEFORE_START, this.dispatchEvent);
  h.listen(fx, goog.fx.Dragger.EventType.START, this.handleDragStart_);
  h.listen(fx, goog.fx.Dragger.EventType.DRAG, this.handleDragging_);
  h.listen(fx, goog.fx.Dragger.EventType.END, this.handleDragStop_);
  h.listen(fx, goog.fx.Dragger.EventType.EARLY_CANCEL, this.handleCancel_);

  // FIXME : ....
  fx.setScrollTarget(this.getParent().getGridElement());
};

/**
 * On drag
 * 
 * @param {goog.events.Event} e Drag event
 * @private
 */
net.bluemind.calendar.day.ui.Event.prototype.handleDragStart_ = function(e) {
  this.initial_ = {
      size: goog.style.getSize(this.getElement()),
      position : goog.style.getPageOffset(this.getElement())
  }
  goog.dom.classlist.add(this.getElement(), goog.getCssName('event-dragged'));
  // FIXME: How to put in css
  this.getElement().style.width = 'calc(100% + 1em)';
  this.getElement().style.left = '0';
  e.dragger.resetDrag(e);
  this.dispatchEvent(e);
};

/**
 * Set current view calendars
 * 
 * @param {Array} calendars
 */
net.bluemind.calendar.day.ui.Event.prototype.setCalendars = function(calendars) {
  this.calendars_ = calendars;
};

/**
 * @param {goog.fx.DragEvent} e The event.
 * @private
 */
net.bluemind.calendar.day.ui.Event.prototype.handleDragStop_ = function(e) {
  goog.dom.classlist.remove(this.getElement(), goog.getCssName('event-dragged'));
    this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
    this.changed_ = false;
};

/**
 * @param {goog.fx.DragEvent} e The event.
 * @private
 */
net.bluemind.calendar.day.ui.Event.prototype.handleDragging_ = function(e) {
  var position = goog.style.getPageOffset(this.getElement());
  var size = goog.style.getSize(this.getElement());
  if (this.dragged_(position)) {
   this.reschedule_(position);
  } 
  if (this.resized_(size)) {
    this.redurabilize_(position, size);
  }
  
  if (this.changed_) {
    this.initial_ = {
        size: size,
        position : position
    }
    this.refreshContent();
  }

};


net.bluemind.calendar.day.ui.Event.prototype.dragged_ = function(position) {
  return  position.y != this.initial_.position.y || position.x != this.initial_.position.x;
};

net.bluemind.calendar.day.ui.Event.prototype.reschedule_ = function(position) {
  var start = this.getDateForCoordinate_(position);
  var duration = (start.getTime() - this.getModel().dtstart.getTime()) / 1000;
  if (!(start instanceof goog.date.DateTime)) {
    this.getModel().dtstart.set(start);
    duration = Math.round(duration / 86400);
    this.getModel().dtend.add(new goog.date.Interval(goog.date.Interval.DAYS, duration));
  } else {
    this.getModel().dtstart = start;
    this.getModel().dtend.add(new goog.date.Interval(goog.date.Interval.SECONDS, duration));
  }
  this.changed_ = true;
};

net.bluemind.calendar.day.ui.Event.prototype.resized_ = function(size) {
  return size.height != this.initial_.size.height;
};

net.bluemind.calendar.day.ui.Event.prototype.redurabilize_ = function(position, size) {
  position.y += size.height + 1;
  var end = this.getDateForCoordinate_(position);
  if (!(end instanceof goog.date.DateTime)) {
    this.getModel().dtend.set(end);
  } else {
    this.getModel().dtend = end;
  }
  this.changed_ = true;
};


/**
 * @param {goog.fx.DragEvent} e The event.
 * @private
 */
net.bluemind.calendar.day.ui.Event.prototype.handleCancel_ = function(e) {
  this.dispatchEvent(goog.events.EventType.MOUSEDOWN);
};

/**
 * Set event classes and tooltip
 */
net.bluemind.calendar.day.ui.Event.prototype.refreshContainer = function() {
  var event = this.getModel();
  var classlist = [];
  classlist.push(goog.getCssName('event'));

  classlist.push(goog.getCssName('inDayEvent'));

  if (event.states.pending) {
    classlist.push(goog.getCssName('pending'));
  }

  if (event.states.tentative) {
    classlist.push(goog.getCssName('tentative'));
  }

  if (event.states.declined) {
    classlist.push(goog.getCssName('declined'));
  }
  if (event.states.private_) {
    classlist.push(goog.getCssName('private'));
  }
  if (event.states.updatable) {
    classlist.push(goog.getCssName('updatable'));
  }
  if (event.states.short) {
    classlist.push(goog.getCssName('short'));
  }
  if (event.states.meeting) {
    classlist.push(goog.getCssName('meeting'));
  }

  if (!event.states.synced) {
    classlist.push(goog.getCssName('not-synchronized'));
  } else {
    if (event.states.past) {
      classlist.push(goog.getCssName('past'));
    }
  }

  classlist.push(goog.getCssName('calendar'));

  var el = this.getElement();
  goog.dom.classlist.addAll(el, classlist);

  var calendar = goog.array.find(this.calendars_, function(calendar) {
    return calendar.uid == event.calendar;
  });

  el.style.backgroundColor = calendar.color.background;
  el.style.color = calendar.color.foreground;

  el.title = event.summary;
};

/**
 * Set event headers and contents
 */
net.bluemind.calendar.day.ui.Event.prototype.refreshContent = function() {
  var start = this.getModel().dtstart;
  var end = this.getModel().dtend;
  var summary = this.getModel().summary;
  var location = this.getModel().location
  var dom = this.getDomHelper(), content = this.getContentElement_();
  var header = content.firstChild, description = content.lastChild;
  var locationEl;
  dom.removeChildren(header);
  dom.removeChildren(description);
  if (location) {
    locationEl = dom.createDom('span', goog.getCssName('ev-location'), ", " + location);
  }
  // FIX use model instead of calculating available space ( getSize and
  // getFontSize are time consuming )
  // var min = goog.style.getFontSize(this.getElement()) * 2;
  // var cur = goog.style.getSize(this.getElement()).height;
  if (!this.getModel().states.short) {
    dom.setTextContent(header, this.format.format(start) + ' - ' + this.format.format(end));
    dom.appendChild(description, dom.createDom('span', goog.getCssName('ev-content'), summary));
    if (locationEl) {
      dom.appendChild(description, locationEl);
    }
  } else {
    var el = dom.createDom('span', goog.getCssName('ev-header'));
    dom.setTextContent(header, this.format.format(start) + ' - ' + summary);
    if (locationEl) {
      dom.appendChild(header, locationEl);
    }
  }
};

/**
 * Get event content
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.day.ui.Event.prototype.getContentElement_ = function() {
  return this.getElement().lastChild;
};

/**
 * @param {goog.events.Event} e Mouse event
 */
net.bluemind.calendar.day.ui.Event.prototype.forceResize = function(e) {
  this.resize_.startDrag(e);
  this.getHandler().listen(this.resize_, goog.fx.Dragger.EventType.EARLY_CANCEL, this.handleDragStop_);
  goog.dom.classlist.add(this.getElement(), goog.getCssName('event-dragged'));
  this.getElement().style.width = 'calc(100% + 1em)';
};

/**
 * Get event content
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.day.ui.Event.prototype.setDragLimit_ = function() {
  var el = this.getElement();
  var box = goog.style.getSize(el);
  var container = goog.dom.getElementByClass(goog.getCssName('dayContainer'), goog.dom.getElement('bodyContainer'));

  var size = goog.style.getSize(container);

  this.drag_.setGrid(new goog.math.Size(size.width, 21));

  var margin = goog.style.getSize(goog.dom.getElement('leftPanelHour')).width;
  var position = goog.style.getRelativePosition(container, el);
  var width = goog.style.getSize(goog.dom.getElement('bodyContent')).width - margin - size.width;

  this.drag_.setLimits(new goog.math.Rect(position.x + el.offsetLeft - 6, 0, width + 6, 1008 - box.height));
};
