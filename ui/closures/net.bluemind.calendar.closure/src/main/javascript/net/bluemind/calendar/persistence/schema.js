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
 * @fileoverview Provides schema for container item persistence
 */
goog.provide('net.bluemind.calendar.persistence.schema');

/**
 * @type {!DatabaseSchema}
 */
net.bluemind.calendar.persistence.schema = /** @type {!DatabaseSchema} */
({
  resetTags : ['3.1.22000', '3.1.22528', '3.1.23062', '4.1.35480'],
  stores : [ {
    name : 'item',
    keyPath : 'id',
    type : 'TEXT',
    indexes : [ {
      name : 'container',
      keyPath : 'container',
      type : 'TEXT'
    }, {
      name : 'container, uid',
      keyPath : [ 'container', 'uid' ]
    }, {
      name : 'container, order, uid',
      keyPath : [ 'container', 'order', 'uid' ]
    }, {
      name : 'fulltext',
      keyPath : 'fulltext',
      type : 'TEXT',
      multiEntry : true
    }, {
      keyPath : [ 'container', 'end' ],
      name : 'container, end'
    }, {
      keyPath : 'start',
      name : 'start',
      type : 'TEXT'
    }, {
      keyPath : 'end',
      name : 'end',
      type : 'TEXT'
    }, {
      keyPath : 'value.uid',
      name : 'value.uid',
      type : 'TEXT'
    } ]
  }, {
    name : 'changes',
    keyPath : 'uid',
    type : 'TEXT',
    indexes : [ {
      name : 'container',
      keyPath : 'container',
      type : 'TEXT'
    } ]
  }, {
    name : 'container',
    keyPath : 'uid',
    type : 'TEXT'
  }, {
    name : 'last_sync',
    keyPath : 'container',
    type : 'TEXT'
  }, {
    name : 'configuration',
    keyPath : 'property'
  }, {
    name : 'csettings',
    keyPath : 'uid',
    type : 'TEXT'
  } ]
}
);
