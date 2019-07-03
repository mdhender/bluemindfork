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
 * @fileoverview Calendar month view.
 */

goog.provide('bluemind.calendar.view.Month');

goog.require('goog.Disposable');
goog.require('goog.date');

/**
 * BlueMind Calendar months view
 *
 * @constructor
 * @extends {goog.Disposable}
 */
bluemind.calendar.view.Month = function() {
  this.manager_ = bluemind.manager;
};
goog.inherits(bluemind.calendar.view.Month, goog.Disposable);

/**
 * Calendar manager
 *
 * @type {bluemind.calendar.Manager}
 * @private
 */
bluemind.calendar.view.Month.prototype.manager_;

/**
 * Calendar start date
 *
 * @type {goog.date.Date}
 * @private
 */
bluemind.calendar.view.Month.prototype.start_;

/**
 * Calendar end date
 *
 * @type {goog.date.Date}
 * @private
 */
bluemind.calendar.view.Month.prototype.end_;

/**
 * Listeners
 *
 * @type {Array}
 * @private
 */
bluemind.calendar.view.Month.prototype.handler_;

/**
 * display month
 */
bluemind.calendar.view.Month.prototype.display = function() {
  this.manager_.setNextInterval(new goog.date.Interval(
      goog.date.Interval.MONTHS, 1));
  this.manager_.setPrevInterval(new goog.date.Interval(
      goog.date.Interval.MONTHS, -1));

  var d = this.manager_.getCurrentDate().clone();
  d.setDate(1);

  var wday = d.getWeekday();
  var days = d.getNumberOfDaysInMonth();

  // days in previous month
  d.add(new goog.date.Interval(goog.date.Interval.MONTHS, -1));
  d.setDate(d.getNumberOfDaysInMonth() - (wday - 1));
  this.start_ = d.clone();

  this.end_ = this.start_.clone();
  // end = begin + 6 weeks
  this.end_.add(new goog.date.Interval(0, 0, 42, 0, 0, -1));

  this.manager_.getEvents(this.start_, this.end_).addCallback(function(events) {
    if (bluemind.manager.lockUI()) {
      bluemind.manager.clear();
      bluemind.manager.unlockUI();
    }
    for (var i = 0; i < events.length; i++) {
      this.manager_.register(events[i]);
    }
    this.initGrid();
    this.manager_.refreshView();
  }, this);
};

/**
 * display month
 */
bluemind.calendar.view.Month.prototype.displayToday = function() {
  this.manager_.setCurrentDate(this.manager_.getToday().clone());
};

/**
 * Draw the calendar grid
 *
 */
bluemind.calendar.view.Month.prototype.initGrid = function() {
  goog.dom.getElement('viewContainer').innerHTML =
    bluemind.calendar.template.monthView();

  var d = this.start_.clone();
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

  var working_days = bluemind.me['working_days'].split(',');
  var shortWeekDays = goog.i18n.DateTimeSymbols_en_ISO.SHORTWEEKDAYS;

  var monthContainer = goog.dom.getElement('monthContainer');
  var monthWeekNumContainerTable =
    goog.dom.getElement('monthWeekNumContainerTable');

  for (var y = 0; y < nbWeeks; y++) { // weeks of month
    var div = goog.dom.createDom('div', {
      'class' : cssMonthRow
    });

    var curDate =  grid[y][0];
    var weeknum = curDate.getWeekNumber();
    var table = goog.dom.createDom('table', {
      'id' : 'month-row-table_' + y,
      'class' : 'month-row-table'
    });
    var trDayNum = goog.dom.createDom('tr', {
      'class': cssDayLabel
    });

    goog.dom.appendChild(table, trDayNum);
    var trDayContent = goog.dom.createDom('tr', {
      'id' : 'weekBgTr_' + weeknum,
      'class' : cssDayContent + ' ' + cssSpacer
    });

    goog.dom.appendChild(table, trDayContent);

    // Week num
    var monthWeekNumContainerTr = goog.dom.createDom('tr');
    var wn = goog.dom.createDom('span', {
      'id' : curDate.getTime(),
      'class': cssClickable
    });
    goog.dom.setTextContent(wn, weeknum);
    this.getHandler().listen(wn, goog.events.EventType.CLICK, function(e) {
      e.stopPropagation();
      var date = new goog.date.Date();
      date.setTime(e.target.id);
      bluemind.manager.setCurrentDate(date);
      bluemind.view.week();
    });
    var monthWeekNumContainerTd = goog.dom.createDom('td');
    monthWeekNumContainerTd.appendChild(wn);
    monthWeekNumContainerTr.appendChild(monthWeekNumContainerTd);
    monthWeekNumContainerTable.appendChild(monthWeekNumContainerTr);

    for (var x = 0; x < 7; x++) { // days of week
      var o = grid[y][x];
      var tdDayNum = goog.dom.createDom('td', {
        'class': cssMgDayNum
      });

      var label = goog.dom.createDom('span', {
        'id': o.getTime(),
        'class': cssClickable
      });
      goog.dom.setTextContent(label, o.getDate());
      this.getHandler().listen(label, goog.events.EventType.MOUSEDOWN, function(e) {
        e.stopPropagation();
      });
      this.getHandler().listen(label, goog.events.EventType.CLICK, function(e) {
        e.stopPropagation();
        var date = new goog.date.Date();
        date.setTime(e.target.id);
        bluemind.manager.setCurrentDate(date);
        bluemind.view.day();
      });

      goog.dom.appendChild(tdDayNum, label);

      goog.dom.appendChild(trDayNum, tdDayNum);

      var tdDayContent = goog.dom.createDom('td', {
        'id': 'dayEventContainer_' + o.getWeekNumber() +
           '_' + o.getDay(),
        'class' : cssDayEventContainer
      });
      if (!goog.array.contains(working_days,
        shortWeekDays[o.getDay()].toLowerCase())) {
        goog.dom.classes.add(tdDayContent, cssOutOfWork);
      }

      goog.dom.appendChild(trDayContent, tdDayContent);

      if (goog.date.isSameDay(o)) {
        goog.dom.classes.add(tdDayNum, goog.getCssName('todayLabel'));
        goog.dom.classes.add(tdDayContent, goog.getCssName('today'));
      }
    }

    goog.dom.appendChild(div, table);
    goog.dom.appendChild(monthContainer, div);
  }

  var df = new goog.i18n.DateTimeFormat('EEEE');
  var dContainer = goog.dom.getElement('monthDayLabel');
  for (var i = 0; i < 7; i++) {
    var o = grid[0][i];
    var th = goog.dom.createDom('th', {}, df.format(o));
    goog.dom.appendChild(dContainer, th);
  }

  this.getHandler().listen(monthContainer, goog.events.EventType.MOUSEDOWN,
    bluemind.calendar.Controller.getInstance().drawAllDayEvent);
};

/**
 * Go to the next date interval
 *
 */
bluemind.calendar.view.Month.prototype.next = function() {
  this.manager_.next();
};

/**
 * Go to the previous date interval
 *
 */
bluemind.calendar.view.Month.prototype.prev = function() {
  this.manager_.prev();
};

/**
 * Return
 *
 * @return {string} month.
 */
bluemind.calendar.view.Month.prototype.getName = function() {
  return 'month';
};

/**
 * Return this.start_
 *
 * @return {goog.date.Date} current date.
 */
bluemind.calendar.view.Month.prototype.getStart = function() {
  return this.start_;
};

/**
 * Return this.end_
 *
 * @return {goog.date.Date} current date.
 */
bluemind.calendar.view.Month.prototype.getEnd = function() {
  return this.end_;
};

/**
 * Get date of the column containing given coordinate
 * in the grid
 * @param {goog.math.Coordinate} coords Coordinates to be located.
 * @return {goog.date.Date} The corresponding date.
 */
bluemind.calendar.view.Month.prototype.getDateForCoordinate = function(coords) {
  var row;
  var weeks = goog.dom.getElementsByClass(goog.getCssName('month-row'));
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
  var days = goog.dom.getElementsByClass(goog.getCssName('mg-daynum'),
    weeks[row]);
  for (var i = 0; i < days.length; i++) {
    var day = days[i];
    var pos = goog.style.getPageOffsetLeft(day);
    var size = goog.style.getSize(day);
    if (pos <= (coords.x + 6) && (pos + size.width) > (coords.x + 6)) {
      col = i;
      break;
    }
  }
  var d = new goog.date.Date(this.start_);
  d.add(new goog.date.Interval(0, 0, row * 7 + col));
  return d;
};

/**
 * @inheritDoc
 */
bluemind.calendar.view.Month.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.getHandler().removeAll();
};

/**
 * @inheritDoc
 */
bluemind.calendar.view.Month.prototype.dispose = function() { 
  goog.base(this, 'dispose');
  this.getHandler().removeAll();
};

/**
 * crappy hack.
 * @param {number} limit max evt to show.
 */
bluemind.calendar.view.Month.prototype.what = function(limit) {
  var tables = goog.dom.getElementsByClass('month-row-table');
  var cssDisabled = goog.getCssName('disabled');
  var cssContainsAllday = goog.getCssName('containsAllday');
  var cssAllDayCollapsed = goog.getCssName('alldayCollapsed');
  limit = Math.max(1, limit - 1);
  for (var i = 0; i < tables.length; i++) {
    var table = tables[i];
    var idx = 0;
    var trs = goog.dom.getElementsByClass(cssContainsAllday, table);

    for (var j = 0; j < trs.length; j++) {
      var tr = trs[j];
      if (j < limit) {
        goog.dom.classes.remove(tr, cssDisabled);
      } else {
        goog.dom.classes.add(tr, cssDisabled);
      }
    }

    var adc = goog.dom.getElementsByClass(cssAllDayCollapsed, table);
    for (var k = 0; k < adc.length; k++) {
      var d = adc[k];
      var nbitems = parseInt(goog.dom.getTextContent(d));
      if (nbitems > limit) {
        goog.dom.classes.remove(d, cssDisabled);
      } else {
        goog.dom.classes.add(d, cssDisabled);
      }
    }
  }
};

/**
 *
 */
bluemind.calendar.view.Month.prototype.getHandler = function() {
  return this.handler_ ||
         (this.handler_ = new goog.events.EventHandler(this));
};

