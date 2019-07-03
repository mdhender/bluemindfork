/*
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

goog.provide('net.bluemind.calendar.vevent.ui.Freebusy');
goog.provide('net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent');

goog.require('net.bluemind.date.DateTime');
goog.require('bluemind.fx.HorizontalDragger');
goog.require('bluemind.fx.ReverseWidthResizer');
goog.require('bluemind.fx.WidthResizer');
goog.require('goog.dom');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventType');
goog.require('goog.fx.Dragger.EventType');
goog.require('goog.fx.DragScrollSupport');
goog.require('goog.i18n.DateTimeFormat');
goog.require('goog.i18n.DateTimeFormat.Format');
goog.require('goog.soy');
goog.require('goog.structs.Map');
goog.require('goog.ui.Tooltip');
goog.require('goog.userAgent');
goog.require('net.bluemind.calendar.api.CalendarsClient');
goog.require('net.bluemind.calendar.vevent.ui.EventDetailsPopup');

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.vevent.ui.Freebusy = function(ctx, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.ctx = ctx;
  this.attendees_ = new goog.structs.Map();
  this.dummyEvent_ = new net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent(this);
  this.lock_ = false;
  this.slots_ = new goog.structs.Map();
  this.tooltips_ = new goog.structs.Map();
  this.tooltip_ = new goog.ui.Tooltip();
  this.attempts_ = 0;
  this.timeout = null;
  this.userMapping = {};
  this.visible_ = false;
};
goog.inherits(net.bluemind.calendar.vevent.ui.Freebusy, goog.ui.Component);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.ctx;

/**
 * @type {net.bluemind.date.DateRange}
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.range;

/**
 * Attendees
 * 
 * @type {goog.structs.Map}
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.attendees_;

/**
 * Dummy event
 * 
 * @type {net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent}
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.dummyEvent_;

/**
 * Visibility flag
 * 
 * @type {boolean}
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.visible_;

/**
 * Lock flag
 * 
 * @type {boolean}
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.lock_;

/**
 * Freebusy slots
 * 
 * @type {goog.structs.Map}
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.slots_;

/**
 * Freebusy tootips
 * 
 * @type {goog.structs.Map}
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.tooltips_;

/**
 * Freebusy dom tootip
 * 
 * @type {goog.ui.Tooltip}
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.tooltip;

/**
 * Nb autopick max attempts
 * 
 * @type {number}
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.MAX_AUTOPICK_ATTEMPTS_ = 8;

/**
 * Nb autopick attemps
 * 
 * @type {number}
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.attempts_;

/**
 * Visibility flag
 * 
 * @return {boolean} visible.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.isVisible = function() {
  return this.visible_;
};

/**
 * Lock flag
 * 
 * @private
 * @return {boolean} lock.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.isLock_ = function() {
  return this.lock_;
};

/**
 * Set lock flag
 * 
 * @param {boolean} lock lock flag.
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.setLock_ = function(lock) {
  this.lock_ = lock;
};

/**
 * Get one calendar slots
 * 
 * @param {number} id calendar id.
 * @return {Array} calendar slots.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.getCalendarSlots = function(id) {
  return this.slots_.get(id);
};

/**
 * Get one calendar tooltips
 * 
 * @param {number} id calendar id.
 * @return {Array} calendar tooltips.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.getCalendarTooltips = function(id) {
  return this.tooltips_.get(id);
};

/**
 * Calendar dom tooltip
 * 
 * @return {goog.ui.Tooltip} calendar tooltip.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.getTooltip = function() {
  return this.tooltip_;
};

/**
 * Update busy slots
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.updateBusySlots = function() {
  var times = this.getDomHelper().getElement('all-attendees').children;
  var start = times.item(0).id.match(/day-([^-])-.*/).pop() * 48;
  this.currentBusySlots_ = new Array(336);
  var slots = this.slots_.getValues();
  goog.array.forEach(slots, function(s) {
    for (var i = 0; i < s.length; i++) {
      var index = (i + start) % s.length;
      if (s[index] == 'BUSY' || s[index] == 'BUSYTENTATIVE') {
        this.currentBusySlots_[i] = 'Busy';
      }
      if (s[index] == 'BUSYUNAVAILABLE') {
        this.currentBusySlots_[i] = 'BusyUnavailable';
      }
    }
  }, this);
  for (var i = 0; i < this.currentBusySlots_.length; i++) {
    var time = times.item(i);
    if (!time) {
      continue;
    }
    goog.dom.classlist.enable(time, goog.getCssName('outOfWorkFreeBusy'),
        this.currentBusySlots_[i] == 'BusyUnavailable');
    goog.dom.classlist.enable(time, goog.getCssName('Busy'), this.currentBusySlots_[i] == 'Busy');
  }
  this.checkAvailability();
};

/**
 * check attendees availabilities. warn if one of attendees is unavailable
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.checkAvailability = function() {
  var start = this.dummyEvent_.getSlot(this.getParent().getModel().dtstart);
  var end = this.dummyEvent_.getSlot(this.getParent().getModel().dtend);
  var available = true;
  this.getParent().availabilityWarn(this.isFreeInterval_(start, end));
};

/**
 * check if an interval is not busy
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.isFreeInterval_ = function(start, end) {
  var available = !this.getModel().states.allday;
  for (var i = start; i < end; i++) {
    if (!this.currentBusySlots_[i]) {
      available = true;
    } else if (this.currentBusySlots_[i] == 'Busy') {
      return false;
    } else if (!this.getModel().states.allday && this.currentBusySlots_[i] == 'BusyUnavailable') {
      return false;
    }
  }
  return available;
};

/** @inheritDoc */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.decorateInternal = function(element) {
  this.setElementInternal(element);
};

/** @inheritDoc */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.enterDocument = function() {
  net.bluemind.calendar.vevent.ui.Freebusy.superClass_.enterDocument.call(this);

  this.getHandler().listen(goog.dom.getElement('freebusy-timeline-container'), goog.events.EventType.MOUSEDOWN,
      this.updateDummyEventOnMouseDown_);
  
  this.getHandler().listen(goog.dom.getElement('freebusy-timeline-container'), goog.events.EventType.MOUSEOVER,
      this.showEventDetails_);
  
  this.getHandler().listen(goog.dom.getElement('freebusy-timeline-container'), goog.events.EventType.MOUSEOUT,
      this.hideEventDetails_);
};



/**
 * Show/hide freebusy
 * 
 * @param {boolean} v visible flag.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.setVisible = function(v) {
  this.visible_ = v;
  goog.style.showElement(goog.dom.getElement('bm-ui-form-fieldset-freebusy'), v);

  var begin = new net.bluemind.date.DateTime(this.getModel().dtstart);
  var end = new net.bluemind.date.DateTime(this.getModel().dtend);

  this.updateDummyEventOnFormUpdate(begin, end, true);
  this.loveIE_();
};

/**
 * Freebusy toolbar
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.initToolbar = function() {
  // North toolbar
  var tb = new goog.ui.Toolbar();
  this.addChild(tb);
  tb.render(goog.dom.getElement('freebusy-north-toolbar'));
  var buttonRenderer = goog.ui.style.app.ButtonRenderer.getInstance();
  /** @meaning calendar.toolbar.today */
  var MSG_TODAY = goog.getMsg('Today');
  var today = new goog.ui.Button(MSG_TODAY, buttonRenderer);
  tb.addChild(today, true);
  this.getHandler().listen(today, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.today();
  });
  tb.addChild(new goog.ui.ToolbarSeparator(), true);
  var prev = new goog.ui.Button('\u25C4', buttonRenderer);
  /** @meaning calendar.freebusy.previous */
  var MSG_PREVIOUS = goog.getMsg('Previous period');
  prev.setTooltip(MSG_PREVIOUS);
  prev.addClassName(goog.getCssName('goog-button-base-first'));
  tb.addChild(prev, true);
  this.getHandler().listen(prev, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.prev();
  });

  var next = new goog.ui.Button('\u25BA', buttonRenderer);
  /** @meaning calendar.freebusy.next */
  var MSG_NEXT = goog.getMsg('Next period');
  next.setTooltip(MSG_NEXT);
  next.addClassName(goog.getCssName('goog-button-base-last'));
  tb.addChild(next, true);
  this.getHandler().listen(next, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.next();
  }, false, this);

  var dateRange = new goog.dom.createElement('span');
  goog.dom.setProperties(dateRange, {
    'id' : 'freebusyDateRange'
  });
  goog.dom.classes.add(dateRange, goog.getCssName('freebusyDateRange'));
  goog.dom.appendChild(tb.getContentElement(), dateRange);

  // South toolbar
  tb = new goog.ui.Toolbar();
  this.addChild(tb);
  tb.render(goog.dom.getElement('freebusy-south-toolbar'));

  /** @meaning calendar.freebusy.previous.autoPick */
  var MSG_PREVIOUS_AUTO = goog.getMsg('«');
  var autoPickPrev = new goog.ui.Button(MSG_PREVIOUS_AUTO, buttonRenderer);
  autoPickPrev.addClassName(goog.getCssName('goog-button-base-first'));
  tb.addChild(autoPickPrev, true);
  this.getHandler().listen(autoPickPrev, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.autoPickPrev_();
  }, false, this);
  /** @meaning calendar.freebusy.next.autoPick */
  var MSG_NEXT_AUTO = goog.getMsg('AutoPick next »');
  var autoPickNext = new goog.ui.Button(MSG_NEXT_AUTO, buttonRenderer);
  autoPickNext.addClassName(goog.getCssName('goog-button-base-last'));
  tb.addChild(autoPickNext, true);
  this.getHandler().listen(autoPickNext, goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    this.autoPickNext_();
  }, false, this);
};

/**
 * Init freebusy grid.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.initGrid = function() {
  var trDays = goog.dom.createDom('tr');
  var trHours = goog.dom.createDom('tr');
  var trAttendees = goog.dom.createDom('tr', {
    'id' : 'all-attendees'
  });
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
    var td = goog.dom.createDom('td', {
      'id' : 'day' + i,
      'colspan' : '48'
    });
    goog.dom.classes.add(td, cssDayLabel);
    goog.dom.appendChild(trDays, td);

    for (var j = 0; j < 24; j++) {
      var td = goog.dom.createDom('td', {
        'id' : 'day-' + i + '-hour-' + j,
        'colspan' : '2'
      }, j + ':00');

      goog.dom.classes.add(td, cssDayHourLabel);

      if (j == 0) {
        goog.dom.classes.add(td, cssDaySeparator);
      }

      goog.dom.appendChild(trHours, td);
    }

    for (var j = 0; j < 48; j++) {
      var td = goog.dom.createDom('td', {
        'id' : 'day-' + i + '-hour-' + j / 2 + '-all-attendees'
      });

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

  goog.dom.appendChild(goog.dom.getElement('freebusy-timeline-container'), trDays);

  goog.dom.appendChild(goog.dom.getElement('freebusy-timeline-container'), trHours);

  goog.dom.appendChild(goog.dom.getElement('freebusy-timeline-container'), trAttendees);

  this.dummyEvent_.decorate(goog.dom.getElement('fb-dummy-event'));
  this.updateGrid();
};

/**
 * Update grid labels
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.updateGrid = function() {

  if (goog.date.Date.compare(this.getModel().dtend, this.range.getStartDate()) > 0
      && goog.date.Date.compare(this.getModel().dtstart, this.range.getEndDate()) < 0) {
    this.dummyEvent_.show(true);
  } else {
    this.dummyEvent_.show(false);
  }

  var current = this.range.getStartDate().clone();
  while (goog.date.Date.compare(current, this.range.getEndDate()) < 0) {
    var lbl = goog.i18n.DateTimeSymbols.WEEKDAYS[(current.getDay()) % 7] + ' ' + current.getDate() + ' '
        + goog.i18n.DateTimeSymbols.MONTHS[current.getMonth()];
    goog.dom.setTextContent(goog.dom.getElement('day' + current.getDay()), lbl);
    current.add(new goog.date.Interval(0, 0, 1));
  }
  var df = new goog.i18n.DateTimeFormat(goog.i18n.DateTimeFormat.Format.MEDIUM_DATE);
  goog.dom.setTextContent(goog.dom.getElement('freebusyDateRange'), df.format(this.range.getStartDate()) + ' - '
      + df.format(this.range.getLastDate()));
};

/**
 * Update dummy event onclick
 * 
 * @param {goog.events.BrowserEvent} e browser event.
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.updateDummyEventOnMouseDown_ = function(e) {
  var target = e.target;
  if (goog.dom.classes.has(target, goog.getCssName('day-hour'))
      || goog.dom.classes.has(target, goog.getCssName('day-half-hour'))
      || goog.dom.classes.has(target, goog.getCssName('day-hour-label'))) {
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
 * Show Event details on mouse over
 * 
 * @param {goog.events.BrowserEvent} e browser event.
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.showEventDetails_ = function(e) {
  clearTimeout(this.timeout);
  var target = e.target;
  if (goog.dom.classes.has(target, goog.getCssName('Busy'))){

  // sample parent id = timeline-latd
    var parent = goog.dom.getParentElement(target);
    var parentId = parent.getAttribute('id');  
    
    var latd = parentId.substring(parentId.indexOf('-')+1);
    if (latd == null || latd == 'undefined'){
      latd = 'me';
    }
    
    if (latd == 'attendees'){
      return;
    }
    
    var userDir = this.userMapping[latd];
    var userUid = userDir.substring(userDir.lastIndexOf('/')+1);
    
  // sample target id = day-5-hour-15.5-calendar-undefined
    var timeIndicator = target.getAttribute('id');
    var splitted = timeIndicator.split('-');
    var day = splitted[1];
    var hour = splitted[3];
    var min = 0;
    if (hour.indexOf('.5') > 0){
      min = 30;
      hour = hour.split('.')[0];
    }
    
    var evtDate = this.range.getStartDate().clone();
    evtDate.add(new goog.date.Interval(goog.date.Interval.DAYS, day-1));
    var eventDate = new net.bluemind.date.DateTime(evtDate.getFullYear(), evtDate.getMonth(), evtDate.getDate(), hour, min);
    var eventDateEnd = new net.bluemind.date.DateTime(evtDate.getFullYear(), evtDate.getMonth(), evtDate.getDate(), hour, (min+30 > 59 ? 59 : min+30));
    
    var that = this;
    this.timeout = setTimeout(function(){ that.loadEventInfos(userUid, eventDate, eventDateEnd, e) }, 1000);
  }
}

net.bluemind.calendar.vevent.ui.Freebusy.prototype.loadEventInfos = function(userUid, eventDateStart, eventDateEnd, event) {
 this.ctx.service('calendars').search(userUid, '', [eventDateStart, eventDateEnd]).then(function(res) {
   if (res.length > 0){
   var popup = new net.bluemind.calendar.vevent.ui.EventDetailsPopup();

   var dateFormatter = this.ctx.helper('dateformat').formatter;
   var dateHelper = this.ctx.helper('date');
   goog.array.forEach(res, function(evt) {
      var date = null;
      var allday = evt['value']['main']['dtstart']['precision'] == 'Date';
      var start = dateHelper.fromIsoString(evt['value']['main']['dtstart']['iso8601']);
      var end = dateHelper.fromIsoString(evt['value']['main']['dtend']['iso8601']);

      if (!allday) {
        date = dateFormatter.time.format(start);
      } else {
        date = dateFormatter.date.format(start);
        end.add(new goog.date.Interval(0, 0, -1));
      }
      if (goog.date.isSameDay(start, end)) {
        if (!allday) {
          date += ' - ' + dateFormatter.time.format(end);
        }
      } else if (!allday) {
        date += ' - ' + dateFormatter.time.format(end);
      } else {
        date += ' - ' + dateFormatter.date.format(end);
      }
      evt.formattedDate = date;
   });

   res = goog.array.filter(res, function(evt) {
     return evt['value']['main']['transparency'] == 'Opaque';
   });

   popup.setModel(res);
   popup.setId('create-popup');
     this.addChild(popup, false);
     this.getChild('create-popup').attach(event);
     this.getChild('create-popup').render();
     this.getChild('create-popup').setVisible(true);
   }
 }, null, this);
 
};

net.bluemind.calendar.vevent.ui.Freebusy.prototype.hideEventDetails_ = function(e) {
  var popup = this.getChild('create-popup');
  if (popup){
    popup.setVisible(false);
    this.removeChild(popup, true);
  }
}

/**
 * Update dummy event on form update
 * 
 * @param {bluemind.date.Date} b date begin.
 * @param {bluemind.date.Date} e date end.
 * @param {boolean} f a boolean (don't know what).
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.updateDummyEventOnFormUpdate = function(b, e, f) {
  if (!this.lock_ && this.visible_) {
    if (!(goog.date.Date.compare(b, this.range.getStartDate()) > 0 && goog.date.Date
        .compare(b, this.range.getEndDate()) < 0)
        || f) {
      this.gotoDate(b);
    }
    this.dummyEvent_.updateDateBegin(b);
    if (this.getModel().states.allday) {
      this.dummyEvent_.setDuration(((e.getTime() - b.getTime()) / 1000));
    } else {
      this.dummyEvent_.setDuration((e.getTime() - b.getTime()) / 1000);
    }
  }
};

/**
 * Prev interval
 * 
 * @param {Object} opt_callback Optional callback.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.prev = function(opt_callback) {
  this.range.getStartDate().add(new goog.date.Interval(0, 0, -7));
  this.gotoDate(this.range.getStartDate(), opt_callback);
};

/**
 * Next interval
 * 
 * @param {Object} opt_callback Optional callback.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.next = function(opt_callback) {
  this.range.getStartDate().add(new goog.date.Interval(0, 0, 7));
  this.gotoDate(this.range.getStartDate(), opt_callback);
};

/**
 * Go to Today
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.today = function() {
  var d = new net.bluemind.date.DateTime();
  this.gotoDate(d);
};

/**
 * AutoPick Prev
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.autoPickPrev_ = function() {
  this.setLock_(true);
  var begin = this.getParent().getModel().dtstart;
  var step = 1;
  if (this.getModel().states.allday) {
    step = 48;
  }
  var start = this.dummyEvent_.getSlot(begin) - step;
  var free = false;

  while (!free && start >= 0) {
    free = this.isFreeInterval_(start, start + this.dummyEvent_.getDuration());
    if (!free) {
      start = start - step;
    }
  }
  if (free) {
    this.setLock_(false);
    var current = new net.bluemind.date.DateTime(this.range.getStartDate());
    current.add(new goog.date.Interval(goog.date.Interval.MINUTES, 30 * start));
    this.getParent().setDTStart(current);
    this.attempts_ = 0;
  } else {
    if (this.attempts_ < this.MAX_AUTOPICK_ATTEMPTS_) {
      this.attempts_++;
      this.prev(goog.bind(this.autoPickPrevWeek, this));
    } else {
      this.attempts_ = 0;
      /** @meaning calendar.freebusy.noSlot */
      var MSG_NO_SLOT = goog.getMsg('No free slots found. Do you want to search further?');
      if (confirm(MSG_NO_SLOT)) {
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
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.autoPickNext_ = function() {
  this.setLock_(true);
  var begin = this.getParent().getModel().dtstart;
  var step = 1;
  if (this.getModel().states.allday) {
    step = 48;
  }
  var start = this.dummyEvent_.getSlot(begin) + step;
  var free = false;
  while (!free && start <= 336 - this.dummyEvent_.getDuration()) {
    free = this.isFreeInterval_(start, start + this.dummyEvent_.getDuration());
    if (!free) {
      start = start + step;
    }
  }
  if (free) {
    this.setLock_(false);
    var current = new net.bluemind.date.DateTime(this.range.getStartDate());
    current.add(new goog.date.Interval(goog.date.Interval.MINUTES, 30 * start));
    this.getParent().setDTStart(current);
    this.attempts_ = 0;
  } else {
    if (this.attempts_ < this.MAX_AUTOPICK_ATTEMPTS_) {
      this.attempts_++;
      this.next(goog.bind(this.autoPickNextWeek, this));
    } else {
      this.attempts_ = 0;
      /** @meaning calendar.freebusy.noSlot */
      var MSG_NO_SLOT = goog.getMsg('No free slots found. Do you want to search further?');
      if (confirm(MSG_NO_SLOT)) {
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
net.bluemind.calendar.vevent.ui.Freebusy.prototype.autoPickPrevWeek = function() {
  var d = new net.bluemind.date.DateTime(this.range.getEndDate());
  d.add(new goog.date.Interval(goog.date.Interval.MINUTES, -30 * this.dummyEvent_.getDuration()));
  this.getParent().setDTStart(d);
  this.autoPickPrev_();
};

/**
 * AutoPick next free slot in the next week
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.autoPickNextWeek = function() {
  this.getParent().setDTStart(new net.bluemind.date.DateTime(this.range.getStartDate()));
  this.autoPickNext_();
};

/**
 * Change freebusy date
 * 
 * @param {goog.date.Date} date fb date.
 * @param {Object} opt_callback Optional callback.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.gotoDate = function(date, opt_callback) {

  goog.array.forEach(this.tooltips_.getKeys(), function(k) {
    this.tooltips_.set(k, new goog.structs.Map());
  }, this);

  var busySlots = goog.dom.getElementsByTagNameAndClass('div', goog.getCssName('slot'));
  for (var i = 0; i < busySlots.length; i++) {
    goog.dom.removeNode(busySlots[i]);
  }

  this.range = net.bluemind.date.DateRange.thisWeek(new net.bluemind.date.Date(date))
  this.updateGrid();
  var promises = [];
  goog.array.forEach(this.attendees_.getValues(), function(attendee) {
    var promise = this.freebusyRequest(attendee, this.range, true).then(function(slots) {
      this.setFreeBusy_(attendee, slots);
    }, function (){
      this.setFreeBusy_(attendee);
    }, this);
    promises.push(promise);
  }, this);

  goog.Promise.all(promises).then(function() {
    if (opt_callback) {
      opt_callback();
    }
    this.updateBusySlots();
  }, null, this);
  // FIXME
  // bluemind.calendar.Controller.getInstance().addFreebusyAttendees(
  // this, this.attendees_.getValues(), opt_callback);

};

/**
 * Change freebusy date
 * 
 * @param {goog.date.Date} date fb date.
 * @param {Object} opt_callback Optional callback.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.setFreeBusy_ = function(attendee, opt_slots) {
  var times = this.getDomHelper().getElement('timeline-' + attendee['mailto']).children;
  var start = times.item(0).id.match(/day-([^-])-.*/).pop() * 48;
  
  if (!opt_slots){
    for (var i = 0; i < times.length; i++) {
      goog.dom.classlist.enable(times[i], goog.getCssName('NotAvailable'), true);
    }
  } else {
    this.slots_.set(attendee['mailto'], opt_slots);
    for (var i = 0; i < opt_slots.length; i++) {
      var index = (i + start) % opt_slots.length;
      var slot = opt_slots[index];
      var time = times.item(i);
      if (!time) {
        continue;
      }
      // TODO css for BUSYTENTATIVE ?
      goog.dom.classlist.enable(time, goog.getCssName('Busy'), (slot == 'BUSY' || slot == 'BUSYTENTATIVE'));
      goog.dom.classlist.enable(time, goog.getCssName('outOfWorkFreeBusy'), slot == 'BUSYUNAVAILABLE');
      goog.dom.classlist.enable(time, goog.getCssName('NotAvailable'), slot == 'NOTAVAILABLE');
    }
  }
};

/**
 * Add an attendee.
 * 
 * @param {Object} a attendee.
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.addAttendee_ = function(a) {
  var email = a['mailto'];
  var dn = a['commonName'];
  
  if (!goog.isDefAndNotNull(email)){
    // handle my events without organizer
    this.userMapping['me'] = a['dir'];
  } else {
    this.userMapping[email] = a['dir'];  
  }

  if (!goog.dom.getElement('timeline-' + email)) {
    var att = goog.dom.createDom('tr', {
      'id' : 'label-' + email
    });
    var attDn = goog.dom.createDom('td', {}, dn);
    goog.dom.classes.add(attDn, goog.getCssName('attendee'));
    goog.dom.appendChild(att, attDn);
    goog.dom.appendChild(goog.dom.getElement('freebusy-attendees-container-tbody'), att);

    var timeline = goog.dom.createDom('tr', {
      'id' : 'timeline-' + email
    });

    var cssDaySeparator = goog.getCssName('day-separator');
    var cssDayHour = goog.getCssName('day-hour');
    var cssDayHalfHour = goog.getCssName('day-half-hour');

    var slots = new Array(336);
    var shortWeekDays = goog.i18n.DateTimeSymbols_en_ISO.SHORTWEEKDAYS;
    var busy = false;
    for (var d = 1; d <= 7; d++) {
      var i = d % 7;
      for (var j = 0; j < 48; j++) {
        var css = '';
        if (!this.lock_) {
          if (j % 2 == 0) {
            css += ' ' + cssDayHalfHour;
          } else {
            css += ' ' + cssDayHour;
          }
          if (j == 0) {
            css += ' ' + cssDaySeparator;
          }
          var timelinecontent = goog.dom.createDom('td', {
            'id' : 'day-' + i + '-hour-' + j / 2 + '-calendar-' + email,
            'class' : css
          });
          goog.dom.appendChild(timeline, timelinecontent);
        }
      }
      busy = false;
    }
    if (!this.lock_) {
      goog.dom.appendChild(goog.dom.getElement('freebusy-timeline-container'), timeline);
    }
    this.slots_.set(email + '', slots);
    this.tooltips_.set(email, new goog.structs.Map());
  }
  if (a['type'] == 'contact') {
    /** @meaning calendar.freebusy.noInformation */
    var MSG_NO_INFORMATION = goog.getMsg('No information');
    var width = goog.dom.getElement('freebusy-timeline-container').offsetWidth;
    var div = goog.dom.createDom('div', {
      'style' : 'width:' + width + 'px',
      'title' : MSG_NO_INFORMATION,
      'class' : goog.getCssName('slot') + ' ' + goog.getCssName('no-information')
    });
    goog.dom.appendChild(goog.dom.getElement('day-1-hour-0-calendar-' + email), div);
  }
  this.loveIE_();

  this.freebusyRequest(a, this.range, true).then(function(slots) {
    this.setFreeBusy_(a, slots);
    this.updateBusySlots();
  }, function (){
    this.setFreeBusy_(a);
  }, this);

};

/**
 * Add attendees
 * 
 * @param {Array} attendees attendees.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.addAttendees = function(attendees) {
  goog.array.forEach(attendees, function(a) {
    if (a['mailto'] && this.attendees_.containsKey(a['mailto'])) {
      return;
    }
    
    if (a['dir'] && this.attendees_.containsKey(a['dir'])) {
      return;
    }
    
    if( a['dir']) {
      this.attendees_.set(a['dir'], a);
      this.addAttendee_(a);
    } else if( a['mailto']) {
      this.attendees_.set(a['mailto'], a);
      this.addAttendee_(a);
    }
  }, this);
};

/**
 * Remove an attendee.
 * 
 * @param {Object} attendee.
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.removeAttendee = function(attendee) {
  var calendar = attendee['mailto'];
  goog.dom.removeNode(goog.dom.getElement('timeline-' + calendar));
  goog.dom.removeNode(goog.dom.getElement('label-' + calendar));
  this.attendees_.remove(calendar);
  this.attendees_.remove(attendee['dir']);
  this.slots_.remove(calendar);
  this.tooltips_.remove(calendar);

  var el = goog.dom.getElement('all-attendees-' + calendar);
  if (el) {
    var evts = goog.dom.getElementsByTagNameAndClass('div', 'all-attendees-' + calendar);
    for (var i = 0; i < evts.length; i++) {
      goog.dom.removeNode(evts[i]);
    }
  }

  this.updateBusySlots();
  this.loveIE_();
};

/**
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.prototype.loveIE_ = function() {
  if (goog.userAgent.IE) {
    var h = goog.style.getSize(goog.dom.getElement('freebusy-attendees-container')).height;
    goog.style.setHeight(goog.dom.getElement('freebusy-container'), h);
  }
};

/**
 * @param {net.bluemind.calendar.vevent.ui.Freebusy} fb Freebusy.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent = function(fb, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.unit_ = 21; // 20px (td witdh) + 1px (border)
  this.fb_ = fb;
  this.pick_ = false;
};
goog.inherits(net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent, goog.ui.Component);

/** @inheritDoc */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.decorateInternal = function(element) {
  this.setElementInternal(element);
};

/**
 * Freebusy
 * 
 * @type {net.bluemind.calendar.vevent.ui.Freebusy} fb_ freebusy.
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.fb_;

/**
 * Event drag
 * 
 * @type {bluemind.fx.HorizontalDragger} drag_ drag.
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.drag_;

/**
 * Grid unit
 * 
 * @type {number} one hour in px
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.unit_;

/**
 * Event resizer
 * 
 * @type {bluemind.fx.WidthResizer} resizeEnd_ resizer.
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.resizeEnd_;

/**
 * Pick action
 * 
 * @type {boolean} pick action.
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.pick_;

/**
 * duration
 * 
 * @type {number} duration in slot.
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.duration_;

/**
 * Get resizeEnd
 * 
 * @return {bluemind.fx.WidthResizer} resizeEnd_.
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.getResizeEnd = function() {
  return this.resizeEnd_;
};

/**
 * Pick action
 * 
 * @param {boolean} pick pick action.
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.setPick = function(pick) {
  this.pick_ = pick;
};

/**
 * Event resizer
 * 
 * @param {bluemind.fx.ReverseWidthResizer} resizeBegin_ resizer.
 * @private
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.resizeBegin_;

/** @inheritDoc */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.enterDocument = function() {
  net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.superClass_.enterDocument.call(this);

  var gridSize = new goog.math.Size(this.unit_, 0);
  var scrollTarget = goog.dom.getElement('freebusy-container');

  // Drag
  this.drag_ = new bluemind.fx.HorizontalDragger(this.element_);
  this.drag_.setScrollTarget(scrollTarget);
  this.drag_.setGrid(gridSize);

  this.getHandler().listen(this.drag_, bluemind.fx.Dragger.EventType.BEFORE_START, function(e) {
    var cwidth = goog.dom.getElement('freebusy-data-container').offsetWidth;
    var ewidth = this.element_.offsetWidth;
    this.drag_.setLimits(new goog.math.Rect(0, 0, cwidth - ewidth, 0));

    this.scroll_ = new goog.fx.DragScrollSupport(scrollTarget, 1);

  }, false, this);

  this.getHandler().listen(this.drag_, goog.fx.Dragger.EventType.END, function(e) {
    this.fb_.setLock_(true);
    var begin = Math.round(e.left / this.unit_);
    if (this.fb_.getModel().states.allday) {
      var datebegin = new net.bluemind.date.Date(this.fb_.range.getStartDate());
      datebegin.add(new goog.date.Interval(goog.date.Interval.DAYS, Math.round(begin / 48)));

    } else {
      var datebegin = new net.bluemind.date.DateTime(this.fb_.range.getStartDate());
      datebegin.add(new goog.date.Interval(goog.date.Interval.MINUTES, (begin * 30)));
    }
    this.fb_.getParent().setDTStart(datebegin);
    goog.dispose(this.scroll_);
    this.updateDateBegin(this.fb_.getModel().dtstart);

    this.fb_.setLock_(false);
  }, false, this);

  // End resizer
  this.resizeEnd_ = new bluemind.fx.WidthResizer(this.element_, goog.dom.getElement('fb-dummy-event-resizer-end'));
  this.resizeEnd_.setScrollTarget(scrollTarget);
  this.resizeEnd_.setGrid(gridSize);

  this.getHandler().listen(this.resizeEnd_, bluemind.fx.Dragger.EventType.BEFORE_START, function(e) {
    var cwidth = goog.dom.getElement('freebusy-data-container').offsetWidth;
    var elementOffsetLeft = this.element_.offsetLeft;
    this.resizeEnd_.setLimits(new goog.math.Rect(elementOffsetLeft, 0, cwidth - elementOffsetLeft, 0));

    this.scroll_ = new goog.fx.DragScrollSupport(scrollTarget, 1);

  }, false, this);

  this.getHandler().listen(
      this.resizeEnd_,
      goog.fx.Dragger.EventType.END,
      function(e) {
        this.fb_.setLock_(true);

        var elementOffsetLeft = this.element_.offsetLeft;

        if (this.pick_) {
          this.pick_ = false;
          var begin = Math.round(elementOffsetLeft / this.unit_);
          var datebegin = new net.bluemind.date.DateTime(this.fb_.range.getStartDate());
          datebegin.add(new goog.date.Interval(goog.date.Interval.MINUTES, (begin * 30)));
          this.fb_.getParent().setDTStart(datebegin);
        }

        var end = Math.round((elementOffsetLeft + this.element_.offsetWidth) / this.unit_);
        var dateend = new net.bluemind.date.DateTime(this.fb_.range.getStartDate());
        dateend.add(new goog.date.Interval(goog.date.Interval.MINUTES, (end * 30)));
        this.fb_.getParent().setDTEnd(dateend);
        goog.dispose(this.scroll_);

        this.setDuration((this.fb_.getParent().getModel().dtend.getTime() - this.fb_.getParent().getModel().dtstart
            .getTime()) / 1000);

        this.fb_.setLock_(false);
      }, false, this);

  this.getHandler().listen(goog.dom.getElement('fb-dummy-event-resizer-end'), goog.events.EventType.MOUSEDOWN,
      function(e) {
        e.stopPropagation();
      });

  this.getHandler().listen(goog.dom.getElement('fb-dummy-event-resizer-end'), goog.events.EventType.MOUSEUP,
      function(e) {
        goog.dispose(this.scroll_);
      });

  // Begin resizer
  this.resizeBegin_ = new bluemind.fx.ReverseWidthResizer(this.element_, goog.dom
      .getElement('fb-dummy-event-resizer-begin'));
  this.resizeBegin_.setScrollTarget(scrollTarget);
  this.resizeBegin_.setGrid(gridSize);

  this.getHandler().listen(
      this.resizeBegin_,
      bluemind.fx.Dragger.EventType.BEFORE_START,
      function(e) {
        this.resizeBegin_.setLimits(new goog.math.Rect(0, 0, this.element_.offsetLeft + this.element_.offsetWidth * 2
            - this.unit_, 0));

        this.scroll_ = new goog.fx.DragScrollSupport(scrollTarget, 1);

      }, false, this);

  this.getHandler().listen(this.resizeBegin_, goog.fx.Dragger.EventType.END, function(e) {
    this.fb_.setLock_(true);

    var elementOffsetLeft = this.element_.offsetLeft;

    var begin = Math.round(elementOffsetLeft / this.unit_);

    var datebegin = new net.bluemind.date.DateTime(this.fb_.range.getStartDate());
    datebegin.add(new goog.date.Interval(goog.date.Interval.MINUTES, (begin * 30)));

    goog.dispose(this.scroll_);

    this.setDuration((this.fb_.getParent().getModel().dtend.getTime() - datebegin.getTime()) / 1000);
    var dtend = this.fb_.getParent().getModel().dtend.clone();
    this.fb_.getParent().setDTStart(datebegin);
    this.fb_.getParent().setDTEnd(dtend);
    this.fb_.setLock_(false);
  });

  this.getHandler().listen(goog.dom.getElement('fb-dummy-event-resizer-begin'), goog.events.EventType.MOUSEDOWN,
      function(e) {
        e.stopPropagation();
      });

  this.getHandler().listen(goog.dom.getElement('fb-dummy-event-resizer-begin'), goog.events.EventType.MOUSEUP,
      function(e) {
        goog.dispose(this.scroll_);
      });

};

/**
 * Update evt position.
 * 
 * @param {number} left left position.
 * @param {boolean} scroll scroll flag.
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.updatePosition = function(left, scroll) {
  goog.style.setPosition(this.element_, left);
  if (scroll) {
    goog.dom.getElement('freebusy-container').scrollLeft = left - (4.5 * this.unit_);
  }
};

/**
 * Update evt position from datebegin
 * 
 * @param {net.bluemind.date.DateTime} d evt date begin.
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.updateDateBegin = function(d) {
  // %<-------- CRAPPY --------------------------%<
  var h = 0;
  if (d instanceof goog.date.DateTime) {
    h += d.getHours();
    if (d.getMinutes() == 30) {
      h += 0.5;
    } else if (d.getMinutes() > 30) {
      h++;
    }
  }
  // %<-------- CRAPPY --------------------------%<
  var id = 'day-' + d.getDay() + '-hour-' + h + '-all-attendees';
  var slot = goog.dom.getElement(id);
  this.updatePosition(slot.offsetLeft, true);
};

/**
 * Update evt witdh.
 * 
 * @param {number} duration dummy event duration.
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.setDuration = function(duration) {
  this.duration_ = (duration / 1800);
  goog.style.setWidth(this.element_, this.unit_ * (this.duration_) + 3);

};

/**
 * evt witdh.
 * 
 * @return {number} duration event duration.
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.getDuration = function() {
  return this.duration_;
};

/**
 * Show/Hide dummy event
 * 
 * @param {boolean} b show/hide flag.
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.show = function(b) {
  goog.style.showElement(this.element_, b);
};

/**
 * Get slot
 * 
 * @param {net.bluemind.date.DateTime} d date.
 * @return {number} slot number.
 */
net.bluemind.calendar.vevent.ui.Freebusy.DummyEvent.prototype.getSlot = function(d) {
  var day = d.getDay();
  var index = (day == 0) ? 6 : day - 1;

  var h = d.getHours ? d.getHours() : 0;
  h += d.getMinutes ? Math.round(d.getMinutes() / 30) / 2 : 1;

  var start = 48 * index + h * 2;
  return start;
};
