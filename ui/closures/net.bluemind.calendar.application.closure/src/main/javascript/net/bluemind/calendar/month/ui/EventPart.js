goog.provide("net.bluemind.calendar.month.ui.EventPart");

goog.require("goog.array");
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
goog.require("net.bluemind.calendar.month.templates");
goog.require("bluemind.fx.HorizontalDragger");
goog.require("bluemind.fx.Dragger");
goog.require("bluemind.fx.Dragger.EventType");

/**
 * @constructor
 * @param {Object} model Event object model
 * @param {function(goog.math.Coordinate): goog.date.DateLike} callback
 * Coordinate callback
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.month.ui.EventPart = function(model, callback, calendars, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.getDateForCoordinate_ = callback;
  this.setModel(model);
  this.calendars_ = calendars;
};

goog.inherits(net.bluemind.calendar.month.ui.EventPart, goog.ui.Component);

/**
 * @type {function(goog.math.Coordinate): goog.date.DateLike}
 * @private
 */
net.bluemind.calendar.month.ui.EventPart.getDateForCoordinate_;

/**
 * Calendars
 * 
 * @type {Array}
 */
net.bluemind.calendar.month.ui.EventPart.prototype.calendars_;

/** @override */
net.bluemind.calendar.month.ui.EventPart.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var event = this.getModel();
  var classlist = [];
  classlist.push(goog.getCssName('event'));
  classlist.push(goog.getCssName('allDayEvent'));

  if (event.states.pending) {
    classlist.push(goog.getCssName('pending'));
  }
  if (event.states.private_) {
    classlist.push(goog.getCssName('private'));
  }
  if (event.states.declined) {
    classlist.push(goog.getCssName('declined'));
  }
  if (event.states.updatable) {
    classlist.push(goog.getCssName('updatable'));
  }
  if (event.states.short) {
    classlist.push(goog.getCssName('inday'));
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
  if (event.states.short) {
    el.style.color = calendar.color.single;
  } else {
    el.style.backgroundColor = calendar.color.background;
    el.style.color = calendar.color.foreground;
  }
  el.title = event.tooltip;
  el.id = event.id;
  el.innerHTML = net.bluemind.calendar.month.templates.eventpart(event);

  if (event.states.updatable) {
    this.drag_ = new bluemind.fx.Dragger(el);

    this.drag_.setHysteresis(2);

    // FIXME : ?????
    // goog.object.set(this.resize_, 'scroll', null);
    // goog.object.set(this.drag_, 'scroll', null);
  }
};

/** @override */
net.bluemind.calendar.month.ui.EventPart.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  if (this.drag_) {
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
 * Add drag handler on event component
 * 
 * @param {bluemind.fx.Dragger} fx
 * @param {Element} el
 * @private
 */
net.bluemind.calendar.month.ui.EventPart.prototype.addHandlers_ = function(fx, el) {
  var h = this.getHandler();

  h.listen(el, goog.events.EventType.MOUSEDOWN, goog.events.Event.stopPropagation);
  h.listen(el, goog.events.EventType.CLICK, goog.events.Event.stopPropagation);
  h.listen(fx, bluemind.fx.Dragger.EventType.BEFORE_START, this.dispatchEvent);
  h.listen(fx, bluemind.fx.Dragger.EventType.BEFORE_START, this.setDragLimit_);
  h.listen(fx, goog.fx.Dragger.EventType.START, this.dispatchEvent);
  h.listen(fx, goog.fx.Dragger.EventType.START, this.handleDragStart_);
  h.listen(fx, goog.fx.Dragger.EventType.DRAG, this.handleDragging_);
  h.listen(fx, goog.fx.Dragger.EventType.END, this.handleDragStop_);
  h.listen(fx, goog.fx.Dragger.EventType.EARLY_CANCEL, this.handleCancel_);

  // FIXME : ....
  fx.setScrollTarget(this.getParent().getParent().getGridElement());
};

/**
 * On drag
 * 
 * @param {goog.events.Event} e Drag event
 * @private
 */
net.bluemind.calendar.month.ui.EventPart.prototype.handleDragStart_ = function(e) {
  var el = this.getElement();
  goog.dom.classlist.add(el, goog.getCssName('event-dragged'));
  var container = this.getDomHelper().getElementByClass(goog.getCssName('mg-daynum'));
  var width = goog.style.getSize(container).width;

  // this.fratricide();
  el.style.width = (width - 8) + 'px';
  var client = goog.style.getClientPosition(el);
  var left = (e.clientX - client.x);
  left -= (e.clientX - client.x) % width;
  el.style.left = left + 'px';
  // console.log(left);
  e.dragger.resetDrag(e);
  // this.pouicotage('fratricide');// In google calendar they hide all event
  // instance and draw a new one
  this.dispatchEvent(e);
};

/**
 * @param {goog.fx.DragEvent} e The event.
 * @private
 */
net.bluemind.calendar.month.ui.EventPart.prototype.handleDragStop_ = function(e) {
  goog.dom.classlist.remove(this.getElement(), goog.getCssName('event-dragged'));
  this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
};

/**
 * @param {goog.fx.DragEvent} e The event.
 * @private
 */
net.bluemind.calendar.month.ui.EventPart.prototype.handleDragging_ = function(e) {
  var position = goog.style.getPageOffset(this.getElement());
  var size = goog.style.getSize(this.getElement());
  var start = this.getDateForCoordinate_(position);
  var interval, model = this.getModel();
  if (goog.date.Date.compare(start, model.dtstart) < 0) {
    interval = new goog.date.Interval(0, 0, -1);
  } else {
    interval = new goog.date.Interval(0, 0, 1)
  }
  while (!goog.date.isSameDay(start, model.dtstart)) {
    model.dtstart.add(interval);
    model.dtend.add(interval);
  }

};

/**
 * Set limit and grid for resizing fx
 * 
 * @private
 */
net.bluemind.calendar.month.ui.EventPart.prototype.setDragLimit_ = function() {
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
net.bluemind.calendar.month.ui.EventPart.prototype.handleCancel_ = function(e) {
  this.dispatchEvent(goog.events.EventType.MOUSEDOWN);
};
