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
 * @fileoverview Events list componnent.
 */

goog.provide('bluemind.calendar.ui.event.List');

goog.require('bluemind.calendar.event.template');
goog.require('bluemind.calendar.template.i18n');
goog.require('goog.date');
goog.require('goog.i18n.DateTimeFormat');
goog.require('goog.i18n.DateTimeFormat.Format');
goog.require('goog.soy');


/**
 * @param {goog.date.Date} from from.
 * @param {goog.date.Date} to to.
 * @param {Array} events Event extIds.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.event.List = function(from, to, events, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.df_ = new goog.i18n.DateTimeFormat(
    goog.i18n.DateTimeFormat.Format.FULL_DATE);
  this.events_ = events;
  this.from_ = from;
  this.to_ = to;

  this.dateTimeHelper_ = bluemind.i18n.DateTimeHelper.getInstance();

};
goog.inherits(bluemind.calendar.ui.event.List, goog.ui.Component);

/**
 * date time helper
 * @type {bluemind.i18n.DateTimeHelper}
 * @private
 */
bluemind.calendar.ui.event.List.prototype.dateTimeHelper_;

/**
 * Date formatter
 *
 * @type {goog.i18n.DateTimeFormat}

 * @private
 */
bluemind.calendar.ui.event.List.prototype.df_;

/**
 * Events
 *
 * @type {Array}
 */
bluemind.calendar.ui.event.List.prototype.events_;

/**
 * From
 *
 * @type {goog.date.Date}
 */
bluemind.calendar.ui.event.List.prototype.from_;

/**
 * To
 *
 * @type {goog.date.Date}
 */
bluemind.calendar.ui.event.List.prototype.to_;

/**
 * Events
 *
 * @type {Array}
 */
bluemind.calendar.ui.event.List.prototype.events;

/** @inheritDoc */
bluemind.calendar.ui.event.List.prototype.createDom = function() {
  var back = new bluemind.calendar.ui.control.BackToCalendar();
  var tb = bluemind.ui.Toolbar.getInstance();
  tb.removeChildren();
  tb.getWest().addChild(back, true);

  if (this.events_.length == 0) {
    var element = goog.soy.renderAsElement(
      bluemind.calendar.event.template.emptyResultList);
    this.setElementInternal(/** @type {Element} */ (element));
    goog.dom.getElement('viewContainer').innerHTML = '';
    goog.dom.appendChild(goog.dom.getElement('viewContainer'), element);
  } else {
    var element = goog.soy.renderAsElement(
      bluemind.calendar.event.template.list);
    this.setElementInternal(/** @type {Element} */ (element));
    goog.dom.getElement('viewContainer').innerHTML = '';
    goog.dom.appendChild(goog.dom.getElement('viewContainer'), element);

    var sortedEvents = new Array();
    var startDay = this.dateTimeHelper_.formatDate(this.from_);
    var start = this.from_.getTime();
    var end = this.to_.getTime();

    goog.object.forEach(this.events_, function(ev) {
      if (ev) {
        var ocs = ev['occurrences'];
        var duration = ev['duration'];
        goog.object.forEach(ocs, function(oc) {
          var ocStart = oc['date'];
          var ocEnd = ocStart + (duration * 1000);
          if (ocStart <= end && ocEnd >= start &&
            bluemind.manager.visibleCalendars_.containsKey(oc['calendar'])) {
            ev['date'] = ocStart;
            ev['calendar'] = oc['calendar'];
            ev['participation'] = oc['participation'];
            ev['calendarLabel'] = oc['calendarLabel'];
            ev['klass'] =
              bluemind.manager.getCalendar(ev['calendar']).getClass();
            var evt = new bluemind.calendar.model.Event(ev);
            if (bluemind.manager.isEventVisible(evt)) {
              if (goog.date.Date.compare(evt.getDate(), this.from_) < 0) {
                evt.getDate().set(this.from_);
              }
              goog.array.insert(sortedEvents, evt);
              goog.array.sort(sortedEvents, function(e1, e2) {
                return goog.date.Date.compare(e1.getDate(), e2.getDate());
              });
            }
          }
        }, this);
      }
    }, this);

    var oneDayInterval = new goog.date.Interval(goog.date.Interval.DAYS, 1);
    goog.array.forEach(sortedEvents, function(evt) {
      var evtBegin = evt.getDate().clone();
      var evtEnd = evt.getEnd().clone();
      var size =
        Math.ceil((evtEnd.getTime() - evtBegin.getTime()) / 86400000);
      if (!evt.isAllday()) {
        var b = evt.getDate().clone();
        b.setHours(0);
        b.setMinutes(0);
        b.setSeconds(0);
        var e = evt.getEnd().clone();
        e.add(new goog.date.Interval(0, 0, 1, 0, 0, -1));
        e.setHours(0);
        e.setMinutes(0);
        e.setSeconds(0);
        size = Math.ceil((e.getTime() - b.getTime()) / 86400000);
      }
      size = Math.max(1, size);

      if (goog.date.Date.compare(evtEnd, this.to_) > 0) {
        var diff =
          Math.ceil((evtEnd.getTime() - this.to_.getTime()) / 86400000);
        size = size - diff;
      }

      if (goog.date.Date.compare(evtBegin, this.from_) < 0) {
        var diff =
          Math.ceil((this.from_.getTime() - evtBegin.getTime()) / 86400000);
        size = size - diff;
      }
      for (var d = 0; d < size; d++) {
        this.add_(evt);
        evt.getDate().add(oneDayInterval);
      }
    }, this);
  }
  bluemind.resize();
};

/**
 * Add event to list
 * @param {bluemind.calendar.model.Event} evt Event.
 * @private
 */
bluemind.calendar.ui.event.List.prototype.add_ = function(evt) {

  var evtId = this.dateTimeHelper_.formatDate(evt.getDate());

  var tbody = new goog.dom.createDom('tbody',
    {'id': 'tbody' + evtId});

  var tr = new goog.dom.createDom('tr');

  var tdDate = new goog.dom.createDom('td',
    {'id': 'day' + evtId,
     'rowspan': 1},
    this.df_.format(evt.getDate()));
  goog.dom.classes.add(tdDate, goog.getCssName('date'));
  if (goog.date.isSameDay(evt.getDate())) {
    goog.dom.classes.add(tdDate, goog.getCssName('today'));
  }

  var time =
    this.dateTimeHelper_.formatTime(evt.getDate()) +
      ' - ' + this.dateTimeHelper_.formatTime(evt.getEnd());
  if (evt.isAllday()) {
    time = bluemind.calendar.template.i18n.allday();
  }

  var tdTime = new goog.dom.createDom('td', {}, time);
  goog.dom.classes.add(tdTime, goog.getCssName('time'));

  var tdCalendar = new goog.dom.createDom('td');
  goog.dom.classes.add(tdCalendar, goog.getCssName('calendar'));

  var klass = bluemind.manager.getCalendar(evt.getCalendar()).getClass();
  var calendar = new goog.dom.createDom('div');
  goog.dom.classes.add(calendar, bluemind.fixCssName(klass, false));
  goog.dom.appendChild(tdCalendar, calendar);

  var detail = evt.getTitle();
  if (evt.getLocation() != '') {
    detail += ', ' + evt.getLocation();
  }
  var tdDetail = new goog.dom.createDom('td',
    {'id': evt.getExtId() + '_' + evtId}, detail);
  goog.dom.classes.add(tdDetail, goog.getCssName('evtDetail'));

  if (goog.dom.getElement('day' + evtId)) {
    var el = goog.dom.getElement('day' + evtId);
    goog.dom.setProperties(el,
    {'rowspan': parseInt(el.getAttribute('rowspan')) + 1});
    goog.dom.classes.add(tr, goog.getCssName('nested'));
    goog.dom.appendChild(
      goog.dom.getElement('tbody' + evtId), tr);
  } else {
    goog.dom.appendChild(goog.dom.getElement('list'), tbody);
    goog.dom.appendChild(tbody, tr);
    goog.dom.appendChild(tr, tdDate);
  }

  goog.dom.appendChild(tr, tdTime);
  goog.dom.appendChild(tr, tdCalendar);
  goog.dom.appendChild(tr, tdDetail);
};

