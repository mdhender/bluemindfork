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
 * @fileoverview Get calendar view data from Web Storage.
 */

goog.provide('bluemind.calendar.model.calendarview.WSCalendarViewHome');

goog.require('bluemind.storage.StorageHelper');

/**
 * Ask session storage for calendar view data.
 * @implements {bluemind.calendar.model.calendarview.ICalendarViewHome}
 * @constructor
 */
bluemind.calendar.model.calendarview.WSCalendarViewHome = function() {
  this.storage_ = bluemind.storage.StorageHelper.getWebStorage();
};

/**
 * @type {goog.debug.Logger}
 * @protected
 */
bluemind.calendar.model.calendarview.WSCalendarViewHome.prototype.logger =
  goog.debug.Logger.getLogger('bluemind.calendar.model.calendarview.WSCalendarViewHome');

/**
 * Standard storage whith the default mechanism.
 * @return {goog.storage.Storage} Storage accessor.
 * @private
 */
bluemind.calendar.model.calendarview.WSCalendarViewHome.storage_;

/** @override */
bluemind.calendar.model.calendarview.WSCalendarViewHome.prototype.init = function(cv) {
};

/** @override */
bluemind.calendar.model.calendarview.WSCalendarViewHome.prototype.store = function(cv) {
};

/** @override */
bluemind.calendar.model.calendarview.WSCalendarViewHome.prototype.remove = function(id) {
};

/** @override */
bluemind.calendar.model.calendarview.WSCalendarViewHome.prototype.get = function(callback) {
};
