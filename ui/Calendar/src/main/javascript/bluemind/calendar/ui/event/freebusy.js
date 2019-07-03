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
 * @fileoverview This is freebusy.
 */

goog.provide('bluemind.calendar.ui.event.Freebusy');
goog.provide('bluemind.calendar.ui.event.Freebusy.DummyEvent');

goog.require('bluemind.date.DateTime');
goog.require('bluemind.fx.HorizontalDragger');
goog.require('bluemind.fx.ReverseWidthResizer');
goog.require('bluemind.fx.WidthResizer');
goog.require('goog.dom');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventType');
goog.require('goog.fx.Dragger.EventType');
goog.require('goog.i18n.DateTimeFormat');
goog.require('goog.i18n.DateTimeFormat.Format');
goog.require('goog.soy');
goog.require('goog.structs.Map');
goog.require('goog.ui.Tooltip');
goog.require('goog.userAgent');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.event.Freebusy = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.manager_ = bluemind.manager;
  this.handler_ = new goog.events.EventHandler(this);
  this.attendees_ = new goog.structs.Map();
  this.initIntervalDates();
  this.dummyEvent_ =
    new bluemind.calendar.ui.event.Freebusy.DummyEvent(this);
  this.visible_ = false;
  this.lock_ = false;
  this.slots_ = new goog.structs.Map();
  this.tooltips_ = new goog.structs.Map();
  this.tooltip_ = new goog.ui.Tooltip();
  this.attempts_ = 0;
};
goog.inherits(bluemind.calendar.ui.event.Freebusy, goog.ui.Component);

/**
 * Manager
 * @type {bluemind.calendar.Manager}
 */
bluemind.calendar.ui.event.Freebusy.prototype.manager_;

/**
 * Events handler
 * @type {goog.events.EventHandler}
 */
bluemind.calendar.ui.event.Freebusy.prototype.handler_;

/**
 * Attendees
 * @type {goog.structs.Map}
 */
bluemind.calendar.ui.event.Freebusy.prototype.attendees_;

/**
 * Start date
 * @type {bluemind.date.DateTime}
 */
bluemind.calendar.ui.event.Freebusy.prototype.start_;

/**
 * End date
 * @type {bluemind.date.DateTime}
 */
bluemind.calendar.ui.event.Freebusy.prototype.end_;

/**
 * Dummy event
 * @type {bluemind.calendar.ui.event.Freebusy.DummyEvent}
 */
bluemind.calendar.ui.event.Freebusy.prototype.dummyEvent_;

/**
 * Visibility flag
 * @type {boolean}
 */
bluemind.calendar.ui.event.Freebusy.prototype.visible_;

/**
 * Lock flag
 * @type {boolean}
 */
bluemind.calendar.ui.event.Freebusy.prototype.lock_;

/**
 * Freebusy slots
 * @type {goog.structs.Map}
 */
bluemind.calendar.ui.event.Freebusy.prototype.slots_;

/**
 * Freebusy tootips
 * @type {goog.structs.Map}
 */
bluemind.calendar.ui.event.Freebusy.prototype.tooltips_;

/**
 * Freebusy dom tootip
 * @type {goog.ui.Tooltip}
 */
bluemind.calendar.ui.event.Freebusy.prototype.tooltip;

/**
 * Event
 * @private
 */
bluemind.calendar.ui.event.Freebusy.prototype.event_;

/**
 * Nb autopick max attempts
 * @type {number}
 */
bluemind.calendar.ui.event.Freebusy.prototype.MAX_AUTOPICK_ATTEMPTS_ = 8;

/**
 * Nb autopick attemps
 * @type {number}
 */
bluemind.calendar.ui.event.Freebusy.prototype.attempts_;

/**
 * set event
 * @param {bluemind.calendar.model.Event} evt
 */
bluemind.calendar.ui.event.Freebusy.prototype.setEvent = function(evt) {
  this.event_ = evt;
};

/**
 * Get start date
 * @return {bluemind.date.DateTime} start date.
 */
bluemind.calendar.ui.event.Freebusy.prototype.getStart = function() {
  return this.start_;
};

/**
 * Get end date
 * @return {bluemind.date.DateTime} end date.
 */
bluemind.calendar.ui.event.Freebusy.prototype.getEnd = function() {
  return this.end_;
};
/**
 * get handler
 * @return {goog.events.EventHandler} handler.
 */
bluemind.calendar.ui.event.Freebusy.prototype.getHandler = function() {
  return this.handler_;
};

/**
 * Visibility flag
 * @return {boolean} visible.
 */
bluemind.calendar.ui.event.Freebusy.prototype.isVisible = function() {
  return this.visible_;
};

/**
 * Lock flag
 * @private
 * @return {boolean} lock.
 */
bluemind.calendar.ui.event.Freebusy.prototype.isLock_ = function() {
  return this.lock_;
};

/**
 * Set lock flag
 * @param {boolean} lock lock flag.
 * @private
 */
bluemind.calendar.ui.event.Freebusy.prototype.setLock_ = function(lock) {
  this.lock_ = lock;
};

/**
 * Get one calendar slots
 * @param  {number} id calendar id.
 * @return {Array} calendar slots.
 */
bluemind.calendar.ui.event.Freebusy.prototype.getCalendarSlots = function(id) {
  return this.slots_.get(id);
};

/**
 * Get one calendar tooltips
 * @param  {number} id calendar id.
 * @return {Array} calendar tooltips.
 */
bluemind.calendar.ui.event.Freebusy.prototype.getCalendarTooltips =
  function(id) {
  return this.tooltips_.get(id);
};

/**
 * Calendar dom tooltip
 * @return {goog.ui.Tooltip} calendar tooltip.
 */
bluemind.calendar.ui.event.Freebusy.prototype.getTooltip = function() {
  return this.tooltip_;
};

/**
 * Update busy slots
 */
bluemind.calendar.ui.event.Freebusy.prototype.updateBusySlots = function() {
  this.currentBusySlots_ = new Array(336);
  var slots = this.slots_.getValues();
  goog.array.forEach(slots, function(s) {
    for(var i = 0; i < s.length; i++) {
      if (s[i] != null) {
        this.currentBusySlots_[i] =
          goog.array.concat(s[i], this.currentBusySlots_[i]);
        this.currentBusySlots_[i] =
          goog.array.filter(this.currentBusySlots_[i], goog.isDef);
        goog.array.removeDuplicates(this.currentBusySlots_[i]);
      }
    }
  }, this);

  this.checkAvailability();
};

/**
 * check attendees availabilities. warn if one of attendees is unavailable
 */
bluemind.calendar.ui.event.Freebusy.prototype.checkAvailability = function() {
 var start = this.dummyEvent_.getSlot(bluemind.view.getView().getDateBegin());
 var end = this.dummyEvent_.getSlot(bluemind.view.getView().getDateEnd());
 var available = true;
 for (var i = start; i < end; i++) {
   var keys = this.currentBusySlots_[i] || [];
   goog.array.remove(keys, this.event_.getExtId());
   if (keys.length > 0) {
     available = false;
     break;
   }
 }
 bluemind.view.getView().availabilityWarn(available);
};

/** @inheritDoc */
bluemind.calendar.ui.event.Freebusy.prototype.decorateInternal =
  function(element) {
  this.setElementInternal(element);
};

/** @inheritDoc */
bluemind.calendar.ui.event.Freebusy.prototype.enterDocument =
  function() {
  bluemind.calendar.ui.event.Freebusy.superClass_.enterDocument.
    call(this);

  this.handler_.listen(goog.dom.getElement('freebusy-timeline-container'),
    goog.events.EventType.MOUSEDOWN, this.updateDummyEventOnMouseDown_);
};

/**
 * Show/hide freebusy
 * @param {boolean} v visible flag.
 */
bluemind.calendar.ui.event.Freebusy.prototype.setVisible = function(v) {
  this.visible_ = v;
  goog.style.showElement(
    goog.dom.getElement('bm-ui-form-fieldset-freebusy'), v);

  var begin = bluemind.view.getView().getDateBegin();
  var end = bluemind.view.getView().getDateEnd();
  if (bluemind.view.getView().getModel().isAllday()) {
    begin.setHours(0);
    begin.setMinutes(0);
    end.add(new goog.date.Interval(0, 0, 1));
  }
  this.updateDummyEventOnFormUpdate(begin, end, true);
};

/**
 * set initial date begin/end.
 */
bluemind.calendar.ui.event.Freebusy.prototype.initIntervalDates = function() {
  this.start_ = this.manager_.getCurrentDate().clone();
  var d = this.start_.getDay();
  if (d == 0) {
    this.start_.add(new goog.date.Interval(goog.date.Interval.DAYS, -6));
  } else {
    this.start_.add(new goog.date.Interval(goog.date.Interval.DAYS, -d + 1));
  }
  this.end_ = this.start_.clone();
  this.end_.add(new goog.date.Interval(goog.date.Interval.DAYS, 7));
};

/**
 * Freebusy toolbar
 */
bluemind.calendar.ui.event.Freebusy.prototype.initToolbar = function() {
  // North toolbar
  var tb = new bluemind.ui.Toolbar();
  tb.render(goog.dom.getElement('freebusy-north-toolbar'));

  var today = new goog.ui.Button(bluemind.calendar.template.i18n.today(),
    new goog.ui.style.app.ButtonRenderer.getInstance());
  tb.getWest().addChild(today, true);
  goog.events.listen(today, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.today();
  }, false, this);
  tb.getWest().addChild(new goog.ui.ToolbarSeparator(), true);

  var prev = new goog.ui.Button('\u25C4',
    new goog.ui.style.app.ButtonRenderer.getInstance());
  prev.setTooltip(bluemind.calendar.template.i18n.previousPeriod());
  prev.addClassName(goog.getCssName('goog-button-base-first'));
  tb.getWest().addChild(prev, true);
  goog.events.listen(prev, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.prev();
  }, false, this);

  var next = new goog.ui.Button('\u25BA',
    new goog.ui.style.app.ButtonRenderer.getInstance());
  next.setTooltip(bluemind.calendar.template.i18n.nextPeriod());
  next.addClassName(goog.getCssName('goog-button-base-last'));
  tb.getWest().addChild(next, true);
  goog.events.listen(next, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.next();
  }, false, this);

  var dateRange = new goog.dom.createElement('span');
  goog.dom.setProperties(dateRange, {'id': 'freebusyDateRange'});
  goog.dom.classes.add(dateRange, goog.getCssName('freebusyDateRange'));
  goog.dom.appendChild(tb.getWest().getContentElement(), dateRange);

  // South toolbar
  var tb = new bluemind.ui.Toolbar();
  tb.render(goog.dom.getElement('freebusy-south-toolbar'));

  var autoPickPrev = new goog.ui.Button(
    bluemind.calendar.template.i18n.autoPickPrev(),
    new goog.ui.style.app.ButtonRenderer.getInstance());
  autoPickPrev.addClassName(goog.getCssName('goog-button-base-first'));
  tb.getWest().addChild(autoPickPrev, true);
  goog.events.listen(autoPickPrev, goog.ui.Component.EventType.ACTION,
    function(e) {
      e.stopPropagation();
      this.autoPickPrev_();
    }, false, this);

  var autoPickNext = new goog.ui.Button(
    bluemind.calendar.template.i18n.autoPickNext(),
    new goog.ui.style.app.ButtonRenderer.getInstance());
  autoPickNext.addClassName(goog.getCssName('goog-button-base-last'));
  tb.getWest().addChild(autoPickNext, true);
  goog.events.listen(autoPickNext, goog.ui.Component.EventType.ACTION,
    function(e) {
      e.stopPropagation();
      this.autoPickNext_();
    }, false, this);
};

/**
 * Init freebusy grid.
 */
bluemind.calendar.ui.event.Freebusy.prototype.initGrid = function() {
  var trDays = goog.dom.createDom('tr');
  var trHours = goog.dom.createDom('tr');
  var trAttendees = goog.dom.createDom('tr', {'id': 'all-attendees'});
  goog.dom.classes.add(trAttendees, goog.getCssName('all-attendees'));
  goog.dom.classes.add(trAttendees, goog.getCssName('all-attendees-container'));

  var cssDayLabel = goog.getCssName('day-label');
  var cssDayHourLabel = goog.getCssName('day-hour-label');
  var cssDaySeparator = goog.getCssName('day-separator');
  var cssDayHour = goog.getCssName('day-hour');
  var cssDayHalfHour = goog.getCssName('day-half-hour');
  var cssDayHourContent = goog.getCssName('day-hour-content');

  for (var d = 1; d <= 7; d++) {
    var i = d % 7;
    var td = goog.dom.createDom('td',
      {'id': 'day' + i, 'colspan': '48'});
    goog.dom.classes.add(td, cssDayLabel);
    goog.dom.appendChild(trDays, td);

    for (var j = 0; j < 24; j++) {
      var td = goog.dom.createDom('td',
        {'id': 'day-' + i + '-hour-' + j,
         'colspan': '2'}, j + ':00');

      goog.dom.classes.add(td, cssDayHourLabel);

      if (j == 0) {
        goog.dom.classes.add(td, cssDaySeparator);
      }

      goog.dom.appendChild(trHours, td);
    }

    for (var j = 0; j < 48; j++) {
      var td = goog.dom.createDom('td',
        {'id': 'day-' + i + '-hour-' + j / 2 + '-all-attendees'});

      if (j % 2 == 0) {
        goog.dom.classes.add(td, cssDayHalfHour);
      } else {
        goog.dom.classes.add(td, cssDayHour);
      }
      if (j == 0) {
        goog.dom.classes.add(td, cssDaySeparator);
      }

      var div = goog.dom.createDom('div');

      goog.dom.classes.add(div, cssDayHourContent);
      goog.dom.appendChild(td, div);
      goog.dom.appendChild(trAttendees, td);
    }
  }

  goog.dom.appendChild(
    goog.dom.getElement('freebusy-timeline-container'), trDays);

  goog.dom.appendChild(
    goog.dom.getElement('freebusy-timeline-container'), trHours);

  goog.dom.appendChild(
    goog.dom.getElement('freebusy-timeline-container'), trAttendees);

  this.dummyEvent_.decorate(goog.dom.getElement('fb-dummy-event'));
  this.updateGrid();
};

/**
 * Update grid labels
 */
bluemind.calendar.ui.event.Freebusy.prototype.updateGrid = function() {

  if (goog.date.Date.compare(
        bluemind.view.getView().getDateEnd(), this.start_) > 0 &&
      goog.date.Date.compare(
        bluemind.view.getView().getDateBegin(), this.end_) < 0) {
    this.dummyEvent_.show(true);
  } else {
    this.dummyEvent_.show(false);
  }

  var current = this.start_.clone();
  while (goog.date.Date.compare(current, this.end_) < 0) {
   var lbl =
      goog.i18n.DateTimeSymbols.WEEKDAYS[(current.getDay()) % 7] + ' ' +
      current.getDate() + ' ' +
      goog.i18n.DateTimeSymbols.MONTHS[current.getMonth()];
      goog.dom.setTextContent(goog.dom.getElement('day' + current.getDay()),
        lbl);
      current.add(new goog.date.Interval(0, 0, 1));
  }
  var df = new goog.i18n.DateTimeFormat(
    goog.i18n.DateTimeFormat.Format.MEDIUM_DATE);
  var end = this.end_.clone();
  end.add(new goog.date.Interval(0, 0, -1));
  goog.dom.setTextContent(
    goog.dom.getElement('freebusyDateRange'),
    df.format(this.start_) + ' - ' + df.format(end));
};

/**
 * Update dummy event onclick
 * @param {goog.events.BrowserEvent} e browser event.
 * @private
 */
bluemind.calendar.ui.event.Freebusy.prototype.updateDummyEventOnMouseDown_ =
  function(e) {
  var target = e.target;
  if (goog.dom.classes.has(target, goog.getCssName('day-hour')) ||
    goog.dom.classes.has(target, goog.getCssName('day-half-hour')) ||
    goog.dom.classes.has(target, goog.getCssName('day-hour-label'))) {
    this.setLock_(true);
    this.dummyEvent_.show(true);
    this.dummyEvent_.setPick(true);
    this.dummyEvent_.updatePosition(target.offsetLeft, false);
    if (goog.dom.classes.has(target, goog.getCssName('day-hour-label'))) {
      this.dummyEvent_.setDuration(3600);
    } else {
      this.dummyEvent_.setDuration(1800);
    }
    this.dummyEvent_.getResizeEnd().startDrag(e);
    this.setLock_(false);
  }
};

/**
 * Update dummy event on form update
 * @param {bluemind.date.DateTime} b date begin.
 * @param {bluemind.date.DateTime} e date end.
 * @param {boolean} f a boolean (don't know what).
 */
bluemind.calendar.ui.event.Freebusy.prototype.updateDummyEventOnFormUpdate =
  function(b, e, f) {
  if (!this.lock_ && this.visible_) {
    if (!(goog.date.Date.compare(b, this.start_) > 0 &&
      goog.date.Date.compare(b, this.end_) < 0) || f) {
      this.gotoDate(b);
    }
    this.dummyEvent_.updateDateBegin(b);
    if (bluemind.view.getView().getModel().isAllday()) {
      this.dummyEvent_.setDuration(((e.getTime() - b.getTime()) / 1000) + 86400);
    } else {
      this.dummyEvent_.setDuration((e.getTime() - b.getTime()) / 1000);
    }
  }
};

/**
 * Prev interval
 * @param {Object} opt_callback Optional callback.
 */
bluemind.calendar.ui.event.Freebusy.prototype.prev = function(opt_callback) {
  this.start_.add(new goog.date.Interval(0, 0, -7));
  this.gotoDate(this.start_, opt_callback);
};

/**
 * Next interval
 * @param {Object} opt_callback Optional callback.
 */
bluemind.calendar.ui.event.Freebusy.prototype.next = function(opt_callback) {
  this.start_.add(new goog.date.Interval(0, 0, 7));
  this.gotoDate(this.start_, opt_callback);
};

/**
 * Go to Today
 */
bluemind.calendar.ui.event.Freebusy.prototype.today = function() {
  var d = new bluemind.date.DateTime();
  d.set(this.manager_.getToday());
  this.gotoDate(d);
};

/**
 * AutoPick Prev
 * @private
 */
bluemind.calendar.ui.event.Freebusy.prototype.autoPickPrev_ = function() {
  this.setLock_(true);
  var begin = bluemind.view.getView().getDateBegin();
  var step = 1;
  if (bluemind.view.getView().getModel().isAllday()) {
    step = 48;
  }
  var start = this.dummyEvent_.getSlot(begin) - step;
  var free = false;

  while (!free && start >= 0) {
    free = true;
    for (var i = start; i < (start + this.dummyEvent_.getDuration()); i++) {
      if (this.currentBusySlots_[i]) {
        free = false;
        break;
      }
    }
    if (!free) {
      start = start - step;
    }
  }
  if (free) {
    this.setLock_(false);
    var current = this.start_.clone();
    current.add(
      new goog.date.Interval(goog.date.Interval.MINUTES, 30 * start));
    bluemind.view.getView().setDateTimeBegin(current);
    this.attempts_ = 0;
  } else {
     if (this.attempts_ < this.MAX_AUTOPICK_ATTEMPTS_) {
       this.attempts_++;
       this.prev(goog.bind(this.autoPickPrevWeek, this));
     } else {
       this.attempts_ = 0;
       if (confirm(bluemind.calendar.template.i18n.freebusyNoSlotsFound())) {
         this.prev(goog.bind(this.autoPickPrevWeek, this));
       } else {
         this.setLock_(false);
         // this.today();
       }
     }
  }
  this.dummyEvent_.show(true);
};

/**
 * AutoPick Next
 * @private
 */
bluemind.calendar.ui.event.Freebusy.prototype.autoPickNext_ = function() {
  this.setLock_(true);
  var begin = bluemind.view.getView().getDateBegin();
  var step = 1;
  if (bluemind.view.getView().getModel().isAllday()) {
    step = 48;
  }
  var start = this.dummyEvent_.getSlot(begin) + step;
  var free = false;
  while (!free && start <= 336 - this.dummyEvent_.getDuration()) {
    free = true;
    for (var i = start; i < (start + this.dummyEvent_.getDuration()); i++) {
      if (this.currentBusySlots_[i]) {
        free = false;
        break;
      }
    }
    if (!free) {
      start = start + step;
    }
  }
  if (free) {
    this.setLock_(false);
    var current = this.start_.clone();
    current.add(
      new goog.date.Interval(goog.date.Interval.MINUTES, 30 * start));
    bluemind.view.getView().setDateTimeBegin(current);
    this.attempts_ = 0;
  } else {
     if (this.attempts_ < this.MAX_AUTOPICK_ATTEMPTS_) {
       this.attempts_++;
       this.next(goog.bind(this.autoPickNextWeek, this));
     } else {
       this.attempts_ = 0;
       if (confirm(bluemind.calendar.template.i18n.freebusyNoSlotsFound())) {
         this.next(goog.bind(this.autoPickNextWeek, this));
       } else {
         this.setLock_(false);
         // this.today();
       }
     }
  }
  this.dummyEvent_.show(true);
};

/**
 * AutoPick previous free slot in the previous week
 */
bluemind.calendar.ui.event.Freebusy.prototype.autoPickPrevWeek = function() {
  var d = this.end_.clone();
  var begin = bluemind.view.getView().getDateBegin();
  var end = bluemind.view.getView().getDateEnd();
  d.add(new goog.date.Interval(goog.date.Interval.MINUTES,
    -30 * this.dummyEvent_.getDuration()));
  bluemind.view.getView().setDateTimeBegin(d);
  this.autoPickPrev_();
};


/**
 * AutoPick next free slot in the next week
 */
bluemind.calendar.ui.event.Freebusy.prototype.autoPickNextWeek = function() {
  bluemind.view.getView().setDateTimeBegin(this.start_);
  this.autoPickNext_();
};

/**
 * Change freebusy date
 * @param {goog.date.Date} date fb date.
 * @param {Object} opt_callback Optional callback.
 */
bluemind.calendar.ui.event.Freebusy.prototype.gotoDate =
  function(date, opt_callback) {

  goog.array.forEach(this.tooltips_.getKeys(), function(k) {
    this.tooltips_.set(k, new goog.structs.Map());
  }, this);

  var busySlots =
    goog.dom.getElementsByTagNameAndClass('div', goog.getCssName('slot'));
  for (var i = 0; i < busySlots.length; i++) {
    goog.dom.removeNode(busySlots[i]);
  }

  this.start_ = date.clone();
  this.start_.setHours(0);
  this.start_.setMinutes(0);
  this.start_.setSeconds(0);
  var d = this.start_.getDay();
  if (d == 0) {
    this.start_.add(new goog.date.Interval(0, 0, -6));
  } else {
    this.start_.add(new goog.date.Interval(0, 0, -d + 1));
  }
  this.end_ = this.start_.clone();
  this.end_.add(new goog.date.Interval(0, 0, 7));
  this.updateGrid();

  bluemind.calendar.Controller.getInstance().addFreebusyAttendees(
    this, this.attendees_.getValues(), opt_callback);
};

/**
 * Add an attendee.
 * @param {Object} a attendee.
 * @param {number} dayStartsAt day starts at.
 * @param {number} dayEndsAt day ends at.
 * @param {Array} workingDays working days.
 * @private
 */
bluemind.calendar.ui.event.Freebusy.prototype.addAttendee_ =
  function(a, dayStartsAt, dayEndsAt, workingDays) {

  if (dayStartsAt == null) dayStartsAt = 9;
  if (dayEndsAt == null) dayEndsAt = 18;

  var calendar = a['calendar'];
  var email = a['email'];
  var dn = a['displayName'];
  var myself = (a['type'] == 'user' && a['id'] == bluemind.me['id']);

  if (!goog.dom.getElement('timeline-' + calendar)) {
    var att = goog.dom.createDom('tr',
      {'id': 'label-' + calendar});
    var attDn = goog.dom.createDom('td', {}, dn);
    goog.dom.classes.add(attDn, goog.getCssName('attendee'));
    goog.dom.appendChild(att, attDn);
    goog.dom.appendChild(
      goog.dom.getElement('freebusy-attendees-container-tbody'), att);

    var timeline = goog.dom.createDom('tr', {'id': 'timeline-' + calendar});

    var cssDaySeparator = goog.getCssName('day-separator');
    var cssDayHour = goog.getCssName('day-hour');
    var cssDayHalfHour = goog.getCssName('day-half-hour');
    var cssOutOfWork = goog.getCssName('outOfWorkFreeBusy');

    var slots = new Array(336);
    var shortWeekDays = goog.i18n.DateTimeSymbols_en_ISO.SHORTWEEKDAYS;
    var wd = workingDays.split(',');
    var busy = false;
    for (var d = 1; d <= 7; d++) {
      var i = d % 7;
      if (!goog.array.contains(wd, shortWeekDays[i].toLowerCase())) {
        busy = true;
      }
      for (var j = 0; j < 48; j++) {
        var css = '';
        if (busy ||
          (dayStartsAt < dayEndsAt && !(j / 2 >= dayStartsAt && j / 2 < dayEndsAt)) ||
          (dayStartsAt > dayEndsAt && (j / 2 >= dayEndsAt && j / 2 < dayStartsAt))) {
          css += ' ' + cssOutOfWork;
          slots[((d - 1) * 48) + j] = new Array();
        }

        if (!this.lock_) {
          if (j % 2 == 0) {
            css += ' ' + cssDayHalfHour;
          } else {
            css += ' ' + cssDayHour;
          }
          if (j == 0) {
            css += ' ' + cssDaySeparator;
          }
          var timelinecontent = goog.dom.createDom('td',
            {'id': 'day-' + i + '-hour-' + j / 2 + '-calendar-' + calendar,
             'class' : css});
          goog.dom.appendChild(timeline, timelinecontent);
        }
      }
      busy = false;
    }
    if (!this.lock_) {
      goog.dom.appendChild(
        goog.dom.getElement('freebusy-timeline-container'), timeline);
    }
    this.slots_.set(calendar + '', slots);
    this.tooltips_.set(calendar, new goog.structs.Map());
  } else {
    var slots = new Array(336);
    var shortWeekDays = goog.i18n.DateTimeSymbols_en_ISO.SHORTWEEKDAYS;
    var wd = workingDays.split(',');
    var busy = false;
    for (var d = 1; d <= 7; d++) {
      var i = d % 7;
      if (!goog.array.contains(wd, shortWeekDays[i].toLowerCase())) {
        busy = true;
      }
      for (var j = 0; j < 48; j++) {
        if (busy ||
          (dayStartsAt < dayEndsAt && !(j / 2 >= dayStartsAt && j / 2 < dayEndsAt)) ||
          (dayStartsAt > dayEndsAt && (j / 2 >= dayEndsAt && j / 2 < dayStartsAt))) {
          slots[((d - 1) * 48) + j] = new Array();
        }
      }
      busy = false;
    }
    this.slots_.set(calendar + '', slots);
    this.tooltips_.set(calendar, new goog.structs.Map());
  }
  if (a['type'] == 'contact') {
    var width = goog.dom.getElement('freebusy-timeline-container').offsetWidth;
    var div = goog.dom.createDom('div',
      {'style': 'width:' + width + 'px',
       'title': bluemind.calendar.template.i18n.noInformation(),
       'class': goog.getCssName('slot') + ' ' + goog.getCssName('no-information')});
    goog.dom.appendChild(
      goog.dom.getElement('day-1-hour-0-calendar-' + calendar), div);
  }
  this.loveIE_();
};

/**
 * Add attendees
 * @param {Array} attendees attendees.
 */
bluemind.calendar.ui.event.Freebusy.prototype.addAttendees =
  function(attendees) {
  goog.array.forEach(attendees, function(a) {
    var cal =  bluemind.manager.getCalendar(a['calendar']);
    if (cal != null) {
      a['dayStart'] = cal.getDayStart();
      a['dayEnd'] = cal.getDayEnd();
      a['workingDays'] = cal.getWorkingDays();
    }
    this.attendees_.set(a['calendar'], a);
    if (a['id'] == bluemind.me['id']) {
      this.addAttendee_(a,
        parseFloat(bluemind.me['work_hours_start']),
        parseFloat(bluemind.me['work_hours_end']),
        bluemind.me['working_days']);
    } else if (a['type'] == 'user' || a['type'] == 'resource') {
        this.addAttendee_(a,
            parseFloat(a['dayStart']),
            parseFloat(a['dayEnd']),
            a['workingDays']);
    } else {
      // Contact
      this.addAttendee_(a,
        parseFloat(0),
        parseFloat(24),
        'mon,tue,wed,thu,fri,sat,sun');
    }
  }, this);
};

/**
 * Remove an attendee.
 * @param {number} calendar attendee calendar id.
 */
bluemind.calendar.ui.event.Freebusy.prototype.removeAttendee =
  function(calendar) {
  goog.dom.removeNode(goog.dom.getElement('timeline-' + calendar));
  goog.dom.removeNode(goog.dom.getElement('label-' + calendar));

  var evts =
    goog.dom.getElementsByTagNameAndClass('div', 'all-attendees-' + calendar);
  for (var i = 0; i < evts.length; i++) {
    goog.dom.removeNode(evts[i]);
  }

  this.attendees_.remove(calendar);
  this.slots_.remove(calendar);
  this.tooltips_.remove(calendar);
  this.updateBusySlots();
  this.loveIE_();
};

/**
 * @private
 */
bluemind.calendar.ui.event.Freebusy.prototype.loveIE_ = function() {
  if (goog.userAgent.IE) {
    var h = goog.style.getSize(
      goog.dom.getElement('freebusy-attendees-container')).height;
    goog.style.setHeight(goog.dom.getElement('freebusy-container'), h);
  }
};

/**
 * @param {bluemind.calendar.ui.event.Freebusy} fb Freebusy.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent = function(fb, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.handler_ = new goog.events.EventHandler(this);
  this.unit_ = 21; // 20px (td witdh) + 1px (border)
  this.fb_ = fb;
  this.pick_ = false;
};
goog.inherits(bluemind.calendar.ui.event.Freebusy.DummyEvent,
  goog.ui.Component);

/** @inheritDoc */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.decorateInternal =
  function(element) {
  this.setElementInternal(element);
};

/**
 * Freebusy
 * @type {bluemind.calendar.ui.event.Freebusy} fb_ freebusy.
 * @private
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.fb_;

/**
 * Event handler
 * @type {goog.events.EventHandler} handler_ event handler.
 * @private
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.handler_;

/**
 * Event drag
 * @type {bluemind.fx.HorizontalDragger} drag_ drag.
 * @private
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.drag_;

/**
 * Grid unit
 * @type {number} one hour in px
 * @private
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.unit_;

/**
 * Event resizer
 * @type {bluemind.fx.WidthResizer} resizeEnd_ resizer.
 * @private
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.resizeEnd_;

/**
 * Pick action
 * @type {boolean} pick action.
 * @private
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.pick_;

/**
 * duration
 * @type {number} duration in slot.
 * @private
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.duration_;

/**
 * Get resizeEnd
 * @return {bluemind.fx.WidthResizer} resizeEnd_.
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.getResizeEnd =
  function() {
  return this.resizeEnd_;
};

/**
 * Pick action
 * @param {boolean} pick pick action.
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.setPick =
  function(pick) {
  this.pick_ = pick;
};

/**
 * Event resizer
 * @param {bluemind.fx.ReverseWidthResizer} resizeBegin_ resizer.
 * @private
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.resizeBegin_;

/** @inheritDoc */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.enterDocument =
  function() {
  bluemind.calendar.ui.event.Freebusy.DummyEvent.superClass_.enterDocument.
    call(this);

  var gridSize = new goog.math.Size(this.unit_, 0);
  var scrollTarget = goog.dom.getElement('freebusy-container');

  // Drag
  this.drag_ = new bluemind.fx.HorizontalDragger(this.element_);
  this.drag_.setScrollTarget(scrollTarget);
  this.drag_.setGrid(gridSize);

  this.getHandler().listen(this.drag_,
    bluemind.fx.Dragger.EventType.BEFORE_START,
    function(e) {
      var cwidth =
        goog.dom.getElement('freebusy-data-container').offsetWidth;
      var ewidth = this.element_.offsetWidth;
      this.drag_.setLimits(
        new goog.math.Rect(0, 0, cwidth - ewidth, 0));

      this.scroll_ = new goog.fx.DragScrollSupport(scrollTarget, 1);

    }, false, this);

  this.getHandler().listen(this.drag_,
    goog.fx.Dragger.EventType.END,
    function(e) {
      this.fb_.setLock_(true);
      var begin = Math.round(e.left / this.unit_);
      var datebegin = this.fb_.getStart().clone();
      datebegin.add(
        new goog.date.Interval(goog.date.Interval.MINUTES, (begin * 30)));
      bluemind.view.getView().setDateTimeBegin(datebegin);
      goog.dispose(this.scroll_);
      this.fb_.setLock_(false);
    }, false, this);

  // End resizer
  this.resizeEnd_ = new bluemind.fx.WidthResizer(this.element_,
    goog.dom.getElement('fb-dummy-event-resizer-end'));
  this.resizeEnd_.setScrollTarget(scrollTarget);
  this.resizeEnd_.setGrid(gridSize);

  this.getHandler().listen(this.resizeEnd_,
    bluemind.fx.Dragger.EventType.BEFORE_START,
    function(e) {
      var cwidth = goog.dom.getElement('freebusy-data-container').offsetWidth;
      var elementOffsetLeft = this.element_.offsetLeft;
      this.resizeEnd_.setLimits(
        new goog.math.Rect(elementOffsetLeft, 0,
          cwidth - elementOffsetLeft, 0));

      this.scroll_ = new goog.fx.DragScrollSupport(scrollTarget, 1);

    }, false, this);

  this.getHandler().listen(this.resizeEnd_,
    goog.fx.Dragger.EventType.END,
    function(e) {
      this.fb_.setLock_(true);

      var elementOffsetLeft = this.element_.offsetLeft;

      if (this.pick_) {
        this.pick_ = false;
        var begin = Math.round(elementOffsetLeft / this.unit_);
        var datebegin = this.fb_.getStart().clone();
        datebegin.add(
          new goog.date.Interval(goog.date.Interval.MINUTES, (begin * 30)));
        bluemind.view.getView().setDateTimeBegin(datebegin);
      }

      var end = Math.round(
        (elementOffsetLeft + this.element_.offsetWidth) / this.unit_);
      var dateend = this.fb_.getStart().clone();
      dateend.add(
        new goog.date.Interval(goog.date.Interval.MINUTES, (end * 30)));
      bluemind.view.getView().setDateTimeEnd(dateend);
      goog.dispose(this.scroll_);

      this.setDuration((bluemind.view.getView().getDateEnd().getTime() -
        bluemind.view.getView().getDateBegin().getTime()) / 1000);

      this.fb_.setLock_(false);
    }, false, this);

  this.handler_.listen(goog.dom.getElement('fb-dummy-event-resizer-end'),
    goog.events.EventType.MOUSEDOWN, function(e) { e.stopPropagation();});

  this.handler_.listen(goog.dom.getElement('fb-dummy-event-resizer-end'),
    goog.events.EventType.MOUSEUP, function(e) {goog.dispose(this.scroll_);});

  // Begin resizer
  this.resizeBegin_ = new bluemind.fx.ReverseWidthResizer(
    this.element_, goog.dom.getElement('fb-dummy-event-resizer-begin'));
  this.resizeBegin_.setScrollTarget(scrollTarget);
  this.resizeBegin_.setGrid(gridSize);

  this.getHandler().listen(this.resizeBegin_,
    bluemind.fx.Dragger.EventType.BEFORE_START,
    function(e) {
      this.resizeBegin_.setLimits(
        new goog.math.Rect(0, 0,
          this.element_.offsetLeft + this.element_.offsetWidth * 2 -
          this.unit_, 0));

      this.scroll_ = new goog.fx.DragScrollSupport(scrollTarget, 1);

    }, false, this);

  this.getHandler().listen(this.resizeBegin_,
    goog.fx.Dragger.EventType.END,
    function(e) {
      this.fb_.setLock_(true);

      var elementOffsetLeft = this.element_.offsetLeft;

      var begin = Math.round(elementOffsetLeft / this.unit_);

      var datebegin = this.fb_.getStart().clone();
      datebegin.add(
        new goog.date.Interval(goog.date.Interval.MINUTES, (begin * 30)));

      goog.dispose(this.scroll_);

      this.setDuration((bluemind.view.getView().getDateEnd().getTime() -
        datebegin.getTime()) / 1000);

      bluemind.view.getView().setDateTimeBegin(datebegin);

      this.fb_.setLock_(false);
    }, false, this);

  this.handler_.listen(goog.dom.getElement('fb-dummy-event-resizer-begin'),
    goog.events.EventType.MOUSEDOWN, function(e) { e.stopPropagation();});

  this.handler_.listen(goog.dom.getElement('fb-dummy-event-resizer-begin'),
    goog.events.EventType.MOUSEUP, function(e) {goog.dispose(this.scroll_);});

};

/**
 * Update evt position.
 * @param {number} left left position.
 * @param {boolean} scroll scroll flag.
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.updatePosition =
  function(left, scroll) {
  goog.style.setPosition(this.element_, left);
  if (scroll) {
    goog.dom.getElement('freebusy-container').scrollLeft =
      left - (4.5 * this.unit_);
  }
};

/**
 * Update evt position from datebegin
 * @param {bluemind.date.DateTime} d evt date begin.
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.updateDateBegin =
  function(d) {
  // %<-------- CRAPPY --------------------------%<
  var h = d.getHours();
  if (d.getMinutes() == 30) {
    h = h + 0.5;
  } else if (d.getMinutes() > 30) {
    h++;
  }
  // %<-------- CRAPPY --------------------------%<
  var id = 'day-' + d.getDay() + '-hour-' + h + '-all-attendees';
  var slot = goog.dom.getElement(id);
  this.updatePosition(slot.offsetLeft, true);
};

/**
 * Update evt witdh.
 * @param {number} duration dummy event duration.
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.setDuration =
  function(duration) {
  this.duration_ = (duration / 1800);
  bluemind.view.getView().setDuration(duration);
  goog.style.setWidth(this.element_, this.unit_ * (this.duration_) + 3);
};

/**
 * evt witdh.
 * @return {number} duration event duration.
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.getDuration =
  function() {
  return this.duration_;
};

/**
 * Show/Hide dummy event
 * @param {boolean} b show/hide flag.
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.show = function(b) {
  goog.style.showElement(this.element_, b);
};

/**
 * Get slot
 * @param {bluemind.date.DateTime} d date.
 * @return {number} slot number.
 */
bluemind.calendar.ui.event.Freebusy.DummyEvent.prototype.getSlot =
  function(d) {
  var day = d.getDay();
  var index = (day == 0) ? 6 : day - 1;

  var h = d.getHours();
  if (d.getMinutes() == 30) {
    h = h + 0.5;
  } else if (d.getMinutes() > 30) {
    h++;
  }

  var start = 48 * index + h * 2;
  return start;
};

