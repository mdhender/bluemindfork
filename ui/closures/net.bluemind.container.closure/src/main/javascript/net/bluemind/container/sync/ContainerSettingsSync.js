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

goog.provide('net.bluemind.container.sync.ContainerSettingsSync');

goog.require('net.bluemind.sync.SyncService');
goog.require('net.bluemind.container.service.ContainersService');
goog.require('net.bluemind.container.service.ContainerService');

goog.require('bluemind.storage.StorageHelper');
goog.require('bluemind.string');
goog.require('goog.log');
goog.require('goog.log.Logger');
goog.require('goog.async.DeferredList');
goog.require('net.bluemind.core.container.api.ContainerManagementClient');
/**
 * Abstract class for synchronisation services based on container model.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {net.bluemind.sync.SyncService}
 */
net.bluemind.container.sync.ContainerSettingsSync = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
};
goog.inherits(net.bluemind.container.sync.ContainerSettingsSync, net.bluemind.sync.SyncService);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @protected
 */
net.bluemind.container.sync.ContainerSettingsSync.prototype.ctx;

/** @override */
net.bluemind.container.sync.ContainerSettingsSync.prototype.getName = function() {
  return 'Settings';
};

/** @override */
net.bluemind.container.sync.ContainerSettingsSync.prototype.isEnabled = function() {
  return this.ctx.service('calendars').isLocal();
}

/** @override */
net.bluemind.container.sync.ContainerSettingsSync.prototype.syncInternal = function() {
  goog.log.info(this.logger, 'Synchronize containers settings with blue-mind core');
  var storage = bluemind.storage.StorageHelper.getWebStorage();

  var ret = this.ctx.service('calendars').css_.getSettingsChanges() //
  .then(
      function(changes) {
        var futures = goog.array.map(changes, function(containerChange) {
          var cMgmt = new net.bluemind.core.container.api.ContainerManagementClient(this.ctx_.rpc, '',
              containerChange['uid']);
          return cMgmt.setPersonalSettings(containerChange['value']).then(function() {
            return this.ctx.service('containers').settingsApplied(containerChange['uid']);
          }, null, this);
        }, this);
        return goog.async.DeferredList.gatherResults(futures);
      }, null, this);
  return goog.async.Deferred.fromPromise(ret);
};
