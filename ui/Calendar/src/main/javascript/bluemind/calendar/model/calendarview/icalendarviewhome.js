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
 * @fileoverview Interface for calendar view model management.
 */

goog.provide('bluemind.calendar.model.calendarview.ICalendarViewHome');

goog.require('bluemind.calendar.model.CalendarView');
goog.require('goog.async.Deferred');

/**
 * Interface for attendee storage.
 * @interface
 */
bluemind.calendar.model.calendarview.ICalendarViewHome = function() {
};

/**
 * Init storage.
 * @return {goog.async.Deferred} Deferred init state.
 */
bluemind.calendar.model.calendarview.ICalendarViewHome.prototype.init = function() {
};

/**
 * Save a calendar view
 * @param {bluemind.calendar.model.CalendarView} cv Calendar view.
 */
bluemind.calendar.model.calendarview.ICalendarViewHome.prototype.store = function(cv) {
};

/**
 * Delete a calendar view
 * @param {number} id view id.
 */
bluemind.calendar.model.calendarview.ICalendarViewHome.prototype.remove = function(id) {
};

/**
 * Get user calendar views
 * @param {Object} callback callback function.
 */
bluemind.calendar.model.calendarview.ICalendarViewHome.prototype.get = function(callback) {
};
