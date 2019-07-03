/*
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

goog.provide("net.bluemind.date.DateRange");

goog.require("net.bluemind.date.Date");
goog.require("goog.date.Date");
goog.require("goog.date.DateRange");
goog.require("goog.date.DateTime");
goog.require("goog.date.Interval");
goog.require("goog.date.DateRange.Iterator");
goog.require("goog.i18n.DateTimeSymbols");

/**
 * Constructs a date range with an inclusive start and exclusive end
 * 
 * @constructor
 * @param {goog.date.Date} startDate The first date in the range.
 * @param {goog.date.Date} endDate The last date in the range.
 * @final
 */
net.bluemind.date.DateRange = function(startDate, endDate) {
  if (!(startDate instanceof net.bluemind.date.Date)) {
    startDate = new net.bluemind.date.Date(startDate);
  }
  if (endDate instanceof goog.date.DateTime) {
    var temp = new net.bluemind.date.Date(endDate);
    if (goog.date.Date.compare(endDate, temp) > 0) {
      temp.add(new goog.date.Interval(goog.date.Interval.DAYS, 1));
    }
    endDate = temp;
  } else if (!(endDate instanceof net.bluemind.date.Date)) {
    endDate = new net.bluemind.date.Date(endDate);
  }
  this.range_ = new goog.date.DateRange(startDate, endDate);
  var end = endDate.clone();
  end.add(new goog.date.Interval(goog.date.Interval.DAYS, -1));
  this.inclusive_ = new goog.date.DateRange(startDate, end);
};

/**
 * @private
 * @type {goog.date.DateRange}
 */
net.bluemind.date.DateRange.prototype.range_;

/**
 * @private
 * @type {goog.date.DateRange}
 */
net.bluemind.date.DateRange.prototype.inclusive_;

/**
 * @return {goog.date.Date} The first date in the range.
 */
net.bluemind.date.DateRange.prototype.getStartDate = function() {
  return this.range_.getStartDate();
};

/**
 * @return {goog.date.Date} The last date in the range.
 */
net.bluemind.date.DateRange.prototype.getEndDate = function() {
  return this.range_.getEndDate();
};

/**
 * @return {goog.date.DateRange} The last date in the range.
 */
net.bluemind.date.DateRange.prototype.getEndDate = function() {
  return this.range_.getEndDate();
};

/**
 * @return {goog.date.DateRange} The last date in the range.
 */
net.bluemind.date.DateRange.prototype.getLastDate = function() {
  return this.inclusive_.getEndDate();
};

/**
 * Tests if a date falls within this range.
 * 
 * @param {goog.date.Date} date The date to test.
 * @return {boolean} Whether the date is in the range.
 */
net.bluemind.date.DateRange.prototype.contains = function(date) {
  return date.valueOf() >= this.getStartDate().valueOf() && date.valueOf() < this.getEndDate().valueOf();
};


/**
 * Tests if a date falls before this range.
 * 
 * @param {goog.date.Date} date The date to test.
 * @return {boolean} Whether the date is in the range.
 */
net.bluemind.date.DateRange.prototype.isAfter = function(date) {
  return date.valueOf() < this.getStartDate().valueOf() ;
};


/**
 * Tests if a date falls after this range.
 * 
 * @param {goog.date.Date} date The date to test.
 * @return {boolean} Whether the date is in the range.
 */
net.bluemind.date.DateRange.prototype.isBefore = function(date) {
  return  date.valueOf() >= this.getEndDate().valueOf();
};

/**
 * Only iterate over included days
 * 
 * @return {!goog.iter.Iterator} An iterator over the date range.
 */
net.bluemind.date.DateRange.prototype.iterator = function() {
  return new goog.date.DateRange.Iterator(this.inclusive_);
};

/**
 * Iterate over all included days + the excluded range end
 * 
 * @return {!goog.iter.Iterator} An iterator over the date range.
 */
net.bluemind.date.DateRange.prototype.fullIterator = function() {
  return new goog.date.DateRange.Iterator(this.range_);
};

/**
 * Number of days included in the range
 * 
 * @return {!number} Range length in days.
 */
net.bluemind.date.DateRange.prototype.count = function() {
  // TODO : Not the best way
  return Math.round((this.getEndDate().getTime() - this.getStartDate().getTime()) / 86400000);
};

/**
 * Clone the current object. Start and end date will be also cloned
 * 
 * @return {!net.bluemind.date.DateRange} Clone of this object.
 */
net.bluemind.date.DateRange.prototype.clone = function() {
  var start = new net.bluemind.date.Date().clone();
  var end = new net.bluemind.date.Date().clone();
  return new net.bluemind.date.DateRange(start, end);
};

/**
 * Returns the range from yesterday to yesterday.
 * 
 * @param {goog.date.Date=} opt_today The date to consider today. Defaults to
 *          today.
 * @return {!net.bluemind.date.DateRange} The range that includes only
 *         yesterday.
 */
net.bluemind.date.DateRange.yesterday = function(opt_today) {
  var today = goog.date.DateRange.cloneOrCreate_(opt_today);
  var yesterday = goog.date.DateRange.offsetInDays_(today, -1);
  return new net.bluemind.date.DateRange(yesterday, today);
};

/**
 * Returns the range from today to today.
 * 
 * @param {goog.date.Date=} opt_today The date to consider today. Defaults to
 *          today.
 * @return {!net.bluemind.date.DateRange} The range that includes only today.
 */
net.bluemind.date.DateRange.today = function(opt_today) {
  var today = goog.date.DateRange.cloneOrCreate_(opt_today);
  var tomorrow = goog.date.DateRange.offsetInDays_(today, 1);
  return new net.bluemind.date.DateRange(today, tomorrow);
};

/**
 * Returns the range that includes the seven days that end yesterday.
 * 
 * @param {goog.date.Date=} opt_today The date to consider today. Defaults to
 *          today.
 * @return {!net.bluemind.date.DateRange} The range that includes the seven days
 *         that end yesterday.
 */
net.bluemind.date.DateRange.last7Days = function(opt_today) {
  var today = goog.date.DateRange.cloneOrCreate_(opt_today);
  return new net.bluemind.date.DateRange(goog.date.DateRange.offsetInDays_(today, -7), today);
};

/**
 * Returns the range that starts the first of this month and ends the last day
 * of this month.
 * 
 * @param {goog.date.Date=} opt_today The date to consider today. Defaults to
 *          today.
 * @return {!net.bluemind.date.DateRange} The range that starts the first of
 *         this month and ends the last day of this month.
 */
net.bluemind.date.DateRange.thisMonth = function(opt_today) {
  var today = goog.date.DateRange.cloneOrCreate_(opt_today);
  return new net.bluemind.date.DateRange(goog.date.DateRange.offsetInMonths_(today, 0), goog.date.DateRange
      .offsetInMonths_(today, 1));
};

/**
 * Returns the range that starts the first of last month and ends the last day
 * of last month.
 * 
 * @param {goog.date.Date=} opt_today The date to consider today. Defaults to
 *          today.
 * @return {!net.bluemind.date.DateRange} The range that starts the first of
 *         last month and ends the last day of last month.
 */
net.bluemind.date.DateRange.lastMonth = function(opt_today) {
  var today = goog.date.DateRange.cloneOrCreate_(opt_today);
  return new net.bluemind.date.DateRange(goog.date.DateRange.offsetInMonths_(today, -1), goog.date.DateRange
      .offsetInMonths_(today, 0));
};

/**
 * Returns the range that starts the first day ok the week containing the fist
 * day of this month and ends the last day of the week, 6 weeks later.
 * 
 * @param {goog.date.Date=} opt_today The date to consider today. Defaults to
 *          today.
 * @return {!net.bluemind.date.DateRange} The range that starts the first of
 *         this month and ends the last day of this month.
 */
net.bluemind.date.DateRange.thisCalendarMonth = function(opt_today) {
  var today = goog.date.DateRange.cloneOrCreate_(opt_today);
  var start = goog.date.DateRange.offsetInMonths_(today, 0);
  start = goog.date.DateRange.offsetInDays_(start, -start.getWeekday());
  var end = goog.date.DateRange.offsetInDays_(start, 6 * 7);
  return new net.bluemind.date.DateRange(start, end);
};

/**
 * Returns the seven-day range that starts on the first day of the week (see
 * {@link goog.i18n.DateTimeSymbols.FIRSTDAYOFWEEK}) on or before today.
 * 
 * @param {goog.date.Date=} opt_today The date to consider today. Defaults to
 *          today.
 * @return {!net.bluemind.date.DateRange} The range that starts the Monday on or
 *         before today and ends the Sunday on or after today.
 */
net.bluemind.date.DateRange.thisWeek = function(opt_today) {
  var today = goog.date.DateRange.cloneOrCreate_(opt_today);
  var start = goog.date.DateRange.offsetInDays_(today, -today.getWeekday());
  var end = goog.date.DateRange.offsetInDays_(start, 7);
  return new net.bluemind.date.DateRange(start, end);
};

/**
 * Returns the seven-day range that ends the day before the first day of the
 * week (see {@link goog.i18n.DateTimeSymbols.FIRSTDAYOFWEEK}) that contains
 * today.
 * 
 * @param {goog.date.Date=} opt_today The date to consider today. Defaults to
 *          today.
 * @return {!net.bluemind.date.DateRange} The range that starts seven days
 *         before the Monday on or before today and ends the Sunday on or before
 *         yesterday.
 */
net.bluemind.date.DateRange.lastWeek = function(opt_today) {
  var thisWeek = net.bluemind.date.DateRange.thisWeek(opt_today);
  var start = goog.date.DateRange.offsetInDays_(thisWeek.getStartDate(), -7);
  var end = goog.date.DateRange.offsetInDays_(thisWeek.getEndDate(), -7);
  return new net.bluemind.date.DateRange(start, end);
};

/**
 * Returns the range that starts seven days before the Monday on or before today
 * and ends the Friday before today.
 * 
 * @param {goog.date.Date=} opt_today The date to consider today. Defaults to
 *          today.
 * @return {!net.bluemind.date.DateRange} The range that starts seven days
 *         before the Monday on or before today and ends the Friday before
 *         today.
 */
net.bluemind.date.DateRange.thisBusinessWeek = function(opt_today) {
  // FIXME: This is not a joke. It might not work with Bengali local because
  // Bengali weekend is in the middle of Bengali week.

  var weekendStart = goog.i18n.DateTimeSymbols.WEEKENDRANGE[0], weekendEnd = goog.i18n.DateTimeSymbols.WEEKENDRANGE[1];
  var today = goog.date.DateRange.cloneOrCreate_(opt_today);
  var firstDay = today.getFirstDayOfWeek();
  var workWeekStart = weekendEnd + 1;
  var weekEndLength = (workWeekStart - weekendStart + 7) % 7;
  var start = goog.date.DateRange.offsetInDays_(today, ((workWeekStart - firstDay) % 7) - today.getWeekday());
  var end = goog.date.DateRange.offsetInDays_(start, 7 - weekEndLength);
  return new net.bluemind.date.DateRange(start, end);
};

/**
 * Returns the range that starts seven days before the Monday on or before today
 * and ends the Friday before today.
 * 
 * @param {goog.date.Date=} opt_today The date to consider today. Defaults to
 *          today.
 * @return {!net.bluemind.date.DateRange} The range that starts seven days
 *         before the Monday on or before today and ends the Friday before
 *         today.
 */
net.bluemind.date.DateRange.lastBusinessWeek = function(opt_today) {
  var thisWeek = net.bluemind.date.DateRange.thisBusinessWeek(opt_today);
  var start = goog.date.DateRange.offsetInDays_(thisWeek.getStartDate(), -7);
  var end = goog.date.DateRange.offsetInDays_(thisWeek.getEndDate(), -7);
  return new net.bluemind.date.DateRange(start, end);
};

/**
 * Returns the range that includes all days between January 1, 1900 and December
 * 31, 9999.
 * 
 * @param {goog.date.Date=} opt_today The date to consider today. Defaults to
 *          today.
 * @return {!net.bluemind.date.DateRange} The range that includes all days
 *         between January 1, 1900 and December 31, 9999.
 */
net.bluemind.date.DateRange.allTime = function(opt_today) {
  return new net.bluemind.date.DateRange(goog.date.DateRange.MINIMUM_DATE, goog.date.DateRange.MAXIMUM_DATE);
};
