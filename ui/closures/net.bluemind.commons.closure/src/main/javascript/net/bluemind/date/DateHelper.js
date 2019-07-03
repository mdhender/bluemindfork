/* BEGIN LICENSE
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
goog.provide("net.bluemind.date");
goog.provide("net.bluemind.date.DateHelper");

goog.require("goog.date");
goog.require("goog.string");
goog.require("net.bluemind.timezone");
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.date.DateTime");
goog.require("net.bluemind.date.ZonedDate");
goog.require("net.bluemind.timezone.TimeZoneHelper");

/**
 * @param {net.bluemind.timezone.TimeZoneHelper=} opt_helper Timezone Helper
 * @constructor
 */
net.bluemind.date.DateHelper = function(opt_helper) {
  if (!opt_helper) {
    opt_helper = new net.bluemind.timezone.TimeZoneHelper();
  }
  this.helper_ = opt_helper;
  this.isoCache_ = {};
};

/**
 * @type {net.bluemind.timezone.TimeZoneHelper}
 * @private
 */
net.bluemind.date.DateHelper.prototype.helper_;

/**
 * @type {Object.<String, goog.date.Date>}
 * @private
 */
net.bluemind.date.DateHelper.prototype.isoCache_;

/**
 * Build a date / datetime from timestamp. The date will be converted to the
 * given timezone or to the default timezone.
 * 
 * Check type is suppressed because a date can be passed to
 * goog.date.setIso8601DateTime instead of a datetime.
 * 
 * @suppress {checkTypes}
 * @param {Number} time Timestamp
 * @param {string} opt_precision Date or DateTime (Default : DateTime)
 * @param {string | goog.i18n.TimeZone} opt_tz Date timezone, default timezone
 *                used if not set.
 * @return {net.bluemind.date.ZonedDate}
 */
net.bluemind.date.DateHelper.prototype.fromTime = function(time, opt_precision, opt_tz) {
  var tz = this.getTimeZone_(opt_tz);
  if (!opt_precision || opt_precision == 'DateTime') {
    var date = new net.bluemind.date.DateTime(tz);
  } else {
    var date = new net.bluemind.date.Date();
  }
  date.setTime(time);
  return date;
};

/**
 * Build a date / datetime from iso8601 string. The date will be converted from
 * the string timezone to the given timezone or to the default timezone.
 * 
 * Check type is suppressed because a date can be passed to
 * goog.date.setIso8601DateTime instead of a datetime.
 * 
 * @suppress {checkTypes}
 * @param {string} iso8601 ISO String
 * @param {string | goog.i18n.TimeZone=} opt_tz Date timezone, default timezone
 *                used if not set.
 * @return {net.bluemind.date.ZonedDate}
 */
net.bluemind.date.DateHelper.prototype.fromIsoString = function(iso8601, opt_tz) {
  var formatted = goog.string.trim(iso8601);
  var tz = this.getTimeZone_(opt_tz);
  var cache = formatted + '@' + tz.getTimeZoneId();
  if (this.isoCache_[cache]) {
    return this.isoCache_[cache].clone();
  }
  var delim = formatted.indexOf('T') == -1 ? ' ' : 'T';
  var parts = formatted.split(delim);
  if (parts.length < 2) {
    var date = new net.bluemind.date.Date();
    goog.date.setIso8601DateTime(date, formatted);
    this.isoCache_[cache] = date.clone();
    return date;
  } else {
    var datetime = new net.bluemind.date.DateTime(tz)
    goog.date.setIso8601DateTime(datetime, formatted);
    this.isoCache_[cache] = datetime.clone();
    return datetime;
  }
};

/**
 * Get an iso string superior at all other supported date
 * 
 * @param {boolean=} opt_verbose Whether the verbose format should be used
 *                instead of the default compact one.
 * @return {string}
 */
net.bluemind.date.DateHelper.prototype.getIsoEndOfTime = function(opt_verbose) {
  var iso = [ 9999, 12, 31 ];
  if (opt_verbose) {
    return iso.join('-');
  }
  return iso.join('');
};

/**
 * Create a zoned date from a bluemind date. The timezone of the date will be
 * the one defined in the bmDateTime or the system timezone if null. If passed
 * the given timezone will be used for the created date. If not the object
 * timezone or the system timezone will be used.
 * 
 * @param {net.bluemind.date.ZonedDate} date Date Time
 * @param {string | goog.i18n.TimeZone} opt_timezone Date timezone.
 * @return {Object} BmDateTime
 */
net.bluemind.date.DateHelper.prototype.toBMDateTime = function(date, opt_timezone) {
  var bm = {};
  if (date instanceof net.bluemind.date.Date || !date.getTimeZone) {
    bm['precision'] = 'Date';
    bm['timezone'] = null;
  } else {
    if (opt_timezone) {
      date = this.helper_.convert(date, opt_timezone)
    }
    bm['precision'] = 'DateTime';
    bm['timezone'] = date.getTimeZone().getTimeZoneId();
  }
  bm['iso8601'] = date.toIsoString(true, true)
  return bm;
};

/**
 * Create a zoned date. Input can be a bluemind date or a timestamp. If passed
 * the given timezone will be used for the created date. If not the object
 * timezone or the system timezone will be used.
 * 
 * @param {Object|number} datetime BM Date Time
 * @param {string | goog.i18n.TimeZone} opt_timezone Date timezone.
 * @return {net.bluemind.date.ZonedDate}
 */
net.bluemind.date.DateHelper.prototype.create = function(datetime, opt_timezone) {
  if (goog.isObject(datetime) && goog.isDefAndNotNull(datetime['iso8601'])) {
    var d = this.fromBMDateTime(datetime);
    if (goog.isDefAndNotNull(opt_timezone)) {
      d = this.changeTimeZone(d, opt_timezone);
    }
    return d;
  }
  return null;
};

/**
 * Create a zoned date from a bluemind date. The timezone of the date will be
 * the one defined in the bmDateTime or the system timezone if null. If passed
 * the given timezone will be used for the created date. If not the object
 * timezone or the system timezone will be used.
 * 
 * @param {Object} bmDateTime BM Date Time
 * @return {net.bluemind.date.ZonedDate}
 */
net.bluemind.date.DateHelper.prototype.fromBMDateTime = function(bmDateTime) {
  if (goog.isDefAndNotNull(bmDateTime['timezone'])) {
    return this.fromIsoString(bmDateTime['iso8601'], bmDateTime['timezone']);
  }
  return this.fromIsoString(bmDateTime['iso8601']);
};

/**
 * Get the timezone matching the given parameter
 * 
 * @param {goog.i18n.TimeZone | string} opt_tz
 * @return {goog.i18n.TimeZone}
 */
net.bluemind.date.DateHelper.prototype.getTimeZone_ = function(opt_tz) {
  var tz = opt_tz ? opt_tz : net.bluemind.timezone.DEFAULT;
  if (goog.isString(tz)) {
    return this.helper_.getTimeZone(tz);
  }
  return tz;
};

/**
 * Convert from a date format to another date format.
 * 
 */
net.bluemind.date.DateHelper.prototype.convert = function() {

};

/**
 * Change date timezone WITHOUT changing date (timestamp will change).
 * 
 * @param {net.bluemind.date.ZonedDate} date
 * @param {goog.i18n.TimeZone | string} tz
 * @return {net.bluemind.date.ZonedDate}
 */
net.bluemind.date.DateHelper.prototype.changeTimeZone = function(date, tz) {
  if (date instanceof net.bluemind.date.DateTime) {
    var timezone = this.getTimeZone_(tz)
    if (date.getTimeZone().getTimeZoneId() != timezone.getTimeZoneId()) {
      return new net.bluemind.date.DateTime(date, timezone);
    }
  }
  return date;
}

/**
 * Change date timezone WITHOUT changing date (timestamp will change).
 * 
 * @param {goog.date.Date} date
 * @param {goog.i18n.TimeZone | string} tz
 * @return {goog.date.Date}
 */
net.bluemind.date.DateHelper.prototype.toTimeZone = function(date, tz) {
  if (date instanceof net.bluemind.date.DateTime) {
    var d = new net.bluemind.date.DateTime(this.getTimeZone_(tz));
    d.setTime(date.getTime())
    return d;
  }
  return date;
}
