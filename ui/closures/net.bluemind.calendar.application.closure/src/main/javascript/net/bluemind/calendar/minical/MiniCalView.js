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

goog.provide("net.bluemind.calendar.minical.MiniCalView");

goog.require("goog.ui.DatePicker");

/**
 * Bluemind DatePicker
 * 
 * @param {goog.date.Date|Date=} opt_date Date to initialize the date picker
 *          with, defaults to the current date.
 * @param {Object=} opt_dateTimeSymbols Date and time symbols to use. Defaults
 *          to goog.i18n.DateTimeSymbols if not set.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.DatePicker}
 */
net.bluemind.calendar.minical.MiniCalView = function(opt_date, opt_dateTimeSymbols, opt_domHelper) {
  goog.base(this, opt_date, opt_dateTimeSymbols);
  this.setAllowNone(false);
  this.setUseNarrowWeekdayNames(true);
  this.setUseSimpleNavigationMenu(true);
  this.setShowToday(false);
  this.setDecorator(goog.bind(this.isSelected_, this));
};
goog.inherits(net.bluemind.calendar.minical.MiniCalView, goog.ui.DatePicker);

/**
 * @type {net.bluemind.date.DateRange}
 */
net.bluemind.calendar.minical.MiniCalView.prototype.range;

/**
 * @type {boolean}
 * @private
 */
net.bluemind.calendar.minical.MiniCalView.prototype.lock_;

/**
 * @param {goog.date.Date} date Date to decorate
 * @return {string} Selected cell class name
 * @private
 */
net.bluemind.calendar.minical.MiniCalView.prototype.isSelected_ = function(date) {
  var dtstart = null;
  var dtend = null;
  if (this.range && this.range.contains(date)) {
    return goog.getCssName(this.getBaseCssClass(), 'highlighted');
  }
  return '';

};

/** @override */
net.bluemind.calendar.minical.MiniCalView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.getElement().id = 'minicalendar'
};

/**
 * Sets the selected date without raising events
 * 
 * @param {goog.date.Date|Date} date Date to select or null to select nothing.
 */
net.bluemind.calendar.minical.MiniCalView.prototype.setDateInternal = function(date) {
  this.lock_ = true;
  this.setDate(date);
  this.lock_ = false;
};

/** @override */
net.bluemind.calendar.minical.MiniCalView.prototype.dispatchEvent = function(e) {
  if (!this.lock_) {
    return goog.base(this, 'dispatchEvent', e);
  }
  return false;
};
