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

goog.provide('net.bluemind.container.sync.ContainerSync');

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
 * @constructor
 * @extends {net.bluemind.sync.SyncService}
 */
net.bluemind.container.sync.ContainerSync = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
};
goog.inherits(net.bluemind.container.sync.ContainerSync, net.bluemind.sync.SyncService);

/**
 * Container type
 * 
 * @type {string}
 * @protected
 */
net.bluemind.container.sync.ContainerSync.prototype.type;

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @protected
 */
net.bluemind.container.sync.ContainerSync.prototype.ctx;

/** @override */
net.bluemind.container.sync.ContainerSync.prototype.syncInternal = function() {
  goog.log.info(this.logger, 'Synchronize ' + this.getName() + ' with blue-mind core');
  var storage = bluemind.storage.StorageHelper.getWebStorage();

  var service = this.getContainersService();
  if (service.available()) {
    return this.containersToSyncList().addCallback(function(objects) {
      var containers = goog.array.map(objects, this.adaptContainer, this);
      var results = goog.array.map(containers, this.syncContainer, this);
      return goog.async.DeferredList.gatherResults(results).addCallback(function() {
        storage.set(this.type + '-sync', goog.now());
      }, this);
    }, this);
  } else {
    return goog.Promise.resolve(null);
  }
};

/** @override */
net.bluemind.container.sync.ContainerSync.prototype.isEnabled = function() {
  return this.getContainersService().available();
};

/**
 * 
 * @return {goog.Promise}
 * @protected
 */
net.bluemind.container.sync.ContainerSync.prototype.containersToSyncList = function() {
  return this.getContainersService().list(this.type);
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
net.bluemind.container.sync.ContainerSync.prototype.adaptContainer = function(container) {
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
net.bluemind.container.sync.ContainerSync.prototype.adaptItem = function(item) {
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
net.bluemind.container.sync.ContainerSync.prototype.getContainerService = function() {
  return this.ctx.service('container');
};

/**
 * Get Containers service.
 * 
 * @returns {net.bluemind.container.service.ContainerService|net.bluemind.container.service.ContainersService}
 * @protected
 */
net.bluemind.container.sync.ContainerSync.prototype.getContainersService = function() {
  return this.ctx.service('containers');
};

/**
 * Get current item client api
 * 
 * @param {string} uid Container uid
 * @return {net.bluemind.container.sync.ContainerSyncClient} Container api.
 */
net.bluemind.container.sync.ContainerSync.prototype.getClient = goog.abstractMethod;

/**
 * Synchronise a container
 * 
 * @param {Object} container Container to synchronise
 * @returns {goog.async.Deferred}
 * @protected
 */
net.bluemind.container.sync.ContainerSync.prototype.syncContainer = function(container) {
  var containerUid = container['uid'];
  var service = this.getContainerService();
  var containersService = this.getContainersService();

  var client = this.getClient(containerUid);
  var context = {};
  context.changes = {
    'add' : [],
    'delete' : [],
    'modify' : []
  }
  var p = service.getLocalChangeSet(containerUid).then(function(changes) {
    var getters = goog.array.map(goog.array.filter(changes, function(change) {
      return change.type == 'modified';
    }), function(change) {
      return service.getItem(containerUid, change['itemId']).then(function(entry) {
        context.changes['modify'].push({
          'uid' : entry['uid'],
          'value' : entry['value']
        });
      }, null, this);
    }, this);

    goog.array.forEach(goog.array.filter(changes, function(change) {
      return change.type == 'deleted';
    }), function(change) {
      context.changes['delete'].push({
        'uid' : change['itemId']

      });
    });
    return goog.Promise.all(getters);
  }, null, this).then(function() {
      if (context.changes['modify'].length > 0 || context.changes['delete'].length > 0
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
    context.changed = goog.array.clone(uids);

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
  }, null, this);

  return goog.async.Deferred.fromPromise(p);
};
