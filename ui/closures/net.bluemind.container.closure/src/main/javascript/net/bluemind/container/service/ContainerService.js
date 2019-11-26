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
 * @fileoverview Manage container storage.
 */

goog.provide("net.bluemind.container.service.ContainerService");
goog.provide("net.bluemind.container.service.ContainerService.EventType");

goog.require("goog.events");
goog.require("goog.events.EventTarget");
goog.require("net.bluemind.container.persistence.DBItemHome");

/**
 * Frontend for accessing to items storage manager.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context. *
 * @param {string=} databaseName Database name
 * @extends {goog.events.EventTarget}
 * @constructor
 */
net.bluemind.container.service.ContainerService = function(ctx, databaseName) {
  goog.base(this);
  this.ctx = ctx;
  this.databaseName = databaseName;
  this.database = null;
  this.initDb_();
};
goog.inherits(net.bluemind.container.service.ContainerService, goog.events.EventTarget);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.container.service.ContainerService.prototype.ctx;

/**
 * Entry storage backend
 * 
 * @type {net.bluemind.container.persistence.DBItemHome}
 */
net.bluemind.container.service.ContainerService.prototype.entryBackend_;

/**
 * @suppress {missingProperties}
 */
net.bluemind.container.service.ContainerService.prototype.initDb_ = function() {
  if (!this.database) {
    this.database = this.ctx.service('database').getDatabase(this.databaseName);
    if (this.database) {
      this.entryBackend_ = new net.bluemind.container.persistence.DBItemHome(this.database);
    }
  }
}

/**
 * @return {boolean}
 */
net.bluemind.container.service.ContainerService.prototype.available = function() {
  this.initDb_();
  return this.database != null;
}

/**
 * Get item by uid.
 * 
 * @param {string} containerId Item uid.
 * @return {goog.async.Deferred} With item object as result.
 */
net.bluemind.container.service.ContainerService.prototype.getItem = function(containerId, id) {
  this.initDb_();
  return this.entryBackend_.getItem(containerId, id);
};

/**
 * Get item by uid.
 * 
 * @param {string} containerId Item uid.
 * @return {goog.async.Deferred} With item object as result.
 */
net.bluemind.container.service.ContainerService.prototype.getMultiple = function(containerId, ids) {
  this.initDb_();
  return goog.async.DeferredList.gatherResults(goog.array.map(ids, function(id) {
    return this.entryBackend_.getItem(containerId, id);
  }, this));
};

/**
 * Get all items from a container.
 * 
 * @param {string} containerId Container uid
 * @param {number=} opt_offset Optional offset number (default 0).
 * @return {goog.async.Deferred} Deferred object containing container list.
 */
net.bluemind.container.service.ContainerService.prototype.getItems = function(containerId, opt_offset) {
  this.initDb_();
  return this.entryBackend_.getItems(containerId, opt_offset);
};

/**
 * count all items from a container.
 * 
 * @param {string} containerId Container uid
 * @return {goog.async.Deferred} Deferred object containing container list.
 */
net.bluemind.container.service.ContainerService.prototype.countItems = function(containerId) {
  this.initDb_();
  return this.entryBackend_.count(containerId);
};

/**
 * count all items from a container.
 * 
 * @param {string} containerId Container uid
 * @param {Array.<string>=} opt_index
 * @return {goog.async.Deferred} Deferred object containing container list.
 */
net.bluemind.container.service.ContainerService.prototype.getItemsIndex = function(containerId, opt_index) {
  this.initDb_();
  return this.entryBackend_.getIndex(containerId, opt_index);
};

/**
 * @param {string} containerId
 * @param {string} id
 * @param {string} toContainerId
 * @returns {goog.async.Deferred}
 */
net.bluemind.container.service.ContainerService.prototype.copyItem = function(containerId, id, toContainerId) {
  this.initDb_();
  return this.entryBackend_.getItem(containerId, id).addCallback(function(item) {
    if (item == null) {
      throw 'Item ' + id + ' not found';
    } else {
      item['container'] = toContainerId;
      return this.entryBackend_.storeItem(toContainerId, item)
    }
  }, this);
};

/**
 * Get item position in container. The position is given according to the
 * 'order' index.
 * 
 * @param {string} containerId
 * @param {{id: string, order: string}} item
 * @return {goog.async.Deferred}
 */
net.bluemind.container.service.ContainerService.prototype.getItemPosition = function(containerId, item) {
  this.initDb_();
  return this.entryBackend_.getPosition(containerId, item);
};
/**
 * @param {string} containerId
 * @param {string} id
 * @param {string} toContainerId
 * @returns {goog.async.Deferred}
 */
net.bluemind.container.service.ContainerService.prototype.moveItem = function(containerId, id, toContainerId) {
  this.initDb_();
  return this.entryBackend_.getItem(containerId, id).addCallback(function(item) {
    if (item == null) {
      throw 'Item ' + id + ' not found';
    } else {
      item['container'] = toContainerId;
      return this.entryBackend_.storeItem(toContainerId, item)
    }
  }, this).addCallback(function() {
    return this.entryBackend_.deleteItem(containerId, id);
  }, this);
};

/**
 * @param {string} containerId
 * @param {string} id
 * @returns {goog.async.Deferred}
 */
net.bluemind.container.service.ContainerService.prototype.deleteItem = function(containerId, id) {
  this.initDb_();
  var ret = this.entryBackend_.deleteItem(containerId, id);
  ret.addCallback(function() {
    this.dispatchItemsChanged_(containerId);
  }, this);
  return ret;
};

/**
 * @param {string} containerId
 * @param {string} id
 * @return {goog.Promise}
 */
net.bluemind.container.service.ContainerService.prototype.deleteItemWithoutChangeLog = function(containerId, id) {
  this.initDb_();
  var ret = this.entryBackend_.deleteItemWithoutChangeLog(containerId, id);
  ret.addCallback(function() {
    this.dispatchItemsChanged_(containerId);
  }, this);
  return ret;
};

/**
 * Create or update a task entry.
 * 
 * @param {Object} entry Item to create.
 * @return {goog.async.Deferred} Created task object.
 */
net.bluemind.container.service.ContainerService.prototype.storeItem = function(entry) {
  this.initDb_();
  var ret = this.entryBackend_.storeItem(entry['container'], entry);
  ret.addCallback(function() {
    this.dispatchItemsChanged_(entry['container']);
  }, this);
  return ret;
};

/**
 * add an item to the changelog.
 * 
 * @param {Object} entry Item to create.
 * @return {goog.async.Deferred} Created task object.
 */
net.bluemind.container.service.ContainerService.prototype.addToChangeLog = function(entry) {
  this.initDb_();
  return this.entryBackend_.addToChangeLog(entry);
};

/**
 * Create or update a task entry.
 * 
 * @param {Object} entry Item to create.
 * @return {goog.async.Deferred} Created task object.
 */
net.bluemind.container.service.ContainerService.prototype.storeItemWithoutChangeLog = function(entry) {
  this.initDb_();
  var ret = this.entryBackend_.storeItemWithoutChangeLog(entry['container'], entry);
  ret.addCallback(function() {
    this.dispatchItemsChanged_(entry['container']);
  }, this);
  return ret;
};

/**
 * Create or update a task entry.
 * 
 * @param {string} container Container uid
 * @param {Array.<Object>} entries Item to create.
 * @return {goog.async.Deferred} Created task object.
 */
net.bluemind.container.service.ContainerService.prototype.storeItemsWithoutChangeLog = function(container, entries) {
  this.initDb_();
  var ret = this.entryBackend_.storeItemsWithoutChangeLog(container, entries);
  ret.addCallback(function() {
    this.dispatchItemsChanged_(container);
  }, this);
  return ret;
};

/**
 * @param {string} containerId
 * @returns {goog.async.Deferred}
 */
net.bluemind.container.service.ContainerService.prototype.getLocalChangeSet = function(containerId) {
  this.initDb_();
  return this.entryBackend_.getLocalChangeSet(containerId);
};

/**
 * Get all items from a container matching the given query.
 * 
 * @param {string} containerId Container uid
 * @param {Object} query Matching query // FIXME
 * @param {number=} opt_offset Optional offset number (default none).
 * @param {number=} opt_limit Optional limit number (default none).
 * @return {goog.async.Deferred} Deferred object containing container list.
 */
net.bluemind.container.service.ContainerService.prototype.searchItems = function(containerId, query, opt_offset,
    opt_limit) {
  this.initDb_();
  if (containerId) {
    query.push([ 'container', '=', containerId ]);
  }
  return this.entryBackend_.searchItems(query, opt_offset, opt_limit);
};
/**
 * @param {string} containerId
 * @private
 */
net.bluemind.container.service.ContainerService.prototype.dispatchItemsChanged_ = function(containerId) {
  var e = new goog.events.Event(net.bluemind.container.service.ContainerService.EventType.CHANGE);
  e.container = containerId;
  this.dispatchEvent(e);
};

/**
 * @param {string} containerId
 * @param {Array} changed
 * @param {Array} deleted
 * @param {Array} errors
 * @param version
 * @returns
 */
net.bluemind.container.service.ContainerService.prototype.syncItems = function(containerId, changed, deleted, version) {
  if (changed.length > 0 || deleted.length > 0) {
    this.initDb_();
    return this.entryBackend_.syncItems(containerId, changed, deleted, version).addCallback(function() {
      this.dispatchItemsChanged_(containerId);
    }, this);
  }
  return new goog.Promise.resolve();

};

/**
 * @param {string} containerId
 * @param {Array} changed
 * @param {Array} deleted
 * @param {Array} errors
 * @param version
 * @returns
 */
net.bluemind.container.service.ContainerService.prototype.clearChangelog = function (containerId, changed, deleted, errors) {
  if (changed.length > 0 || deleted.length > 0 || errors.length > 0) {
    this.initDb_();
    return this.entryBackend_.clearChangelog(containerId, changed, deleted, errors);
  }
  return new goog.Promise.resolve();

};
/**
 * @enum {string}
 */
net.bluemind.container.service.ContainerService.EventType = {
  CHANGE : goog.events.getUniqueId('changed')
};
