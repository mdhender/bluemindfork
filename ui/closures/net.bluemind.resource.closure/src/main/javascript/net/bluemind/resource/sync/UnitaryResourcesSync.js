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

goog.provide("net.bluemind.resource.sync.UnitaryResourcesSync");

goog.require("goog.log");
goog.require("net.bluemind.resource.sync.ResourcesClientSync");
goog.require("net.bluemind.container.sync.UnitaryContainerSync");
goog.require("goog.async.Deferred");
/**
 * Synchronize resources data with bm-core.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {net.bluemind.container.sync.ContainerSync}
 */
net.bluemind.resource.sync.UnitaryResourcesSync = function(ctx, containerUid) {
  goog.base(this, ctx, containerUid);
  this.logger = goog.log.getLogger('net.bluemind.resource.sync.UnitaryResourcesSync');
};
goog.inherits(net.bluemind.resource.sync.UnitaryResourcesSync, net.bluemind.container.sync.UnitaryContainerSync);

/** @override */
net.bluemind.resource.sync.UnitaryResourcesSync.prototype.getClient = function(uid) {
  return new net.bluemind.resource.sync.ResourcesClientSync(this.ctx, uid);
};

/** @override */
net.bluemind.resource.sync.UnitaryResourcesSync.prototype.getName = function() {
  return 'Resources ('+this.containerUid+')';
};

/** @override */
net.bluemind.resource.sync.UnitaryResourcesSync.prototype.getContainerService = function() {
  return this.ctx.service('resources').cs_;
};

/** @override */
net.bluemind.resource.sync.UnitaryResourcesSync.prototype.getContainersService = function() {
  return this.ctx.service('resources').css_;
};

/** @override */
net.bluemind.resource.sync.UnitaryResourcesSync.prototype.adaptItem = function(item) {
	// FIXME
  var i = item;
  i['name'] = i['displayName'];
  if (!i['name']){
	  //prevent npe
	  i['name'] = "";
  }
  i['order'] = bluemind.string.normalize(i['name'])
  i['tags'] = goog.array.map(i['value']['explanatory']['categories'], function(t) {
    return t['itemUid'];
});
  
  
  return i;
};