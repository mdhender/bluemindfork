goog.provide("net.bluemind.calendar.month.ui.Event");

goog.require("goog.dom");
goog.require("goog.style");
goog.require("goog.date.Interval");
goog.require("goog.dom.classlist");
goog.require("goog.events.Event");
goog.require("goog.events.EventType");
goog.require("goog.fx.Dragger.EventType");
goog.require("goog.math.Rect");
goog.require("goog.math.Size");
goog.require("goog.ui.Component");
goog.require("goog.ui.Component.EventType");
goog.require("net.bluemind.calendar.month.ui.EventPart");

/**
 * @constructor
 * @param {Object} model Event object model
 * @param {function(goog.math.Coordinate): goog.date.DateLike} callback
 *          Coordinate callback
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.month.ui.Event = function(model, callback, calendars, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.getDateForCoordinate_ = callback;
  this.setModel(model);
  this.setCalendars(calendars);

};

goog.inherits(net.bluemind.calendar.month.ui.Event, goog.ui.Component);

/**
 * @type {function(goog.math.Coordinate): goog.date.DateLike}
 * @private
 */
net.bluemind.calendar.month.ui.Event.getDateForCoordinate_;

/**
 * Calendars
 * 
 * @type {Array}
 */
net.bluemind.calendar.month.ui.Event.prototype.calendars_;

/** @override */
net.bluemind.calendar.month.ui.Event.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.getElement().style.display = 'none';
};

/** @override */
net.bluemind.calendar.month.ui.Event.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  if (this.getChildCount() > 0) {
    this.forEachChild(this.addHandlers_, this);
  }
  this.getHandler().listen(this, goog.events.EventType.MOUSEDOWN, goog.events.Event.stopPropagation);
  this.getHandler().listen(this, goog.events.EventType.CLICK, goog.events.Event.stopPropagation);
};

/**
 * Set current view calendars
 * 
 * @param {Array} calendars
 */
net.bluemind.calendar.month.ui.Event.prototype.setCalendars = function(calendars) {
  this.calendars_ = calendars;
};

/**
 * Add event part
 * 
 * @param {Element} el
 * @private
 */
net.bluemind.calendar.month.ui.Event.prototype.addPart = function(container) {
  var part = new net.bluemind.calendar.month.ui.EventPart(this.getModel(), this.getDateForCoordinate_, this.calendars_,
      this.getDomHelper());
  this.addChild(part);
  part.render(container);
};

/**
 * Add drag handler on event component
 * 
 * @param {bluemind.fx.Dragger} fx
 * @param {Element} el
 * @private
 */
net.bluemind.calendar.month.ui.Event.prototype.addHandlers_ = function(child) {
  var h = this.getHandler();
  h.listen(child, goog.fx.Dragger.EventType.START, this.handleDragStart_);
};

/**
 * On drag
 * 
 * @param {goog.events.Event} e Drag event
 * @private
 */
net.bluemind.calendar.month.ui.Event.prototype.handleDragStart_ = function(e) {
  var child = e.target;
  while (this.getChildCount() > 1) {
    if (this.getChildAt(0) != child) {
      this.removeChildAt(0, true);
    } else {
      this.removeChildAt(1, true);
    }
  }

};

/**
 * @param {goog.fx.DragEvent} e The event.
 * @private
 */
net.bluemind.calendar.month.ui.Event.prototype.handleDragStop_ = function(e) {
  goog.dom.classlist.remove(this.getElement(), goog.getCssName('event-dragged'));
  this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
};

/**
 * @param {goog.fx.DragEvent} e The event.
 * @private
 */
net.bluemind.calendar.month.ui.Event.prototype.handleDragging_ = function(e) {
  var position = goog.style.getPageOffset(this.getElement());
  var size = goog.style.getSize(this.getElement());
  var start = this.getDateForCoordinate_(position);
  var interval, model = this.getModel();
  if (goog.date.Date.compare(start, model.dtstart) < 0) {
    interval = new goog.date.Interval(0, 0, -1);
  } else {
    interval = new goog.date.Interval(0, 0, 1)
  }
  while (!goog.date.isSameDay(start, model.start)) {
    model.dtstart.add(interval);
    model.dtend.add(interval);
  }
};

/**
 * Set limit and grid for resizing fx
 * 
 * @private
 */
net.bluemind.calendar.month.ui.Event.prototype.setDragLimit_ = function() {
  // TODO : Should be determined by the view
  // getContainerBounds for exemple
  var el = this.getElement();
  var container = goog.dom.getAncestorByClass(el, goog.getCssName('month-grid'));
  var row = goog.dom.getAncestorByClass(el, goog.getCssName('month-row'));
  var height = goog.style.getSize(row).height;
  var width = goog.style.getSize(goog.dom.getElementByClass(goog.getCssName('mg-daynum'))).width + 1;
  var box = goog.style.getSize(el);
  var size = goog.style.getSize(container);
  var position = goog.style.getRelativePosition(container, el);
  this.drag_.setGrid(new goog.math.Size(width, height));
  this.drag_.setLimits(new goog.math.Rect(((position.x + el.offsetLeft) - 6), (position.y + el.offsetTop),
      ((size.width - width) + 6), (size.height - box.height)));

};

/**
 * @param {goog.fx.DragEvent} e The event.
 * @private
 */
net.bluemind.calendar.month.ui.Event.prototype.handleCancel_ = function(e) {
  this.dispatchEvent(goog.events.EventType.MOUSEDOWN);
};
