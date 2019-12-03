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
 * @fileoverview View class for application header (Logo + logo).
 */

goog.provide("net.bluemind.calendar.month.MonthView");

goog.require("goog.array");
goog.require("goog.date");
goog.require("goog.dom");
goog.require("goog.iter");
goog.require("goog.object");
goog.require("goog.style");
goog.require("goog.date.Date");
goog.require("goog.date.Interval");
goog.require("goog.dom.ViewportSizeMonitor");
goog.require("goog.dom.classlist");
goog.require("goog.events.EventType");
goog.require("goog.fx.Dragger.EventType");
goog.require("goog.i18n.DateTimeFormat");
goog.require("goog.i18n.DateTimeSymbols_en_ISO");
goog.require("goog.math.Coordinate");
goog.require("goog.math.Matrix");
goog.require("goog.math.Rect");
goog.require("goog.ui.Component");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.PopupBase.EventType");
goog.require("net.bluemind.calendar.day.ui.ConsultPopup");
goog.require("net.bluemind.calendar.day.ui.CreationPopup");
goog.require("net.bluemind.calendar.day.ui.UpdatePopup");
goog.require("net.bluemind.calendar.month.templates");// FIXME - unresolved
// required symbol
goog.require("net.bluemind.calendar.month.ui.Event");
goog.require("net.bluemind.calendar.day.ui.EventList");
goog.require("net.bluemind.calendar.vevent.EventType");
goog.require("net.bluemind.calendar.vevent.VEventEvent");
goog.require("net.bluemind.date.Date");
goog.require("bluemind.calendar.fx.AlldayResizer");// FIXME - unresolved
// required symbol
// required symbol
goog.require('net.bluemind.calendar.vevent.defaultValues');

/**
 * View class for Calendar months view.
 * 
 * @param {net.bluemind.i18n.DateTimeHelper.Formatter} format Formatter
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.month.MonthView = function(ctx, format, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.ctx = ctx;
  this.format = format;
  var popup = new net.bluemind.calendar.day.ui.CreationPopup(format);
  popup.setId('create-popup');
  this.addChild(popup, true);
  popup = new net.bluemind.calendar.day.ui.ConsultPopup(format);
  popup.setId('consult-popup');
  this.addChild(popup, true);
  popup = new net.bluemind.calendar.day.ui.UpdatePopup(format);
  popup.setId('update-popup');
  this.addChild(popup, true);
  popup = new net.bluemind.calendar.day.ui.EventList(format);
  popup.setId('events-overflow');
  this.addChild(popup, true);

  this.addChild(popup, true);
  this.events_ = [];
};

goog.inherits(net.bluemind.calendar.month.MonthView, goog.ui.Component);

/**
 * @type {net.bluemind.date.DateRange}
 */
net.bluemind.calendar.month.MonthView.prototype.range;

/**
 * @type {number}
 */
net.bluemind.calendar.month.MonthView.prototype.length;

/**
 * @type {goog.date.Date}
 */
net.bluemind.calendar.month.MonthView.prototype.date;

/**
 * @type {Array.<number>}
 */
net.bluemind.calendar.month.MonthView.prototype.workingDays;

/**
 * @type {goog.i18n.TimeZone}
 */
net.bluemind.calendar.month.MonthView.prototype.tz;

/**
 * @type {goog.dom.ViewportSizeMonitor}
 */
net.bluemind.calendar.month.MonthView.prototype.sizeMonitor_

/**
 * @type {Array.<goog.ui.Component>}
 */
net.bluemind.calendar.month.MonthView.prototype.events_;

/**
 * @type {goog.i18n.DateTimeFormat}
 */
net.bluemind.calendar.month.MonthView.prototype.format;

/** @override */
net.bluemind.calendar.month.MonthView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.getElement().innerHTML = net.bluemind.calendar.month.templates.main();
  goog.dom.classlist.add(this.getElement(), goog.getCssName('month-view'));
};

/** @override */
net.bluemind.calendar.month.MonthView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.sizeMonitor_ = new goog.dom.ViewportSizeMonitor();
  this.getHandler().listen(this.sizeMonitor_, goog.events.EventType.RESIZE, this.handleResize_);
  this.resize_();

};

/**
 * Show view for given date range
 * 
 * @param {net.bluemind.date.DateRange} range Date range
 * @param {goog.date.Date} date current date
 */
net.bluemind.calendar.month.MonthView.prototype.show = function(range, date) {
  this.range = range;
  this.date = date;
  this.length = 0;
  goog.iter.forEach(this.range.iterator(), function() {
    this.length++;
  }, this);
  goog.array.forEach(this.events_, function(event) {
    this.removeChild(event);
    event.dispose();
  }, this);
  this.events_ = [];
  this.initGrid();
  this.drawAllDayEvents_();
  this.resize_();
};

/**
 * Register an event
 * 
 * @param {goog.ui.Component} child Event
 */
net.bluemind.calendar.month.MonthView.prototype.addEventChild = function(child) {
  this.addChild(child);
  this.events_.push(child);
  this.getHandler().listen(child, goog.events.EventType.MOUSEDOWN, this.handleEventMouseDown_);
  this.getHandler().listen(child, goog.ui.Component.EventType.CHANGE, this.handleEventChanged_);
};

/**
 * Draw the calendar grid
 */
net.bluemind.calendar.month.MonthView.prototype.initGrid = function() {
  var dom = this.getDomHelper();
  dom.removeChildren(this.getDayLabelElement());
  dom.removeChildren(this.getWeekLabelElement());
  dom.removeChildren(this.getEventElement());

  var d = this.range.getStartDate().clone();
  var dayInterval = new goog.date.Interval(goog.date.Interval.DAYS, 1);
  var nbWeeks = 6;
  var grid = [];
  for (var y = 0; y < nbWeeks; y++) {
    grid[y] = [];
    for (var x = 0; x < 7; x++) {
      grid[y][x] = d.clone();
      d.add(dayInterval);
    }
  }

  var cssMonthRow = goog.getCssName('month-row');
  var cssDayLabel = goog.getCssName('dayLabel');
  var cssDayContent = goog.getCssName('dayContent');
  var cssSpacer = goog.getCssName('spacer');
  var cssMgDayNum = goog.getCssName('mg-daynum');
  var cssDayEventContainer = goog.getCssName('dayEventContainer');
  var cssOutOfWork = goog.getCssName('outOfWork');
  var cssMonthWeekNum = goog.getCssName('monthWeekNum');
  var cssClickable = goog.getCssName('clickable');

  var working_days = this.workingDays;
  var shortWeekDays = goog.i18n.DateTimeSymbols_en_ISO.SHORTWEEKDAYS;

  var monthContainer = this.getEventElement();
  var monthWeekNumContainerTable = this.getWeekLabelElement();

  for (var y = 0; y < nbWeeks; y++) { // weeks of month
    var div = dom.createDom('div', {
      'class' : cssMonthRow
    });

    var curDate = grid[y][0];
    var weeknum = curDate.getWeekNumber();
    var table = dom.createDom('table', {
      'id' : 'month-row-table_' + y,
      'class' : 'month-row-table'
    });
    var trDayNum = dom.createDom('tr', {
      'class' : cssDayLabel
    });

    dom.appendChild(table, trDayNum);
    var trDayContent = dom.createDom('tr', {
      'id' : 'weekBgTr_' + weeknum,
      'class' : cssDayContent + ' ' + cssSpacer
    });

    dom.appendChild(table, trDayContent);

    // Week num
    var monthWeekNumContainerTr = dom.createDom('tr');
    var wn = dom.createDom('span', {
      'id' : curDate.getTime(),
      'class' : cssClickable
    });
    dom.setTextContent(wn, weeknum);
    this.getHandler().listen(wn, goog.events.EventType.CLICK, function(e) {
      e.stopPropagation();
      var date = new net.bluemind.date.Date();
      date.setTime(e.target.id);
      var url = '/day/?range=week&date=' + date.toIsoString();
      var loc = goog.global['location'];
      loc.hash = url;
    });
    var monthWeekNumContainerTd = dom.createDom('td');
    monthWeekNumContainerTd.appendChild(wn);
    monthWeekNumContainerTr.appendChild(monthWeekNumContainerTd);
    monthWeekNumContainerTable.appendChild(monthWeekNumContainerTr);

    for (var x = 0; x < 7; x++) { // days of week
      var o = grid[y][x];
      var tdDayNum = dom.createDom('td', {
        'class' : cssMgDayNum
      });

      var label = dom.createDom('span', {
        'id' : o.getTime(),
        'class' : cssClickable
      });
      dom.setTextContent(label, o.getDate());
      this.getHandler().listen(label, goog.events.EventType.MOUSEDOWN, function(e) {
        e.stopPropagation();
      });
      this.getHandler().listen(label, goog.events.EventType.CLICK, function(e) {
        e.stopPropagation();
        var date = new net.bluemind.date.Date();
        date.setTime(e.target.id);
        var url = '/day/?range=day&date=' + date.toIsoString();
        var loc = goog.global['location'];
        loc.hash = url;
      });

      dom.appendChild(tdDayNum, label);

      dom.appendChild(trDayNum, tdDayNum);

      var tdDayContent = dom.createDom('td', {
        'id' : 'dayEventContainer_' + o.getWeekNumber() + '_' + o.getDay(),
        'class' : cssDayEventContainer
      });
      if (!goog.array.contains(working_days, shortWeekDays[o.getDay()].toLowerCase())) {
        goog.dom.classlist.add(tdDayContent, cssOutOfWork);
      }

      dom.appendChild(trDayContent, tdDayContent);

      if (goog.date.isSameDay(o, new net.bluemind.date.DateTime())) {
        goog.dom.classlist.add(tdDayNum, goog.getCssName('todayLabel'));
        goog.dom.classlist.add(tdDayContent, goog.getCssName('today'));
      }
    }

    dom.appendChild(div, table);
    dom.appendChild(monthContainer, div);
  }

  var df = new goog.i18n.DateTimeFormat('EEEE');
  var dContainer = this.getDayLabelElement();
  dom.appendChild(dContainer, dom.createDom('td'));
  for (var i = 0; i < 7; i++) {
    var o = grid[0][i];
    var th = dom.createDom('th', {}, df.format(o));
    dom.appendChild(dContainer, th);
  }
  this.getHandler().listen(monthContainer, goog.events.EventType.MOUSEDOWN, this.handleMouseDown_);
};

/**
 * Resize grid
 * 
 * @param {goog.events.Event=} opt_evt
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.resize_ = function(opt_evt) {
  var grid = this.getGridElement();
  var size = this.sizeMonitor_.getSize();
  var elPos = goog.style.getClientPosition(grid);
  var height = size.height - elPos.y - 3;
  grid.style.height = height + 'px';
  var c = this.getEventElement();
  var wn = this.getWeekLabelElement();
  c.style.height = height + 'px';
  wn.style.height = height + 'px';
  var monthRowHeight = Math.floor(((height / 6) - 13) / 20);
  var limit = Math.max(0, monthRowHeight);
  this.what(limit);
};

/**
 * @param {*} calendars
 */
net.bluemind.calendar.month.MonthView.prototype.setCalendars = function(calendars) {
  this.calendars = calendars;
  this.getChild("events-overflow").setCalendars(calendars);
  this.getChild('create-popup').setCalendars(calendars);
};

/**
 * Handle Resize grid
 * 
 * @param {goog.events.Event=} opt_evt
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.handleResize_ = function(opt_evt) {
  this.resize_();
  this.getChild('create-popup').hide();
};

/**
 * Get date of the column containing given coordinate in the grid
 * 
 * @param {goog.math.Coordinate} coords Coordinates to be located.
 * @return {goog.date.Date} The corresponding date.
 */
net.bluemind.calendar.month.MonthView.prototype.getDateForCoordinate = function(coords) {
  var row;
  var weeks = this.getElementsByClass(goog.getCssName('month-row'));
  for (var i = 0; i < weeks.length; i++) {
    var week = weeks[i];
    var pos = goog.style.getPageOffsetTop(week);
    var size = goog.style.getSize(week);
    if (pos <= coords.y && (pos + size.height) >= coords.y) {
      row = i;
      break;
    }
  }
  var col;
  var days = this.getDomHelper().getElementsByClass(goog.getCssName('mg-daynum'), weeks[row]);
  for (var i = 0; i < days.length; i++) {
    var day = days[i];
    var pos = goog.style.getPageOffsetLeft(day);
    var size = goog.style.getSize(day);
    if (pos <= (coords.x + 6) && (pos + size.width) > (coords.x + 6)) {
      col = i;
      break;
    }
  }
  var d = this.range.getStartDate().clone();
  d.add(new goog.date.Interval(0, 0, row * 7 + col));
  return d;
};

/**
 * @param {goog.events.BrowserEvent} e
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.handleMouseDown_ = function(e) {
  var calendars = this.getWritablesCalendars_();
  if (e.isMouseActionButton() && calendars.length > 0) {
    var left = goog.style.getPageOffsetLeft(e.target);
    var date = this.getDateForCoordinate(new goog.math.Coordinate(left, e.clientY));
    if (!this.range.contains(date)) {
      return;
    }
    var el = /** @type {Element} */
    (this.getDomHelper().getAncestorByTagNameAndClass(e.target, 'div'));
    var bounds = new goog.math.Rect(left, goog.style.getPageOffsetTop(el), goog.style.getSize(e.target).width,
        goog.style.getSize(el).height);
    var rect = goog.style.getBounds(e.currentTarget);
    var rect = goog.style.getBounds(e.currentTarget);
    rect.left -= 8;
    rect.width += 8;
    var resize = new bluemind.calendar.fx.AlldayResizer(bounds, rect);
    this.registerDisposable(resize);
    resize.startDrag(e);
    this.getHandler().listenOnce(resize, goog.fx.Dragger.EventType.END, function(e) {
      this.createEvent_(date, e.dragger, bounds);
    });

  }
};

/**
 * On event dragged
 * 
 * @param {goog.date.Date} start Start date
 * @param {bluemind.calendar.fx.AlldayResizer} resizer Resizer.
 * @param {goog.math.Rect} bounds Drag relative bounds
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.createEvent_ = function(start, resizer, bounds) {
  var days = 0, top, element;
  var container = resizer.getDraw();
  var children = this.getDomHelper().getChildren(container);
  for (var i = 0; i < children.length; i++) {
    var size = goog.style.getSize(children[i]);
    days += Math.round(size.width / bounds.width);
    element = children[i];
  }
  var top = goog.style.getPageOffsetTop(element) - bounds.top;
  var left = Math.abs(goog.style.getPageOffsetLeft(element) - bounds.left);
  var end = start.clone();
  end.add(new goog.date.Interval(0, 0, 1));
  var duration;
  if (top > 1 || (top >= 0 && top < 1 && left < 1)) {
    end = start.clone();
    var interval = new goog.date.Interval(0, 0, days);
    duration = interval.getTotalSeconds();
    end.add(interval);
  } else {
    start = end.clone();
    var interval = new goog.date.Interval(0, 0, -days);
    duration = -interval.getTotalSeconds();
    start.add(interval);
  }
  var calendars = this.getWritablesCalendars_();
  var evt = {
    summary : '',
    dtstart : start,
    dtend : end,
    tooltip : '',
    location : '',
    start : start,
    end : end,
    timezones: {
      start: null,
      end: null
    },
    states : {
      allday : true,
      pending : false,
      private_ : false,
      busy : false,
      updatable : true,
      short : false,
      past : (end.getTime() < goog.now()),
      meeting : false,
      main: true,
      owned : true
    },
    tags : [],
    uid: net.bluemind.mvp.UID.generate(),
    duration : duration,
    calendars : calendars,
    calendar : this.getDefaultCalendar_()
  };
  
  if (this.ctx.settings.get('default_allday_event_alert') && !isNaN(parseInt(this.ctx.settings.get('default_allday_event_alert')))) {
    evt.alarm = [{
      trigger : this.ctx.settings.get('default_allday_event_alert'),
      action : net.bluemind.calendar.vevent.defaultValues.action
    }]
  }

  var popup = this.getChild('create-popup');
  popup.setModel(evt);
  popup.attach(element);
  popup.setVisible(true);
  this.getHandler().listenOnce(popup, goog.ui.PopupBase.EventType.HIDE, function() {
    // FIXME Do be do
    resizer.dispose();
  });
};

/**
 * crappy hack.
 * 
 * @param {number} limit max evt to show.
 */
net.bluemind.calendar.month.MonthView.prototype.what = function(limit) {
  var tables = this.getDomHelper().getElementsByClass('month-row-table');
  var cssDisabled = goog.getCssName('disabled');
  var cssContainsAllday = goog.getCssName('containsAllday');
  var cssAllDayCollapsed = goog.getCssName('alldayCollapsed');
  limit = Math.max(1, limit - 1);
  for (var i = 0; i < tables.length; i++) {
    var table = tables[i];
    var idx = 0;
    var trs = this.getDomHelper().getElementsByClass(cssContainsAllday, table);

    for (var j = 0; j < trs.length; j++) {
      var tr = trs[j];
      if (j < limit) {
        goog.dom.classlist.remove(tr, cssDisabled);
      } else {
        goog.dom.classlist.add(tr, cssDisabled);
      }
    }

    var adc = this.getDomHelper().getElementsByClass(cssAllDayCollapsed, table);
    for (var k = 0; k < adc.length; k++) {
      var d = adc[k];
      var nbitems = parseInt(this.getDomHelper().getTextContent(d), 10);
      if (nbitems > limit) {
        goog.dom.classlist.remove(d, cssDisabled);
      } else {
        goog.dom.classlist.add(d, cssDisabled);
      }
    }
  }
};

/**
 * Get day label container
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.getDayLabelElement = function() {
  return this.getElement() && /** @type {Element} */
  (this.getElement().firstChild.firstChild.firstChild.firstChild);
};

/**
 * Get grid
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.getGridElement = function() {
  return this.getElement() && /** @type {Element} */
  (this.getElement().firstChild.nextSibling);
};

/**
 * Get week label
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.getWeekLabelElement = function() {
  return this.getElement() && /** @type {Element} */
  (this.getGridElement().firstChild.firstChild.firstChild.firstChild.firstChild);
};

/**
 * Get Event container
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.getEventElement = function() {
  return this.getElement() && /** @type {Element} */
  (this.getGridElement().firstChild.firstChild.firstChild.lastChild.firstChild);
};

/**
 * @return {Object} Default calendar
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.getDefaultCalendar_ = function() {
  for (var i = 0; i < this.calendars.length; i++) {
    if (this.calendars[i].states.main) {
      return this.calendars[i].uid;
    }
  }
};

/**
 * @return {Array} writable calendars
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.getWritablesCalendars_ = function() {
  return goog.array.filter(this.calendars, function(calendar) {
    return calendar.states.writable;
  });
};

/**
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.drawAllDayEvents_ = function() {
  var model = this.getModel();
  var nbDays = 7; // FIXME: can't display more than 7 days in a week
  var cssDisabled = goog.getCssName('disabled');
  var cssContainsAllday = goog.getCssName('containsAllday');
  var cssSpacer = goog.getCssName('spacer');
  var cssContainsAlldayCollapsed = goog.getCssName('containsAlldayCollapsed');
  var cssAlldayCollapsed = goog.getCssName('alldayCollapsed');
  var oneDayInterval = new goog.date.Interval(0, 0, 1);
  var events = [];
  var tad = goog.dom.getElement('toggleAllday');

  var dayIndex = 0;
  goog.object.forEach(model.weeks, function(entries, weeknum) {
    var nbEventsPerDay = [];
    for (var i = 0; i < nbDays; i++) {
      nbEventsPerDay[i] = 0;
    }
    var trs = new goog.math.Matrix(1, nbDays);

    entries.sort(function(entry1, entry2) {
      var d1 = entry1.event.dtstart.clone();
      var d2 = entry2.event.dtstart.clone();
      if (!entry1.event.states.allday && entry1.size > 1) {
        d1.setHours(0);
      }
      if (!entry2.event.states.allday && entry2.size > 1) {
        d2.setHours(0);
      }
      var diff = d1.getTime() - d2.getTime();
      if (diff != 0)
        return diff;
      diff = entry2.event.dtend.getTime() - entry1.event.dtend.getTime();
      if (diff != 0)
        return diff;
      diff = entry1.event.calendar - entry2.event.calendar;
      if (diff != 0)
        return diff;
      return goog.getUid(entry1.event) - goog.getUid(entry2.event);
    });

    goog.array.forEach(entries, function(entry) {
      var evt = entry.event
      var eventSize = entry.size;
      var start = entry.start;
      var coords = {
        row : 0,
        entry : entry
      };
      var evtPosition = new goog.math.Matrix(1, nbDays);
      var evtInterval = new goog.date.Interval(0, 0, -start.getDay() + evt.startDay);

      var begin = start.clone();
      begin.add(evtInterval);
      for (var i = 0; i < eventSize; i++) {
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
        coords.row = size;
        begin = start.clone();
        begin.add(evtInterval);
        for (var i = 0; i < eventSize; i++) {
          var idx = begin.getDay();
          begin.add(oneDayInterval);
          nbEventsPerDay[idx] = size + 1;
        }
      } else {
        // add event to "free" row
        begin = start.clone();
        begin.add(evtInterval);
        for (var i = 0; i < eventSize; i++) {
          var idx = begin.getDay();
          trs.setValueAt(cur, idx, goog.getUid(evt));
          begin.add(oneDayInterval);
          nbEventsPerDay[idx] = Math.max(cur + 1, nbEventsPerDay[idx]);
        }
        coords.row = cur;
      }
      events.push(coords);
    }, this);

    var trId = 'weekBgTr_' + weeknum;
    var tr = goog.dom.getElement(trId);
    if (tr) {
      var id = goog.dom.getParentElement(tr).id.split('_');
      var delta = parseInt(id[1], 10);
      for (var idx = 0; idx < trs.getSize().height; idx++) {
        var ctr = tr.cloneNode(true);
        goog.dom.classlist.add(ctr, cssContainsAllday);
        goog.dom.classlist.remove(ctr, cssSpacer);
        goog.dom.setProperties(ctr, {
          'id' : trId + '_' + idx
        });
        goog.dom.insertSiblingBefore(ctr, tr);
        var children = goog.dom.getChildren(ctr);
        for (var i = 0; i < children.length; i++) {
          var td = children[i];
          goog.dom.setProperties(td, {
            'id' : td.id + '_' + idx
          });
        }
      }

      // Collapsed tr
      var ctr = tr.cloneNode(true);
      goog.dom.classlist.addAll(ctr, [ cssContainsAlldayCollapsed, cssDisabled ]);
      goog.dom.classlist.remove(ctr, cssSpacer);
      goog.dom.setProperties(ctr, {
        'id' : 'weekBgTrCollapsed_' + weeknum
      });
      goog.dom.insertSiblingBefore(ctr, tr);
      var children = goog.dom.getChildren(ctr);
      for (var i = 0; i < children.length; i++) {
        var td = children[i];
        goog.dom.setProperties(td, {
          'id' : td.id + '_collapsed'
        });
        var idx = td.id.split('_')[2];

        var nbEvents = parseInt(nbEventsPerDay[idx], 10);
        if (nbEvents > 0) {
          var collapsed = goog.dom.createDom('div', {
            'id' : 'container_' + (delta * 7 + i),
            'class' : cssAlldayCollapsed
          }, '' + nbEvents, goog.dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-caret-down') ]));

          goog.dom.appendChild(td, collapsed);
          this.getHandler().listen(collapsed, goog.events.EventType.MOUSEDOWN, function(e) {
            e.stopPropagation();
            var id = e.currentTarget.id.split('_');
            var date = this.range.getStartDate().clone();
            date.add(new goog.date.Interval(goog.date.Interval.DAYS, parseInt(id[1])));
            this.getChild('events-overflow').setModel(this.getModel().days[date.getDayOfYear()]);
            this.getChild('events-overflow').date = date;
            this.getChild('events-overflow').setVisible(true);
            this.getChild('events-overflow').attach(e.currentTarget);

          }, false, collapsed);
        }
      }
    }

  }, this);

  var callback = goog.bind(this.getDateForCoordinate, this);
  goog.array.forEach(events, function(coords) {
    var size = coords.entry.size;
    var start = coords.entry.start;
    var evt = coords.entry.event;
    var uid = evt.calendar + ':' + evt.uid + ':' + (evt.recurrenceId || '');
    if (!this.getChild(uid)) {
      var child = new net.bluemind.calendar.month.ui.Event(evt, callback, this.calendars, this.getDomHelper());
      child.setId(uid);
      this.addEventChild(child);
      child.render();
    } else {
      var child = this.getChild(uid);
    }

    var idx = coords.row
    var trId = 'weekBgTr_' + start.getWeekNumber();
    var container = this.getDomHelper().getElement(
        'dayEventContainer_' + start.getWeekNumber() + '_' + start.getDay() + '_' + idx);
    goog.dom.setProperties(container, {
      'colspan' : size
    });
    var tr = goog.dom.getElement(trId + '_' + idx);
    for (var i = 0; i < (size - 1); i++) {
      var td = goog.dom.getNextElementSibling(container);
      if (goog.dom.contains(tr, td)) {
        goog.dom.removeNode(td);
      }
    }

    child.addPart(container);

  }, this);
};

/**
 * @param {goog.events.BrowserEvent} e
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.handleEventChanged_ = function(e) {
  var child = e.target;
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.SAVE, child.getModel());
  this.dispatchEvent(e)
};

/**
 * @param {goog.events.BrowserEvent} e
 * @private
 */
net.bluemind.calendar.month.MonthView.prototype.handleEventMouseDown_ = function(e) {
  var child = e.target;
  if (!child.getModel().states.updatable) {
    var popup = this.getChild('consult-popup');
  } else {
    var popup = this.getChild('update-popup');
  }
  popup.calendars = this.calendars;
  popup.setModel(child.getModel());
  popup.attach(child.getElement());
  popup.setVisible(true);
};
