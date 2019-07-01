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
 * @fileoverview Allday event list bubble componnent.
 */

goog.provide("net.bluemind.calendar.day.ui.EventList");

goog.require("goog.array");
goog.require("goog.date");
goog.require("goog.dom");
goog.require("goog.date.Date");
goog.require("goog.date.Interval");
goog.require("goog.dom.classes");
goog.require("goog.dom.classlist");
goog.require("net.bluemind.calendar.day.ui.Popup");

/**
 * @param {net.bluemind.i18n.DateTimeHelper.Formatter} format
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.day.ui.EventList = function(format, calendars, opt_domHelper) {
  goog.base(this, format, opt_domHelper);
  this.calendars_ = calendars;

};
goog.inherits(net.bluemind.calendar.day.ui.EventList, net.bluemind.calendar.day.ui.Popup);

/**
 * Calendars
 * 
 * @type {Array}
 */
net.bluemind.calendar.day.ui.EventList.prototype.calendars_;

/** @inheritDoc */
net.bluemind.calendar.day.ui.EventList.prototype.setModel = function(obj) {
  goog.base(this, 'setModel', obj);
};

/** @inheritDoc */
net.bluemind.calendar.day.ui.EventList.prototype.setCalendars = function(calendars) {
  this.calendars_ = calendars;
};

/** @inheritDoc */
net.bluemind.calendar.day.ui.EventList.prototype.setVisible = function(visible) {
  var container = this.getContentElement();
  goog.dom.removeChildren(container);
  if (visible) {
    goog.dom.setTextContent(this.getElementByClass(goog.getCssName('eb-title')), this.format.date.format(this.date));
    goog.array.forEach(this.getModel(), function(e) {
      goog.dom.appendChild(container, this.event_(e));
    }, this);
    this.popup_.setVisible(true);
    this.setListeners();
  } else {
    this.popup_.setVisible(false);
    this.unsetListeners_();
  }
};

/**
 * @param {Object} event Event model
 * @private
 */
net.bluemind.calendar.day.ui.EventList.prototype.event_ = function(event) {
  if (event.states.allday) {
    var summary = event.summary;
  } else {
    var summary = event.dtstart.toIsoTimeString(false) + ' ' + event.summary;
  }
  var classlist = [];

  classlist.push(goog.getCssName('event'));
  classlist.push(goog.getCssName('allDayEvent'));

  if (event.states.pending) {
    classlist.push(goog.getCssName('pending'));
  }
  if (event.states.declined) {
    classlist.push(goog.getCssName('declined'));
  }
  if (event.states.private_) {
    classlist.push(goog.getCssName('private'));
  }
  if (event.states.updatable) {
    classlist.push(goog.getCssName('updatable'));
  }
  if (event.states.short) {
    classlist.push(goog.getCssName('inday'));
  }

  if (event.states.meeting) {
    classlist.push(goog.getCssName('meeting'));
  }
  if (!event.states.synced) {
    classlist.push(goog.getCssName('not-synchronized'));
  } else {
    if (event.states.past) {
      classlist.push(goog.getCssName('past'));
    }
  }
  classlist.push(goog.getCssName('calendar'));

  var left = !goog.date.isSameDay(event.dtstart, this.date);
  var next = this.date.clone();
  next.add(new goog.date.Interval(0, 0, 1));
  var right = (goog.date.Date.compare(event.dtend, next) > 0);

  var content = goog.dom.createDom('div');
  goog.dom.classlist.addAll(content, classlist);

  var calendar = goog.array.find(this.calendars_, function(calendar) {
    return calendar.uid == event.calendar;
  });

  if (event.states.short) {
    content.style.color = calendar.color.single;
  } else {
    content.style.backgroundColor = calendar.color.background;
    content.style.color = calendar.color.foreground;
  }

  content.title = event.tooltip;
  content.innerHTML = net.bluemind.calendar.day.templates.overflow(event);

  var table = goog.dom.createDom('table');
  goog.dom.classes.add(table, goog.getCssName('alldayEvents'));

  var tr = goog.dom.createDom('tr');
  goog.dom.appendChild(table, tr);

  var td = goog.dom.createDom('td');
  goog.dom.classes.add(td, goog.getCssName('left'));
  goog.dom.appendChild(tr, td);
  if (left) {
    var leftExtension = goog.dom.createDom('div');
    goog.dom.classlist.addAll(leftExtension, classlist);
    leftExtension.style.backgroundColor = calendar.color.background;
    leftExtension.style.color = calendar.color.foreground;
    leftExtension.title = event.tooltip;
    leftExtension.innerHTML = net.bluemind.calendar.day.templates.overflowExtension(event);
    goog.dom.appendChild(td, leftExtension);

  }

  td = goog.dom.createDom('td');
  goog.dom.appendChild(td, content);
  goog.dom.appendChild(tr, td);

  var td = goog.dom.createDom('td');
  goog.dom.classes.add(td, goog.getCssName('right'));
  goog.dom.appendChild(tr, td);

  if (right) {
    var rightExtension = goog.dom.createDom('div');
    goog.dom.classlist.addAll(rightExtension, classlist);
    rightExtension.style.backgroundColor = calendar.color.background;
    rightExtension.style.color = calendar.color.foreground;
    rightExtension.title = event.tooltip;
    rightExtension.innerHTML = net.bluemind.calendar.day.templates.overflowExtension(event);
    goog.dom.appendChild(td, rightExtension);
  }
  return table;
};
