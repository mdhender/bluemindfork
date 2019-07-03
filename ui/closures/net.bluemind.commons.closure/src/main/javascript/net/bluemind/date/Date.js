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
goog.provide("net.bluemind.date.Date");

goog.require("goog.date.Date");
goog.require("net.bluemind.timezone");

/**
 * @constructor
 * @param {number|goog.date.DateLike=} opt_year
 * @param {number=} opt_month
 * @param {number=} opt_date
 * @extends {goog.date.Date}
 */
net.bluemind.date.Date = function(opt_year, opt_month, opt_date) {
  goog.base(this, opt_year, opt_month, opt_date);
}
goog.inherits(net.bluemind.date.Date, goog.date.Date);

/**
 * @return {!net.bluemind.date.Date}
 * @override
 */
net.bluemind.date.Date.prototype.clone = function() {
  var date = new net.bluemind.date.Date(this.date);
  date.setFirstDayOfWeek(this.getFirstDayOfWeek());
  date.setFirstWeekCutOffDay(this.getFirstWeekCutOffDay());

  return date;
};

/** @override */
net.bluemind.date.Date.prototype.toIsoString = function(opt_verbose, opt_tz) {
  return goog.base(this, 'toIsoString', opt_verbose, false);
}

/** @override */
net.bluemind.date.Date.prototype.toUTCIsoString = function(opt_verbose, opt_tz) {
  return goog.base(this, 'toUTCIsoString', opt_verbose, false);
}

/**
 * Get relative time depending on a given tz or on default tz. 
 * 
 * @param {goog.i18n.TimeZone=} opt_tz
 * @override
 */
net.bluemind.date.Date.prototype.getTime = function(opt_tz) {
  if (!opt_tz) {
    opt_tz = net.bluemind.timezone.DEFAULT;
  }
  return goog.base(this, 'getTime') - ((this.getTimezoneOffset() - opt_tz.getOffset(this.date)) * 60 * 1000);
}

/**
 * Reset time part
 * 
 * @private
 */
net.bluemind.date.Date.prototype.resetTime_ = function() {
  if (this.date.getHours() != 0) this.date.setHours(0);
  if (this.date.getMinutes() != 0) this.date.setMinutes(0);
  if (this.date.getSeconds() != 0) this.date.setSeconds(0);
  if (this.date.getMilliseconds() != 0) this.date.setMilliseconds(0);
}
/**
 * set relative time depending on a given tz or on default tz. 
 * 
 * @param {number} ms Number of milliseconds since 1 Jan 1970.
 * @param {goog.i18n.TimeZone=} opt_tz
 * @override
 */
net.bluemind.date.Date.prototype.setTime = function(ms, opt_tz) {
  if (!opt_tz) {
    opt_tz = net.bluemind.timezone.DEFAULT;
  }
  goog.base(this, 'setTime', ms);
  // Since we handle a a date object, there is no inner timerzone. 
  // Using timestamp in this context is quite confusing :
  // A timestamp must be used with a timezone to be converted as a date / datetime.
  var offset = opt_tz.getOffset(this.date) - this.getTimezoneOffset()
  if (offset != 0) {
    ms -= (offset * 60 * 1000);
    goog.base(this, 'setTime', ms);
  }
  this.resetTime_();
}

/** 
 * Set Year / Month / Day part of a date object
 * @param {number|goog.date.Date} year Year or date object
 * @param {number=} opt_month
 * @param {number=} opt_date 
 */
net.bluemind.date.Date.prototype.setDatePart = function(year, opt_month, opt_date) {
  if (goog.isObject(year)) {
    this.set(year);
  } else if (goog.isDefAndNotNull(opt_month) && goog.isDefAndNotNull(opt_date)) {
    this.date = new Date(year, opt_month, opt_date);
  } 
}

/**
 * Bugfix
 * @override
 */
goog.date.setIso8601TimeOnly_ = function(d, formatted) {
  // first strip timezone info from the end
  var parts = formatted.match(goog.date.splitTimezoneStringRegex_);

  var offset = 0, timezoned = !!parts;  // local time if no timezone info
  if (parts) {
    if (parts[0] != 'Z') {
      offset = Number(parts[2]) * 60 + Number(parts[3]);
      offset *= parts[1] == '-' ? 1 : -1;
    }
    formatted = formatted.substr(0, formatted.length - parts[0].length);
  }

  // then work out the time
  parts = formatted.match(goog.date.splitTimeStringRegex_);
  if (!parts) {
    return false;
  }

  d.setHours(Number(parts[1]));
  d.setMinutes(Number(parts[2]) || 0);
  d.setSeconds(Number(parts[3]) || 0);
  d.setMilliseconds(parts[4] ? Number(parts[4]) * 1000 : 0);

  if (timezoned && offset != d.getTimezoneOffset()) {
    offset -= d.getTimezoneOffset();
    // adjust the date and time according to the specified timezone
    d.setTime(d.getTime() + offset * 60000);
  }

  return true;
};
