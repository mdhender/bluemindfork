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
goog.provide('bluemind.calendar.schema');

/**
 * @type {!Object}
 */
bluemind.calendar.schema = {
  stores: [{
    name: 'event',
    keyPath: 'extId',
    type: 'TEXT',
    indexes: [
      { keyPath: 'updated', name: 'updated', type: 'TEXT'}]
    }, {
      name: 'occurrence',
      keyPath: 'id',
      type: 'NUMERIC'
    }, {
      name: 'pending',
      keyPath: 'id',
      type: 'INTEGER',
      autoIncrement: true,
      indexes: [
        { keyPath: 'extId', name: 'extId', type: 'TEXT'},
        { keyPath: ['calendar', 'date'], name: 'calendar, date' }]
    }, {
      name: 'attendee',
      keyPath: 'calendar',
      type: 'TEXT'
    }, {
      name: 'lastsync',
      keyPath: 'calendar',
      type: 'INTEGER'
    }, {
      name: 'removed',
      keyPath: 'extId',
      type: 'TEXT'
    }
  ]
};

