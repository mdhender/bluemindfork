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
 * @fileoverview
 *
 * Provides the URL Map for the calendar application.
 */
goog.provide('bluemind.calendar.urls');

goog.require('bluemind.calendar.handlers.WeekHandler');
goog.require('relief.handlers.errors');

/**
 * @type {!relief.nav.URLMap}
 */
bluemind.calendar.urls = {
//  '': bluemind.calendar.handlers.WeekHandler,
 // '/': bluemind.calendar.handlers.WeekHandler,
 // '/week': bluemind.calendar.handlers.WeekHandler,

  // We'll use the simple error handlers provided by Relief.
  ':401': relief.handlers.errors.Error401,
  ':404': relief.handlers.errors.Error404,
  ':501': relief.handlers.errors.Error501
};
