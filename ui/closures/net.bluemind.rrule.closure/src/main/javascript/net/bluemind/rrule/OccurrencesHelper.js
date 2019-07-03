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

goog.provide("net.bluemind.rrule.OccurrencesHelper");

goog.require("RRule");
goog.require("goog.array");
goog.require("goog.date.Date");
goog.require("goog.date.DateTime");
goog.require("goog.structs.Map");

/**
 * Helper to calculate event occurrence
 * 
 * @constructor
 */
net.bluemind.rrule.OccurrencesHelper = function() {
};

/**
 * Cache for rrule performance.
 * 
 * @type {Object}
 */
net.bluemind.rrule.OccurrencesHelper.cache_ = {};

/**
 * Expand occurrences of a vevent series within a range of date.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @param {<Object>} vseries VEventSeries
 * @param {goog.date.DateRange} range Range of date
 * @return {Array.<Object>} vseries Serie with expanded occurrences
 */
net.bluemind.rrule.OccurrencesHelper.prototype.expandSeries = function(ctx, vseries, range) {
  if (goog.isDefAndNotNull(vseries['value']['main']) && goog.isDefAndNotNull(vseries['value']['main']['rrule'])) {
    var dates = this.getOccurrences(ctx, vseries, range);
    var start = ctx.helper('date').create(vseries['value']['main']['dtstart']);
    var end = ctx.helper('date').create(vseries['value']['main']['dtend']);
    var json = goog.global['JSON'].stringify(vseries['value']['main']);
    var date = start.clone(), duration = 0, utc = net.bluemind.timezone.UTC;
    if (start && end) {
      duration = end.getTime(utc) - start.getTime(utc);
    }
    var exdates = [];
    if (goog.isArray(vseries['value']['main']['exdate'])) {
      goog.array.forEach(vseries['value']['main']['exdate'], function(exdate) {
        exdates.push(exdate['iso8601']);
      });
    }
    var occurrences = vseries['value']['occurrences'];
    vseries['value']['occurrences'] = [];    
    goog.array.forEach(occurrences, function(occurrence) {
      exdates.push(occurrence['recurid']['iso8601']);
      var dtstart = ctx.helper('date').create(occurrence['dtstart']), dtend = ctx.helper('date').create(occurrence['dtend']);
      if (goog.date.Date.compare(dtstart, range.getEndDate()) < 0
        && goog.date.Date.compare(dtend, range.getStartDate()) > 0) {
          vseries['value']['occurrences'].push(occurrence);
      }
    });
    goog.array.forEach(dates, function(instance) {
      date.set(instance);
      var iso8601 = date.toIsoString(true, true);
      if (this.isAnExDate_(ctx, exdates, iso8601)) {
        return;
      }
      var occurrence = goog.global['JSON'].parse(json);
      occurrence['dtstart']['iso8601'] = iso8601;
      date.setTime(date.getTime(utc) + duration, utc);
      occurrence['dtend']['iso8601'] = date.toIsoString(true, true);
      occurrence['exdate'] = null;
      occurrence['recurid'] = occurrence['dtstart'];
      vseries['value']['occurrences'].push(occurrence);
    }, this);
  } else if (goog.isDefAndNotNull(vseries['value']['main'])) {
    //FIXME ... 
    vseries['value']['occurrences'].push(vseries['value']['main']);
  }
  return vseries;
};


/**
 * Calculate occurrences of a list of vevent in a range of date.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @param {Array.<Object>} vevents Vevents list
 * @param {goog.date.DateRange} range Range of date
 * @return {Array.<Object>} List of vevent occurrences.
 */
net.bluemind.rrule.OccurrencesHelper.prototype.getEventOccurrences = function(ctx, vevents, range) {
  var occurrences = [], exceptions = new goog.structs.Map(), recurring = [];
  goog.array.forEach(vevents, function(vevent) {
    if (!exceptions.containsKey(vevent['container'])) {
      exceptions.set(vevent['container'], new goog.structs.Map());
    }
    if (goog.isDefAndNotNull(vevent['value']['recurid'])) {
      var exdates = exceptions.get(vevent['container']).get(vevent['value']['uid']) || [];
      exdates.push(vevent['value']['recurid']);
      exceptions.get(vevent['container']).set(vevent['value']['uid'], exdates);
    }
    goog.isDefAndNotNull(vevent['value']['rrule']) ? recurring.push(vevent) : occurrences.push(vevent);
  }, this);
  goog.array.forEach(recurring, function(vevent) {
    var exdates = exceptions.get(vevent['container']).get(vevent['value']['uid']) || [];
    goog.array.extend(occurrences, this.getRecurringEventOccurrences(ctx, vevent, range, exdates));
  }, this);
  return occurrences;
};

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @param {Object} vevent Recurring Vevent
 * @param {goog.date.DateRange} range Range of date
 * @param {Array.<goog.date.Date>} exdates List of date to exclude from
 *          occurrences.
 * @return {Array.<Object>} Clone of the input vevent with effective dtstart
 *         and no rrule.
 */
net.bluemind.rrule.OccurrencesHelper.prototype.getRecurringEventOccurrences = function(ctx, vevent, range, exdates) {
  var occurrences = [];
  var dates = this.getOccurrences(ctx, vevent['value'], range.getStartDate(), range.getEndDate(), exdates);
  var start = ctx.helper('date').create(vevent['value']['dtstart']);
  var end = ctx.helper('date').create(vevent['value']['dtend']);
  var duration = 0;
  if (start && end) {
    duration = end.getTime() - start.getTime();
  }
  var json = goog.global['JSON'].stringify(vevent);
  var dt = start.clone();
  goog.array.forEach(dates, function(date) {
    var occurrence = goog.global['JSON'].parse(json);
    dt.set(date);
    occurrence['value']['dtstart']['iso8601'] = dt.toIsoString(true, true);
    dt.setTime(dt.getTime() + duration);
    occurrence['value']['dtend']['iso8601'] = dt.toIsoString(true, true);
    occurrence['value']['exdate'] = null;
    occurrence['value']['recurid'] = occurrence['value']['dtstart'];
    occurrences.push(occurrence);
  });
  return occurrences;
};

/**
 * Get event occurrences date for a reccurring event
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @param {Object} vseries VEventSeries data
 * @param {goog.date.DateRange} range Range of date

 * @param {Array.<goog.date.Date>} opt_exdates List of date to exclude from
 *          occurrences.
 * @return {Array.<goog.date.Date>} Occurrences date
 */
net.bluemind.rrule.OccurrencesHelper.prototype.getOccurrences = function(ctx, vseries, range) {

  var dates = [], from = range.getStartDate(), until = range.getEndDate();
  var main = vseries['value']['main'];
  var dtstart = ctx.helper('date').create(main['dtstart']);
  if (!(dtstart instanceof goog.date.DateTime)) {
    dtstart = new net.bluemind.date.DateTime(dtstart);
  }
  dtstart.setMilliseconds(0);
  if (main['dtend']) {
    var dtend = ctx.helper('date').create(main['dtend']);
    var duration = dtend.getTime() - dtstart.getTime();
    from = from.clone();
    from.setTime(from.getTime() - duration);
  }

  if (main['rrule']) {
    var opt = {};

    opt.dtstart = dtstart;

    if (main['rrule']['frequency']) {
      opt.freq = this.freq_(main['rrule']['frequency']);
    }

    if (main['rrule']['interval'] && main['rrule']['interval'] > 0) {
      opt.interval = main['rrule']['interval'];
    }

    if (main['rrule']['count']) {
      opt.count = main['rrule']['count'];
    }

    if (main['rrule']['until']) {
      opt.until = ctx.helper('date').create(main['rrule']['until'], dtstart.getTimeZone());
    }

    if (main['rrule']['bySecond']) {
      opt.bysecond = this.intList_(main['rrule']['bySecond']);
    }

    if (main['rrule']['byMinute']) {
      opt.byminute = this.intList_(main['rrule']['byMinute']);
    }

    if (main['rrule']['byHour']) {
      opt.byhour = this.intList_(main['rrule']['byHour']);
    }

    if (main['rrule']['byDay']) {
      opt.byweekday = this.dayList_(main['rrule']['byDay']);
    }

    if (main['rrule']['byMonthDay']) {
      opt.bymonthday = this.intList_(main['rrule']['byMonthDay']);
    }

    if (main['rrule']['byWeekNo']) {
      opt.byweekno = this.intList_(main['rrule']['byWeekNo']);
    }
    if (main['rrule']['byYearDay']) {
      opt.byyearday = this.intList_(main['rrule']['byYearDay']);
    }

    if (main['rrule']['byWeekNo']) {
      opt.byweekno = this.intList_(main['rrule']['byWeekNo']);
    }
    if (main['rrule']['byMonth']) {
      opt.bymonth = this.intList_(main['rrule']['byMonth']);
    }

    var json = goog.global['JSON'].stringify(opt);
    var rule = net.bluemind.rrule.OccurrencesHelper.cache_[json] || new RRule(opt);
    net.bluemind.rrule.OccurrencesHelper.cache_[json] = rule;
    var ocs = rule.between(from, until, [ true, false ]);
    goog.array.extend(dates, ocs);
  } else if (goog.date.Date.compare(dtstart, until) < 0 && goog.date.Date.compare(dtstart, from) > 0) {
    dates.push(dtstart);
  }

  if (goog.isArray(main['rdate'])) {
    for (var i = 0; i < main['rdate'].length; i++) {
      var ts = ctx.helper('date').create(main['rdate'][i]);
      if (ts.getTime() > from.getTime() && ts.getTime() < until.getTime()) {
        dates.push(ts);
      }
    }
  }
  
  goog.array.removeDuplicates(dates);
  return dates;
}

/**
 * Convert day list from BM syntax to RRule syntax
 * 
 * @param {Array.<string>} days
 * @return {Array.<Object>}
 * @private
 */
net.bluemind.rrule.OccurrencesHelper.prototype.dayList_ = function(days) {
  if (days.length == 0) {
    return null;
  }
  return goog.array.map(days, function(d) {
    var ret = null;
    switch (d['day']) {
    case "SU":
      ret = RRule.SU;
      break;
    case "MO":
      ret = RRule.MO;
      break;
    case "TU":
      ret = RRule.TU;
      break;
    case "WE":
      ret = RRule.WE;
      break;
    case "TH":
      ret = RRule.TH;
      break;
    case "FR":
      ret = RRule.FR;
      break;
    case "SA":
      ret = RRule.SA;
      break;
    }
    if (d['offset'] !== 0) {
      return ret.nth(d['offset']);
    } else {
      return ret;
    }
  });
};

/**
 * Convert integer list from BM syntax to RRule syntax
 * 
 * @param {Array.<integer>} values
 * @return {Array.<integer>}
 * @private
 */
net.bluemind.rrule.OccurrencesHelper.prototype.intList_ = function(values) {
  if (values.length > 0) {
    return values;
  }
  return null;
};

/**
 * Convert frequency from BM syntax to RRule syntax
 * 
 * @param {string} frequency
 * @return {integer}
 * @private
 */
net.bluemind.rrule.OccurrencesHelper.prototype.freq_ = function(frequency) {
  var ret = null;
  switch (frequency) {
  case "SECONDLY":
    ret = RRule.SECONDLY;
    break;
  case "MINUTELY":
    ret = RRule.MINUTELY;
    break;
  case "HOURLY":
    ret = RRule.HOURLY;
    break;
  case "DAILY":
    ret = RRule.DAILY;
    break;
  case "WEEKLY":
    ret = RRule.WEEKLY;
    break;
  case "MONTHLY":
    ret = RRule.MONTHLY;
    break;
  case "YEARLY":
    ret = RRule.YEARLY;
    break;
  }
  return ret;
}

/**
 * Does this date must be excluded
 * 
 * @param {Date} date Events date
 * @param { <Array.<Object>} exdate Exdate rule.
 * @private
 * @return {boolean}
 */
net.bluemind.rrule.OccurrencesHelper.prototype.isAnExDate_ = function(ctx, exdates, iso8601) {
  return goog.array.some(exdates, function(exdate) {
    return exdate == iso8601;
  });};
