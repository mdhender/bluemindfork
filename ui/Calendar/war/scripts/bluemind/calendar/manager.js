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
 * @fileoverview Calendar manager.
 */

goog.provide('bluemind.calendar.Manager');
goog.provide('bluemind.calendar.manager.NumberPool');

goog.require('bluemind.calendar.model.Event');
goog.require('bluemind.calendar.model.EventHome');
goog.require('bluemind.calendar.model.EventStorageEventType');
goog.require('bluemind.calendar.template');
goog.require('bluemind.date.DateTime');
goog.require('goog.Uri.QueryData');
goog.require('goog.array');
goog.require('goog.date');
goog.require('goog.date.Date');
goog.require('goog.date.DateRange');
goog.require('goog.dom');
goog.require('goog.dom.xml');
goog.require('goog.events');
goog.require('goog.events.EventHandler');
goog.require('goog.math.Matrix');
goog.require('goog.object');
goog.require('goog.structs.Map');
goog.require('goog.structs.Set');
goog.require('goog.ui.Popup');

/**
 * BlueMind calendar.
 * @constructor
 */
bluemind.calendar.Manager = function() {
  this.dayCol_ = new Array();
  this.alldayEvents_ = new Array();
  this.events_ = new goog.structs.Map();
  this.eventsIndex_ = new goog.structs.Map();
  this.tagHandler_ = new goog.events.EventHandler();
  this.tagByLabel_ = new goog.structs.Map();
  this.hiddenTags_ = new goog.structs.Set();
};


bluemind.calendar.Manager.prototype.setup = function() {
  goog.events.listen(bluemind.calendar.model.EventHome.getInstance(),
    bluemind.calendar.model.EventStorageEventType.EVENT_UPDATE,
    this.onEventUpdate, false, this);

  goog.events.listen(bluemind.calendar.model.EventHome.getInstance(),
    bluemind.calendar.model.EventStorageEventType.MASS_EVENT_UPDATE,
    this.onMassEventUpdate, false, this);

  goog.events.listen(bluemind.calendar.model.EventHome.getInstance(),
    bluemind.calendar.model.EventStorageEventType.EVENT_REMOVE,
    this.onEventRemove, false, this);

  goog.events.listen(bluemind.calendar.model.EventHome.getInstance(),
    bluemind.calendar.model.EventStorageEventType.MASS_EVENT_REMOVE,
    this.onMassEventRemove, false, this);

  goog.events.listen(bluemind.calendar.model.EventHome.getInstance(),
    bluemind.calendar.model.EventStorageEventType.DOSYNC,
    this.onDoSync, false, this);

  this.today_ = new goog.date.Date();
  this.currentDate_ = this.today_.clone();
  this.visibleCalendars_ = new goog.structs.Map();
  this.calendars_ = new goog.structs.Map();
  this.calendarClassPool_ = new bluemind.calendar.manager.NumberPool(1, 21);
};

/**
 * @type {Array}
 * @private
 */
bluemind.calendar.Manager.prototype.dayCol_;

/**
 * @type {Array}
 * @private
 */
bluemind.calendar.Manager.prototype.alldayEvents_;

/**
 * Store all event by uid
 * @type {goog.structs.Map}
 * @private
 */
bluemind.calendar.Manager.prototype.events_;

/**
 * Alternative index for event map
 * @type {goog.structs.Map}
 * @private
 */
bluemind.calendar.Manager.prototype.eventsIndex_;

/**
 * Event to update or remove from view
 * @type {goog.structs.Map}
 * @private
 */
bluemind.calendar.Manager.prototype.pending_;

/**
 * @type {goog.events.EventHandler}
 * @private
 */
bluemind.calendar.Manager.prototype.tagHandler_;

/**
 * Flag indicating the current calendar view (days|month|agenda).
 * @type {string}
 * @private
 */
bluemind.calendar.Manager.prototype.currentView_ = 'days';


/**
 * Flag indicating the number of days to display.
 * @type {number}
 * @private
 */
bluemind.calendar.Manager.prototype.nbDays_ = 7;


/**
 * Today ! w00t
 * @type {goog.date.Date}
 * @private
 */
bluemind.calendar.Manager.prototype.today_;

/**
 * Current date
 * @type {goog.date.Date}
 * @private
 */
bluemind.calendar.Manager.prototype.currentDate_;


/**
 * next interval
 * @type {goog.date.Interval}
 * @private
 */
bluemind.calendar.Manager.prototype.nextInterval_ = null;


/**
 * Previous interval
 * @type {goog.date.Interval}
 * @private
 */
bluemind.calendar.Manager.prototype.prevInterval_ = null;

/**
 * Calendars to display.
 * @type {goog.structs.Map}
 * @private
 */
bluemind.calendar.Manager.prototype.calendars_;

/**
 * Calendars to display.
 * @type {goog.structs.Map}
 * @private
 */
bluemind.calendar.Manager.prototype.visibleCalendars_;

/**
 * Calendars to display.
 * FIXME: Should be a static var in calendar
 * @type {bluemind.calendar.manager.NumberPool}
 * @private
 */
bluemind.calendar.Manager.prototype.calendarClassPool_;

/**
 * Listeners
 *
 * @type {Array}
 * @private
 */
bluemind.calendar.Manager.prototype.handler_;

/**
 *
 */
bluemind.calendar.Manager.prototype.getCalendarClassPool = function() {
  return this.calendarClassPool_;
};

/**
 * get today !
 * @return {goog.date.Date} today.
 */
bluemind.calendar.Manager.prototype.getToday = function() {
  return this.today_;
};

/**
 * set today
 * @param {goog.date.Date} today.
 */
bluemind.calendar.Manager.prototype.setToday = function(today) {
  return this.today_ = today;
};

/**
 * get the current date
 * @return {goog.date.Date} current date.
 */
bluemind.calendar.Manager.prototype.getCurrentDate = function() {
  return this.currentDate_;
};


/**
 * set the current date
 * @param {goog.date.Date} date current date to set.
 */
bluemind.calendar.Manager.prototype.setCurrentDate = function(date) {
  this.currentDate_ = date;
};

/**
 * get the number of day
 * @return {number} number of days to display.
 */
bluemind.calendar.Manager.prototype.getNbDays = function() {
  return this.nbDays_;
};


/**
 * set the number of day
 * @param {number} days the number of days to set.
 */
bluemind.calendar.Manager.prototype.setNbDays = function(days) {
  this.nbDays_ = days;
};

/**
 * get next interval
 * @return {goog.date.Interval} next interval.
 */
bluemind.calendar.Manager.prototype.getNextInterval = function() {
  return this.nextInterval_;
};


/**
 * set next interval
 * @param {goog.date.Interval} interval the next interval.
 */
bluemind.calendar.Manager.prototype.setNextInterval = function(interval) {
  this.nextInterval_ = interval;
};


/**
 * set next interval
 * @return {goog.date.Interval} prev interval.
 */
bluemind.calendar.Manager.prototype.getPrevInterval = function() {
  return this.prevInterval_;
};


/**
 * set next interval
 * @param {goog.date.Interval} interval the prev interval.
 */
bluemind.calendar.Manager.prototype.setPrevInterval = function(interval) {
  this.prevInterval_ = interval;
};

/**
 * Get subscribed calendars.
 * @return {Array} .
 */
bluemind.calendar.Manager.prototype.getCalendars = function() {
  return this.calendars_.getValues();
};

/**
 * Get subscribed calendars ids.
 * @return {Array} .
 */
bluemind.calendar.Manager.prototype.getCalendarsIds = function() {
  return this.calendars_.getKeys();
};

/**
 * Get a calendar by id
 * @param {number} id Calendar id.
 * @return {bluemind.calendar.model.Calendar} .
 */
bluemind.calendar.Manager.prototype.getCalendar = function(id) {
  return this.calendars_.get(id);
};

/**
 * Return registered events.
 * @return {goog.structs.Map} events.
 */
bluemind.calendar.Manager.prototype.getRegisteredEvents = function() {
  return this.events_;
};

/**
 * Return visible calendars.
 * @return {Array} visible calendars.
 */
bluemind.calendar.Manager.prototype.getVisibleCalendars = function() {
  return this.visibleCalendars_;
};

/**
 * remove from visible calendars.
 * @param {integer} id.
 */
bluemind.calendar.Manager.prototype.removeFromVisibleCalendars = function(id) {
  return this.visibleCalendars_.remove(id);
};

/**
 * Register an event
 * @param {bluemind.calendar.model.Event} evt event.
 */
bluemind.calendar.Manager.prototype.register = function(evt) {
  if (this.isEventVisible(evt)) {
    bluemind.manager.events_.set(goog.getUid(evt), evt);
    var keys = bluemind.manager.eventsIndex_.get(evt.getExtId());
    if (!keys) keys = [];
    goog.array.insert(keys, goog.getUid(evt));
    bluemind.manager.eventsIndex_.set(evt.getExtId(), keys);
    bluemind.manager.addToCache_(evt);
  }
};

/**
 * Add an event to all dataset used to draw the view
 * @param {bluemind.calendar.model.Event} evt event.
 * @private
 */
bluemind.calendar.Manager.prototype.addToCache_ = function(evt) {
  var end = evt.getEnd().clone();
  end.add(new goog.date.Interval(0, 0, 0, 0, 0, -1));

  if (!evt.isAllday() && goog.date.isSameDay(evt.getDate(), end) &&
    bluemind.view.getView().getName() != 'month') {
    var start = evt.getDate().getHours() + evt.getDate().getMinutes() / 60;
    var duration = evt.getDuration() / 3600;
    var end = start + duration;
    var index = evt.getDay();
    var startSlot = Math.floor(start * 2);
    var endSlot = Math.ceil(end * 2);
    if (!bluemind.manager.dayCol_[index]) {
      bluemind.manager.dayCol_[index] = new Array();
    }
    for (var i = startSlot; i < endSlot; i++) {
      if (!bluemind.manager.dayCol_[index][i]) {
        bluemind.manager.dayCol_[index][i] = new Array();
      }
      bluemind.manager.dayCol_[index][i].push(evt);
    }
  } else {
    var evtBegin = evt.getDate().clone();
    var evtEnd = evt.getEnd().clone();

    var viewBegin = bluemind.view.getView().start_.clone();
    var viewEnd = bluemind.view.getView().end_.clone();
    if (bluemind.view.getView().getName() == 'month') {
      if (evt.startWeek != end.getWeekNumber() && !evt.multiweeks) {

        var firstTimeISeeYou =
          bluemind.utils.DateHelper.max(evtBegin, viewBegin);
        var lastTimeISeeYou = bluemind.utils.DateHelper.min(evtEnd, viewEnd);
        lastTimeISeeYou.add(
          new goog.date.Interval(goog.date.Interval.SECONDS, -1));

        var beginWeek = goog.date.DateRange.thisWeek(firstTimeISeeYou);
        var endWeek = goog.date.DateRange.thisWeek(lastTimeISeeYou);

        var dtstart = beginWeek.getStartDate();
        var dtend = endWeek.getEndDate();

        var interval = new goog.date.Interval(goog.date.Interval.DAYS, 7);
        var i = 0;
        if (goog.date.Date.compare(evtBegin, firstTimeISeeYou) < 0) {
          i = Math.ceil((firstTimeISeeYou.getTime() - evtBegin.getTime())/(1000 * 60 * 60 * 24)/7);
        }
        while (goog.date.Date.compare(dtstart, dtend) < 0) {
          var child = evt.giveBirth();
          child.startWeek = dtstart.getWeekNumber();
          child.multiweeks = true;
          child.d = i++;
          bluemind.manager.addToCache_(child);
          dtstart.add(interval);
        }

        return;

      }

      viewBegin = evtBegin.clone();
      viewBegin.setHours(0);
      viewBegin.setMinutes(0);
      viewBegin.setSeconds(0);
      var diff = viewBegin.getWeekday();
      viewBegin.add(new goog.date.Interval(0, 0, (evt.d * 7) - diff));
      viewEnd = viewBegin.clone();
      viewEnd.add(new goog.date.Interval(0, 0, 7));
    }

    if (goog.date.Date.compare(evtEnd, viewEnd) > 0) {
      evtEnd = viewEnd;
      evt.addRightExtension();
    }
    if (goog.date.Date.compare(evtBegin, viewBegin) < 0) {
      evtBegin = viewBegin;
      evt.addLeftExtension();
      evt.startDay = viewBegin.getDay();
      evt.startWeek = viewBegin.getWeekNumber();
    }

    var tempDate = new goog.date.Date(evtBegin);
    var oneDayInterval = new goog.date.Interval(goog.date.Interval.DAYS, 1);
    tempDate.add(oneDayInterval);
    evt.alldaySize = 1;
    while (goog.date.Date.compare(tempDate, evtEnd) < 0) {
      tempDate.add(oneDayInterval);
      evt.alldaySize++;
    }
    var weekNum = evt.startWeek;
    if (bluemind.view.getView().getName() == 'days' &&
      bluemind.manager.getNbDays() == 1) {
      weekNum = bluemind.manager.getCurrentDate().getWeekNumber();
      evt.startWeek = weekNum;
    }

    if (!bluemind.manager.alldayEvents_[weekNum]) {
      bluemind.manager.alldayEvents_[weekNum] = new Array();
    }
    bluemind.manager.alldayEvents_[weekNum].push(evt);

  }
};

/**
 * Re calculate dataset used to draw the grid (should be follow by a redraw)
 * @private
 */
bluemind.calendar.Manager.prototype.refreshCache_ = function() {
  bluemind.manager.resetCache_();
  var events = bluemind.manager.events_.getValues();
  for (var i = 0; i < events.length; i++) {
    var evt = events[i];
    evt.infanticide();
    bluemind.manager.addToCache_(evt);
  }
};

/**
 * Erase all dataset used to draw the grid
 * @private
 */
bluemind.calendar.Manager.prototype.resetCache_ = function() {
  bluemind.manager.dayCol_ = new Array();
  bluemind.manager.alldayEvents_ = new Array();
};

/**
 * Unregister an event
 * @param {bluemind.calendar.Manager.Event} evt calendar event.
 */
bluemind.calendar.Manager.prototype.unregister = function(evt) {
  bluemind.manager.events_.remove(goog.getUid(evt));
  var keys = bluemind.manager.eventsIndex_.get(evt.getExtId());
  goog.array.remove(keys, goog.getUid(evt));
  if (keys.length == 0) {
    bluemind.manager.eventsIndex_.remove(evt.getExtId());
  } else {
    bluemind.manager.eventsIndex_.set(evt.getExtId(), keys);
  }
  bluemind.manager.refreshCache_();
};


/**
 * Clear this.dayCol_
 * Remove events from DOM
 */
bluemind.calendar.Manager.prototype.clear = function() {
  bluemind.manager.resetCache_();
  bluemind.manager.resetView_();
  bluemind.manager.events_ = new goog.structs.Map();
  bluemind.manager.eventsIndex_ = new goog.structs.Map();
};

/**
 * Reset view
 * @private
 */
bluemind.calendar.Manager.prototype.resetView_ = function() {
  var events = bluemind.manager.events_.getValues();
  for (var i = 0; i < events.length; i++) {
    var evt = events[i];
    evt.dispose();
    evt = null;
  }

  var alldayContainer =
    goog.dom.getElementsByTagNameAndClass('tr',
    goog.getCssName('containsAllday'));
  for (var i = 0; i < alldayContainer.length; i++) {
    goog.dom.removeNode(alldayContainer[i]);
  }

  var alldayCollapsed =
    goog.dom.getElementsByTagNameAndClass('tr',
    goog.getCssName('containsAlldayCollapsed'));
  for (var i = 0; i < alldayCollapsed.length; i++) {
    goog.dom.removeNode(alldayCollapsed[i]);
  }
};

/**
 * Refresh view
 */
bluemind.calendar.Manager.prototype.refreshView = function() {
  if (bluemind.view.getView() != null) {
    bluemind.manager.getHandler().removeAll()
    switch (bluemind.view.getView().getName()) {
      case 'days':
      case 'month':
        if (bluemind.manager.lockUI()) {
          bluemind.manager.refreshCache_();
          bluemind.manager.resetView_();
          bluemind.manager.redrawAllday();
          bluemind.manager.redraw();
          bluemind.resize();
          bluemind.manager.unlockUI();
        }
        break;
      case 'agenda':
        bluemind.view.getView().display();
        break;
      default:
    }
  }
};

/**
 * Load a calendar view
 * @param {bluemind.calendar.model.CalendarView} cv calendar view.
 */
bluemind.calendar.Manager.prototype.loadView = function(cv) {
  goog.object.forEach(this.calendars_.getValues(), function(c) {
    c.removeDom();
  }, this);

  this.calendars_.clear();
  this.visibleCalendars_.clear();
  this.calendarClassPool_.releaseObjects();

  var storage = bluemind.storage.StorageHelper.getWebStorage();
  var key = 'calendars';
  var list = storage.get(key);
  if (!list) list = [];
  goog.array.forEach(list, function(c) {
    storage.remove('calendar-' + c);
  });

  goog.array.forEach(cv.getCalendars(), function(cal) {
    this.registerCalendar(cal);
  }, this);

  bluemind.cookies.set('visibleCalendars', bluemind.manager.visibleCalendars_.getKeys());

  var props = cv.getProperties();
  switch (props.get('view')) {
    case 'days':
      bluemind.view.day();
      break;
    case 'week':
      bluemind.view.week();
      break;
    case 'month':
      bluemind.view.month();
      break;
    case 'agenda':
      bluemind.view.agenda();
      break;
    default:
      bluemind.view.week();
  }
  bluemind.sync.SyncEngine.getInstance().execute();
};

/**
 * Lock for ui concurrent redraw
 * @return {boolean} is ui locked.
 */
bluemind.calendar.Manager.prototype.lockUI = function() {
  if (!bluemind.manager.lock) {
    bluemind.manager.lock = true;
  }
  return bluemind.manager.lock;
};


/**
 * Unlock ui redraw
 */
bluemind.calendar.Manager.prototype.unlockUI = function() {
  bluemind.manager.lock = false;
};

/**
 * redraw events (conflict)
 */
bluemind.calendar.Manager.prototype.redraw = function() {
  var updated = new Object();
  goog.array.forEach(bluemind.manager.dayCol_, function(day, v) {
    var unit;
    goog.array.forEach(day, function(cell, key) {
      cell.sort(function(evt1, evt2) {
        var diff = evt1.getDate().getTime() - evt2.getDate().getTime();
        if (diff != 0) return diff;
        var diff = evt2.getDuration() - evt1.getDuration();
        if (diff != 0) return diff;
        var diff = evt1.getKlass() - evt2.getKlass();
        if (diff != 0) return diff;
        return goog.getUid(evt1) - goog.getUid(evt2);
      });
      var usedPositions = new Object();
      var position = 0;

      goog.array.forEach(cell, function(evt, idx) {
        var updatedId = goog.getUid(evt);
        var coords;
        if (!(coords = goog.object.get(updated, updatedId))) {
          if (goog.object.isEmpty(usedPositions)) unit = {'value' : 1};
          while (goog.object.containsKey(usedPositions, position)) {
            position++;
          }
          var end = {'value' : position};
          if ((idx + 1) == cell.length) {
            while (end['value'] < unit['value'] &&
              !goog.object.containsKey(usedPositions, end['value'])) {
              end['value']++;
            }
          }
          if (end['value'] == unit['value']) end = unit;
          coords =
            {'position': position, 'unit': unit, 'end': end, 'occurrence': evt};
          goog.object.add(updated, updatedId, coords);
        }
        goog.object.set(usedPositions, coords['position'], true);
        if (cell.length > unit['value']) unit['value'] = cell.length;
        if ((coords['position'] + 1) < cell.length && (idx + 1) < cell.length) {
          coords['end'] = {'value' : coords['position'] + 1};
        }
      });
    });
  });

  goog.object.forEach(updated, function(coords) {
    var size = coords['end']['value'] - coords['position'];
    var e = coords['occurrence'];
    e.createDom(coords['unit']['value'], coords['position'], size);
  });
};


/**
 * redraw allday events
 */
bluemind.calendar.Manager.prototype.redrawAllday = function() {
  if (bluemind.manager.alldayEvents_.length > 0) {
    var nbDays = 7; //FIXME: can't display more than 7 days in a week
    var cssDisabled = goog.getCssName('disabled');
    var cssContainsAllday = goog.getCssName('containsAllday');
    var cssSpacer = goog.getCssName('spacer');
    var cssContainsAlldayCollapsed = goog.getCssName('containsAlldayCollapsed');
    var cssAlldayCollapsed = goog.getCssName('alldayCollapsed');
    var oneDayInterval = new goog.date.Interval(0, 0, 1);
    var events = new Array();
    var tad = goog.dom.getElement('toggleAllday');

    var dayIndex = 0;
    goog.array.forEach(bluemind.manager.alldayEvents_, function(evts, weeknum) {
      var nbEventsPerDay = new Array();
      for (var i = 0; i < nbDays; i++) {
        nbEventsPerDay[i] = 0;
      }
      var trs = new goog.math.Matrix(1, nbDays);

      evts.sort(function(evt1, evt2) {
        var d1 = evt1.getDate().clone();
        var d2 = evt2.getDate().clone();
        if (evt1.isAllday() || evt1.alldaySize > 1) {
          d1.setHours(0);
        }
        if (evt2.isAllday() || evt2.alldaySize > 1) {
          d2.setHours(0);
        }
        var diff = d1.getTime() - d2.getTime();
        if (diff != 0) return diff;
        var diff = evt2.getDuration() - evt1.getDuration();
        if (diff != 0) return diff;
        var diff = evt1.getKlass() - evt2.getKlass();
        if (diff != 0) return diff;
        return goog.getUid(evt1) - goog.getUid(evt2);
      });

      goog.array.forEach(evts, function(evt) {
        var coords = {'row': 0, 'evt': evt};
        var evtPosition = new goog.math.Matrix(1, nbDays);
        var evtInterval = new goog.date.Interval(0, 0, - evt.getDay() + evt.startDay);

        var begin = evt.getDate().clone();
        begin.add(evtInterval);
        for (var i = 0; i < evt.alldaySize; i++) {
          var idx = begin.getDay();
          evtPosition.setValueAt(0, idx, goog.getUid(evt));
          begin.add(oneDayInterval);
          nbEventsPerDay[idx]++;
        }

        var size = trs.getSize().height;

        var isFree = true;
        var cur = 0;
        for (var i = 0; i < size; i++) {
          isFree = true;
          for (var j = 0; j < nbDays; j++) {
            if (evtPosition.getValueAt(0, j) && trs.getValueAt(i, j)) {
              isFree = false;
              break;
            }
          }
          if (isFree) {
            cur = i;
            break;
          }
        }

        if (!isFree) {
          // add a new row
          trs = trs.appendRows(evtPosition);
          coords['row'] = size;
          var begin = evt.getDate().clone();
          begin.add(evtInterval);
          for (var i = 0; i < evt.alldaySize; i++) {
            var idx = begin.getDay();
            begin.add(oneDayInterval);
            nbEventsPerDay[idx] = size + 1;
          }
        } else {
          // add event to "free" row
          var begin = evt.getDate().clone();
          begin.add(evtInterval);
          for (var i = 0; i < evt.alldaySize; i++) {
            var idx = begin.getDay();
            trs.setValueAt(cur, idx, goog.getUid(evt));
            begin.add(oneDayInterval);
            nbEventsPerDay[idx] = Math.max(cur + 1, nbEventsPerDay[idx]);
          }
          coords['row'] = cur;
        }
        events.push(coords);
      });


      if (bluemind.view.getView().getName() != 'month' &&
        (trs.getSize().height > 1 ||
        bluemind.cookies.get('isAlldayExpand') == 'true')) {
        goog.dom.classes.remove(tad, cssDisabled);
      }

      var trId = 'weekBgTr_' + weeknum;
      var tr = goog.dom.getElement(trId);
      if (tr) {
        var delta = 0;
        if (bluemind.view.getView().getName() == 'month') {
          var table = goog.dom.getParentElement(tr);
          var id = table.id.split('_');
          delta = parseInt(id[1]);
        }

        for (var idx = 0; idx < trs.getSize().height; idx++) {
          var ctr = tr.cloneNode(true);
          goog.dom.classes.add(ctr, cssContainsAllday);
          goog.dom.classes.remove(ctr, cssSpacer);
          goog.dom.setProperties(ctr, {'id': trId + '_' + idx});
          goog.dom.insertSiblingBefore(ctr, tr);
          var children = goog.dom.getChildren(ctr);
          for (var i = 0; i < children.length; i++) {
            var td = children[i];
            goog.dom.setProperties(td, {'id': td.id + '_' + idx});
          }
        }

        // Collapsed tr
        var ctr = tr.cloneNode(true);
        goog.dom.classes.add(ctr, cssContainsAlldayCollapsed + ' ' + cssDisabled);
        goog.dom.classes.remove(ctr, cssSpacer);
        goog.dom.setProperties(ctr, {'id': 'weekBgTrCollapsed_' + weeknum});
        goog.dom.insertSiblingBefore(ctr, tr);
        var children = goog.dom.getChildren(ctr);
        for (var i = 0; i < children.length; i++) {
          var td = children[i];
          goog.dom.setProperties(td, {'id': td.id + '_collapsed'});
          var idx = td.id.split('_')[2];

          var nbEvents = parseInt(nbEventsPerDay[idx]);
          if (nbEvents > 0) {
            var collapsed = goog.dom.createDom('div', {
              'id': 'container_' + (delta * 7 + i),
              'class': cssAlldayCollapsed
            }, '' + nbEvents);

            goog.dom.appendChild(td, collapsed);
            bluemind.manager.getHandler().listen(collapsed, goog.events.EventType.MOUSEDOWN,
              function(e) {
                e.stopPropagation();
                bluemind.calendar.Controller.getInstance().allDayEventList(e.currentTarget);
              }, false, collapsed);
          }
        }
      }

      if (bluemind.view.getView().getName() != 'month' &&
        bluemind.cookies.get('isAlldayExpand') == 'true') {
        bluemind.view.getView().toggleAllday();
      }
    });
    goog.array.forEach(events, function(coords) {
      var evt = coords['evt'];
      evt.createAlldayDom(coords['row']);
    });
  }
};

/**
 *
 */
bluemind.calendar.Manager.prototype.getHandler = function() {
  return this.handler_ ||
         (this.handler_ = new goog.events.EventHandler(this));
};

/**
 * Go to the next date interval
 */
bluemind.calendar.Manager.prototype.next = function() {
  this.currentDate_.add(this.nextInterval_);
};

/**
 * Go to the previous date interval
 */
bluemind.calendar.Manager.prototype.prev = function() {
  this.currentDate_.add(this.prevInterval_);
};

/**
 * get events.
 *
 * @param {goog.date.Date} start Start date.
 * @param {goog.date.Date} end End date.
 *
 */
bluemind.calendar.Manager.prototype.getEvents = function(start, end) {
  var users = bluemind.manager.visibleCalendars_.getKeys();
  var filter = false;
  var d = new goog.async.Deferred();
  if (users.length > 0) {
    return bluemind.calendar.model.EventHome.getInstance()
        .getEventsByPeriod(start, end, users);
  } else {
    d.callback([]);
  }
  return d;
};

/**
 * Register a calendar
 * @param {bluemind.calendar.model.Calendar} cal calendar.
 */
bluemind.calendar.Manager.prototype.registerCalendar = function(cal) {
  if (!cal.getClass()) {
    cal.setClass(this.calendarClassPool_.getObject());
  } else {
    this.calendarClassPool_.registerObject(cal.getClass());
  }
  this.calendars_.set(parseInt(cal.getId()), cal);
  this.visibleCalendars_.set(parseInt(cal.getId()), cal);
  cal.createDom();

  // Store in localstorage
  var storage = bluemind.storage.StorageHelper.getWebStorage();
  var key = 'calendars';
  var list = storage.get(key);
  if (!list) list = [];
  goog.array.insert(list, parseInt(cal.getId()));
  storage.set(key, list);
  storage.set('calendar-' + cal.getId(), cal.toMap().toObject());
  bluemind.cookies.set('visibleCalendars',
    bluemind.manager.visibleCalendars_.getKeys());
};

/**
 * Unregister a calendar
 * @param {Object} evt browser event.
 */
bluemind.calendar.Manager.prototype.unregisterCalendar = function(evt) {
  bluemind.manager.visibleCalendars_.remove(this.getId());
  bluemind.manager.calendarClassPool_.releaseObject(this.getClass());
  bluemind.manager.calendars_.remove(this.getId());
  this.removeDom();

  bluemind.view.getView().display();
  evt.stopPropagation();

  // Remove from localstorage
  var storage = bluemind.storage.StorageHelper.getWebStorage();
  var key = 'calendars';
  var list = storage.get(key);
  if (!list) list = [];
  goog.array.remove(list, parseInt(this.getId()));
  storage.set(key, list);
  storage.remove('calendar-' + this.getId());
  bluemind.cookies.set('visibleCalendars',
    bluemind.manager.visibleCalendars_.getKeys());

  bluemind.pendingNotification.update();
};

/**
 * Toogle calendar visibility
 * @param {Object} evt event.
 */
bluemind.calendar.Manager.prototype.toggleCalendarVisibility = function(evt) {
  this.toggleVisibility();

  if (this.isVisible()) {
    bluemind.manager.visibleCalendars_.set(parseInt(this.getId()), this);
  } else {
    bluemind.manager.visibleCalendars_.remove(this.getId());
  }

  bluemind.cookies.set('visibleCalendars',
    bluemind.manager.visibleCalendars_.getKeys());

  bluemind.view.getView().display();
  evt.stopPropagation();
};


/**
 * Register a calendar view
 * @param {bluemind.calendar.model.CalendarView} cv calendar view.
 */
bluemind.calendar.Manager.prototype.registerCalendarView = function(cv) {
  var menu = bluemind.view.getViewSelector().getMenu();
  bluemind.savedViews.set(cv.getId(), cv);
  var menuitem = new goog.ui.MenuItem(cv.getLabel(), cv);
  menu.addChild(menuitem, true);

  goog.events.listen(menuitem.getElement(),
    goog.events.EventType.CLICK, function(e) {
    e.stopPropagation();
    bluemind.view.getViewSelector().setSelected(menuitem);
    this.loadView(cv);
  }, false, this);
};

/**
 * Unegister a calendar view
 * @param {goog.ui.MenuItem} item menuitem calendar view.
 */
bluemind.calendar.Manager.prototype.unregisterCalendarView = function(item) {
  bluemind.savedViews.remove(item.getModel().getId());
  item.dispose();
  bluemind.view.getViewSelector().refreshViewSelector();
};

/**
 * On event update, refresh view
 * @param {bluemind.storage.StorageEvent} e Storage event.
 */
bluemind.calendar.Manager.prototype.onEventUpdate = function(e) {
  var eh = bluemind.calendar.model.EventHome.getInstance();
  var vstart = bluemind.view.getView().getStart();
  var vend = bluemind.view.getView().getEnd();
  var calendars = bluemind.manager.visibleCalendars_.getKeys();
  eh.getEventOccurrencesByPeriod(e.stored, vstart, vend, calendars).addCallback(function(ocs) {
    this.purgeEvent_(e.stored);
    for (var i = 0; i < ocs.length; i++) {
      this.register(ocs[i]);
    }
    this.refreshView();
    bluemind.sync.SyncEngine.getInstance().execute();
  }, this);
};

/**
 * On event update, refresh view
 * @param {bluemind.storage.StorageEvent} evts Storage event.
 */
bluemind.calendar.Manager.prototype.onMassEventUpdate = function(evts) {
  var vstart = bluemind.view.getView().getStart();
  var vend = bluemind.view.getView().getEnd();

  var stored = evts.stored;
  var eh = bluemind.calendar.model.EventHome.getInstance();
  var vstart = bluemind.view.getView().getStart();
  var vend = bluemind.view.getView().getEnd();
  var calendars = bluemind.manager.visibleCalendars_.getKeys();
  var deferreds = [];

  goog.array.forEach(stored, function(e) {
    var def = eh.getEventOccurrencesByPeriod(e, vstart, vend, calendars);
    deferreds.push(def);
  });

  new goog.async.DeferredList(deferreds).addCallback(function(results) {
    goog.array.forEach(stored, function(e) {
      this.purgeEvent_(e);
    }, this);

    goog.array.forEach(results, function(res) {
      goog.array.forEach(res[1], function(o) {
        this.register(o);
      }, this);
    }, this);
    this.refreshView();
  }, this);
};

/**
 * On event delete, refresh view
 * @param {bluemind.storage.StorageEvent} e Storage event.
 */
bluemind.calendar.Manager.prototype.onEventRemove = function(e) {
  this.purgeEvent_(e.stored);
  this.refreshView();
  bluemind.sync.SyncEngine.getInstance().execute();
};

/**
 * On event delete, refresh view
 * @param {bluemind.storage.StorageEvent} evts Storage event.
 */
bluemind.calendar.Manager.prototype.onMassEventRemove = function(evts) {
  var stored = evts.stored;
  if (stored.length > 0) {
    goog.array.forEach(stored, function(e) {
      this.purgeEvent_(e);
    }, this);
    this.refreshView();
  }
};

/**
 * On do sync, refresh view
 * @param {bluemind.storage.StorageEvent} evts Storage event.
 */
bluemind.calendar.Manager.prototype.onDoSync = function(evts) {
  if ('pending' == bluemind.view.getView().getName()) {
    bluemind.view.pending();
  }

  var updated = evts.stored.updated || [];
  var deleted = evts.stored.removed || [];
  if (updated.length > 0 || deleted.length > 0) {
    var purge = goog.array.concat(deleted, updated);

    var eh = bluemind.calendar.model.EventHome.getInstance();
    var vstart = bluemind.view.getView().getStart();
    var vend = bluemind.view.getView().getEnd();
    var cals = bluemind.manager.visibleCalendars_.getKeys();

    return eh.getEventsByPeriod(vstart, vend, cals).addCallback(function(evts) {
      purge = goog.array.concat(purge, evts);
      goog.array.forEach(purge, function(e) {
        this.purgeEvent_(e);
      }, this);
      // Double forEach because we need to purge and register in separeted loops
      goog.array.forEach(evts, function(e) {
        this.register(e);
      }, this);
      this.refreshView();
    }, this);
  } else {
    bluemind.resize();
  }
  updated = null;
  deleted = null;
};

/**
 * Unregister all event occurrences
 * @param {bluemind.calendar.Manager.Event} evt calendar event.
 * @private
 */
bluemind.calendar.Manager.prototype.purgeEvent_ = function(evt) {
  var keys = bluemind.manager.eventsIndex_.get(evt.getExtId());
  if (!keys) keys = [];
  for (var i = 0; i < keys.length; i++) {
    var e = bluemind.manager.events_.get(keys[i]);
    e.dispose();
    bluemind.manager.events_.remove(keys[i]);
  }
  bluemind.manager.eventsIndex_.remove(evt.getExtId());
};

/**
 * @public
 */
bluemind.calendar.Manager.prototype.clearTags = function() {
  this.tagByLabel_.clear();
};

/**
 * @public
 */
bluemind.calendar.Manager.prototype.addTag = function(tag) {
  var t = this.tagByLabel_.get(tag.getLabel());
  if (t == null) {
    this.tagByLabel_.set(tag.getLabel(), tag);
    t = tag;
  } else {
    t.setId(tag.getId());
    t.setColor(tag.getColor());
  }
  return t;
};

/**
 * @protected
 * @return {num} The created object.
 */
bluemind.calendar.Manager.prototype.getTag = function(label) {
  return this.tagByLabel_.get(label);
};


/**
 * @protected
 * @return {num} The created object.
 */
bluemind.calendar.Manager.prototype.getTags = function() {
  return this.tagByLabel_.getValues();
};


/**
 * Toogle tag visibility
 * @param {Object} evt event.
 */
bluemind.calendar.Manager.prototype.toggleTagVisibility = function(tag, el) {
  goog.dom.classes.toggle(el, goog.getCssName('disabled'));
  if (this.hiddenTags_.contains(tag.getLabel())) {
    this.hiddenTags_.remove(tag.getLabel()); 
  } else {
    this.hiddenTags_.add(tag.getLabel());
  }
  bluemind.view.getView().display();
};

/**
 * Toogle tag visibility
 * @param {Object} evt event.
 */
bluemind.calendar.Manager.prototype.isEventVisible = function(evt) {
  if (this.hiddenTags_.getCount() == 0) {
    return true;
  }
  var tags = evt.getTags(); 
  visible = !this.hiddenTags_.contains('*')
  for (var i = 0; i < tags.length; i++) {
    var label = tags[i].getLabel();
    if (this.tagByLabel_.containsKey(label)) {
      if (!this.hiddenTags_.contains(label)) {
        return true;
      } else {
        visible = false;
      }
    }
  }
  return visible;
};


/**
 * Toogle tag visibility
 * @param {Object} evt event.
 */
bluemind.calendar.Manager.prototype.isTagVisible = function(tag) {
  var label = tag.getLabel();  
  return !this.tagByLabel_.containsKey(label) || !this.hiddenTags_.contains(label);
};

/**
 * Reload tag ui.
 */
bluemind.calendar.Manager.prototype.reloadTags = function() {
  var container = goog.dom.getElement('bm-tags-items');
  this.tagHandler_.removeAll();
  goog.dom.removeChildren(container);
  if (this.tagByLabel_.getCount() == 0) {
    goog.style.showElement(
        goog.dom.getElement('bm-selector-tags'), false);
    this.switchTab('bm-selector-calendar');
  } else {
    goog.style.showElement(
        goog.dom.getElement('bm-selector-tags'), true);
    var e = soy.renderAsFragment(bluemind.calendar.template.notag);
    this.tagHandler_.listen(e, goog.events.EventType.CLICK, function(evt) {
       var tag = new bluemind.model.Tag();
       tag.setLabel('*');
       bluemind.manager.toggleTagVisibility(tag, evt.currentTarget);
       evt.stopPropagation();
    });
    goog.dom.appendChild(container, e);

    goog.iter.forEach(this.tagByLabel_, function(tag) {
      var data = tag.serialize();
      var e = soy.renderAsFragment(bluemind.calendar.template.tag, data);
      this.tagHandler_.listen(e, goog.events.EventType.CLICK, function(evt) {
        bluemind.manager.toggleTagVisibility(tag, evt.currentTarget);
        evt.stopPropagation();
      });
      goog.dom.appendChild(container, e);
    }, this);
  }

};

/**
 * Switch between calendar and tag 
 *
 */
bluemind.calendar.Manager.prototype.switchTab = function(tab) {
  switch (tab) {
    case 'bm-selector-tags':
      goog.style.showElement(
        goog.dom.getElement('bm-calendars'), false);
      goog.style.showElement(
        goog.dom.getElement('bm-tags'), true);
      break;
    case 'bm-selector-calendars':
    default:
      goog.style.showElement(
        goog.dom.getElement('bm-tags'), false);
      goog.style.showElement(
        goog.dom.getElement('bm-calendars'), true);
  }
};

/**
 * A realy simple Pool to manage a set off free position
 * @param {number} initialCount init count.
 * @param {number} maxCount max count.
 * @constructor
 */
bluemind.calendar.manager.NumberPool = function(initialCount, maxCount) {
  this.count_ = 0;
  this.minCount_ = initialCount;
  this.maxCount_ = maxCount;
  this.freeQueue_ = [];
  this.createInitial_(maxCount);
};

/**
 * Maximum number of objects allowed
 * @type {number}
 * @private
 */
bluemind.calendar.manager.NumberPool.prototype.maxCount_;

/**
 * Maximum number of objects allowed
 * @type {number}
 * @private
 */
bluemind.calendar.manager.NumberPool.prototype.minCount_;

/**
 * Length of the pool
 * @type {number}
 * @private
 */
bluemind.calendar.manager.NumberPool.prototype.count_;

/**
 * Queue used to store objects that are currently in the pool and available
 * to be used.
 * @type {Array}
 * @private
 */
bluemind.calendar.manager.NumberPool.prototype.freeQueue_;

/**
 * Gets an unused object from the the pool, if there is one available,
 * otherwise creates a new one.
 * @return {*} An object from the pool or a new one if necessary.
 */
bluemind.calendar.manager.NumberPool.prototype.getObject = function() {
  if (this.freeQueue_.length) {
   return this.freeQueue_.shift();
  }
  return this.createObject();
};


/**
 * Returns an object to the pool so that it can be reused. If the pool is
 * already full, the object is disposed instead.
 * @param {*} obj The object to release.
 */
bluemind.calendar.manager.NumberPool.prototype.releaseObject = function(obj) {
  if (this.freeQueue_.length < this.maxCount_) {
    this.freeQueue_.push(obj);
    this.freeQueue_.sort();
  }
};

/**
 * release objects
 */
bluemind.calendar.manager.NumberPool.prototype.releaseObjects = function() {
  this.count_ = 0;
  this.freeQueue_ = [];
  this.createInitial_(this.minCount_);
};

/**
 * Register one class
 * @param {*} obj The object to register.
 */
bluemind.calendar.manager.NumberPool.prototype.registerObject = function(obj) {
  goog.array.remove(this.freeQueue_, obj);
};

/**
 * Populates the pool with initialCount objects.
 * @param {number} initialCount The number of objects to add to the pool.
 * @private
 */
bluemind.calendar.manager.NumberPool.prototype.createInitial_ =
  function(initialCount) {
  if (initialCount > this.maxCount_) {
    throw Error('[bluemind.calendar.manager.NumberPool]' +
       'Initial cannot be greater than max');
  }
  for (var i = 0; i < initialCount; i++) {
    this.freeQueue_.push(this.createObject());
  }
};


/**
 * Should be overriden by sub-classes to return an instance of the object type
 * that is expected in the pool.
 * @protected
 * @return {num} The created object.
 */
bluemind.calendar.manager.NumberPool.prototype.createObject = function() {
  return this.count_++% this.maxCount_;
};

