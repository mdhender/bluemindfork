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
 * @fileoverview pending events list.
 */

goog.provide('bluemind.calendar.view.Pending');

goog.require('bluemind.calendar.model.EventHome');
goog.require('bluemind.calendar.ui.pending.List');
goog.require('goog.Disposable');
goog.require('goog.ui.Checkbox');
goog.require('goog.ui.Checkbox.State');

/**
 * BlueMind pending events
 *
 * @constructor
 * @extends {goog.Disposable}
 */
bluemind.calendar.view.Pending = function() {
  this.manager_ = bluemind.manager;
};
goog.inherits(bluemind.calendar.view.Pending, goog.Disposable);

/**
 * Calendar manager
 *
 * @type {bluemind.calendar.Manager}
 * @private
 */
bluemind.calendar.view.Pending.prototype.manager_;

/**
 * Pending events component
 *
 * @type {bluemind.calendar.ui.pending.List}
 * @private
 */
bluemind.calendar.view.Pending.prototype.comp_;


/**
 * Agenda start date
 *
 * @type {goog.date.Date}
 * @private
 */
bluemind.calendar.view.Pending.prototype.start_;

/**
 * Agenda end date
 *
 * @type {goog.date.Date}
 * @private
 */
bluemind.calendar.view.Pending.prototype.end_;

/**
 * @return {bluemind.calendar.ui.pending.List} pending component.
 */
bluemind.calendar.view.Pending.prototype.getComponent = function() {
  return this.comp_;
};

/**
 * display pending events
 *
 */
bluemind.calendar.view.Pending.prototype.display = function() {
  this.comp_ = new bluemind.calendar.ui.pending.List();
  this.show();
};

/**
 * Fetch pending events callback
 */
bluemind.calendar.view.Pending.prototype.show = function() {

  this.start_ = new goog.date.Date();
  this.end_ = this.start_.clone();
  this.end_.add(new goog.date.Interval(goog.date.Interval.DAYS, 7));

  var writable = new Array();
  goog.array.forEach(bluemind.manager.getVisibleCalendars().getKeys(), function(c) {
    if (goog.array.contains(bluemind.writableCalendars, c + '')) {
      writable.push(c);
    }
  });
  bluemind.calendar.model.EventHome.getInstance()
    .getPendingEvents(writable).addCallback(function(events) {
    if (events.length > 0) {
      bluemind.view.getView().getComponent().createDom();
      events.sort(function(e1, e2) {
        return goog.date.Date.compare(e1.getDate(), e2.getDate());
      });

      var calendarList = new goog.structs.Map();

      for (var i = 0; i < events.length; i++) {
        var evt = events[i];
          if (bluemind.manager.isEventVisible(evt)) {
            bluemind.view.getView().getComponent().add(evt);
          if (!calendarList.containsKey(evt.getCalendar())) {
            calendarList.set(evt.getCalendar(), evt.getCalendarLabel());
          }
          bluemind.view.getView().getComponent().events.push(evt);
        }
      }
      bluemind.view.getView().getComponent().getToolbar().
        buildCalendarFilter(calendarList);
    } else {
      bluemind.view.getView().getComponent().createNoEventDom();
    }
    bluemind.resize();
  });
};

/**
 * @return {text} pending.
 */
bluemind.calendar.view.Pending.prototype.getName = function() {
  return 'pending';
};

/**
 * @inheritDoc
 */
bluemind.calendar.view.Pending.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
};

/**
 * Agenda start date
 *
 * @return {goog.date.Date} start date.
 */
bluemind.calendar.view.Pending.prototype.getStart = function() {
  return this.start_;
};

/**
 * Agenda end date
 *
 * @return {goog.date.Date} end date.
 */
bluemind.calendar.view.Pending.prototype.getEnd = function() {
  return this.end_;
};
