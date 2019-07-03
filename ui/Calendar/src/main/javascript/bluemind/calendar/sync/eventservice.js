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
 * @fileoverview Manage event storage on a bluemind server.
 */

goog.provide('bluemind.calendar.sync.EventService');

goog.require('bluemind.calendar.model.EventHome');
goog.require('net.bluemind.sync.SyncService');
goog.require('goog.debug.Logger');

/**
 * Synchronize event data with bm-core.
 * @param {relief.handlers.CommonServiceProvider} sp service provider.
 * @constructor
 * @extends {net.bluemind.sync.SyncService}
 */
bluemind.calendar.sync.EventService = function(sp) {
  goog.base(this);
  this.sp_ = sp;
  this.logger =
  goog.debug.Logger.getLogger('bluemind.calendar.sync.EventService')
};
goog.inherits(bluemind.calendar.sync.EventService, net.bluemind.sync.SyncService);

/**
 * The app's service provider.
 * @type {relief.handlers.CommonServiceProvider}
 * @private
 **/
bluemind.calendar.sync.EventService.prototype.sp_;

/** @override */
bluemind.calendar.sync.EventService.prototype.getName = function() {
  return 'Event';
};

/** @override */
bluemind.calendar.sync.EventService.prototype.syncInternal = function(monitor) {
  return bluemind.calendar.model.EventHome.getInstance().doSync(this.sp_, monitor).addCallback(function(result) {
    bluemind.pendingNotification.update();
  });
};

