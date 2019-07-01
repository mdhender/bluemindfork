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
goog.provide("net.bluemind.date.DateTime");

goog.require("goog.string");
goog.require("goog.date.Date");
goog.require("goog.date.DateTime");
goog.require("goog.i18n.TimeZone");
goog.require("net.bluemind.timezone");

/**
 * @constructor
 * 
 * @param {number|Object|goog.i18n.TimeZone=} opt_year
 * @param {number|goog.i18n.TimeZone=} opt_month
 * @param {number=} opt_date
 * @param {number=} opt_hours
 * @param {number=} opt_minutes
 * @param {number=} opt_seconds
 * @param {number=} opt_milliseconds
 * @param {goog.i18n.TimeZone=} opt_timezone Timezone.
 * @extends {goog.date.DateTime}
 */
net.bluemind.date.DateTime = function(opt_year, opt_month, opt_date, opt_hours, opt_minutes, opt_seconds,
    opt_milliseconds, opt_timezone) {
  if (opt_year instanceof goog.i18n.TimeZone) {
    this.timeZone_ = opt_year;
    opt_year = undefined;
  }
  if (opt_month instanceof goog.i18n.TimeZone) {
    this.timeZone_ = opt_month;
    opt_month = undefined;
  }
  if (opt_timezone) {
    this.timeZone_ = opt_timezone;
  }
  if (!this.timeZone_) {
    this.timeZone_ = net.bluemind.timezone.DEFAULT;
  }
  if (goog.isNumber(opt_year) && goog.isNumber(opt_month)) {
    this.ts_ = new Date();
    this.date = new Date(opt_year, opt_month, opt_date || 1, opt_hours || 0, opt_minutes || 0, opt_seconds || 0,
        opt_milliseconds || 0);
    this.adjustTs_();
  } else {
    this.date = new Date();
    this.ts_ = new Date(opt_year ? (goog.isNumber(opt_year) ? opt_year : opt_year.getTime(this.timeZone_)) : goog.now());
    this.adjustDate_();
  }

}
goog.inherits(net.bluemind.date.DateTime, goog.date.DateTime);

/**
 * @private {goog.i18n.TimeZone}
 */
net.bluemind.date.DateTime.prototype.timeZone_;

/**
 * @private {!Date}
 */
net.bluemind.date.DateTime.prototype.ts_;

/**
 * @return {!net.bluemind.date.DateTime}
 * @override
 */
net.bluemind.date.DateTime.prototype.clone = function() {
  var date = new net.bluemind.date.DateTime(this, this.timeZone_);
  date.setFirstDayOfWeek(this.getFirstDayOfWeek());
  date.setFirstWeekCutOffDay(this.getFirstWeekCutOffDay());
  return date;
};

/** @override */
net.bluemind.date.DateTime.prototype.toIsoString = function(opt_verbose, opt_tz) {
  var dateString = goog.date.Date.prototype.toIsoString.call(this, opt_verbose);

  if (opt_verbose) {
    return dateString + 'T' + goog.string.padNumber(this.getHours(), 2) + ':'
        + goog.string.padNumber(this.getMinutes(), 2) + ':' + goog.string.padNumber(this.getSeconds(), 2) + '.'
        + goog.string.padNumber(this.getMilliseconds(), 3) + (opt_tz ? this.getTimezoneOffsetString() : '');
  }

  return dateString + 'T' + goog.string.padNumber(this.getHours(), 2) + goog.string.padNumber(this.getMinutes(), 2)
      + goog.string.padNumber(this.getSeconds(), 2) + '.' + goog.string.padNumber(this.getMilliseconds(), 3)
      + (opt_tz ? this.getTimezoneOffsetString().replace(':', '') : '');
};

/** @override */
net.bluemind.date.DateTime.prototype.getTimezoneOffset = function() {
  return this.timeZone_.getOffset(this.date);
};

/**
 * Return date time timezone.
 * 
 * @return {goog.i18n.TimeZone}
 */
net.bluemind.date.DateTime.prototype.getTimeZone = function() {
  return this.timeZone_;
};

/** @override */
net.bluemind.date.DateTime.prototype.add = function(interval) {
 goog.date.Date.prototype.add.call(this, interval);

 if (interval.hours) {
   this.setUTCHours(this.getUTCHours() + interval.hours);
 }
 if (interval.minutes) {
   this.setUTCMinutes(this.getUTCMinutes() + interval.minutes);
 }
 if (interval.seconds) {
   this.setUTCSeconds(this.getUTCSeconds() + interval.seconds);
 }
};

/** @override */
net.bluemind.date.DateTime.prototype.set = function(date) {
  goog.base(this, 'set', date);
  if (goog.isFunction(date.getHours)) {
    this.date.setHours(date.getHours());
    this.date.setMinutes(date.getMinutes());
    this.date.setSeconds(date.getSeconds());
    this.date.setMilliseconds(date.getMilliseconds());
  }
  this.adjustTs_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setFullYear = function(year) {
  this.date.setFullYear(year);
  this.adjustTs_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setMonth = function(month) {
  this.date.setMonth(month);
  this.adjustTs_();
};

/** 
 * Set Year / Month / Day part of a datetime object
 * @param {number|goog.date.DateLike} year Year or date object
 * @param {number=} opt_month
 * @param {number=} opt_date 
 */
net.bluemind.date.DateTime.prototype.setDatePart = function(year, opt_month, opt_date) {
  if (goog.isObject(year)) {
    this.date = new Date(year.getFullYear(), year.getMonth(), year.getDate(), this.getHours(), this.getMinutes(), this.getSeconds(), this.getMilliseconds());
  } else if (goog.isDefAndNotNull(opt_month) && goog.isDefAndNotNull(opt_date)) {
    this.date = new Date(year, opt_month, opt_date, this.getHours(), this.getMinutes(), this.getSeconds(), this.getMilliseconds());
  } 
  this.adjustTs_();
}

/** @override */
net.bluemind.date.DateTime.prototype.setDate = function(date) {
  this.date.setDate(date);
  this.adjustTs_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setHours = function(hours) {
  this.date.setHours(hours);
  this.adjustTs_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setMinutes = function(minutes) {
  this.date.setMinutes(minutes);
  this.adjustTs_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setSeconds = function(seconds) {
  this.date.setSeconds(seconds);
  this.adjustTs_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setMilliseconds = function(ms) {
  this.date.setMilliseconds(ms);
  this.adjustTs_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setTime = function(time) {
  this.ts_.setTime(time);
  this.adjustDate_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setUTCHours = function(hours) {
  this.ts_.setUTCHours(hours);
  this.adjustDate_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setUTCFullYear = function(year) {
  this.ts_.setFullYear(year);
  this.adjustDate_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setUTCMonth = function(month) {
  this.ts_.setUTCMonth(month);
  this.adjustDate_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setUTCDate = function(date) {
  this.ts_.setUTCDate(date);
  this.adjustDate_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setUTCMinutes = function(minutes) {
  this.ts_.setUTCMinutes(minutes);
  this.adjustDate_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setUTCSeconds = function(seconds) {
  this.ts_.setUTCSeconds(seconds);
  this.adjustDate_();
};

/** @override */
net.bluemind.date.DateTime.prototype.setUTCMilliseconds = function(ms) {
  this.ts_.setUTCMilliseconds(ms);
  this.adjustDate_();
};

/** @override */
net.bluemind.date.DateTime.prototype.getUTCFullYear = function() {
  return this.ts_.getUTCFullYear();
};

/** @override */
net.bluemind.date.DateTime.prototype.getUTCMonth = function() {
  return (
  /** @type {goog.date.month} */
  (this.ts_.getUTCMonth())//
  );
};

/** @override */
net.bluemind.date.DateTime.prototype.getUTCDay = function() {
  return (
  /** @type {goog.date.weekDay} */
  (this.ts_.getUTCDay())//
  );
};

/** @override */
net.bluemind.date.DateTime.prototype.getUTCHours = function() {
  return this.ts_.getUTCHours();
};

/** @override */
net.bluemind.date.DateTime.prototype.getUTCMinutes = function() {
  return this.ts_.getUTCMinutes();
};

/** @override */
net.bluemind.date.DateTime.prototype.getUTCSeconds = function() {
  return this.ts_.getUTCSeconds();
};

/** @override */
net.bluemind.date.DateTime.prototype.getUTCMilliseconds = function() {
  return this.ts_.getUTCMilliseconds();
};

/** @override */
net.bluemind.date.DateTime.prototype.getTime = function() {
  return this.ts_.getTime();
};

/**
 * @private
 */
net.bluemind.date.DateTime.prototype.adjustTs_ = function() {
  this.ts_.setTime(this.date.getTime() + (this.timeZone_.getOffset(this.date) - this.date.getTimezoneOffset()) * 60
      * 1000);
};

/**
 * @private
 */
net.bluemind.date.DateTime.prototype.adjustDate_ = function() {
  this.date.setTime(this.ts_.getTime() + (this.ts_.getTimezoneOffset() - this.timeZone_.getOffset(this.ts_)) * 60
      * 1000);
};
