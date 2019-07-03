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
goog.provide('net.bluemind.authentication.schema');

/**
 * @type {!Object}
 */
net.bluemind.authentication.schema = {
  resetTags : ['3.1.22000', '4.1.35480'],
  stores : [ {
    name : 'configuration',
    keyPath : 'property',
    type : 'TEXT'
  }, {
    name : 'global'
  }, {
    name : 'calendar'
  }, {
    name : 'contact'
  }, {
    name : 'task'
  } ]
};
