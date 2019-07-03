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
 * @fileoverview Calendar agenda view.
 */

goog.provide('bluemind.calendar.view.Agenda');

goog.require('bluemind.calendar.template.i18n');
goog.require('bluemind.utils.DateHelper');
goog.require('goog.Disposable');
goog.require('goog.date');
goog.require('goog.i18n.DateTimeFormat');

/**
 * BlueMind Calendar agenda view
 *
 * @constructor
 * @extends {goog.Disposable}
 */
bluemind.calendar.view.Agenda = function() {
  this.manager_ = bluemind.manager;
  this.manager_.setNextInterval(new goog.date.Interval(
      goog.date.Interval.DAYS, 1));
  this.manager_.setPrevInterval(new goog.date.Interval(
      goog.date.Interval.DAYS, -1));
  this.df_ = new goog.i18n.DateTimeFormat(
    goog.i18n.DateTimeFormat.Format.FULL_DATE);
  this.dtf_ = bluemind.i18n.DateTimeHelper.getInstance();
};
goog.inherits(bluemind.calendar.view.Agenda, goog.Disposable);

/**
 * Calendar manager
 *
 * @type {bluemind.calendar.Manager}
 * @private
 */
bluemind.calendar.view.Agenda.prototype.manager_;

/**
 * Agenda start date
 *
 * @type {goog.date.Date}
 * @private
 */
bluemind.calendar.view.Agenda.prototype.start_;

/**
 * Agenda end date
 *
 * @type {goog.date.Date}
 * @private
 */
bluemind.calendar.view.Agenda.prototype.end_;

/**
 * Date formatter
 *
 * @type {goog.i18n.DateTimeFormat}

 * @private
 */
bluemind.calendar.view.Agenda.prototype.df_;

/**
 * DateTime formatter
 *
 * @type {goog.i18n.DateTimeFormat}
 * @private
 */
bluemind.calendar.view.Agenda.prototype.dtf_;

/**
 * Agenda start date
 *
 * @return {goog.date.Date} start date.
 */
bluemind.calendar.view.Agenda.prototype.getStart = function() {
  return this.start_;
};

/**
 * Agenda end date
 *
 * @return {goog.date.Date} end date.
 */
bluemind.calendar.view.Agenda.prototype.getEnd = function() {
  return this.end_;
};


/**
 * display agenda
 */
bluemind.calendar.view.Agenda.prototype.display = function() {
  this.start_ = this.manager_.getCurrentDate().clone();
  this.end_ = this.start_.clone();
  this.end_.add(new goog.date.Interval(goog.date.Interval.DAYS, 7));
  this.manager_.getEvents(this.start_, this.end_).addCallback(function(events) {
    this.initGrid();
    bluemind.view.getView().buildEventsList(events);
    bluemind.resize();
  }, this);
};

/**
 * Agenda
 *
 */
bluemind.calendar.view.Agenda.prototype.initGrid = function() {
  goog.dom.getElement('viewContainer').innerHTML =
    bluemind.calendar.template.agendaView();
};

/**
 * Build events list
 * @param {Array} events Events list.
 */
bluemind.calendar.view.Agenda.prototype.buildEventsList = function(events) {
  this.initGrid();
  var start = bluemind.view.getView().getStart();
  var end = bluemind.view.getView().getEnd();

  goog.array.sort(events, function(e1, e2) {
    return goog.date.Date.compare(e1.getDate(), e2.getDate());
  });
  goog.array.forEach(events, function(evt) {
    if (bluemind.manager.isEventVisible(evt)) {
      var e = bluemind.utils.DateHelper.min(end, evt.getEnd());
      var date = evt.getDate().clone();
      while (goog.date.Date.compare(date, e) < 0) {
        if (goog.date.Date.compare(date, start) >= 0) {
          bluemind.view.getView().buildEvent(evt, date);
        }
        date.add(new goog.date.Interval(goog.date.Interval.DAYS, 1));
      }
    }
  });
};

/**
 * Draw an event for one line (date) of the planning view
 * @param {bluemind.calendar.model.Event} evt Event to draw.
 * @param {goog.date.Date} date Date of the line where the event while be drawn.
 */
bluemind.calendar.view.Agenda.prototype.buildEvent = function(evt, date) {
  var tbody = new goog.dom.createDom('tbody',
    {'id': 'tbody' + date.getDayOfYear()});

  var tr = new goog.dom.createDom('tr');

  var tdDate = new goog.dom.createDom('td',
    {'id': 'day' + date.getDayOfYear(),
     'rowspan': 1},
    this.df_.format(date));
  goog.dom.classes.add(tdDate, goog.getCssName('date'));
  if (goog.date.isSameDay(date)) {
    goog.dom.classes.add(tdDate, goog.getCssName('today'));
  }

  var time =
    this.dtf_.formatTime(date) + ' - ' + this.dtf_.formatTime(evt.getEnd());
  if (evt.isAllday()) {
    time = bluemind.calendar.template.i18n.allday();
  }

  var tdTime = new goog.dom.createDom('td', {}, time);
  goog.dom.classes.add(tdTime, goog.getCssName('time'));

  var tdCalendar = new goog.dom.createDom('td');
  goog.dom.classes.add(tdCalendar, goog.getCssName('calendar'));

  var calendar = new goog.dom.createDom('div');
  var klass = bluemind.manager.getCalendar(evt.getCalendar()).getClass();
  goog.dom.classes.add(calendar, bluemind.fixCssName(klass, false));
  goog.dom.appendChild(tdCalendar, calendar);

  var detail = evt.getTitle();
  if (evt.isPrivate()) {
    detail = bluemind.calendar.template.i18n.isPrivate();
  } else {
    if (evt.getLocation() != '') {
      detail += ', ' + evt.getLocation();
    }
  }
  var evtDetail = new goog.dom.createDom('a', {}, detail);

  if(evt.getAttendees().length > 1) {
    var meeting = goog.dom.createDom('img',
     {'src': 'themes/default/images/meeting_dark.png'});
    goog.dom.appendChild(evtDetail, meeting);
  }

  if(evt.isPrivate()) {
    var priv = goog.dom.createDom('img',
     {'src': 'themes/default/images/private_dark.png'});
    goog.dom.appendChild(evtDetail, priv);
  }

  if(evt.getRepeatKind() != 'none') {
    var repeat = goog.dom.createDom('img',
     {'src': 'themes/default/images/repeat_dark.png'});
    goog.dom.appendChild(evtDetail, repeat);
  }

  goog.array.forEach(evt.getTags(), function(tag) {
    var tag = goog.dom.createDom('div',
      {'title' : tag.getLabel(), 'style': 'background-color: #'+tag.getColor()});
    goog.dom.appendChild(evtDetail, tag);
  });

  if (evt.isUpdatable()) {
    goog.events.listen(
      evtDetail,
      goog.events.EventType.CLICK, function(e) {
        e.stopPropagation();
        bluemind.calendar.Controller.getInstance().updateEventScreen(evt);
      },
    false, this);
    goog.dom.classes.add(evtDetail, goog.getCssName('detail'));
  } else if (!evt.isPrivate()) {
    goog.events.listen(
      evtDetail,
      goog.events.EventType.CLICK,
      bluemind.calendar.Controller.getInstance().consultEventScreen, false, evt);
    goog.dom.classes.add(evtDetail, goog.getCssName('detail'));
  }

  var tdDetail = new goog.dom.createDom('td',
    {'id': evt.getExtId() + '_' + date.getDayOfYear()});
  goog.dom.classes.add(tdDetail, goog.getCssName('evtDetail'));
  goog.dom.appendChild(tdDetail, evtDetail);

  if (goog.dom.getElement('day' + date.getDayOfYear())) {
    var el = goog.dom.getElement('day' + date.getDayOfYear());
    goog.dom.setProperties(el,
    {'rowspan': parseInt(el.getAttribute('rowspan')) + 1});
    goog.dom.classes.add(tr, goog.getCssName('nested'));
    goog.dom.appendChild(
      goog.dom.getElement('tbody' + date.getDayOfYear()), tr);
  } else {
    goog.dom.appendChild(goog.dom.getElement('agenda'), tbody);
    goog.dom.appendChild(tbody, tr);
    goog.dom.appendChild(tr, tdDate);
  }

  goog.dom.appendChild(tr, tdTime);
  goog.dom.appendChild(tr, tdCalendar);
  goog.dom.appendChild(tr, tdDetail);

};

/**
 * Go to the next date interval
 *
 */
bluemind.calendar.view.Agenda.prototype.next = function() {
  this.manager_.next();
};

/**
 * Go to the previous date interval
 *
 */
bluemind.calendar.view.Agenda.prototype.prev = function() {
  this.manager_.prev();
};

/**
 * Show the current day
 */
bluemind.calendar.view.Agenda.prototype.displayToday = function() {
  this.manager_.setCurrentDate(this.manager_.getToday().clone());
};

/**
 * Return
 *
 * @return {string} agenda.
 */
bluemind.calendar.view.Agenda.prototype.getName = function() {
  return 'agenda';
};

/**
 * Return this.start_
 *
 * @return {goog.date.Date} currentDate.
 */
bluemind.calendar.view.Agenda.prototype.getStart = function() {
  return this.start_;
};

/**
 * @inheritDoc
 */
bluemind.calendar.view.Agenda.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
};
