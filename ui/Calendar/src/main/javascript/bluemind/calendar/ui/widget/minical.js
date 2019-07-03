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

goog.provide('bluemind.calendar.ui.widget.MiniCalendar');

goog.require('goog.date.Date');
goog.require('goog.ui.DatePicker');
goog.require('goog.ui.DatePicker.Events');


/**
 * Bluemind DatePicker
 *
 * @param {goog.date.Date|Date=} opt_date Date to initialize the date picker
 *     with, defaults to the current date.
 * @param {Object=} opt_dateTimeSymbols Date and time symbols to use.
 *     Defaults to goog.i18n.DateTimeSymbols if not set.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.DatePicker}
 */
bluemind.calendar.ui.widget.MiniCalendar =
  function(opt_date, opt_dateTimeSymbols, opt_domHelper) {
  goog.ui.DatePicker.call(this, opt_date, opt_dateTimeSymbols);
  this.setAllowNone(false);
  this.setUseNarrowWeekdayNames(true);
  this.setUseSimpleNavigationMenu(true);
  this.setShowToday(false);
  this.setDecorator(goog.bind(this.isSelected_, this));
};
goog.inherits(bluemind.calendar.ui.widget.MiniCalendar, goog.ui.DatePicker);

bluemind.calendar.ui.widget.MiniCalendar.prototype.isSelected_ = function(o) {
  var dtstart = null;
  var dtend = null;
  if (bluemind.view) {
    dtstart = bluemind.view.getView().getStart();
    dtend = bluemind.view.getView().getEnd();
  }

  // Selected date
  if (dtstart && dtend && goog.date.Date.compare(dtstart, o) <= 0 &&
    goog.date.Date.compare(dtend, o) > 0) {
    return goog.getCssName(this.getBaseCssClass(), 'selected');
  }

};


/**
 * @param {goog.events.BrowserEvent} event Click event.
 * @private
 */
bluemind.calendar.ui.widget.MiniCalendar.prototype.handleGridClick_ =
  function(event) {
  if (event.target.tagName == 'TD') {

    var selected = goog.dom.classes.has(event.target,
      goog.getCssName('goog-date-picker-selected'));

    // colIndex/rowIndex is broken in Safari, find position by looping
    var el, x = -2, y = -2; // first col/row is for weekday/weeknum
    for (el = event.target; el; el = el.previousSibling, x++) {}
    for (el = event.target.parentNode; el; el = el.previousSibling, y++) {}
    var obj = this.grid_[y][x];
    this.setDate(obj.clone());

    var view = bluemind.view.getView().getName();
    bluemind.manager.setCurrentDate(obj.clone());
    if (selected) {
      switch (view) {
        case 'days':
          if (bluemind.manager.getNbDays() == 1) {
            bluemind.view.week();
          } else {
            bluemind.view.day();
          }
          break;
        case 'month':
          bluemind.view.week();
          break;
        case 'pending':
          bluemind.view.week();
          break;
        case 'agenda':
          bluemind.view.getView().display();
        default:
          bluemind.view.week();
      }
    } else {
      if (view == 'pending' || view == 'form') {
        bluemind.view.week();
      } else {
        bluemind.view.getView().display();
      }
    }
    bluemind.view.updateToolbar();
  }
};
