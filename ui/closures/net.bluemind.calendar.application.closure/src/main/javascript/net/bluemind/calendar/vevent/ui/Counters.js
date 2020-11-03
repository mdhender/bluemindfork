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

goog.provide('net.bluemind.calendar.vevent.ui.Counters');
goog.provide('net.bluemind.calendar.vevent.ui.Counters.DummyEvent');

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
goog.require('goog.ui.Select');
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
net.bluemind.calendar.vevent.ui.Counters = function(ctx, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.ctx = ctx;
  this.attendees_ = new goog.structs.Map();
  this.counters_ = new goog.structs.Map();
  this.dummyEvent_ = new net.bluemind.calendar.vevent.ui.Counters.DummyEvent(this);
  this.lock_ = false;
  this.slots_ = new goog.structs.Map();
  this.tooltips_ = new goog.structs.Map();
  this.tooltip_ = new goog.ui.Tooltip();
  this.attempts_ = 0;
  this.timeout = null;
  this.userMapping = {};
  this.visible_ = false;
};
goog.inherits(net.bluemind.calendar.vevent.ui.Counters, goog.ui.Component);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.ctx;

/**
 * @type {net.bluemind.date.DateRange}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.range;

/**
 * Attendees
 * 
 * @type {goog.structs.Map}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.attendees_;

/**
 * Attendees
 * 
 * @type {goog.structs.Map}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.counters_;

/**
 * email
 * 
 * @type {string}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.activeProposition_;

/**
 * initialDtstart
 * 
 * @type {string}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.initialDtstart;

/**
 * initialDtend
 * 
 * @type {string}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.initialDtend;

/**
 * Dummy event
 * 
 * @type {net.bluemind.calendar.vevent.ui.Counters.DummyEvent}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.dummyEvent_;

/**
 * Visibility flag
 * 
 * @type {boolean}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.visible_;

/**
 * Lock flag
 * 
 * @type {boolean}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.lock_;

/**
 * Freebusy slots
 * 
 * @type {goog.structs.Map}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.slots_;

/**
 * Freebusy tootips
 * 
 * @type {goog.structs.Map}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.tooltips_;

/**
 * Freebusy dom tootip
 * 
 * @type {goog.ui.Tooltip}
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.tooltip;

/**
 * Visibility flag
 * 
 * @return {boolean} visible.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.isVisible = function() {
  return this.visible_;
};

/**
 * Lock flag
 * 
 * @private
 * @return {boolean} lock.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.isLock_ = function() {
  return this.lock_;
};

/**
 * Set lock flag
 * 
 * @param {boolean} lock lock flag.
 * @private
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.setLock_ = function(lock) {
  this.lock_ = lock;
};

/**
 * Get one calendar slots
 * 
 * @param {number} id calendar id.
 * @return {Array} calendar slots.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.getCalendarSlots = function(id) {
  return this.slots_.get(id);
};

/**
 * Get one calendar tooltips
 * 
 * @param {number} id calendar id.
 * @return {Array} calendar tooltips.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.getCalendarTooltips = function(id) {
  return this.tooltips_.get(id);
};

/**
 * Calendar dom tooltip
 * 
 * @return {goog.ui.Tooltip} calendar tooltip.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.getTooltip = function() {
  return this.tooltip_;
};

/**
 * Update busy slots
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.updateBusySlots = function() {
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
    goog.dom.classlist.enable(time, goog.getCssName('counters-outOfWorkFreeBusy'),
        this.currentBusySlots_[i] == 'BusyUnavailable');
    goog.dom.classlist.enable(time, goog.getCssName('Busy'), this.currentBusySlots_[i] == 'Busy');
  }
  this.checkAvailability();
};

/**
 * check attendees availabilities. warn if one of attendees is unavailable
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.checkAvailability = function() {
  var parentModel = this.getParentModel_();
  var start = this.dummyEvent_.getSlot(parentModel.dtstart);
  var end = this.dummyEvent_.getSlot(parentModel.dtend);
  var available = true;
  this.getParent().availabilityWarn(this.isFreeInterval_(start, end));
};

/**
 * check if an interval is not busy
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.isFreeInterval_ = function(start, end) {
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
net.bluemind.calendar.vevent.ui.Counters.prototype.decorateInternal = function(element) {
  this.setElementInternal(element);
};

/** @inheritDoc */
net.bluemind.calendar.vevent.ui.Counters.prototype.enterDocument = function() {
  net.bluemind.calendar.vevent.ui.Counters.superClass_.enterDocument.call(this);
 
  this.getHandler().listen(goog.dom.getElement('counters-timeline-container'), goog.events.EventType.MOUSEOVER,
      this.showEventDetails_);
  
  this.getHandler().listen(goog.dom.getElement('counters-timeline-container'), goog.events.EventType.MOUSEOUT,
      this.hideEventDetails_);

  this.getHandler().listen(goog.dom.getElementByClass('bm-ui-form-counterselection'), goog.events.EventType.CHANGE, function(event) { 
    var selectedValue = goog.dom.getElementByClass('bm-ui-form-counterselection').value;
    if (selectedValue == 'reset'){
      this.activeProposition_ = 'reset';
      this.updateDummyEventOnFormUpdate(this.initialDtstart, this.initialDtend, true);
    } else {
      this.goToProposition_(selectedValue);
    }
  });
};



/**
 * Show/hide freebusy
 * 
 * @param {boolean} v visible flag.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.setVisible = function(v) {
  this.visible_ = v;
  goog.style.showElement(goog.dom.getElement('bm-ui-form-fieldset-counters'), v);

  if (v){
    var begin = new net.bluemind.date.DateTime(this.getModel().dtstart);
    var end = new net.bluemind.date.DateTime(this.getModel().dtend);

    this.updateDummyEventOnFormUpdate(begin, end, true);
    this.loveIE_();
  }
};

/**
 * Freebusy toolbar
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.initToolbar = function() {
  // North toolbar
  var tb = new goog.ui.Toolbar();
  this.addChild(tb);
  tb.render(goog.dom.getElement('counters-north-toolbar'));
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
    'id' : 'countersDateRange'
  });
  goog.dom.classes.add(dateRange, goog.getCssName('countersDateRange'));
  goog.dom.appendChild(tb.getContentElement(), dateRange);

  // South toolbar
  tb = new goog.ui.Toolbar();
  this.addChild(tb);
  tb.render(goog.dom.getElement('counters-south-toolbar'));

  /** @meaning calendar.counter.acceptProposition */
  var MSG_ACCEPT = goog.getMsg('Accept proposition');
  var accept = new goog.ui.Button(MSG_ACCEPT, buttonRenderer);
  accept.addClassName(goog.getCssName('counter-accept'));
  this.getHandler().listen(accept, goog.ui.Component.EventType.ACTION, function(e) {
    this.acceptProposition_();
  }, false, this);
  accept.setId('counterSelectionP');
  this.addChild(accept);
  this.getChild('counterSelectionP').render(goog.dom.getElement('counterSelectionP'));
};


/**
 * Accept selected proposition
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.acceptProposition_ = function(c) {
  if (this.activeProposition_ == 'reset'){
    this.getParent().applyCounterDates(this.initialDtstart, this.initialDtend);
  } else {
    var concernedCounter = this.counters_.get(this.activeProposition_);
    this.getParent().applyCounterDates(concernedCounter.dtstart, concernedCounter.dtend);
  }
  this.getParent().onAllDayChange_();
  if (!c){
    this.acceptProposition_(true);
  }
}

/**
 * Init freebusy grid.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.initGrid = function() {
  var trDays = goog.dom.createDom('tr');
  var trHours = goog.dom.createDom('tr');
  var trAttendees = goog.dom.createDom('tr', {
    'id' : 'counters-all-attendees'
  });
  goog.dom.classes.add(trAttendees, goog.getCssName('counters-all-attendees'));
  goog.dom.classes.add(trAttendees, goog.getCssName('counters-all-attendees-container'));

  var cssDayLabel = goog.getCssName('day-label');
  var cssDayHourLabel = goog.getCssName('day-hour-label');
  var cssDaySeparator = goog.getCssName('day-separator');
  var cssDayHour = goog.getCssName('day-hour');
  var cssDayHalfHour = goog.getCssName('day-half-hour');
  var cssDayHourContent = goog.getCssName('day-hour-content');

  for (var d = 1; d <= 7; d++) {
    var i = d % 7;
    var td = goog.dom.createDom('td', {
      'id' : 'counters-day' + i,
      'colspan' : '48'
    });
    goog.dom.classes.add(td, cssDayLabel);
    goog.dom.appendChild(trDays, td);

    for (var j = 0; j < 24; j++) {
      var td = goog.dom.createDom('td', {
        'id' : 'counters-day-' + i + '-hour-' + j,
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
        'id' : 'counters-day-' + i + '-hour-' + j / 2 + '-all-attendees'
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

  goog.dom.appendChild(goog.dom.getElement('counters-timeline-container'), trDays);

  goog.dom.appendChild(goog.dom.getElement('counters-timeline-container'), trHours);

  goog.dom.appendChild(goog.dom.getElement('counters-timeline-container'), trAttendees);

  this.dummyEvent_.decorate(goog.dom.getElement('counters-dummy-event'));

  this.updateGrid();
};

/**
 * Update grid labels
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.updateGrid = function() {

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
    goog.dom.setTextContent(goog.dom.getElement('counters-day' + current.getDay()), lbl);
    current.add(new goog.date.Interval(0, 0, 1));
  }
  var df = new goog.i18n.DateTimeFormat(goog.i18n.DateTimeFormat.Format.MEDIUM_DATE);
  goog.dom.setTextContent(goog.dom.getElement('countersDateRange'), df.format(this.range.getStartDate()) + ' - '
      + df.format(this.range.getLastDate()));
};

/**
 * Show Event details on mouse over
 * 
 * @param {goog.events.BrowserEvent} e browser event.
 * @private
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.showEventDetails_ = function(e) {
  clearTimeout(this.timeout);
  var target = e.target;
  if (goog.dom.classes.has(target, goog.getCssName('counters-Busy'))){

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

net.bluemind.calendar.vevent.ui.Counters.prototype.loadEventInfos = function(userUid, eventDateStart, eventDateEnd, event) {
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

net.bluemind.calendar.vevent.ui.Counters.prototype.hideEventDetails_ = function(e) {
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
net.bluemind.calendar.vevent.ui.Counters.prototype.updateDummyEventOnFormUpdate = function(b, e, f) {
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

    var dfd = new goog.i18n.DateTimeFormat(goog.i18n.DateTimeFormat.Format.MEDIUM_DATE);
    var dft = new goog.i18n.DateTimeFormat(goog.i18n.DateTimeFormat.Format.MEDIUM_TIME);
    
    if (b.date.getHours()){
      var evtDate = dfd.format(b) + ' ' + dft.format(b) + ' - ' + dfd.format(e) + ' ' + dft.format(e);
    } else {
      var endCloned = e.clone();
      endCloned.add(new goog.date.Interval(0, 0, -1));
      evtDate = dfd.format(b) + ' - ' + dfd.format(endCloned);
    }
    goog.dom.getElement("selected-counter-date").textContent=evtDate;
};

/**
 * Prev interval
 * 
 * @param {Object} opt_callback Optional callback.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.prev = function(opt_callback) {
  this.range.getStartDate().add(new goog.date.Interval(0, 0, -7));
  this.gotoDate(this.range.getStartDate(), opt_callback);
};

/**
 * Next interval
 * 
 * @param {Object} opt_callback Optional callback.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.next = function(opt_callback) {
  this.range.getStartDate().add(new goog.date.Interval(0, 0, 7));
  this.gotoDate(this.range.getStartDate(), opt_callback);
};

/**
 * Go to Today
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.today = function() {
  var d = new net.bluemind.date.DateTime();
  this.gotoDate(d);
};

/**
 * get the parent model
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.getParentModel_ = function() {
  return this.getParent().getModel().counter ? this.getParent().getModel().counter : this.getParent().getModel();
}

/**
 * Change freebusy date
 * 
 * @param {goog.date.Date} date fb date.
 * @param {Object} opt_callback Optional callback.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.gotoDate = function(date, opt_callback) {

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
};

/**
 * Change freebusy date
 * 
 * @param {goog.date.Date} date fb date.
 * @param {Object} opt_callback Optional callback.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.setFreeBusy_ = function(attendee, opt_slots) {
  var times = this.getDomHelper().getElement('counters-timeline-' + attendee['mailto']).children;
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
net.bluemind.calendar.vevent.ui.Counters.prototype.addAttendee_ = function(a) {
  var email = a['mailto'];
  var dn = a['commonName'];
  
  if (!goog.isDefAndNotNull(email)){
    // handle my events without organizer
    this.userMapping['me'] = a['dir'];
  } else {
    this.userMapping[email] = a['dir'];  
  }

  if (!goog.dom.getElement('counters-timeline-' + email)) {
    var att = goog.dom.createDom('tr', {
      'id' : 'counters-label-' + email
    });
    var attDn = goog.dom.createDom('td', {}, dn);
    goog.dom.classes.add(attDn, goog.getCssName('counters-attendee'));
    goog.dom.appendChild(att, attDn);
    goog.dom.appendChild(goog.dom.getElement('counters-attendees-container-tbody'), att);
    
    var timeline = goog.dom.createDom('tr', {
      'id' : 'counters-timeline-' + email
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
            'id' : 'counters-day-' + i + '-hour-' + j / 2 + '-calendar-' + email,
            'class' : css
          });
          goog.dom.appendChild(timeline, timelinecontent);
        }
      }
      busy = false;
    }
    goog.dom.appendChild(goog.dom.getElement('counters-timeline-container'), timeline);
    this.slots_.set(email + '', slots);
    this.tooltips_.set(email, new goog.structs.Map());
  }
  if (a['type'] == 'contact') {
    /** @meaning calendar.freebusy.noInformation */
    var MSG_NO_INFORMATION = goog.getMsg('No information');
    var width = goog.dom.getElement('counters-timeline-container').offsetWidth;
    var div = goog.dom.createDom('div', {
      'style' : 'width:' + width + 'px',
      'title' : MSG_NO_INFORMATION,
      'class' : goog.getCssName('slot') + ' ' + goog.getCssName('no-information')
    });
    goog.dom.appendChild(goog.dom.getElement('counters-day-1-hour-0-calendar-' + email), div);
  }
  this.loveIE_();

  this.freebusyRequest(a, this.range, true).then(function(slots) {
    this.setFreeBusy_(a, slots);
    this.updateBusySlots();
  }, function (){
    this.setFreeBusy_(a);
  }, this);

};

net.bluemind.calendar.vevent.ui.Counters.prototype.goToProposition_ = function(email){
  this.activeProposition_ = email;
  var concernedCounter = this.counters_.get(email);
  this.updateDummyEventOnFormUpdate(concernedCounter.dtstart, concernedCounter.dtend, true);
  goog.style.showElement(goog.dom.getElement('counters-dummy-event'), true);
}

/**
 * Add counters
 * 
 * @param {Array} counters.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.setCounters = function(counters) {
  /** @meaning calendar.event.eventdate */
  var MSG_EVENT = goog.getMsg('Event date');
  this.addOption_(MSG_EVENT, 'reset');
  this.activeProposition_ = 'reset';
  goog.array.forEach(counters, function(counter) {
    this.counters_.set(counter.originator.email, counter.counter);
    this.addOption_(counter.originator.commonName, counter.originator.email);
    this.addAttendees(counter.counter.attendees);
  }, this);
}

net.bluemind.calendar.vevent.ui.Counters.prototype.addOption_ = function(text, value) {
  var opt = goog.dom.createElement('option');
  opt.appendChild(document.createTextNode(text));
  opt.value = value; 
  goog.dom.getElementByClass('bm-ui-form-counterselection').appendChild(opt);
}

/**
 * Add attendees
 * 
 * @param {Array} counters.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.setAttendees = function(attendees) {
  var attendeesWithoutCounter = goog.array.filter(attendees, function(attendee) {
    return !this.counters_.containsKey(attendee.mailto);
  }, this);
  this.addAttendees(attendeesWithoutCounter);
}

/**
 * Add attendees
 * 
 * @param {Array} attendees attendees.
 */
net.bluemind.calendar.vevent.ui.Counters.prototype.addAttendees = function(attendees) {
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
net.bluemind.calendar.vevent.ui.Counters.prototype.removeAttendee = function(attendee) {
  var calendar = attendee['mailto'];
  goog.dom.removeNode(goog.dom.getElement('counters-timeline-' + calendar));
  goog.dom.removeNode(goog.dom.getElement('counters-label-' + calendar));
  this.attendees_.remove(calendar);
  this.attendees_.remove(attendee['dir']);
  this.slots_.remove(calendar);
  this.tooltips_.remove(calendar);

  var el = goog.dom.getElement('counters-all-attendees-' + calendar);
  if (el) {
    var evts = goog.dom.getElementsByTagNameAndClass('div', 'counters-all-attendees-' + calendar);
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
net.bluemind.calendar.vevent.ui.Counters.prototype.loveIE_ = function() {
  if (goog.userAgent.IE) {
    var h = goog.style.getSize(goog.dom.getElement('counters-attendees-container')).height;
    goog.style.setHeight(goog.dom.getElement('counters-container'), h);
  }
};

/**
 * @param {net.bluemind.calendar.vevent.ui.Counters} fb Freebusy.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent = function(fb, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.unit_ = 21; // 20px (td witdh) + 1px (border)
  this.fb_ = fb;
  this.pick_ = false;
};
goog.inherits(net.bluemind.calendar.vevent.ui.Counters.DummyEvent, goog.ui.Component);

/** @inheritDoc */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.decorateInternal = function(element) {
  this.setElementInternal(element);
};

/**
 * Freebusy
 * 
 * @type {net.bluemind.calendar.vevent.ui.Counters} fb_ freebusy.
 * @private
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.fb_;

/**
 * Event drag
 * 
 * @type {bluemind.fx.HorizontalDragger} drag_ drag.
 * @private
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.drag_;

/**
 * Grid unit
 * 
 * @type {number} one hour in px
 * @private
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.unit_;

/**
 * Event resizer
 * 
 * @type {bluemind.fx.WidthResizer} resizeEnd_ resizer.
 * @private
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.resizeEnd_;

/**
 * Pick action
 * 
 * @type {boolean} pick action.
 * @private
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.pick_;

/**
 * duration
 * 
 * @type {number} duration in slot.
 * @private
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.duration_;

/**
 * Get resizeEnd
 * 
 * @return {bluemind.fx.WidthResizer} resizeEnd_.
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.getResizeEnd = function() {
  return this.resizeEnd_;
};

/**
 * Pick action
 * 
 * @param {boolean} pick pick action.
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.setPick = function(pick) {
  this.pick_ = pick;
};

/**
 * Event resizer
 * 
 * @param {bluemind.fx.ReverseWidthResizer} resizeBegin_ resizer.
 * @private
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.resizeBegin_;

/** @inheritDoc */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.enterDocument = function() {
  net.bluemind.calendar.vevent.ui.Counters.DummyEvent.superClass_.enterDocument.call(this);

  var gridSize = new goog.math.Size(this.unit_, 0);
  var scrollTarget = goog.dom.getElement('counters-container');

};

/**
 * Update evt position.
 * 
 * @param {number} left left position.
 * @param {boolean} scroll scroll flag.
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.updatePosition = function(left, scroll) {
  goog.style.setPosition(this.element_, left);
  if (scroll) {
    goog.dom.getElement('counters-container').scrollLeft = left - (4.5 * this.unit_);
  }
};

/**
 * Update evt position from datebegin
 * 
 * @param {net.bluemind.date.DateTime} d evt date begin.
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.updateDateBegin = function(d) {
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
  var id = 'counters-day-' + d.getDay() + '-hour-' + h + '-all-attendees';
  var slot = goog.dom.getElement(id);
  this.updatePosition(slot.offsetLeft, true);
};

/**
 * Update evt witdh.
 * 
 * @param {number} duration dummy event duration.
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.setDuration = function(duration) {
  this.duration_ = (duration / 1800);
  goog.style.setWidth(this.element_, this.unit_ * (this.duration_) + 3);

};

/**
 * evt witdh.
 * 
 * @return {number} duration event duration.
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.getDuration = function() {
  return this.duration_;
};

/**
 * Show/Hide dummy event
 * 
 * @param {boolean} b show/hide flag.
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.show = function(b) {
  goog.style.showElement(this.element_, b);
};

/**
 * Get slot
 * 
 * @param {net.bluemind.date.DateTime} d date.
 * @return {number} slot number.
 */
net.bluemind.calendar.vevent.ui.Counters.DummyEvent.prototype.getSlot = function(d) {
  var day = d.getDay();
  var index = (day == 0) ? 6 : day - 1;

  var h = d.getHours ? d.getHours() : 0;
  h += d.getMinutes ? Math.round(d.getMinutes() / 30) / 2 : 1;

  var start = 48 * index + h * 2;
  return start;
};
