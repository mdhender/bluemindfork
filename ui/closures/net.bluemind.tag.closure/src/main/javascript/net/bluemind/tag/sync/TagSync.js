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
 * @fileoverview Contact synchronization service.
 */

goog.provide('net.bluemind.tag.sync.TagSync');

goog.require('net.bluemind.tag.sync.TagSyncClient');
goog.require('net.bluemind.container.sync.ContainerSync');
goog.require('net.bluemind.core.container.api.ContainersClient');
/**
 * Synchronize tags data with bm-core.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {net.bluemind.container.sync.ContainerSync}
 */
net.bluemind.tag.sync.TagSync = function(ctx) {
  goog.base(this, ctx);
  this.logger = goog.log.getLogger('net.bluemind.tag.sync.TagSync');
  this.type = 'tag';
};
goog.inherits(net.bluemind.tag.sync.TagSync,
  net.bluemind.container.sync.ContainerSync);

/** @override */
net.bluemind.tag.sync.TagSync.prototype.getClient = function(uid) {
  return new net.bluemind.tag.sync.TagSyncClient(this.ctx, uid);
};

/** @override */
net.bluemind.tag.sync.TagSync.prototype.getContainerService = function() {
  return this.ctx.service('tags').cs_;
};

/** @override */
net.bluemind.tag.sync.TagSync.prototype.getContainersService = function() {
  return this.ctx.service('tags').css_;
};
/** @override */
net.bluemind.tag.sync.TagSync.prototype.containersToSyncList = function() {
  if( this.ctx.online) {
    var client = new net.bluemind.core.container.api.ContainersClient(this.ctx.rpc, '');
    var ret= client.all({'type':'tags', 'verb':['Read']}).then(function(containers) {
     // no need to wait completion
      this.ctx.service('tags').css_.sync('todolist',containers);
      return containers;
    }, null, this);
    return goog.async.Deferred.fromPromise(ret);
  } else {
    return goog.base(this, 'containersToSyncList');
  }
}
/** @override */
net.bluemind.tag.sync.TagSync.prototype.getName = function() {
  return 'Tag';
};

