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
 * @fileoverview Interface for container model management.
 */

goog.provide('net.bluemind.container.service.ContainersService');
goog.provide('net.bluemind.container.service.ContainersService.EventType');

goog.require('goog.events.EventTarget');
goog.require('goog.async.Deferred');
goog.require('goog.array');
goog.require('bluemind.storage.StorageHelper');
goog.require('net.bluemind.container.persistence.DBContainerHome');
goog.require('net.bluemind.container.persistence.DBItemHome');

/**
 * Service provider object for containers
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @param {string=} databaseName Database name
 * @constructor
 * @extends {goog.events.EventTarget}
 */
net.bluemind.container.service.ContainersService = function(ctx, databaseName) {
  goog.base(this);
  this.ctx = ctx;
  this.databaseName = databaseName;
  this.initDb_();
};
goog.inherits(net.bluemind.container.service.ContainersService, goog.events.EventTarget);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.container.service.ContainersService.prototype.ctx;

/**
 * Container storage backend
 * 
 * @type {net.bluemind.container.persistence.IContainerHome}
 * @private
 */
net.bluemind.container.service.ContainersService.prototype.backend_;

/**
 * Entry storage backend
 * 
 * @type {net.bluemind.container.persistence.DBItemHome}
 * @private
 */
net.bluemind.container.service.ContainersService.prototype.entryBackend_;

/**
 * @suppress {missingProperties}
 * @private
 */
net.bluemind.container.service.ContainersService.prototype.initDb_ = function() {
  if (!this.database) {
    if (this.ctx.databaseAvailable) {
      this.database = this.ctx.service('database').getDatabase(this.databaseName);
      if (this.database) {
        this.backend_ = new net.bluemind.container.persistence.DBContainerHome(this.database);
        this.entryBackend_ = new net.bluemind.container.persistence.DBItemHome(this.database);
      }
    }
  }
}

/**
 * @return {boolean}
 */
net.bluemind.container.service.ContainersService.prototype.available = function() {
  this.initDb_();
  return this.database != null;
}

/**
 * List containers by type
 * 
 * @param {string=} opt_type
 * @return {goog.async.Deferred}
 */
net.bluemind.container.service.ContainersService.prototype.list = function(opt_type) {
  this.initDb_();
  if (opt_type == undefined) {
    return this.backend_.getContainers();
  } else {
    var result = new goog.async.Deferred();
    this.backend_.getContainers().addCallback(function(res) {
      var t = goog.array.filter(res, function(c) {
        return c['type'] == opt_type;
      });
      result.callback(t);
    }).addErrback(function(e) {
      result.errback(e);
    });

    return result;
  }
};

/**
 * Get container by uids.
 * 
 * @param {Array.<string>} uids Container uids.
 * @return {goog.async.Deferred} With container object as result.
 */
net.bluemind.container.service.ContainersService.prototype.mget = function(uids) {
  this.initDb_();
  return this.backend_.getContainers(uids);
};

/**
 * Get container by uid.
 * 
 * @param {string} uid Container uid.
 * @return {goog.async.Deferred} With container object as result.
 */
net.bluemind.container.service.ContainersService.prototype.get = function(uid) {
  this.initDb_();
  return this.backend_.getContainer(uid);
};

/**
 * @param {string} uid
 * @return {goog.Promise}
 */
net.bluemind.container.service.ContainersService.prototype.remove = function(uid) {
  this.initDb_();
  return this.backend_.deleteContainer(uid).then(function() {
    return this.dispatchEvent(net.bluemind.container.service.ContainersService.EventType.CHANGE);
  }, null, this);
};

/**
 * @param {Array.<Object>} containers
 * @return {goog.Promise} With containers object as result.
 */
net.bluemind.container.service.ContainersService.prototype.addMultiple = function(containers) {
  this.initDb_();
  return this.backend_.store(containers).then(function() {
    this.dispatchEvent(net.bluemind.container.service.ContainersService.EventType.CHANGE);
  }, null, this);
};

/**
 * @param {Object} container
 * @return {goog.Promise}
 */
net.bluemind.container.service.ContainersService.prototype.add = function(container) {
  this.initDb_();
  return this.backend_.store([ container ]).then(function() {
    this.dispatchEvent(net.bluemind.container.service.ContainersService.EventType.CHANGE);
  }, null, this);
};

/**
 * @param {string} uid
 * @param {Object} settings
 * @return {goog.Promise}
 */
net.bluemind.container.service.ContainersService.prototype.setSettings = function(uid, settings) {
  this.initDb_();
  return this.backend_.getContainer(uid).then(function(container) {
    container['settings'] = settings;
    this.backend_.store(container);
    this.backend_.markSettingsUpdate(container['uid']);
  }, null, this);
};

net.bluemind.container.service.ContainersService.prototype.setSettingsWithoutChangeLog = function(uid, settings) {
  this.initDb_();
  return this.backend_.getContainer(uid).then(function(container) {
    container['settings'] = settings;
    this.backend_.store(container);
  }, null, this);
};

/**
 * @return {goog.Promise}
 */
net.bluemind.container.service.ContainersService.prototype.getSettingsChanges = function() {
  this.initDb_();
  return this.backend_.getSettingsChanges();
}

/**
 * @param {string} uid
 * @return {goog.Promise}
 */
net.bluemind.container.service.ContainersService.prototype.settingsApplied = function(uid) {
  return this.backend_.settingsApplied(uid);
}
/**
 * FIXME : This method is used to store data from sync service in local storage.
 * Sync service should directly call persistence.
 * 
 * @param type
 * @param containers
 * @return
 */
net.bluemind.container.service.ContainersService.prototype.sync = function(type, containers) {
  return this.backend_.getContainers().then(function(current) {
    return this.backend_.sync(type, containers).addCallback(function(stored) {
      if (this.changes_(current, containers)) {
        this.dispatchEvent(net.bluemind.container.service.ContainersService.EventType.CHANGE);
      }
      return containers;
    }, this);
  }, null, this);

};

net.bluemind.container.service.ContainersService.prototype.changes_ = function(current, containers) {
  if (current.length == containers.length) {
    // sort by uid
    goog.array.sort(current, function(a, b) {
      return goog.string.caseInsensitiveCompare(a['uid'], b['uid']);
    });
    goog.array.sort(containers, function(a, b) {
      return goog.string.caseInsensitiveCompare(a['uid'], b['uid']);
    });

    // search modifications
    var notChanged = goog.array.reduce(containers, function(ret, c, i) {
      return ret && (c['uid'] == current[i]['uid']) && (c['name'] == current[i]['name']);
    }, true);

    if (notChanged) {
      return false;
    } else {
      return true;
    }
  } else {
    return true;
  }
}
/**
 * FIXME : This method is used to store data from sync service in local storage.
 * Sync service should directly call persistence.
 * 
 * @param {Array.<Object>} containers
 * @return
 */
net.bluemind.container.service.ContainersService.prototype.store = function(containers) {
  return this.backend_.store(containers).addCallback(function(stored) {
    return containers;
  }, this);
};

/**
 * FIXME : This method is used to store data from sync service in local storage.
 * Sync service should directly call persistence.
 * 
 * @param {string} containerId
 * @param {number} version
 * @return {goog.async.Deferred} Deferred object
 */
net.bluemind.container.service.ContainersService.prototype.setSyncVersion = function(containerId, version) {
  return this.backend_.setSyncVersion(containerId, version);
};

/**
 * 
 * @param {string} containerId
 * @return {goog.async.Deferred} Deferred object containing sync version
 */
net.bluemind.container.service.ContainersService.prototype.getSyncVersion = function(containerId) {
  return this.backend_.getSyncVersion(containerId);
};

/**
 * Get all items matching the given query.
 * 
 * @param {Object} query Matching query
 * @param {number=} opt_offset Optional offset number (default none).
 * @param {number=} opt_limit Optional limit number (default none).
 * @return {goog.async.Deferred} Deferred object containing container list.
 */
net.bluemind.container.service.ContainersService.prototype.searchItems = function(query, opt_offset, opt_limit) {
  return this.entryBackend_.searchItems(query, opt_offset, opt_limit);
};

/**
 * @return {goog.async.Deferred} Deferred object containing all items
 */
net.bluemind.container.service.ContainersService.prototype.getItems = function() {
  return this.entryBackend_.all();
};

/**
 * @returns {goog.async.Deferred}
 */
net.bluemind.container.service.ContainersService.prototype.getLocalChangeSet = function() {
  return this.entryBackend_.getLocalChangeSet();
};

/**
 * @enum {string}
 */
net.bluemind.container.service.ContainersService.EventType = {
  CHANGE : goog.events.getUniqueId('changed')
};
