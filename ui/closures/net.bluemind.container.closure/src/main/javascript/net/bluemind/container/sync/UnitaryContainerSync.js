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
 * @fileoverview Container synchronisation abstract class
 */

goog.provide('net.bluemind.container.sync.UnitaryContainerSync');

goog.require('net.bluemind.sync.SyncService');
goog.require('net.bluemind.container.service.ContainersService');
goog.require('net.bluemind.container.service.ContainerService');

goog.require('bluemind.storage.StorageHelper');
goog.require('bluemind.string');
goog.require('goog.log');
goog.require('goog.log.Logger');
/**
 * Abstract class for synchronisation services based on container model.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @param {string} containerUid container uid to synchronize
 * @constructor
 * @extends {net.bluemind.sync.SyncService}
 */
net.bluemind.container.sync.UnitaryContainerSync = function(ctx, containerUid) {
  goog.base(this);
  this.ctx = ctx;
  this.containerUid = containerUid;
};
goog.inherits(net.bluemind.container.sync.UnitaryContainerSync, net.bluemind.sync.SyncService);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @protected
 */
net.bluemind.container.sync.UnitaryContainerSync.prototype.ctx;

/** @override */
net.bluemind.container.sync.UnitaryContainerSync.prototype.syncInternal = function() {
  goog.log.info(this.logger, 'Synchronize ' + this.getName() + ' with blue-mind core');

  return this.syncContainerItems();
};

/** @override */
net.bluemind.container.sync.UnitaryContainerSync.prototype.isEnabled = function() {
  return this.getContainersService().available();
};

/**
 * Allow to modify data sent by bm-core before storing them Mandatory fields are :
 * <ul>
 * <li>'uid'
 * </ul>
 * 
 * @param {Object} container Container to modify
 * @returns {Object}
 * @protected
 */
net.bluemind.container.sync.UnitaryContainerSync.prototype.adaptContainer = function(container) {
  var c = container;
  return c;
};

/**
 * Allow to modify data sent by bm-core before storing them Mandatory fields are :
 * <ul>
 * <li>'uid'
 * <li> 'name'
 * <li> 'order'
 * <li> 'container'
 * 
 * @param {Object} item Item to modify
 * @returns {Object}
 * @protected
 */
net.bluemind.container.sync.UnitaryContainerSync.prototype.adaptItem = function(item) {
  var i = item;
  i['name'] = i['displayName'];
  if (i['name']) {
    i['order'] = bluemind.string.normalize(i['name'])
  } else {
    goog.log.warning(this.logger, "item " + item['uid'] + " do not have name!");
    i['order'] = 'none';
    i['name'] = 'none';
    i['displayName'] = 'none';
  }
  return i;
};

/**
 * Get Container service.
 * 
 * @returns {net.bluemind.container.service.ContainerService|net.bluemind.container.service.ContainersService}
 * @protected
 */
net.bluemind.container.sync.UnitaryContainerSync.prototype.getContainerService = function() {
  return this.ctx.service('container');
};

/**
 * Get Containers service.
 * 
 * @returns {net.bluemind.container.service.ContainerService|net.bluemind.container.service.ContainersService}
 * @protected
 */
net.bluemind.container.sync.UnitaryContainerSync.prototype.getContainersService = function() {
  return this.ctx.service('containers');
};

net.bluemind.container.sync.UnitaryContainerSync.prototype.adaptCreate = function(entry) {
  return {
    'uid' : entry['uid'],
    'value' : entry['value']
  };
}

net.bluemind.container.sync.UnitaryContainerSync.prototype.adaptUpdate = function(entry) {
  return {
    'uid' : entry['uid'],
    'value' : entry['value']
  };
}

net.bluemind.container.sync.UnitaryContainerSync.prototype.adaptDelete = function(change) {
  return {
    'uid' : change['itemId']
  };
}

/**
 * Get current item client api
 * 
 * @param {string} uid Container uid
 * @return {net.bluemind.container.sync.UnitaryContainerSyncClient} Container
 *         api.
 */
net.bluemind.container.sync.UnitaryContainerSync.prototype.getClient = goog.abstractMethod;

/**
 * Synchronise a container
 * 
 * @param {Object} container Container to synchronise
 * @returns {goog.async.Deferred}
 * @protected
 */
net.bluemind.container.sync.UnitaryContainerSync.prototype.syncContainerItems = function() {
  var containerUid = this.containerUid;
  var service = this.getContainerService();
  var containersService = this.getContainersService();

  var client = this.getClient(containerUid);
  var context = {};
  context.errors = [];
  context.changes = {
    'add' : [],
    'delete' : [],
    'modify' : []
  }
  var p = service.getLocalChangeSet(containerUid).then(function(changes) {
    var getters = goog.array.map(goog.array.filter(changes, function(change) {
      return change.type == 'modified' || change.type == 'error';
    }), function(change) {
      return service.getItem(containerUid, change['itemId']).then(function(entry) {
        context.changes['modify'].push(this.adaptCreate(entry));
      }, null, this);
    }, this);

    goog.array.forEach(goog.array.filter(changes, function(change) {
      return change.type == 'deleted';
    }), function(change) {
      context.changes['delete'].push(this.adaptDelete(change));
    }, this);
    return goog.Promise.all(getters);
  }, null, this).then(function() {
    if (context.changes['modify'].length > 0 || context.changes['delete'].length > 0 //
        || context.changes['add'].length > 0) {
      return client.updates(context.changes);
    } else {
      return null;
    }
  }, null, this).then(function(resp) {
    if (resp != null) {
      var updated = goog.array.concat(resp['added'] || [], resp['updated']|| []);
      return service.clearChangelog(containerUid, updated, resp['removed'], resp['errors']);
    }
  }, null, this).then(function() {
    return containersService.getSyncVersion(containerUid);
  }, null, this).then(function(version) {
    return client.changeset(version);
  }, null, this).then(function(changeset) {
    context.version = changeset['version'];
    context.deleted = changeset['deleted'];
    var uids = goog.array.join(changeset['created'], changeset['updated']);

    var fetchBlock = function(next, toFetch, ret) {
      return next.then(function() {
        return client.retrieve(toFetch).then(function(v) {
          goog.array.insertArrayAt(ret, v, 0);
        });
      });
    };

    var ret = [];
    var next = goog.Promise.resolve();
    var start = 0;
    while (start < uids.length) {
      var end = start + 500;
      var toFetch = goog.array.slice(uids, start, end);
      next = fetchBlock(next, toFetch, ret);
      start = end;
    }

    return next.then(function() {
      return ret;
    });

  }, null, this).then(function(results) {
    var changes = goog.array.map(results, this.adaptItem, this);
    return service.syncItems(containerUid, changes, context.deleted, [], context.version);
  }, null, this).then(function() {
    return containersService.setSyncVersion(containerUid, context.version)
  }, null, this).then(function() {
    return this.ctx.service('folders').refreshFolder(containerUid);
  }, null, this);

  return goog.async.Deferred.fromPromise(p);
};
