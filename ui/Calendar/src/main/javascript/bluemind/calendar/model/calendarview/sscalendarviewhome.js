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
 * @fileoverview Get calendar view data from Session Storage.
 */

goog.provide('bluemind.calendar.model.calendarview.SSCalendarViewHome');


goog.require('bluemind.calendar.model.calendarview.WSCalendarViewHome');
goog.require('bluemind.storage.StorageHelper');

/**
 * Ask session storage for attendee data.
 * @implements {bluemind.calendar.model.calendarview.ICalendarViewHome}
 * @constructor
 * @extends {bluemind.calendar.model.calendarview.WSCalendarViewHome}
 */
bluemind.calendar.model.calendarview.SSCalendarViewHome = function() {
  this.storage_ = bluemind.storage.StorageHelper.getSessionStorage();
};
goog.inherits(bluemind.calendar.model.calendarview.SSCalendarViewHome,
  bluemind.calendar.model.calendarview.WSCalendarViewHome);

