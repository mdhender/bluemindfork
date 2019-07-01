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
 * @fileoverview Calendar day view.
 */

goog.provide('bluemind.calendar.view.Days');

goog.require('bluemind.calendar.Controller');
goog.require('bluemind.date.DateTime');
goog.require('goog.Disposable');
goog.require('goog.date');
goog.require('goog.date.Date');
goog.require('goog.date.Interval');
goog.require('goog.dom');
goog.require('goog.dom.classes');
goog.require('goog.i18n.DateTimeFormat');
goog.require('goog.math.Coordinate');
goog.require('goog.style');

/**
 * BlueMind Calendar days view
 *
 * @constructor
 * @extends {goog.Disposable}
 */
bluemind.calendar.view.Days = function(manager) {
  this.manager_ = manager;
};
goog.inherits(bluemind.calendar.view.Days, goog.Disposable);

/**
 * Calendar manager
 *
 * @type {bluemind.calendar.Manager}
 * @private
 */
bluemind.calendar.view.Days.prototype.manager_;

/**
 * Calendar start date
 *
 * @type {goog.date.Date}
 * @private
 */
bluemind.calendar.view.Days.prototype.start_;

/**
 * Calendar end date
 *
 * @type {goog.date.Date}
 * @private
 */
bluemind.calendar.view.Days.prototype.end_;

/**
 * Calendar start date
 *
 * @type {boolean}
 * @private
 */
bluemind.calendar.view.Days.prototype.startOnTheFirstDayOfTheWeek_;

/**
 * Listeners
 *
 * @type {Array}
 * @private
 */
bluemind.calendar.view.Days.prototype.handler_;

/**
 * display day view
 */
bluemind.calendar.view.Days.prototype.display = function(autoScroll) {
  if (this.startOnTheFirstDayOfTheWeek_) {
    this.manager_.setNextInterval(
      new goog.date.Interval(0, 0, 7));
    this.manager_.setPrevInterval(
      new goog.date.Interval(0, 0, -7));
  } else {
    this.manager_.setNextInterval(
      new goog.date.Interval(0, 0, this.manager_.getNbDays()));
    this.manager_.setPrevInterval(
      new goog.date.Interval(0, 0, -this.manager_.getNbDays()));
  }

  this.start_ = this.manager_.getCurrentDate().clone();
  if (this.startOnTheFirstDayOfTheWeek_) {
    var fixDate;
    if (this.manager_.getSetting('showweekends') == 'no') {
      fixDate = - this.start_.getIsoWeekday();
    } else {
      fixDate = - this.start_.getWeekday();
    }
    this.start_.add(new goog.date.Interval(goog.date.Interval.DAYS, fixDate));
  }
  this.end_ = this.start_.clone();
  this.end_.add(new goog.date.Interval(0, 0, this.manager_.getNbDays()));

  this.manager_.getEvents(this.start_, this.end_).addCallback(function(events) {
  	var grid = goog.dom.getElement('gridContainer');
  	var scrollTop = null;
    if (grid != null) {
    	scrollTop = grid.scrollTop;
    }
    if (bluemind.manager.lockUI()) {
      bluemind.manager.clear();
      bluemind.manager.unlockUI();
    }

    goog.array.forEach(events, function(evt) {
      this.manager_.register(evt);
    }, this);

    this.initGrid();
    this.initLabels();

    this.manager_.refreshView();
    if (autoScroll) {
	    var start = this.manager_.getSetting('work_hours_start');
	    if (this.nbDays_ == 1 && goog.date.isSameDay(this.getCurrentDate())) {
	      start = new bluemind.date.DateTime().getHours();
	    }
	    goog.dom.getElement('gridContainer').scrollTop = start * 42;
    } else {
    	if (scrollTop != null) {
          goog.dom.getElement('gridContainer').scrollTop = scrollTop;
    	}
    }
  }, this);
};

/**
 * Show the current day
 */
bluemind.calendar.view.Days.prototype.displayToday = function() {
  this.manager_.setCurrentDate(this.manager_.getToday().clone());
};

/**
 * Swich to day view. Start on current day
 *
 * @param {number} nDays the number of days to display.
 */
bluemind.calendar.view.Days.prototype.displayDay = function(nDays) {
  this.manager_.setNbDays(nDays);
  this.startOnTheFirstDayOfTheWeek_ = false;
  this.display(true);
};

/**
 * Swich to week view. Start on the first day of the week
 */
bluemind.calendar.view.Days.prototype.displayWeek = function() {
  if (this.manager_.getSetting('showweekends') == 'yes') {
    this.manager_.setNbDays(7);
  } else {
    this.manager_.setNbDays(5);
  }
  this.startOnTheFirstDayOfTheWeek_ = true;
  this.display(true);
};

/**
 * Draw the calendar grid
 *
 */
bluemind.calendar.view.Days.prototype.initGrid = function() {
  goog.dom.getElement('viewContainer').innerHTML =
    bluemind.calendar.template.daysView();

  goog.dom.removeChildren(goog.dom.getElement('label'));

  var d = goog.dom.getElementsByTagNameAndClass('td',
    goog.getCssName('dayContainer'));
  for (var i = 0; i < d.length; i++) {
    goog.dom.removeChildren(d[i]);
    goog.dom.removeNode(d[i]);
  }

  goog.dom.removeChildren(goog.dom.getElement('allDayContainer'));
  var tdHourContainer = goog.dom.getElement('tdHourContainer');
  goog.dom.setProperties(tdHourContainer, {
    'colspan' : this.manager_.getNbDays()
  });
  var labelContainer = goog.dom.getElement('label');
  var allDayContainer = goog.dom.getElement('allDayContainer');
  var bodyContent = goog.dom.getElement('bodyContent');

  var tzContainer = goog.dom.createDom('td', {
    'id': 'tzContainer',
    'width' : '60px'
  });
  goog.dom.classes.add(tzContainer, goog.getCssName('tzContainer'));

  goog.dom.appendChild(labelContainer, tzContainer);

  var weekNum = goog.dom.createDom('td', {
    'id': 'weekNum',
    'width' : '60px'
  });
  goog.dom.classes.add(weekNum, goog.getCssName('weekNum'));

  var toggleAllday = goog.dom.createDom('div', {
    'id': 'toggleAllday'
  });
  goog.dom.classes.add(toggleAllday, goog.getCssName('toggleAllday'));
  goog.dom.classes.add(toggleAllday, goog.getCssName('expand'));
  goog.dom.classes.add(toggleAllday, goog.getCssName('disabled'));

  var weekNumContainer = goog.dom.createDom('div', {
    'id': 'weekNumContainer'
  });

  goog.dom.appendChild(weekNum, toggleAllday);
  goog.dom.appendChild(weekNum, weekNumContainer);
  goog.dom.appendChild(allDayContainer, weekNum);

  var weekBg = goog.dom.createDom('td', {
    'id' : 'allDayEventsContainer',
    'colspan' : this.manager_.getNbDays()
  });
  goog.dom.classes.add(weekBg, goog.getCssName('allDayEventsContainer'));

  var weekBgTable = goog.dom.createDom('table', {
    'id' : 'weekBg'
  });
  goog.dom.classes.add(weekBgTable, goog.getCssName('weekBg'));

  var weekBgTableTr = goog.dom.createDom('tr', {
    'id' : 'weekBgTr_' + this.start_.getWeekNumber()
  });

  goog.dom.appendChild(weekBgTable, weekBgTableTr);
  goog.dom.appendChild(weekBg, weekBgTable);
  goog.dom.appendChild(allDayContainer, weekBg);
  this.getHandler().listen(weekBg,
    goog.events.EventType.MOUSEDOWN,
    bluemind.calendar.Controller.getInstance().drawAllDayEvent, false, this);

  var s = this.start_.clone();

  var cssDayLabel = goog.getCssName('dayLabel');
  var cssDayContainer = goog.getCssName('dayContainer');
  var cssColEmul = goog.getCssName('column-emul');
  var cssContainer = goog.getCssName('container');
  var cssDayEventContainer = goog.getCssName('dayEventContainer');
  var cssOutOfWork = goog.getCssName('outOfWork');

  var working_days = this.manager_.getSetting('working_days').split(',');
  var shortWeekDays = goog.i18n.DateTimeSymbols_en_ISO.SHORTWEEKDAYS;

  while (goog.date.Date.compare(s, this.end_) < 0) {

    var i = s.getDay();

    var tdLabel = goog.dom.createDom('td', {
      'id' : 'dayLabel' + i
    });
    goog.dom.classes.add(tdLabel, cssDayLabel);

    var td = goog.dom.createDom('td', {
      'id' : 'dayContainer' + i
    });
    goog.dom.classes.add(td, cssDayContainer);

    if (!goog.array.contains(working_days, shortWeekDays[i].toLowerCase())) {
      goog.dom.classes.add(td, cssOutOfWork);
    }

    var col = goog.dom.createDom('div', {
      id: 'col' + i
    });
    goog.dom.classes.add(col, cssColEmul);

    var div = goog.dom.createDom('div', {
      'id' : 'd' + i
    });
    goog.dom.classes.add(div, cssContainer);

    goog.dom.appendChild(col, div);
    goog.dom.appendChild(td, col);
    goog.dom.appendChild(labelContainer, tdLabel);
    goog.dom.appendChild(bodyContent, td);

    var tdDayEventContainer = goog.dom.createDom('td', {
      'id': 'dayEventContainer_' + this.start_.getWeekNumber() + '_' + i
    });
    goog.dom.classes.add(tdDayEventContainer, cssDayEventContainer);
    if (!goog.array.contains(working_days, shortWeekDays[i].toLowerCase())) {
      goog.dom.classes.add(tdDayEventContainer, cssOutOfWork);
    }

    goog.dom.appendChild(weekBgTableTr, tdDayEventContainer);

    this.getHandler().listen(col,
      goog.events.EventType.MOUSEDOWN,
      bluemind.calendar.Controller.getInstance().drawEvent, false, this);
    s.add(new goog.date.Interval(0, 0, 1));
  }

  var container = goog.dom.getElement('hourContainer');
  var hourLabelContainer = goog.dom.getElement('hourLabelContainer');

  var dstart = new bluemind.date.DateTime(2012, 0, 1, 0, 0, 0);
  var dend = dstart.clone();
  dend.add(new goog.date.Interval(goog.date.Interval.DAYS, 1));

  // Custom date timeformat
  var timeformat = new
    goog.i18n.DateTimeFormat(this.manager_.getSetting('timeformat'));
  if (this.manager_.getSetting('timeformat') == 'h:mma') {
    timeformat = new goog.i18n.DateTimeFormat('ha');
  }

  var i = 0;
  var cssFull = goog.getCssName('full');
  var cssHalf = goog.getCssName('half');
  var dsat = parseFloat(this.manager_.getSetting('work_hours_start'));
  var deat = parseFloat(this.manager_.getSetting('work_hours_end'));

  while (goog.date.Date.compare(dstart, dend) < 0) {
    var d = goog.dom.createDom('div', {
      'id' : 1800 * i
    });
    var label = timeformat.format(dstart);
    if (((i / 2) % 1) != 0) {
      label = '';
    }
    var d2 = goog.dom.createDom('div', {
      'id' : 'h' + i
    }, label);

    if (i % 2) {
      goog.dom.classes.add(d, cssFull);
      goog.dom.classes.add(d2, cssFull);
    } else {
      goog.dom.classes.add(d, cssHalf);
      goog.dom.classes.add(d2, cssHalf);
    }

    if ((dsat < deat) && !(i / 2 >= dsat && i / 2 < deat)) {
      goog.dom.classes.add(d, cssOutOfWork);
    } else if ((dsat > deat) && (i / 2 >= deat && i / 2 < dsat)) {
      goog.dom.classes.add(d, cssOutOfWork);
    }

    goog.dom.appendChild(container, d);
    goog.dom.appendChild(hourLabelContainer, d2);
    dstart.add(new goog.date.Interval(goog.date.Interval.SECONDS, 1800));
    i++;
  }

  this.getHandler().listen(toggleAllday,
    goog.events.EventType.CLICK,
    function(e) {
      var alldayCollapsed =
        goog.dom.getElementsByTagNameAndClass('tr',
        goog.getCssName('containsAlldayCollapsed'));
      bluemind.cookies.set('isAlldayExpand',
        goog.dom.classes.has(alldayCollapsed[0],
          goog.getCssName('disabled')));
      this.toggleAllday();
    }, false, this);

  var hourMarker = new goog.dom.createDom('div', {
    'id': 'hour-marker'
  });
  goog.dom.classes.add(hourMarker, goog.getCssName('hour-marker'));
  goog.dom.appendChild(hourLabelContainer, hourMarker);

  var n = new bluemind.date.DateTime();
  goog.style.setStyle(hourMarker, 'top',
    n.getHours() * 42 + n.getMinutes() * (42 / 60) + 'px');

  // FIXME should not be here ..
 // bluemind.hourMarkerTimer.start();
};

/**
 * Set the calendar grid labels
 *
 */
bluemind.calendar.view.Days.prototype.initLabels = function() {
  var current = this.start_.clone();

  goog.dom.appendChild(goog.dom.getElement('weekNumContainer'),
    document.createTextNode(current.getWeekNumber()));

  var tz = this.manager_.getTimeZone();
  goog.dom.appendChild(goog.dom.getElement('tzContainer'),
    document.createTextNode(tz.getGMTString(current)));

  var dtoday = goog.dom.getElementsByTagNameAndClass('td',
    goog.getCssName('today'));
  for (var i = 0; i < dtoday.length; i++) {
    goog.dom.classes.set(dtoday[i], goog.getCssName('dayContainer'));
  }

  var dtoday = goog.dom.getElementsByTagNameAndClass('td',
    goog.getCssName('todayLabel'));
  for (var i = 0; i < dtoday.length; i++) {
    goog.dom.classes.set(dtoday[i], '');
  }

  var dInterval = new goog.date.Interval(0, 0, 1);
  var cssClickable = goog.getCssName('clickable');
  while (goog.date.Date.compare(current, this.end_) < 0) {
    var i = current.getDay();
    if (goog.date.isSameDay(current)) {
      goog.dom.classes.add(goog.dom.getElement('dayLabel' + i),
        goog.getCssName('todayLabel'));
      goog.dom.classes.add(goog.dom.getElement('dayEventContainer_' +
        this.start_.getWeekNumber() + '_' + i), goog.getCssName('today'));
      goog.dom.classes.add(goog.dom.getElement('dayContainer' + i),
        goog.getCssName('today'));

      // Right now!
      var hourMarker = new goog.dom.createDom('div', {
        'id': 'today-hour-marker'
      });
      goog.dom.classes.add(hourMarker, goog.getCssName('hour-marker'));
      goog.dom.appendChild(goog.dom.getElement('col' + i), hourMarker);
      var n = new bluemind.date.DateTime();
      goog.style.setStyle(hourMarker, 'top',
        n.getHours() * 42 + n.getMinutes() * (42 / 60) + 'px');
    }

    var label = goog.dom.createDom('span', {'id': current.getTime()});
    goog.dom.setTextContent(label,
      goog.i18n.DateTimeSymbols.SHORTWEEKDAYS[(current.getDay()) % 7] + ' ' +
      current.getDate() + ' ' +
      goog.i18n.DateTimeSymbols.STANDALONESHORTMONTHS[current.getMonth()]);

    if (bluemind.manager.getNbDays() > 1) {
      goog.dom.classes.add(label, cssClickable);
      this.getHandler().listen(label, goog.events.EventType.CLICK, function(e) {
        e.stopPropagation();
        var date = new goog.date.Date();
        date.setTime(e.target.id);
        bluemind.manager.setCurrentDate(date);
        bluemind.view.day();
      });
    }

    goog.dom.appendChild(goog.dom.getElement('dayLabel' + i), label);

    current.add(dInterval);
  }
};

/**
 * Go to the next date interval
 *
 */
bluemind.calendar.view.Days.prototype.next = function() {
  this.manager_.next();
};

/**
 * Go to the previous date interval
 *
 */
bluemind.calendar.view.Days.prototype.prev = function() {
  this.manager_.prev();
};

/**
 * Return
 *
 * @return {string} days.
 */
bluemind.calendar.view.Days.prototype.getName = function() {
  return 'days';
};

/**
 * Return this.start_
 *
 * @return {goog.date.Date} currentDate.
 */
bluemind.calendar.view.Days.prototype.getStart = function() {
  return this.start_;
};

/**
 * Return this.end_
 *
 * @return {goog.date.Date} currentDate.
 */
bluemind.calendar.view.Days.prototype.getEnd = function() {
  return this.end_;
};

/**
 * Toggle allday
 */
bluemind.calendar.view.Days.prototype.toggleAllday = function() {

  var cssDisabled = goog.getCssName('disabled');

  // hide/show events
  var containers = goog.dom.getElementsByTagNameAndClass('tr',
    goog.getCssName('containsAllday'));
  for (var i = 0; i < containers.length; i++) {
    goog.dom.classes.toggle(containers[i], cssDisabled);
  }

  // toggle allday arrow 
  goog.dom.classes.toggle(goog.dom.getElement('toggleAllday'),
    goog.getCssName('collapse'));

  var alldayCollapsed =
    goog.dom.getElementsByTagNameAndClass('tr',
    goog.getCssName('containsAlldayCollapsed'));
  goog.dom.classes.toggle(alldayCollapsed[0], goog.getCssName('disabled'));

  var table = goog.dom.getElement('table-allday');
  var adc = goog.dom.getElementsByClass(goog.getCssName('alldayCollapsed'), table);

  // hide/show popup button only if needed 
  for (var k = 0; k < adc.length; k++) {
    var d = adc[k];
    var nbitems = parseInt(goog.dom.getTextContent(d));
    if (nbitems > 0) {
      goog.dom.classes.remove(d, cssDisabled);
    } else {
      goog.dom.classes.add(d, cssDisabled);
    }
  }

  bluemind.resize();
};

/**
 * Get datetime of the slot containing given coordinate
 * in the grid
 * TODO : Optimize with a % on hour.width
 * @param {goog.math.Coordinate} coords Coordinates to be located.
 * @return {bluemind.date.DateTime} The corresponding date.
 */
bluemind.calendar.view.Days.prototype.getDateTimeForCoordinate =
  function(coords) {
  var row = 0;
  var hours = goog.dom.getElementsByTagNameAndClass('div', undefined,
    goog.dom.getElement('hourContainer')); // FIXME
  for (var i = 0; i < hours.length; i++) {
    var hour = hours[i];
    var pos = goog.style.getPageOffsetTop(hour);
    var size = goog.style.getSize(hour);
    if (pos <= coords.y && (pos + size.height) > coords.y) {
      break;
    }
    row++;
  }
  var d = new bluemind.date.DateTime(this.getDateForCoordinate(coords));
  d.add(new goog.date.Interval(0, 0, 0, Math.floor(row / 2),
    (row % 2) * 30, 0));
  return d;
};

/**
 * Get date of the column containing given coordinate
 * in the grid
 * TODO : Optimize with a % on day.width
 * @param {goog.math.Coordinate} coords Coordinates to be located.
 * @return {goog.date.Date} The corresponding date.
 */
bluemind.calendar.view.Days.prototype.getDateForCoordinate = function(coords) {
  var col = 0;
  var days = goog.dom.getElementsByTagNameAndClass('td',
    goog.getCssName('dayContainer'));
  for (var i = 0; i < days.length; i++) {
    var day = days[i];
    var pos = goog.style.getPageOffsetLeft(day);
    var size = goog.style.getSize(day);
    if (pos <= coords.x + 6 && (pos + size.width) > (coords.x + 6)) {
      break;
    }
    col++;
  }
  var d = new goog.date.Date(this.start_);
  d.add(new goog.date.Interval(0, 0, col));
  return d;
};

/**
 * @inheritDoc
 */
bluemind.calendar.view.Days.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.getHandler().removeAll();
};

/**
 * @inheritDoc
 */
bluemind.calendar.view.Days.prototype.dispose = function() {
  goog.base(this, 'dispose');
  this.getHandler().removeAll();
};

/**
 *
 */
bluemind.calendar.view.Days.prototype.getHandler = function() {
  return this.handler_ ||
         (this.handler_ = new goog.events.EventHandler(this));
};

