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
 * @fileoverview Minicalendar componnent.
 */

goog.provide('net.bluemind.calendar.list.ListView');

goog.require('goog.date.Date');
goog.require('goog.dom.classlist');
goog.require('goog.ui.DatePicker');
goog.require('goog.ui.DatePicker.Events');
goog.require('goog.dom.ViewportSizeMonitor');
goog.require('goog.events.EventType');
/**
 * View class for Calendar days view.
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.list.ListView = function(ctx, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.ctx = ctx;
};

goog.inherits(net.bluemind.calendar.list.ListView, goog.ui.Component);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.calendar.list.ListView.prototype.ctx;

/**
 * @type {goog.dom.ViewportSizeMonitor}
 */
net.bluemind.calendar.list.ListView.prototype.sizeMonitor_

/**
 * @type {goog.date.DateRange}
 */
net.bluemind.calendar.list.ListView.prototype.range;

/** @meaning calendar.list.allDay */
net.bluemind.calendar.list.ListView.MSG_ALLDAY = goog.getMsg('All day');

/** @override */
net.bluemind.calendar.list.ListView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  goog.dom.classlist.add(this.getElement(), goog.getCssName('list-view'));
};

/** @override */
net.bluemind.calendar.list.ListView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.sizeMonitor_ = new goog.dom.ViewportSizeMonitor();
  this.getHandler().listen(this.sizeMonitor_, goog.events.EventType.RESIZE, this.resize_);
  this.resize_();
};

/**
 * Resize grid
 * 
 * @param {goog.events.Event=} opt_evt
 * @private
 */
net.bluemind.calendar.list.ListView.prototype.resize_ = function(opt_evt) {
  var grid = this.getElement();
  var size = this.sizeMonitor_.getSize();
  var height = size.height - grid.offsetTop - 3;
  grid.style.height = height + 'px';
};
/**
 * Refresh view
 */
net.bluemind.calendar.list.ListView.prototype.refresh = function() {
  this.getHandler().removeAll();
  this.getHandler().listen(this.sizeMonitor_, goog.events.EventType.RESIZE, this.resize_);
  this.getDomHelper().removeChildren(this.getElement());
  this.draw_()
};

/**
 * Draw event list
 */
net.bluemind.calendar.list.ListView.prototype.draw_ = function() {
  var events = this.getModel() || [];
  var table = this.getDomHelper().createDom('table', goog.getCssName('event-list'));
  this.getDomHelper().appendChild(this.getElement(), table);
  for (var i = 0; i < events.length; i++) {
    var event = events[i];
    this.drawEvent(event, table);
  }
};

/**
 * Draw an event for one line (date) of the planning view
 * 
 * @param {Object} event Event to draw
 * @param {Element} parent
 */
net.bluemind.calendar.list.ListView.prototype.drawEvent = function(event, parent) {
  var dom = this.getDomHelper();
  var tbody = dom.createDom('tbody', {
    'id' : 'tbody' + event.date.getDayOfYear()
  });

  var tr = dom.createDom('tr');

  var tdDate = dom.createDom('td', {
    'id' : 'day' + event.date.getDayOfYear(),
    'rowspan' : 1
  }, this.dateFormat.format(event.date));

  goog.dom.classlist.add(tdDate, goog.getCssName('date'));
  if (goog.date.isSameDay(event.date, new net.bluemind.date.DateTime())) {
    goog.dom.classlist.add(tdDate, goog.getCssName('today'));
  }

  if (event.states.allday) {
    var time = net.bluemind.calendar.list.ListView.MSG_ALLDAY;
  } else {
    var time = this.timeFormat.format(event.dtstart) + ' - ' + this.timeFormat.format(event.dtend);
  }

  var tdTime = dom.createDom('td', {}, time);
  goog.dom.classlist.add(tdTime, goog.getCssName('time'));

  var tdCalendar = dom.createDom('td');

  var calendar = dom.createDom('div');
  goog.dom.classlist.add(calendar, goog.getCssName('calendar'));

  var cal = goog.array.find(this.calendars, function(calendar) {
    return calendar.uid == event.calendar;
  });

  calendar.style.backgroundColor = cal.color.background;

  dom.appendChild(tdCalendar, calendar);

  var tdDetail = dom.createDom('td', {
    'id' : event.uid + '_' + event.date.getDayOfYear()
  });
  goog.dom.classlist.add(tdDetail, goog.getCssName('evtDetail'));

  if (!event.states.synced) {
    var synced = dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-refresh') ]);
    /** @meaning general.notice.notSynchronized */
    var MSG_NOT_SYNCHRONIZED = goog.getMsg("Not all modifications are synchronized with the server yet.");
    synced.title = MSG_NOT_SYNCHRONIZED;
    dom.appendChild(tdDetail, synced);
  }
  var detail = event.summary;
  if (event.location) {
    detail += ', ' + event.location;
  }

  var evtDetail = dom.createDom('a', {}, detail);
  dom.appendChild(tdDetail, evtDetail);

  if (event.states.meeting) {
    var meeting = dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-users') ]);
    dom.appendChild(tdDetail, meeting);
  }

  if (event.states.private_) {
    var priv = dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-lock') ]);
    dom.appendChild(tdDetail, priv);
  }

  if (event.states.repeat) {
    var repeat = dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-repeat') ]);
    dom.appendChild(tdDetail, repeat);
  }

  goog.array.forEach(event.tags, function(tag) {
    var tag = dom.createDom('div', {
      'title' : tag.label,
      'style' : 'background-color: #' + tag.color
    });
    goog.dom.classlist.add(tag, goog.getCssName('tag-mark'));
    dom.appendChild(tdDetail, tag);
  });
  var fn = goog.partial(this.handleClick_, event);
  this.getHandler().listen(evtDetail, goog.events.EventType.CLICK, fn);
  goog.dom.classlist.add(evtDetail, goog.getCssName('detail'));

  if (dom.getElement('day' + event.date.getDayOfYear())) {
    var el = dom.getElement('day' + event.date.getDayOfYear());
    dom.setProperties(el, {
      'rowspan' : parseInt(el.getAttribute('rowspan')) + 1
    });
    goog.dom.classlist.add(tr, goog.getCssName('nested'));
    dom.appendChild(dom.getElement('tbody' + event.date.getDayOfYear()), tr);
  } else {
    dom.appendChild(parent, tbody);
    dom.appendChild(tbody, tr);
    dom.appendChild(tr, tdDate);
  }

  dom.appendChild(tr, tdTime);
  dom.appendChild(tr, tdCalendar);
  dom.appendChild(tr, tdDetail);

};

/**
 * @param {Object} event
 * @param {goog.event.Event} e
 */
net.bluemind.calendar.list.ListView.prototype.handleClick_ = function(event, e) {
  // FIXME : should be in presenter not in view
  var loc = goog.global['location'];

  if (event.type == 'vtodo') {
    var uri = new goog.Uri('/vtodo/consult');

  } else {
    var uri = new goog.Uri('/vevent/');
  }
  uri.getQueryData().set('uid', event.uid);
  if (event.states.exception) {
    uri.getQueryData().set('recurrence-id', event.recurrenceId.toIsoString(true, true))
  }
  uri.getQueryData().set('container', event.calendar);
  loc.hash = uri.toString();

};
