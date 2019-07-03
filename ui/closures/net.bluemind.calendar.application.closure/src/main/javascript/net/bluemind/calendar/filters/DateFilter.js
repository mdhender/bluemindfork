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

/** @fileoverview Parse process date range */

goog.provide("net.bluemind.calendar.filters.DateFilter");

goog.require("goog.Promise");
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.date.DateRange");
goog.require("net.bluemind.mvp.Filter");

/**
 * Process date range depending on context parameters. Add
 * 
 * @constructor
 * @extends {net.bluemind.mvp.Filter}
 */
net.bluemind.calendar.filters.DateFilter = function() {
  goog.base(this);
};
goog.inherits(net.bluemind.calendar.filters.DateFilter, net.bluemind.mvp.Filter);

/** @override */
net.bluemind.calendar.filters.DateFilter.prototype.filter = function(ctx) {
  var dtstart, dtend, range;
  if (ctx.params.containsKey('date')) {
    var date = ctx.params.get('date');
    dtstart = ctx.helper('date').fromIsoString(date);
    ctx.session.set('date', dtstart.clone());
  } else if (ctx.session.get('date')) {
    dtstart = ctx.session.get('date').clone();
  } else {
    dtstart = new net.bluemind.date.Date();
    ctx.session.set('date', new net.bluemind.date.Date());
  }
  var r = ctx.params.get('range');
  if (ctx.module && ctx.session.get('range.size') > 7) {
    r = r || 'month'
  } else if (ctx.module && ctx.session.get('range.size') == 1) {
    r = r || 'day'
  } else if (ctx.module && ctx.session.get('range.size') > 0) {
    r = r || 'week'
  }
  r = r || ctx.session.get('defaultview') && ctx.session.get('defaultview').toLowerCase() || 'day';
  // TODO: If dtend or nbdays is set in params.
  // TODO: Use date range algos.


  if (ctx.module == 'month' || r == 'month') {
    range = net.bluemind.date.DateRange.thisCalendarMonth(dtstart);
  } else if (r == 'week') {
    if (ctx.settings.get('showweekends') !== 'true') {
      range = net.bluemind.date.DateRange.thisBusinessWeek(dtstart);
    } else {
      range = net.bluemind.date.DateRange.thisWeek(dtstart);
    }
  } else if (r == 'day') {
    range = net.bluemind.date.DateRange.today(dtstart);
  } else {
    if (ctx.settings.get('showweekends') !== 'true') {
      range = net.bluemind.date.DateRange.thisBusinessWeek(dtstart);
    } else {
      range = net.bluemind.date.DateRange.thisWeek(dtstart);
    }
  }

  ctx.session.set('range', range);
  ctx.session.set('range.size', range.count());

  if (ctx.module == 'list') {
    ctx.session.set('view', 'list');
  } else if (ctx.module == 'search') {
    ctx.session.set('view', 'search');
  } else if (range.count() > 7) {
    ctx.session.set('view', 'month');
  } else if (range.count() == 1) {
    ctx.session.set('view', 'day');
  } else if (ctx.module == 'pending') {
    ctx.session.set('view', 'pending');
  } else {
    ctx.session.set('view', 'week');
  }
  return goog.Promise.resolve();
};
