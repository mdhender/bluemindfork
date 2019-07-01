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
 * @fileoverview Provide services for tasks
 */
goog.provide("net.bluemind.folder.service.FolderSyncClient");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.string");
goog.require("goog.async.DeferredList");
goog.require("net.bluemind.container.sync.ContainerSyncClient");
goog.require("net.bluemind.core.container.api.ContainerManagementClient");
goog.require("net.bluemind.core.container.api.ContainersClient");
goog.require("net.bluemind.directory.api.DirectoryClient");
goog.require("net.bluemind.core.container.api.OwnerSubscriptionsClient");

/**
 * Service provdier object for Tasks
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 */
net.bluemind.folder.service.FolderSyncClient = function(ctx) {
  goog.base(this, ctx.rpc, '');
  this.ctx_ = ctx;
  this.hierarchyClient = new net.bluemind.core.container.api.OwnerSubscriptionsClient(this.ctx_.rpc, '', 
    this.ctx_.user['domainUid'], this.ctx_.user['uid']);

  this.containersClient = new net.bluemind.core.container.api.ContainersClient(ctx.rpc, '');
};
goog.inherits(net.bluemind.folder.service.FolderSyncClient, net.bluemind.container.sync.ContainerSyncClient);

net.bluemind.folder.service.FolderSyncClient.prototype.changeset = function(version) {
  return this.hierarchyClient.changeset(version);
}

net.bluemind.folder.service.FolderSyncClient.prototype.updates = function(updates) {
  return goog.async.Deferred.succeed();
};

net.bluemind.folder.service.FolderSyncClient.prototype.retrieve = function(uids) {
  var folders = [];
  var containersClient = new net.bluemind.core.container.api.ContainersClient(this.ctx_.rpc, '');
  var directoryClient = new net.bluemind.directory.api.DirectoryClient(this.ctx_.rpc, '', this.ctx_.user['domainUid']);
  var acceptedTypes = ['calendar', 'addressbook', 'todolist'];
  return this.hierarchyClient.getMultiple(uids).then(function(subscriptions) {
    folders = goog.array.filter(subscriptions, function(subscription) {
      return goog.array.contains(acceptedTypes, subscription['value']['containerType']);
    });
    var uids = goog.array.map(folders, function(folder) {
      return folder['value']['containerUid'];
    });
    return this.ctx_.service('folders').getFoldersRemote(acceptedTypes, uids);
  }, null, this).then(function(containers) {
    return goog.array.map(folders, function(folder) {
      folder['containerDescriptor'] = goog.array.find(containers, function(container) {
        return folder['value']['containerUid'] == container['uid'];
      })
      return folder;
    });
    
  });
};
