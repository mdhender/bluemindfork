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
 * @fileoverview TodoList synchrnisation service
 */
goog.provide("net.bluemind.folder.sync.FoldersSync");

goog.require("goog.log");
goog.require("net.bluemind.container.service.ContainerService");
goog.require("net.bluemind.container.sync.UnitaryContainerSync");
goog.require("net.bluemind.folder.service.FolderSyncClient");

/**
 * Synchronize folders data with bm-core.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {net.bluemind.container.sync.UnitaryContainerSync}
 */
net.bluemind.folder.sync.FoldersSync = function(ctx) {
  
  goog.base(this, ctx, 'owner_subscriptions_' + ctx.user['uid'] + '_at_' + ctx.user['domainUid']);
  this.logger = goog.log.getLogger('net.bluemind.folder.sync.FoldersSync');
  this.type = 'owner_subscriptions';
};
goog.inherits(net.bluemind.folder.sync.FoldersSync, net.bluemind.container.sync.UnitaryContainerSync);

/** @override */
net.bluemind.folder.sync.FoldersSync.prototype.getClient = function(uid) {
  return new net.bluemind.folder.service.FolderSyncClient(this.ctx);
};

/** @override */
net.bluemind.folder.sync.FoldersSync.prototype.getName = function() {
  return 'Folders';
};

/** @override */
net.bluemind.folder.sync.FoldersSync.prototype.getContainerService = function() {
  return this.ctx.service('folders').cs_;
};

net.bluemind.folder.sync.FoldersSync.prototype.getContainersService = function() {
  return this.ctx.service('folders').css_;
};

/** @override */
net.bluemind.folder.sync.FoldersSync.prototype.adaptItem = function(item) {
  item['name'] = item['containerDescriptor']['name'];
  item['order'] = item['name'];
  return item;
};

net.bluemind.folder.sync.FoldersSync.register = function(ctx, syncEngine) {
  var fs = new net.bluemind.folder.sync.FoldersSync(ctx);
  syncEngine.registerService(fs);

  var handler = new goog.events.EventHandler(fs);
  handler.listen(ctx.service('containersObserver'), net.bluemind.container.service.ContainersObserver.EventType.CHANGE,
      function(e) {
        goog.log.info(this.logger, "Container changed ", e);
        if (e.containerType == 'owner_subscriptions') {
          fs.needSync();
        }
      });

}
