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
 * @fileoverview View class for application day view.
 */

goog.provide("net.bluemind.calendar.day.DayView");

goog.require("goog.array");
goog.require("goog.date");
goog.require("goog.dom");
goog.require("goog.iter");
goog.require("goog.object");
goog.require("goog.string");
goog.require("goog.style");
goog.require("goog.Timer");
goog.require("goog.date.Date");
goog.require("goog.date.Interval");
goog.require("goog.dom.ViewportSizeMonitor");
goog.require("goog.dom.classlist");
goog.require("goog.events.EventType");
goog.require("goog.fx.DragScrollSupport");
goog.require("goog.fx.Dragger.EventType");
goog.require("goog.i18n.DateTimeSymbols");
goog.require("goog.i18n.DateTimeSymbols_en");
goog.require("goog.math.Coordinate");
goog.require("goog.math.Matrix");
goog.require("goog.math.Rect");
goog.require("goog.ui.Component");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.PopupBase.EventType");
goog.require("net.bluemind.calendar.day.templates");
goog.require("net.bluemind.calendar.day.ui.AllDayEvent");
goog.require("net.bluemind.calendar.day.ui.ConsultPopup");
goog.require("net.bluemind.calendar.day.ui.CreationPopup");
goog.require("net.bluemind.calendar.day.ui.Event");
goog.require("net.bluemind.calendar.day.ui.EventList");
goog.require("net.bluemind.calendar.day.ui.PrivateChangesDialog");
goog.require("net.bluemind.calendar.day.ui.RecurringDeleteDialog");
goog.require("net.bluemind.calendar.day.ui.RecurringFormDialog");
goog.require("net.bluemind.calendar.day.ui.RecurringUpdateDialog");
goog.require("net.bluemind.calendar.day.ui.UpdatePopup");
goog.require("net.bluemind.calendar.vevent.EventType");
goog.require("net.bluemind.calendar.vevent.VEventEvent");
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.date.DateTime");
goog.require("bluemind.calendar.fx.AlldayResizer");
goog.require("goog.net.Cookies");
goog.require('net.bluemind.calendar.vevent.defaultValues');
/**
 * View class for Calendar days view.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @param {net.bluemind.i18n.DateTimeHelper.Formatter} format Formatter
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.day.DayView = function(ctx, format, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.ctx = ctx;
  var popup = new net.bluemind.calendar.day.ui.CreationPopup(format, opt_domHelper);
  popup.setId('create-popup');
  this.addChild(popup, true);
  popup = new net.bluemind.calendar.day.ui.ConsultPopup(ctx, format, opt_domHelper);
  popup.setId('consult-popup');
  this.addChild(popup, true);
  popup = new net.bluemind.calendar.day.ui.UpdatePopup(ctx, format, opt_domHelper);
  popup.setId('update-popup');
  this.addChild(popup, true);
  popup = new net.bluemind.calendar.day.ui.EventList(format);
  popup.setId('events-overflow');
  this.addChild(popup, true);

  this.format = format;
  this.events_ = [];

  var cookies = new goog.net.Cookies(window.document);
  var current = cookies.get('isAlldayExpand') != "false";
  cookies.set('isAlldayExpand', (current) ? "true" : "false", 60 * 60 * 24 * 5, '/cal', null, goog.string.startsWith(window.location.protocol, 'https'));
  this.isAlldayExpand = current;
  this.alldayEventRowCount_ = null;
  
  this.timer_ = new goog.Timer(120000); // 2 minutes
  this.registerDisposable(this.timer_);
};

goog.inherits(net.bluemind.calendar.day.DayView, goog.ui.Component);

/**
 * @type {net.bluemind.date.DateRange}
 */
net.bluemind.calendar.day.DayView.prototype.range;

/**
 * @type {number}
 */
net.bluemind.calendar.day.DayView.prototype.length;

/**
 * @type {goog.date.Date}
 */
net.bluemind.calendar.day.DayView.prototype.date;

/**
 * @type {goog.i18n.DateTimeFormat}
 */
net.bluemind.calendar.day.DayView.prototype.format;

/**
 * @type {Array.<number>}
 */
net.bluemind.calendar.day.DayView.prototype.workingDays;

/**
 * @type {Array.<number>}
 */
net.bluemind.calendar.day.DayView.prototype.workingHours;

/**
 * @type {goog.i18n.TimeZone}
 */
net.bluemind.calendar.day.DayView.prototype.tz;

/**
 * @type {goog.dom.ViewportSizeMonitor}
 */
net.bluemind.calendar.day.DayView.prototype.sizeMonitor_

/**
 * @type {goog.fx.DragScrollSupport}
 */
net.bluemind.calendar.day.DayView.prototype.scroll_;

/**
 * @type {Array.<goog.ui.Component>}
 */
net.bluemind.calendar.day.DayView.prototype.events_;


/**
 * @type {number}
 */
net.bluemind.calendar.day.DayView.prototype.alldayEventRowCount_;


/** @override */
net.bluemind.calendar.day.DayView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.getElement().innerHTML = net.bluemind.calendar.day.templates.main();
  goog.dom.classlist.add(this.getElement(), goog.getCssName('day-view'));
};

/** @override */
net.bluemind.calendar.day.DayView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.sizeMonitor_ = new goog.dom.ViewportSizeMonitor();
  this.getHandler().listen(this.sizeMonitor_, goog.events.EventType.RESIZE, this.handleResize_);
  this.getHandler().listen(this.getGridElement(), goog.events.EventType.SCROLL, this.handleScroll_);
  
  this.getHandler().listen(this.timer_, goog.Timer.TICK, function() {
	  	var dom = this.getDomHelper();
		var n = new net.bluemind.date.DateTime();
		
		var hourMarkerDiv = dom.getElement("today-hour-marker");
		if (hourMarkerDiv!=null)
			dom.removeChildren(hourMarkerDiv);
		
		var todayLabel = this.getElementsByClass(goog.getCssName('todayLabel'));
		goog.array.forEach(todayLabel, function(el) {
			goog.dom.classlist.remove(el, goog.getCssName('todayLabel'));
		});
		
		var dayMarker = this.getElementsByClass(goog.getCssName('today'));
		if (dayMarker.length > 0) {
			goog.array.forEach(dayMarker, function(el) {
				goog.dom.classlist.remove(el, goog.getCssName('today'));
			});
		} 
		if (this.range.contains(n)) {
		    var i = n.getDay();
		   	goog.dom.classlist.add(dom.getElement('dayLabel' + i), goog.getCssName('todayLabel'));
		   	goog.dom.classlist.add(
		   		  dom.getElement('dayEventContainer_' + this.range.getStartDate().getWeekNumber() + '_' + i), goog
		             .getCssName('today'));
		  	goog.dom.classlist.add(dom.getElement('dayContainer' + i), goog.getCssName('today'));

		    dom.appendChild(dom.getElement('col' + i), hourMarkerDiv);
		}
		
		var hourMarker = dom.getElement("hour-marker");
		if (hourMarker != null) {
			goog.style.setStyle(hourMarker, 'top', n.getHours() * 21 * this.slot + n.getMinutes() * (21 * this.slot / 60) + 'px');
		}
		var todayHourMarker = dom.getElement("today-hour-marker");
		if (todayHourMarker != null) {
			goog.style.setStyle(todayHourMarker, 'top', n.getHours() * 21 * this.slot + n.getMinutes() * (21 * this.slot / 60) + 'px');
		}
  });
};

/**
 * Show view for given date range
 * 
 * @param {net.bluemind.date.DateRange} range Date range
 * @param {goog.date.Date} date current date
 */
net.bluemind.calendar.day.DayView.prototype.show = function(range, date) {
  // save scroll top before redrawing everything
  var lastScrollTop = this.getGridElement().scrollTop;
  this.range = range;
  this.date = date;
  this.length = range.count();
  this.getElement().style.display = 'none';
  
  this.timer_.stop();
  
  this.initGrid();
  this.initLabels();
  
  this.timer_.dispatchTick();
  this.timer_.start();
  
  goog.array.forEach(this.events_, function(event) {
    this.removeChild(event);
    event.dispose();
  }, this);
  this.events_ = [];

  this.layoutAndDrawEvents_();
  var needResize = this.drawAllDayEvents_();

  this.getElement().style.display = '';
  if (needResize) {
    this.resize_();
  }

  if (this.autoscroll) {
    var start = this.ctx.settings.get('work_hours_start');
    if (this.length == 1 && goog.date.isSameDay(date, new net.bluemind.date.DateTime())) {
      start = new net.bluemind.date.DateTime().getHours();
    }

    this.getGridElement().scrollTop = start * 21 * this.slot;
    this.autoscroll = false;
  } else if (lastScrollTop) {
    this.getGridElement().scrollTop = lastScrollTop;
  }
};

/**
 * Register an event
 * 
 * @param {goog.ui.Component} child Event
 */
net.bluemind.calendar.day.DayView.prototype.addEventChild = function(child) {
  this.addChild(child);
  this.events_.push(child);
};

/**
 * Draw the calendar grid
 */
net.bluemind.calendar.day.DayView.prototype.initGrid = function() {
  var dom = this.getDomHelper();

  dom.removeChildren(this.getDayLabelElement());
  dom.removeChildren(this.getAllDayEventElement());
  dom.removeChildren(this.getInDayEventElement());
  dom.removeChildren(this.getHourLabelElement());

  var labelContainer = this.getDayLabelElement();
  var bodyContent = dom.getElement('bodyContent');

  var tzContainer = dom.createDom('td', {
    'id' : 'tzContainer',
    'width' : '60px'
  });
  goog.dom.classlist.add(tzContainer, goog.getCssName('tzContainer'));

  dom.appendChild(labelContainer, tzContainer);

  var weekNum = dom.createDom('td', {
    'id' : 'weekNum',
    'width' : '60px'
  });
  goog.dom.classlist.add(weekNum, goog.getCssName('weekNum'));

  var toggleAllday = dom.createDom('div', {
    'id' : 'toggleAllday'
  });
  goog.dom.classlist.add(toggleAllday, goog.getCssName('toggleAllday'));
  goog.dom.classlist.add(toggleAllday, goog.getCssName('fa'));
  goog.dom.classlist.add(toggleAllday, goog.getCssName('fa-caret-down'));
  goog.dom.classlist.add(toggleAllday, goog.getCssName('disabled'));

  var weekNumContainer = dom.createDom('div', {
    'id' : 'weekNumContainer'
  });

  dom.appendChild(weekNum, toggleAllday);
  dom.appendChild(weekNum, weekNumContainer);
  dom.appendChild(this.getAllDayEventElement(), weekNum);

  var weekBg = dom.createDom('td', {
    'id' : 'allDayEventsContainer',
    'colspan' : this.length
  });
  goog.dom.classlist.add(weekBg, goog.getCssName('allDayEventsContainer'));

  var weekBgTable = dom.createDom('table', {
    'id' : 'weekBg'
  });
  goog.dom.classlist.add(weekBgTable, goog.getCssName('weekBg'));

  var weekBgTableTr = dom.createDom('tr', {
    'id' : 'weekBgTr_' + this.range.getStartDate().getWeekNumber()
  });

  dom.appendChild(weekBgTable, weekBgTableTr);
  dom.appendChild(weekBg, weekBgTable);
  dom.appendChild(this.getAllDayEventElement(), weekBg);
  this.getHandler().listen(weekBg, goog.events.EventType.MOUSEDOWN, this.handleAllDayMouseDown_);

  var leftPanelHour = dom.createDom('td', {
    'id' : 'leftPanelHour',
    'class' : goog.getCssName('leftPanelHour')
  });
  dom.appendChild(bodyContent, leftPanelHour);

  var hourLabelContainer = dom.createDom('div', {
    'id' : 'hourLabelContainer',
    'style' : 'position:relative;top:-1px;',
    'class' : goog.getCssName('hourLabelContainer')
  });
  dom.appendChild(leftPanelHour, hourLabelContainer);

  var s = this.range.getStartDate().clone();

  var cssDayLabel = goog.getCssName('dayLabel');
  var cssDayContainer = goog.getCssName('dayContainer');
  var cssColEmul = goog.getCssName('column-emul');
  var cssContainer = goog.getCssName('container');
  var cssDayEventContainer = goog.getCssName('dayEventContainer');
  var cssOutOfWork = goog.getCssName('outOfWork');
  // FIXME
  var shortWeekDays = goog.i18n.DateTimeSymbols_en.SHORTWEEKDAYS;
  goog.iter.forEach(this.range.iterator(), function(s) {
    var i = s.getDay();
    var tdLabel = dom.createDom('td', {
      'id' : 'dayLabel' + i
    });
    goog.dom.classlist.add(tdLabel, cssDayLabel);

    var td = dom.createDom('td', {
      'id' : 'dayContainer' + i
    });
    goog.dom.classlist.add(td, cssDayContainer);

    if (!goog.array.contains(this.workingDays, shortWeekDays[i].toLowerCase())) {
      goog.dom.classlist.add(td, cssOutOfWork);
    }

    var col = dom.createDom('div', {
      id : 'col' + i
    });
    goog.dom.classlist.add(col, cssColEmul);

    var div = dom.createDom('div', {
      'id' : 'd' + i
    });
    goog.dom.classlist.add(div, cssContainer);

    dom.appendChild(col, div);
    dom.appendChild(td, col);
    dom.appendChild(labelContainer, tdLabel);
    dom.appendChild(bodyContent, td);

    var tdDayEventContainer = dom.createDom('td', {
      'id' : 'dayEventContainer_' + this.range.getStartDate().getWeekNumber() + '_' + i
    });
    goog.dom.classlist.add(tdDayEventContainer, cssDayEventContainer);
    if (!goog.array.contains(this.workingDays, shortWeekDays[i].toLowerCase())) {
      goog.dom.classlist.add(tdDayEventContainer, cssOutOfWork);
    }

    dom.appendChild(weekBgTableTr, tdDayEventContainer);
    this.getHandler().listen(col, goog.events.EventType.MOUSEDOWN, this.handleInDayMouseDown_);
  }, this);

  var filler = dom.createDom('td', {
    'style' : 'width:60px;'
  });
  dom.appendChild(this.getHourLabelElement(), filler);

  var tdHourContainer = dom.createDom('td', {
    'id' : 'tdHourContainer',
    'colspan' : this.length
  });

  dom.appendChild(this.getHourLabelElement(), tdHourContainer);

  var div = dom.createDom('div', {
    'style' : 'position:relative;'
  });
  dom.appendChild(tdHourContainer, div);

  var container = dom.createDom('div', {
    'id' : 'hourContainer',
    'class' : goog.getCssName('hourContainer')
  });
  dom.appendChild(tdHourContainer, container);

  var dstart = new net.bluemind.date.DateTime(2012, 0, 1, 0, 0, 0, 0, this.tz);
  var dend = dstart.clone();
  dend.add(new goog.date.Interval(goog.date.Interval.DAYS, 1));

  // Custom date timeformat
  var i = 0;
  var cssFull = goog.getCssName('full');
  var cssHalf = goog.getCssName('half');
  var dsat = parseFloat(this.workingHours[0]);
  var deat = parseFloat(this.workingHours[1]);
  
  while (goog.date.Date.compare(dstart, dend) < 0) {
    var d = dom.createDom('div', {
      'id' : 1800 * i
    });
    var label = this.format.time.format(dstart);
    if (((i / 2) % 1) != 0) {
      label = '';
    }
    var d2 = dom.createDom('div', {
      'id' : 'h' + i
    }, label);

    if (i % 2) {
      goog.dom.classlist.add(d, cssFull);
      goog.dom.classlist.add(d2, cssFull);
    } else {
      goog.dom.classlist.add(d, cssHalf);
      goog.dom.classlist.add(d2, cssHalf);
    }

    if ((dsat < deat) && !(i / 2 >= dsat && i / 2 < deat)) {
      goog.dom.classlist.add(d, cssOutOfWork);
    } else if ((dsat > deat) && (i / 2 >= deat && i / 2 < dsat)) {
      goog.dom.classlist.add(d, cssOutOfWork);
    }

    dom.appendChild(container, d);
    dom.appendChild(hourLabelContainer, d2);
    dstart.add(new goog.date.Interval(goog.date.Interval.SECONDS, 1800));
    i++;
  }

  this.getHandler().listen(toggleAllday, goog.events.EventType.CLICK, function(e) {
    this.isAlldayExpand = !this.isAlldayExpand;
    this.ctx.cookies.set('isAlldayExpand', this.isAlldayExpand, 60 * 60 * 24 * 5, '/cal', null, goog.string.startsWith(window.location.protocol, 'https'));
    this.toggleAllday();
  });

  var hourMarker = dom.createDom('div', {
    'id' : 'hour-marker'
  });
  goog.dom.classlist.add(hourMarker, goog.getCssName('hour-marker'));
  dom.appendChild(hourLabelContainer, hourMarker);
};

/**
 * Set the calendar grid labels
 */
net.bluemind.calendar.day.DayView.prototype.initLabels = function() {
  var current = this.range.getStartDate().clone();
  var dom = this.getDomHelper();

  dom.appendChild(dom.getElement('weekNumContainer'), dom.createTextNode(current.getWeekNumber()));

  dom.appendChild(dom.getElement('tzContainer'), dom.createTextNode(this.tz.getGMTString(current)));

  var dtoday = dom.getElementsByTagNameAndClass('td', goog.getCssName('today'));
  for (var i = 0; i < dtoday.length; i++) {
    goog.dom.classlist.set(dtoday[i], goog.getCssName('dayContainer'));
  }

  dtoday = dom.getElementsByTagNameAndClass('td', goog.getCssName('todayLabel'));
  for (var i = 0; i < dtoday.length; i++) {
    goog.dom.classlist.set(dtoday[i], '');
  }

  var cssClickable = goog.getCssName('clickable');
  goog.iter.forEach(this.range.iterator(), function(date) {

    var i = date.getDay();
    if (goog.date.isSameDay(date, new net.bluemind.date.DateTime())) {
      goog.dom.classlist.add(dom.getElement('dayLabel' + i), goog.getCssName('todayLabel'));
      goog.dom.classlist.add(
          dom.getElement('dayEventContainer_' + this.range.getStartDate().getWeekNumber() + '_' + i), goog
              .getCssName('today'));
      goog.dom.classlist.add(dom.getElement('dayContainer' + i), goog.getCssName('today'));

      // Right now!
      var hourMarker = dom.createDom('div', {
        'id' : 'today-hour-marker'
      });
      goog.dom.classlist.add(hourMarker, goog.getCssName('hour-marker'));
      dom.appendChild(dom.getElement('col' + i), hourMarker);
    }

    var label = dom.createDom('span', {
      'id' : date.getTime()
    });
    dom.setTextContent(label, goog.i18n.DateTimeSymbols.SHORTWEEKDAYS[(date.getDay()) % 7] + ' ' + date.getDate() + ' '
        + goog.i18n.DateTimeSymbols.STANDALONESHORTMONTHS[date.getMonth()]);

    if (this.length > 1) {
      goog.dom.classlist.add(label, cssClickable);
      this.getHandler().listen(label, goog.events.EventType.CLICK, function(e) {
        e.stopPropagation();
        var date = new net.bluemind.date.Date();
        date.setTime(e.target.id);
        // FIXME
        var url = '/day/?range=day&date=' + date.toIsoString();
        var loc = goog.global['location'];
        loc.hash = url;
      });
    }

    dom.appendChild(dom.getElement('dayLabel' + i), label);

  }, this);
};

/**
 * Toggle allday
 */
net.bluemind.calendar.day.DayView.prototype.toggleAllday = function() {

  var cssDisabled = goog.getCssName('disabled');

  // hide/show events
  var containers = this.getDomHelper().getElementsByTagNameAndClass('tr', goog.getCssName('containsAllday'));
  for (var i = 0; i < containers.length; i++) {
    goog.dom.classlist.enable(containers[i], cssDisabled, !this.isAlldayExpand);
  }

  // toggle allday arrow
  goog.dom.classlist.enable(this.getDomHelper().getElement('toggleAllday'), goog.getCssName('fa-caret-right'),
      !this.isAlldayExpand);
  goog.dom.classlist.enable(this.getDomHelper().getElement('toggleAllday'), goog.getCssName('fa-caret-down'),
      this.isAlldayExpand);

  var alldayCollapsed = this.getDomHelper().getElementsByTagNameAndClass('tr',
      goog.getCssName('containsAlldayCollapsed'));
  if (alldayCollapsed.length > 0) {
    goog.dom.classlist.enable(alldayCollapsed[0], goog.getCssName('disabled'), this.isAlldayExpand);
  }

  var table = this.getAllDayElement();
  var adc = this.getDomHelper().getElementsByClass(goog.getCssName('alldayCollapsed'), table);

  // hide/show popup button only if needed
  for (var k = 0; k < adc.length; k++) {
    var d = adc[k];
    goog.dom.classlist.remove(d, cssDisabled);
  }

  this.resize_();
};

/**
 * Get datetime of the slot containing given coordinate in the grid TODO :
 * Optimize with a % on hour.width
 * 
 * @param {goog.math.Coordinate} coords Coordinates to be located.
 * @return {net.bluemind.date.DateTime} The corresponding date.
 */
net.bluemind.calendar.day.DayView.prototype.getDateTimeForCoordinate = function(coords) {
  // FIXME: There is issues with firefox and coords like this :
  // 260.03334045410156
  coords.y = Math.ceil(coords.y);
  var row = 0;
  var hours = this.getDomHelper().getElementsByTagNameAndClass('div', null,
      this.getDomHelper().getElement('hourContainer')); // FIXME
  for (var i = 0; i < hours.length; i++) {
    var hour = hours[i];
    var pos = goog.style.getPageOffsetTop(hour);
    var size = goog.style.getSize(hour);
    if (pos <= coords.y && (pos + size.height) > coords.y) {
      break;
    }
    row++;
  }
  var d = new net.bluemind.date.DateTime(this.getDateForCoordinate(coords), this.tz);
  var minutes = row * (60 / this.slot);
  d.setHours(Math.floor(minutes / 60));
  d.setMinutes(minutes % 60);
  return d;
};

/**
 * Resize grid
 * 
 * @param {goog.events.Event=} opt_evt
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.resize_ = function() {
  var grid = this.getGridElement();
  var size = this.sizeMonitor_.getSize();
  var height = size.height - grid.offsetTop - 3;
  grid.style.height = height + 'px';

  var scrollWidth = grid.offsetWidth - grid.clientWidth;
  this.getHeadElement().style.paddingRight = scrollWidth + 'px';
};

/**
 * Handle Resize grid
 * 
 * @param {goog.events.Event=} opt_evt
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.handleResize_ = function(opt_evt) {
  this.resize_();
  this.getChild('create-popup').hide();
};

/**
 * Handle Resize grid
 * 
 * @param {goog.events.Event=} opt_evt
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.handleScroll_ = function(opt_evt) {
  this.getChild('create-popup').hide();
};

/**
 * Get date of the column containing given coordinate in the grid TODO :
 * Optimize with a % on day.width
 * 
 * @param {goog.math.Coordinate} coords Coordinates to be located.
 * @return {goog.date.Date} The corresponding date.
 */
net.bluemind.calendar.day.DayView.prototype.getDateForCoordinate = function(coords) {
  var col = 0;
  var days = this.getDomHelper().getElementsByTagNameAndClass('td', goog.getCssName('dayContainer'));
  for (var i = 0; i < days.length; i++) {
    var day = days[i];
    var pos = goog.style.getPageOffsetLeft(day);
    var size = goog.style.getSize(day);
    if (pos <= (coords.x + 6) && (pos + size.width) > (coords.x + 6)) {
      break;
    }
    col++;
  }
  var d = this.range.getStartDate().clone();
  d.add(new goog.date.Interval(0, 0, col));
  return d;
};

/**
 * Get day grid
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.getHeadElement = function() {
  return this.getElement() && /** @type {Element} */
  (this.getElement().firstChild);
};

/**
 * Get all day container element
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.getAllDayElement = function() {
  return this.getElement() && /** @type {Element} */
  (this.getHeadElement().firstChild);
};

/**
 * Get day label container
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.getDayLabelElement = function() {
  return this.getElement() && /** @type {Element} */
  (this.getAllDayElement().firstChild.firstChild);
};

/**
 * Get all day container element
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.getAllDayEventElement = function() {
  return this.getElement() && /** @type {Element} */
  (this.getAllDayElement().firstChild.lastChild);
};

/**
 * Get day grid
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.getGridElement = function() {
  return this.getElement() && /** @type {Element} */
  (this.getElement().firstChild.nextSibling);
};

/**
 * Get in day container element
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.getInDayElement = function() {
  return this.getElement() && /** @type {Element} */
  (this.getGridElement().firstChild);
};

/**
 * Get hour label
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.getHourLabelElement = function() {
  return this.getElement() && /** @type {Element} */
  (this.getInDayElement().firstChild.firstChild);
};

/**
 * Get Event container
 * 
 * @return {Element}
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.getInDayEventElement = function() {
  return this.getElement() && /** @type {Element} */
  (this.getInDayElement().firstChild.lastChild);
};

/**
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.computeLayout_ = function() {
  var model = this.getModel();
  var updated = {};
  goog.object.forEach(model.days, function(day, v) {
    var unit;
    goog.object.forEach(day, function(cell, key) {
      cell.sort(function(evt1, evt2) {
        var diff = evt1.dtstart.getTime() - evt2.dtstart.getTime();
        if (diff != 0)
          return diff;
        diff = evt2.dtend.getTime() - evt1.dtend.getTime();
        if (diff != 0)
          return diff;
        diff = evt1.calendar - evt2.calendar;
        if (diff != 0)
          return diff;
        return goog.getUid(evt1) - goog.getUid(evt2);
      });
      var usedPositions = {};
      var position = 0;

      goog.array.forEach(cell, function(evt, idx) {
        var updatedId = goog.getUid(evt) + '';
        var coords;
        if (!(coords = goog.object.get(updated, updatedId))) {
          if (goog.object.isEmpty(usedPositions))
            unit = {
              value : 1
            };
          while (goog.object.containsKey(usedPositions, position)) {
            position++;
          }
          var end = {
            value : position
          };
          if ((idx + 1) == cell.length) {
            while (end.value < unit.value && !goog.object.containsKey(usedPositions, end.value)) {
              end.value++;
            }
          }
          if (end.value == unit.value)
            end = unit;
          coords = {
            position : position,
            unit : unit,
            end : end,
            occurrence : evt
          };
          goog.object.add(updated, updatedId, coords);
        }
        goog.object.set(usedPositions, coords.position, true);
        if (cell.length > unit.value)
          unit.value = cell.length;
        if ((coords.position + 1) < cell.length && (idx + 1) < cell.length) {
          coords.end = {
            value : coords.position + 1
          };
        }
      });
    });
  });
  return updated;
}

/**
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.drawEvents_ = function(updated) {
  var callback = goog.bind(this.getDateTimeForCoordinate, this);
  var minute = new goog.date.Interval(0, 0, 0, 0, -1);
  goog.object.forEach(updated, function(coords) {
    var size = coords.end.value - coords.position;
    var event = coords.occurrence;
    var unit = coords.unit.value;
    var position = coords.position;
    var container = this.getDomHelper().getElement('d' + event.dtstart.getDay());
    var child = new net.bluemind.calendar.day.ui.Event(event, callback, this.format.time, this.calendars, this
        .getDomHelper());
    child.createDom();
    var topPosition = event.dtstart.getHours() + event.dtstart.getMinutes() / 60;
    var dtend = event.dtend.clone();
    dtend.add(minute)
    var height = (dtend.getHours() + dtend.getMinutes() / 60) - topPosition;
    if (height <= 0) height = 0.5;
    
    child.getElement().style.bottom = null;
    child.getElement().style.right = null;
    child.getElement().style.left = (100 / unit * position) + '%';
    child.getElement().style.top = (topPosition * 21 * this.slot) + 'px';
    child.getElement().style.width = (100 / unit * size) + '%';
    child.getElement().style.height = (Math.round(height * 21 * this.slot) - 1) + 'px';

    
    this.addEventChild(child);
    child.render(container);
    this.getHandler().listen(child, goog.events.EventType.MOUSEDOWN, this.handleEventMouseDown_);
    this.getHandler().listen(child, goog.ui.Component.EventType.CHANGE, this.handleEventChanged_);
  }, this);
}
/**
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.layoutAndDrawEvents_ = function() {
  var updated = this.computeLayout_();
  this.drawEvents_(updated);
};

/**
 * redraw allday events
 * 
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.drawAllDayEvents_ = function() {
  var model = this.getModel();
  var nbDays = 7; // FIXME: can't display more than 7 days in a week
  var cssDisabled = goog.getCssName('disabled');
  var cssContainsAllday = goog.getCssName('containsAllday');
  var cssSpacer = goog.getCssName('spacer');
  var cssContainsAlldayCollapsed = goog.getCssName('containsAlldayCollapsed');
  var cssAlldayCollapsed = goog.getCssName('alldayCollapsed');
  var oneDayInterval = new goog.date.Interval(0, 0, 1);
  var events = [];
  var ret = false;

  var dayIndex = 0;
  goog.object.forEach(model.weeks, function(evts, weeknum) {
    var nbEventsPerDay = [];
    for (var i = 0; i < nbDays; i++) {
      nbEventsPerDay[i] = 0;
    }
    var trs = new goog.math.Matrix(1, nbDays);

    evts.sort(function(evt1, evt2) {
      var d1 = evt1.dtstart.clone();
      var d2 = evt2.dtstart.clone();
      if (!evt1.states.allday && evt1.size > 1) {
        d1.setHours(0);
      }
      if (!evt2.states.allday && evt2.size > 1) {
        d2.setHours(0);
      }
      var diff = d1.getTime() - d2.getTime();
      if (diff != 0)
        return diff;
      diff = evt2.dtend.getTime() - evt1.dtend.getTime();
      if (diff != 0)
        return diff;
      diff = evt1.calendar - evt2.calendar;
      if (diff != 0)
        return diff;
      return goog.getUid(evt1) - goog.getUid(evt2);
    });

    goog.array.forEach(evts, function(evt) {
      var coords = {
        row : 0,
        evt : evt
      };
      var evtPosition = new goog.math.Matrix(1, nbDays);
      var evtInterval = new goog.date.Interval(0, 0, -evt.start.getDay() + evt.startDay);

      var begin = evt.start.clone();
      begin.add(evtInterval);
      for (var i = 0; i < evt.size; i++) {
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
        begin = evt.start.clone();
        begin.add(evtInterval);
        for (var i = 0; i < evt.size; i++) {
          var idx = begin.getDay();
          begin.add(oneDayInterval);
          nbEventsPerDay[idx] = size + 1;
        }
      } else {
        // add event to "free" row
        begin = evt.start.clone();
        begin.add(evtInterval);
        for (var i = 0; i < evt.size; i++) {
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
      var delta = 0;
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
          }, goog.dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-caret-down') ]));

          goog.dom.appendChild(td, collapsed);
          this.getHandler().listen(collapsed, goog.events.EventType.MOUSEDOWN, function(e) {
            e.stopPropagation();
            var id = e.currentTarget.id.split('_');
            var date = this.range.getStartDate().clone();
            date.add(new goog.date.Interval(goog.date.Interval.DAYS, parseInt(id[1])));
            this.getChild("events-overflow").setCalendars(this.calendars);

            this.getChild('events-overflow').setModel(this.getModel().allday[date.getDay()]);
            this.getChild('events-overflow').date = date;
            this.getChild('events-overflow').setVisible(true);
            this.getChild('events-overflow').attach(e.currentTarget);
          }, false, collapsed);
        }
      }
    }

  }, this);

  var callback = goog.bind(this.getDateForCoordinate, this);
  var r = 0;
  goog.array.forEach(events, function(coords) {
    var evt = coords.evt;
    var idx = coords.row
    var trId = 'weekBgTr_' + evt.start.getWeekNumber();
    var container = this.getDomHelper().getElement(
        'dayEventContainer_' + evt.start.getWeekNumber() + '_' + evt.start.getDay() + '_' + idx);
    goog.dom.setProperties(container, {
      'colspan' : evt.size
    });
    var tr = goog.dom.getElement(trId + '_' + idx);
    for (var i = 0; i < (evt.size - 1); i++) {
      var td = goog.dom.getNextElementSibling(container);
      if (goog.dom.contains(tr, td)) {
        goog.dom.removeNode(td);
      }
    }
    var child = new net.bluemind.calendar.day.ui.AllDayEvent(evt, callback, this.calendars, this.getDomHelper());
    child.createDom();
    this.addEventChild(child);
    this.getHandler().listen(child, goog.events.EventType.MOUSEDOWN, this.handleEventMouseDown_);
    this.getHandler().listen(child, goog.ui.Component.EventType.CHANGE, this.handleEventChanged_);
    child.render(container);
    r = Math.max(r, idx);
  }, this);

  if (events.length > 0) {
    var tad = goog.dom.getElement('toggleAllday');
    goog.dom.classlist.remove(tad, cssDisabled);
  }
  if (this.alldayEventRowCount_ == null || r != this.alldayEventRowCount_) {
    ret = true;
  }
  this.alldayEventRowCount_ = r;

  return ret;
};

/**
 * @param {goog.events.BrowserEvent} e
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.handleEventChanged_ = function(e) {
  var child = e.target;
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.SAVE, child.getModel());
  this.dispatchEvent(e)
};

/**
 * @param {goog.events.BrowserEvent} e
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.handleEventMouseDown_ = function(e) {
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

/**
 * @param {goog.events.BrowserEvent} e
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.handleInDayMouseDown_ = function(e) {
  var calendars = this.getWritablesCalendars_();
  if (e.isMouseActionButton() && calendars.length > 0) {
    var left = goog.style.getPageOffsetLeft(e.target);
    var date = this.getDateTimeForCoordinate(new goog.math.Coordinate(left, e.clientY));
    if (!this.range.contains(date)) {
      return;
    }
    var duration = new goog.date.Interval(goog.date.Interval.MINUTES, (60 / this.slot));
    var end = date.clone();
    end.add(duration);
    
    var event = {
      summary : '',
      tooltip : '',
      location : '',
      draft: true,
      dtstart : date,
      dtend : end,
      start : date,
      end : end,
      timezones: {
        start: date.getTimeZone(),
        end: date.getTimeZone()
      },
      states : {
        allday : false,
        pending : false,
        private_ : false,
        busy : true,
        updatable : true,
        short : false,
        past : (end.getTime() < goog.now()),
        meeting : false,
        main: true,
        master : true,
        draft: true
      },
      tags : [],
      duration : duration.getTotalSeconds(),
      uid: net.bluemind.mvp.UID.generate(),
      calendar : this.getDefaultCalendar_().uid
    };
    
    if (this.ctx.settings.get('default_event_alert') && !isNaN(parseInt(this.ctx.settings.get('default_event_alert')))) {
      var alarmAction = net.bluemind.calendar.vevent.defaultValues.action;
      if (this.ctx.settings.get('default_event_alert_mode')){
        alarmAction = this.ctx.settings.get('default_event_alert_mode');
      }
      event.alarm = [{
        trigger : this.ctx.settings.get('default_event_alert'),
        action : alarmAction
      }]
    }
    
    var callback = goog.bind(this.getDateTimeForCoordinate, this);
    var container = this.getDomHelper().getElement('d' + date.getDay());
    var child = new net.bluemind.calendar.day.ui.Event(event, callback, this.format.time, this.calendars, this
        .getDomHelper());
    child.createDom();
    // evt.createDummyDom(1, 0, 1);
    var top = date.getHours() + (date.getMinutes() / 60);
    var unit = 21 * this.slot;
    child.getElement().style.bottom = null;
    child.getElement().style.right = null;
    child.getElement().style.top = (top * unit) + 'px';
    child.getElement().style.width = null;
    child.getElement().style.left = '0%';
    child.getElement().style.height = (((event.duration / 3600) * unit) - 1) + 'px';
    this.addEventChild(child);
    child.render(container);
    child.forceResize(e);
    this.getHandler().listenOnce(child, goog.ui.Component.EventType.CHANGE, this.createIndayEvent_);
  }
};

/**
 * @param {goog.events.BrowserEvent} e
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.handleAllDayMouseDown_ = function(e) {
  var calendars = this.getWritablesCalendars_();
  if (e.isMouseActionButton() && calendars.length > 0) {
    var left = goog.style.getPageOffsetLeft(e.target);
    var date = this.getDateForCoordinate(new goog.math.Coordinate(left, e.clientY));
    if (!this.range.contains(date)) {
      return;
    }
    var el = /** @type {Element} */
    (goog.dom.getAncestorByTagNameAndClass(e.target, 'table'));
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
      this.createAlldayEvent_(date, e.dragger, bounds);
    });

  }
};

/**
 * Create in day event
 * 
 * @param {goog.events.Event} e Create event
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.createIndayEvent_ = function(e) {
  var child = e.target;
  var popup = this.getChild('create-popup');
  popup.setCalendars(this.calendars);
  popup.setModel(child.getModel());
  popup.attach(child.getElement());
  popup.setVisible(true);
  child.getHandler().listen(popup, net.bluemind.calendar.vevent.EventType.CHANGE, function() {
    this.refreshContainer();
  });
  this.getHandler().listenOnce(popup, goog.ui.PopupBase.EventType.HIDE, function() {
    // FIXME Do be do
    child.dispose();
  });

};

/**
 * On event dragged
 * 
 * @param {goog.date.Date} start Start date
 * @param {bluemind.calendar.fx.AlldayResizer} resizer Resizer.
 * @param {goog.math.Rect} bounds Drag relative bounds
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.createAlldayEvent_ = function(start, resizer, bounds) {
  var element = this.getDomHelper().getFirstElementChild(resizer.getDraw());
  var size = goog.style.getSize(element);
  var days = Math.round(size.width / bounds.width);

  var top = goog.style.getPageOffsetTop(element) - bounds.top;
  var left = Math.abs(goog.style.getPageOffsetLeft(element) - bounds.left);

  var end = start.clone();
  end.add(new goog.date.Interval(0, 0, 1));
  var duration;

  if (top <= 0 && (top != 0 || left >= 1)) {
    start = end.clone();
    start.add(new goog.date.Interval(0, 0, -days));
  } else {
    end = start.clone();
    end.add(new goog.date.Interval(0, 0, days));
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
    draft: true,
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
      master: true,
      main: true,
      draft: true
    },
    tags : [],
    calendars : calendars,
    uid: net.bluemind.mvp.UID.generate(),
    calendar : this.getDefaultCalendar_().uid
  };
  
  if (this.ctx.settings.get('default_allday_event_alert') && !isNaN(parseInt(this.ctx.settings.get('default_allday_event_alert')))) {
    var alarmAction = net.bluemind.calendar.vevent.defaultValues.action;
    if (this.ctx.settings.get('default_event_alert_mode')){
      alarmAction = this.ctx.settings.get('default_event_alert_mode');
    }
    evt.alarm = [{
      trigger : this.ctx.settings.get('default_allday_event_alert'),
      action : alarmAction
    }]
  }

  var popup = this.getChild('create-popup');
  popup.setCalendars(this.calendars);
  popup.setModel(evt);
  popup.attach(element);
  popup.setVisible(true);
  this.getHandler().listenOnce(popup, goog.ui.PopupBase.EventType.HIDE, function() {
    // FIXME Do be do
    resizer.dispose();
  });
};

/**
 * On event dragged
 * 
 * @param {goog.events.Event} e Drag event
 */
net.bluemind.calendar.day.DayView.prototype.onDrag = function(e) {

};

/**
 * On event dragged
 * 
 * @param {goog.events.Event} e Drag event
 */
net.bluemind.calendar.day.DayView.prototype.onDragStart = function(e) {
  this.scroll_ = new goog.fx.DragScrollSupport(this.getGridElement(), 10);
  this.registerDisposable(this.scroll_);
};

/**
 * On event drag stopped
 * 
 * @param {goog.events.Event} e Drag event
 */
net.bluemind.calendar.day.DayView.prototype.onDragEnd = function(e) {
  this.scroll_.dispose();
};

/**
 * @return {Array} writable calendars
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.getWritablesCalendars_ = function() {
  return goog.array.filter(this.calendars, function(calendar) {
    return calendar.states.writable;
  });
};

/**
 * @return {Object} Default calendar
 * @private
 */
net.bluemind.calendar.day.DayView.prototype.getDefaultCalendar_ = function() {
  var wCals = goog.array.filter(this.calendars, function(calendar) {
    return calendar.states.writable;
  });
  for (var i = 0; i < wCals.length; i++) {
    if (wCals[i].states.main) {
      return wCals[i];
    }
  }
  return wCals[0];
};
