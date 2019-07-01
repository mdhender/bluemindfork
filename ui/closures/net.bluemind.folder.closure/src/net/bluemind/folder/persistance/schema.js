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
 * @fileoverview Provides schema for container item persistance
 */
goog.provide('net.bluemind.folder.persistance.schema');

/**
 * @type {!DatabaseSchema}
 */
net.bluemind.folder.persistance.schema = /** @type {!DatabaseSchema} */
({
  resetTags : ['3.1.22000', '3.1.22528', '4.1.35480'],
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
      name : 'containerDescriptor.type',
      keyPath : 'containerDescriptor.type',
      type : 'TEXT'
    }, {
      name : 'containerDescriptor.uid',
      keyPath : 'containerDescriptor.uid',
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
    keyPath : 'property',
    type : 'TEXT'
  }, {
    name : 'csettings',
    keyPath : 'uid',
    type : 'TEXT'
  } ]
});
