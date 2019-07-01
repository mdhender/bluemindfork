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
 * @fileoverview Interface for container model management.
 * 
 */

goog.provide('net.bluemind.container.persistance.IContainerHome');

goog.require('goog.async.Deferred');
/**
 * Interface for the different container storage backend.
 * 
 * @interface
 */
net.bluemind.container.persistance.IContainerHome = function() {
};

/**
 * Get all containers shared with the current user.
 * 
 * @param {Array.<string>=} opt_uids Array of container uids.
 * @return {goog.async.Deferred} Deferred object containing container list.
 */
net.bluemind.container.persistance.IContainerHome.prototype.getContainers = function(opt_uids) {
};

/**
 * Get container detail by id.
 * 
 * @param {string} id
 * @return {goog.async.Deferred} Deferred object containing a container or null
 */
net.bluemind.container.persistance.IContainerHome.prototype.getContainer = function(id) {
};

/**
 * Store containers
 * 
 * @param {string} type Container type.
 * @param {Array.<Object>} containers Containers to store.
 * @return {goog.async.Deferred}
 */
net.bluemind.container.persistance.IContainerHome.prototype.sync = function(type, containers) {
};

/**
 * Store container version
 * 
 * @param {string} containerId
 * @param {number} version
 * @return {goog.async.Deferred}
 */
net.bluemind.container.persistance.IContainerHome.prototype.setSyncVersion = function(containerId, version) {
};

/**
 * get container version
 * 
 * @param {string} containerId
 * @return {goog.async.Deferred}
 */
net.bluemind.container.persistance.IContainerHome.prototype.getSyncVersion = function(containerId) {
};

/**
 * Containers store
 * 
 * @param {Array.<Object>} containers Containers to store.
 * @return {goog.async.Deferred}
 */
net.bluemind.container.persistance.IContainerHome.prototype.store = function(containers) {
};
