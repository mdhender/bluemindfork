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
 * @fileoverview Date utils.
 */

goog.provide('bluemind.utils.DateHelper');

goog.require('goog.date.Date');
goog.require('goog.date.Interval');

/**
 * Provide utils for date manipulation.
 * @constructor
 */
bluemind.utils.DateHelper = function() {};

/**
 * Compare two date and return the earlier.
 * @param {goog.date.DateLike} date1 Date to compare.
 * @param {goog.date.DateLike} date2 Date to compare.
 * @return {goog.date.DateLike} the earlier date.
 */
bluemind.utils.DateHelper.min = function(date1, date2) {
  if (!date2) return date1;
  if (!date1) return date2;
  if (goog.date.Date.compare(date1, date2) > 0) return date2;
  return date1;
};

/**
 * Compare two date and return the later.
 * @param {goog.date.DateLike} date1 Date to compare.
 * @param {goog.date.DateLike} date2 Date to compare.
 * @return {goog.date.DateLike} the later date.
 */
bluemind.utils.DateHelper.max = function(date1, date2) {
  if (!date2) return date1;
  if (!date1) return date2;
  if (goog.date.Date.compare(date1, date2) < 0) return date2;
  return date1;
};

/**
 * Difference between two date.
 * @param {!goog.date.DateLike} date1 Date to compare.
 * @param {!goog.date.DateLike} date2 Date to compare.
 * @param {string=} opt_part Date part to compare.
 * @param {boolean} opt_ceil Ceil the result.
 * @return {number} Difference, unit depends on the opt_part (default ms).
 */
bluemind.utils.DateHelper.diff = function(date1, date2, opt_part, opt_ceil) {
  if (opt_part && opt_ceil) {
    return bluemind.utils.DateHelper.ceilDiff(date1, date2, opt_part);
  } else {
    return bluemind.utils.DateHelper.floorDiff(date1, date2, opt_part);
  }
};

/**
 * Difference between two date. The result will be rounded to the lower value.
 * @param {!goog.date.DateLike} date1 Date to compare.
 * @param {!goog.date.DateLike} date2 Date to compare.
 * @param {string=} opt_part Date part to compare.
 * @return {number} Difference, unit depends on the opt_part (default ms).
 */
bluemind.utils.DateHelper.floorDiff = function(date1, date2, opt_part) {
  var diff;
  switch (opt_part) {
    case goog.date.Interval.YEARS:
      diff = Math.abs(date1.getYear() - date2.getYear());
      break;
    case goog.date.Interval.MONTHS:
      diff = (date1.getYear() - date2.getYear()) * 12;
      diff += date1.getMonth() - date2.getMonth();
      diff = Math.abs(diff);
      break;
    case goog.date.Interval.DAYS:
      //FIXME: rounded (well not even rounded) value not floored.
      var d1 = new goog.date.Date(date1);
      var d2 = new goog.date.Date(date2);
      diff = Math.abs(Math.round((d1.getTime() - d2.getTime()) / 86400000));
      break;
    case goog.date.Interval.HOURS:
      diff = Math.abs((date1.getTime() - date2.getTime()) / 3600000);
      break;
    case goog.date.Interval.MINUTES:
      diff = Math.abs((date1.getTime() - date2.getTime()) / 60000);
      break;
    case goog.date.Interval.SECONDS:
      diff = Math.abs((date1.getTime() - date2.getTime()) / 1000);
      break;
    default:
      diff = Math.abs(date1.getTime() - date2.getTime());
      break;
  }
  return Math.floor(diff);
};


/**
 * Difference between two date. The result will be rounded to the higher value.
 * @param {!goog.date.DateLike} date1 Date to compare.
 * @param {!goog.date.DateLike} date2 Date to compare.
 * @param {string} part Date part to compare.
 * @return {number} Difference, unit depends on the opt_part (default ms).
 */
bluemind.utils.DateHelper.ceilDiff = function(date1, date2, part) {
  var diff;
  var date = date2.clone();
  switch (part) {
    case goog.date.Interval.YEARS:
      diff = Math.abs(date1.getYear() - date2.getYear());
      date.setYear(date1.getYear());
      if (goog.date.Date.compare(date1, date) != 0) diff++;
      break;
    case goog.date.Interval.MONTHS:
      diff = (date1.getYear() - date2.getYear()) * 12;
      diff += date1.getMonth() - date2.getMonth();
      diff = Math.abs(diff);
      date.setYear(date1.getYear());
      date.setMonth(date1.getMonth());
      if (goog.date.Date.compare(date1, date) != 0) diff++;
      break;
    case goog.date.Interval.DAYS:
      //FIXME: rounded (well not even rounded) value not ceiled.
      var d1 = new goog.date.Date(date1);
      var d2 = new goog.date.Date(date2);
      diff = Math.abs(Math.round((d1.getTime() - d2.getTime()) / 86400000));
      break;
    case goog.date.Interval.HOURS:
      diff = Math.abs((date1.getTime() - date2.getTime()) / 3600000);
      break;
    case goog.date.Interval.MINUTES:
      diff = Math.abs((date1.getTime() - date2.getTime()) / 60000);
      break;
    case goog.date.Interval.SECONDS:
      diff = Math.abs((date1.getTime() - date2.getTime()) / 1000);
      break;
  }
  return Math.ceil(diff);
};
