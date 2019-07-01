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

goog.provide("net.bluemind.calendar.filters.CalendarsFilter");

goog.require("goog.array");
goog.require("net.bluemind.mvp.Filter");
goog.require("net.bluemind.calendar.Messages");
/**
 * @constructor
 * 
 * @extends {net.bluemind.mvp.Filter}
 */
net.bluemind.calendar.filters.CalendarsFilter = function() {
  goog.base(this);
}
goog.inherits(net.bluemind.calendar.filters.CalendarsFilter, net.bluemind.mvp.Filter);

/**
 * Higher priority because some filters might use 'calendars' session variable.
 * 
 * @override
 */
net.bluemind.calendar.filters.CalendarsFilter.prototype.priority = 75;

/** @override */
net.bluemind.calendar.filters.CalendarsFilter.prototype.filter = function(ctx) {
  return ctx.service('calendarsMgmt').list('calendar').then(function(calendars) {
    if (calendars && calendars.length) {
      ctx.session.set('calendars', calendars);
      var def = goog.array.find(calendars, function(calendar) {
        return calendar['uid'] && calendar['defaultContainer'] && calendar['owner'] == ctx.user['uid'];
      }) || goog.array.find(calendars, function(calendar) {
        return calendar['uid'] && calendar['defaultContainer'] && calendar['writable'];
      }) || calendars[0] ;
      
      ctx.session.set('calendar.default', def['uid']);
    } else {
      ctx.session.set('calendars', []);
    }
  }, function(error) {
    ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
};
