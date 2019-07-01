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

goog.provide('net.bluemind.tag.sync.UnitaryTagSync');

goog.require('net.bluemind.tag.sync.TagSyncClient');
goog.require('net.bluemind.container.sync.ContainerSync');
goog.require('net.bluemind.core.container.api.ContainersClient');
/**
 * Synchronize tags data with bm-core.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {net.bluemind.container.sync.UnitaryContainerSync}
 */
net.bluemind.tag.sync.UnitaryTagSync = function(ctx, containerUid) {
  goog.base(this, ctx, containerUid);
  this.logger = goog.log.getLogger('net.bluemind.tag.sync.UnitaryTagSync');
};
goog.inherits(net.bluemind.tag.sync.UnitaryTagSync,
   net.bluemind.container.sync.UnitaryContainerSync);

/** @override */
net.bluemind.tag.sync.UnitaryTagSync.prototype.getClient = function(uid) {
  return new net.bluemind.tag.sync.TagSyncClient(this.ctx, uid);
};

/** @override */
net.bluemind.tag.sync.UnitaryTagSync.prototype.getContainerService = function() {
  return this.ctx.service('tags').cs_;
};

/** @override */
net.bluemind.tag.sync.UnitaryTagSync.prototype.getContainersService = function() {
  return this.ctx.service('tags').css_;
};

net.bluemind.tag.sync.UnitaryTagSync.registerAll = function(ctx, syncEngine) {
  syncEngine.registerService(new net.bluemind.tag.sync.UnitaryTagSync(ctx,"tags_"+ctx.user['uid']));
  syncEngine.registerService(new net.bluemind.tag.sync.UnitaryTagSync(ctx,"tags_"+ctx.user['domainUid']));
}

/** @override */
net.bluemind.tag.sync.UnitaryTagSync.prototype.getName = function() {
  return 'Tags ('+this.containerUid+')';
};

