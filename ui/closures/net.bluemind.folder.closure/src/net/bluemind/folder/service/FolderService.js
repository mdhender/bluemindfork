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
goog.provide("net.bluemind.folder.service.FolderService");

goog.require("goog.Promise");
goog.require("net.bluemind.container.service.ContainerService");
goog.require("net.bluemind.directory.api.DirectoryClient");
/**
 * Service provdier object for Tasks
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {net.bluemind.container.service.ContainerService}
 */
net.bluemind.folder.service.FolderService = function(ctx) {
  goog.base(this, ctx);
}
goog.inherits(net.bluemind.folder.service.FolderService, net.bluemind.container.service.ContainerService);

/**
 * @override
 */
net.bluemind.folder.service.FolderService.prototype.syncItems = function(containerId, changed, deleted, version) {
  var tmp = null;
  return goog.base(this, 'syncItems', containerId, changed, deleted, version).then(function() {
    return this.getItems(containerId);
  }, null, this).then(function(folders) {
    var containerUids = goog.array.filter(goog.array.map(folders, function(c) {
      return c['folderContainer'];
    }), goog.isDefAndNotNull);
    var containersClient = new net.bluemind.core.container.api.ContainersClient(this.ctx.rpc, '');
    return containersClient.getContainers(containerUids);

  }, null, this).then(function(containers) {
    var dir = new net.bluemind.directory.api.DirectoryClient(this.ctx.rpc, '', this.ctx.user['domainUid']);
    var futures = goog.array.map(containers, function(c) {
      return dir.findByEntryUid(c['owner']).then(function(entry) {
        if (entry) {
          c['dir'] = entry;
        }
        return c;
      });
    });

    return goog.Promise.all(futures).then(function(containers) {
      return this.ctx.service('containers').sync('addressbook', containers);
    }, null, this);

  }, null, this);
};
