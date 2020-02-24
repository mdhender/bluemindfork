/**
 * /* BEGIN LICENSE
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
 *
 * @format
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
net.bluemind.rrule.OccurrencesHelper = function() {};

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
 * @param {Object} vseries VEventSeries
 * @param {goog.date.DateRange} range Range of date
 * @return {Object} vseries Serie with expanded occurrences
 */
net.bluemind.rrule.OccurrencesHelper.prototype.expandSeries = function(ctx, vseries, range) {
    if (vseries["value"]["main"] != null && vseries["value"]["main"]["rrule"] != null) {
        var dates = this.getOccurrences(ctx, vseries, range);
        var start = ctx.helper("date").create(vseries["value"]["main"]["dtstart"]);
        var end = ctx.helper("date").create(vseries["value"]["main"]["dtend"]);
        var date = start.clone();
        var duration = 0;
        var utc = net.bluemind.timezone.UTC;
        if (start && end) {
            duration = end.getTime(utc) - start.getTime(utc);
        }
        var exdates = [];
        if (goog.isArray(vseries["value"]["main"]["exdate"])) {
            goog.array.forEach(vseries["value"]["main"]["exdate"], function(exdate) {
                exdates.push(exdate["iso8601"]);
            });
        }
        var occurrences = vseries["value"]["occurrences"];
        vseries["value"]["occurrences"] = [];
        goog.array.forEach(occurrences, function(occurrence) {
            exdates.push(occurrence["recurid"]["iso8601"]);
            var dtstart = ctx.helper("date").create(occurrence["dtstart"]);
            var dtend = ctx.helper("date").create(occurrence["dtend"]);
            if (
                goog.date.Date.compare(dtstart, range.getEndDate() || dtstart) < 0 &&
                goog.date.Date.compare(dtend, range.getStartDate() || dtend) > 0
            ) {
                vseries["value"]["occurrences"].push(occurrence);
            }
        });
        goog.array.forEach(
            dates,
            function(instance) {
                date.set(instance);
                var iso8601 = date.toIsoString(true, true);
                if (isAnExDate_(exdates, iso8601)) {
                    return;
                }

                var occurrence = deepClone(vseries["value"]["main"]);
                occurrence['draft'] = true;
                occurrence["dtstart"]["iso8601"] = iso8601;
                date.setTime(date.getTime(utc) + duration, utc);
                occurrence["dtend"]["iso8601"] = date.toIsoString(true, true);
                occurrence["exdate"] = null;
                occurrence["recurid"] = occurrence["dtstart"];
                vseries["value"]["occurrences"].push(occurrence);
            },
            this
        );
    } else if (vseries["value"]["main"] != null) {
        vseries["value"]["occurrences"].push(vseries["value"]["main"]);
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
    var occurrences = [];
    var exceptions = new goog.structs.Map();
    var recurring = [];
    goog.array.forEach(
        vevents,
        function(vevent) {
            if (!exceptions.containsKey(vevent["container"])) {
                exceptions.set(vevent["container"], new goog.structs.Map());
            }
            if (vevent["value"]["recurid"] != null) {
                var exdates = exceptions.get(vevent["container"]).get(vevent["value"]["uid"]) || [];
                exdates.push(vevent["value"]["recurid"]);
                exceptions.get(vevent["container"]).set(vevent["value"]["uid"], exdates);
            }
            vevent["value"]["rrule"] != null ? recurring.push(vevent) : occurrences.push(vevent);
        },
        this
    );
    goog.array.forEach(
        recurring,
        function(vevent) {
            var exdates = exceptions.get(vevent["container"]).get(vevent["value"]["uid"]) || [];
            goog.array.extend(occurrences, this.getRecurringEventOccurrences(ctx, vevent, range, exdates));
        },
        this
    );
    return occurrences;
};

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @param {Object} vevent Recurring Vevent
 * @param {goog.date.DateRange} range Range of date
 *          occurrences.
 * @return {Array.<Object>} Clone of the input vevent with effective dtstart
 *         and no rrule.
 */
net.bluemind.rrule.OccurrencesHelper.prototype.getRecurringEventOccurrences = function(ctx, vevent, range) {
    var occurrences = [];
    var dates = this.getOccurrences(ctx, vevent["value"], range);
    var start = ctx.helper("date").create(vevent["value"]["dtstart"]);
    var end = ctx.helper("date").create(vevent["value"]["dtend"]);
    var duration = 0;
    if (start && end) {
        duration = end.getTime() - start.getTime();
    }
    var dt = start.clone();
    goog.array.forEach(dates, function(date) {
        var occurrence = deepClone(vevent);
        dt.set(date);
        occurrence["value"]["dtstart"]["iso8601"] = dt.toIsoString(true, true);
        dt.setTime(dt.getTime() + duration);
        occurrence["value"]["dtend"]["iso8601"] = dt.toIsoString(true, true);
        occurrence["value"]["exdate"] = null;
        occurrence["value"]["recurid"] = occurrence["value"]["dtstart"];
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
 * @return {Array.<goog.date.Date>} Occurrences date
 */
net.bluemind.rrule.OccurrencesHelper.prototype.getOccurrences = function(ctx, vseries, range) {
    var dateHelperCreate = getDateHelperCreate(ctx);
    var dates = [];
    var from = range.getStartDate();
    var until = range.getEndDate();
    var main = vseries["value"]["main"];
    var dtstart = dateHelperCreate(main["dtstart"]);
    if (!(dtstart instanceof goog.date.DateTime)) {
        dtstart = new net.bluemind.date.DateTime(dtstart);
    }
    dtstart.setMilliseconds(0);
    if (main["dtend"]) {
        var dtend = dateHelperCreate(main["dtend"]);
        var duration = dtend.getTime() - dtstart.getTime();
        from = from.clone();
        from.setTime(from.getTime() - duration);
    }

    if (main["rrule"]) {
        var untilDate = dateHelperCreate(main["rrule"]["until"], dtstart.getTimeZone());
        var opt = toRRuleOptions(dtstart, main["rrule"], untilDate);
        var json = JSON.stringify(opt);
        var rule = net.bluemind.rrule.OccurrencesHelper.cache_[json] || new RRule(opt, false);
        net.bluemind.rrule.OccurrencesHelper.cache_[json] = rule;
        var ocs = rule.between(from, until, [true, false]);
        goog.array.extend(dates, ocs);
    } else if (
        goog.date.Date.compare(dtstart, until || dtstart) < 0 &&
        goog.date.Date.compare(dtstart, from || dtstart) > 0
    ) {
        dates.push(dtstart);
    }

    if (Array.isArray(main["rdate"])) {
        goog.array.extend(
            dates,
            main["rdate"]
                .map(function(date) {
                    return dateHelperCreate(date);
                })
                .filter(function(date) {
                    return date.getTime() > from.getTime() && date.getTime() < until.getTime();
                })
        );
    }

    goog.array.removeDuplicates(dates);
    return dates;
};

function toRRuleOptions(dtstart, rrule, untilDate) {
    return {
        dtstart: dtstart,
        freq: freq_(rrule["frequency"]),
        interval: rrule["interval"] || 1,
        count: rrule["count"],
        until: untilDate,
        bysecond: intList_(rrule["bySecond"]),
        byminute: intList_(rrule["byMinute"]),
        byhour: intList_(rrule["byHour"]),
        byweekday: dayList_(rrule["byDay"]),
        bymonthday: intList_(rrule["byMonthDay"]),
        byyearday: intList_(rrule["byYearDay"]),
        byweekno: intList_(rrule["byWeekNo"]),
        bymonth: intList_(rrule["byMonth"])
    };
}

function getDateHelperCreate(ctx) {
    var dateHelperCreate = ctx.helper("date").create.bind(ctx.helper("date"));
    return function(date, timezone) {
        return date ? dateHelperCreate(date, timezone) : undefined;
    };
}

/**
 * Convert day list from BM syntax to RRule syntax
 *
 * @param {Array} days
 * @return {Array} null or Array
 * @private
 */
function dayList_(days) {
    if (!days || days.length === 0) {
        return null;
    }

    var codec = {
        "SU": RRule.SU,
        "MO": RRule.MO,
        "TU": RRule.TU,
        "WE": RRule.WE,
        "TH": RRule.TH,
        "FR": RRule.FR,
        "SA": RRule.SA
    };
    return days.map(function(day) {
        var ret = codec[day["day"]];
        return day["offset"] !== 0 ? ret.nth(day["offset"]) : ret;
    });
}

/**
 * Convert empty array to null
 *
 * @param {Array} values
 * @return {Array} or null
 */
function intList_(values) {
    if (!values || values.length === 0) {
        return null;
    }
    return values;
}

/**
 * Convert frequency from BM syntax to RRule syntax
 *
 * @param {string} frequency
 * @return {number|null}
 * @private
 */
function freq_(frequency) {
    if (!frequency) {
        return null;
    }
    var toRRuleFrequency = {
        "SECONDLY": RRule.SECONDLY,
        "MINUTELY": RRule.MINUTELY,
        "HOURLY": RRule.HOURLY,
        "DAILY": RRule.DAILY,
        "WEEKLY": RRule.WEEKLY,
        "MONTHLY": RRule.MONTHLY,
        "YEARLY": RRule.YEARLY
    };
    return toRRuleFrequency[frequency];
}

/**
 * Should this date be excluded?
 *
 * @param {Array.<Date>} exdates Array with exdates.
 * @param {Date} iso8601 Event date
 * @private
 * @return {boolean}
 */
function isAnExDate_(exdates, iso8601) {
    return goog.array.some(exdates, function(exdate) {
        return exdate === iso8601;
    });
}

net.bluemind.rrule.OccurrencesHelper.prototype.getNextOccurrence = function(ctx, vseries, dtstart, from) {
    var dateHelperCreate = getDateHelperCreate(ctx);
    var exdates = deepClone(vseries.value["main"]["exdate"]);
    goog.array.extend(
        exdates,
        vseries.value["occurrences"].map(function(event) {
            return event["recurid"];
        })
    );
    if (!(dtstart instanceof goog.date.DateTime)) {
        dtstart = new net.bluemind.date.DateTime(dtstart);
    }
    dtstart.setMilliseconds(0);
    var rruleObject = vseries.value["main"]["rrule"];
    var untilDate = dateHelperCreate(rruleObject["until"], dtstart.getTimeZone());
    var date = this.findNextOccurrenceDate(dtstart, rruleObject, exdates, untilDate, from);
    return date;
};

net.bluemind.rrule.OccurrencesHelper.prototype.findNextOccurrenceDate = function(
    dtstart,
    rruleObject,
    exdates,
    untilDate,
    from
) {
    var rruleOptions = toRRuleOptions(dtstart, rruleObject, untilDate);
    var rrule = new RRule(rruleOptions, false);
    var date = from;
    do {
        date.set(rrule.after(date.clone(), false));
    } while (isAnExDate_(exdates, date.toIsoString(true, true)));
    return date;
};

function deepClone(obj) {
    return JSON.parse(JSON.stringify(obj));
}
