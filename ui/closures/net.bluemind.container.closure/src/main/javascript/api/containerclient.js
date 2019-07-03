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
 * @fileoverview Container abstract class contains sync methods
 */

goog.provide("net.bluemind.container.api.ContainerClient");

goog.require("relief.rpc.Command");// FIXME - unresolved required symbol
/**
 * Container client abstract class.
 * 
 * @param {relief.rpc.RPCService} rpc RPC Service
 * @param {string} base RPC base path
 * @param {string} containerUid Container uid.
 * @extends {net.bluemind.api.BlueMindClient}
 * @constructor
 */
net.bluemind.container.api.ContainerClient = function(rpc, base, containerUid) {
  goog.base(this, rpc, base);
  this.containerUid_ = containerUid;
};

goog.inherits(net.bluemind.container.api.ContainerClient,
    net.bluemind.api.BlueMindClient);
/**
 * RPC service
 * @type {relief.rpc.RPCService} rpc
 * @protected
 */
net.bluemind.container.api.ContainerClient.prototype.rpc;

/**
 * Container api base uri
 * @type {string} base
 * @protected
 */
net.bluemind.container.api.ContainerClient.prototype.base;

/**
 * Container uid
 * @type {string}
 * @protected
 */
net.bluemind.container.api.ContainerClient.prototype.containerUid;

/**
 * Fetch an item from its unique uid
 * 
 * @param {string} uid uid of the entry
 * @return {goog.async.Deferred}
 */
net.bluemind.container.api.ContainerClient.prototype.getComplete = goog.abstractMethod;

/**
 * ChangeLog of the container since a given version.
 * Changelog contains extended description of each change entry
 * 
 * @param {number} since timestamp of first changes we want to retrieve
 * @return {goog.async.Deferred}
 */
net.bluemind.container.api.ContainerClient.prototype.changelog = function(since) {
  var url = this.base_ + "/todolists/" + this.containerUid_ + "/_changelog";
  if (since != null) {
    url += "?since=" + since;
  }
  var cmd = new relief.rpc.Command(null, null, "CHANGELOG_"
      + this.containerUid_, url, "GET");
  return this.execute(cmd);
};

/**
 * ChangeSet of the container since a given version.
 * ChangeSet only contains a set of uid for created / modified / deleted entries.
 * 
 * @param {number} since timestamp of first changes we want to retrieve.
 * @return {goog.async.Deferred}
 */
net.bluemind.container.api.ContainerClient.prototype.changeset  = goog.abstractMethod;

/**
 * Updates multiples entries at once (should be transactional: if one operation
 * fail, nothing is written)
 * 
 * @param {{add: Array, delete: Array, modify: Array}} changes Add/removed/modified
 *        elements.
 * @return {goog.async.Deferred}
 */
net.bluemind.container.api.ContainerClient.prototype.updates = goog.abstractMethod;

/**
 * Send local updates to bm-core and retrieve remote changes since last sync.
 * 
 * @param {number} since timestamp of first changes we want to retrieve.
 * @param {{add: Array, delete: Array, modify: Array}} changes Add/removed/modified
 *        elements.
 * @return ContainerChangeset
 */
net.bluemind.container.api.ContainerClient.prototype.sync = goog.abstractMethod;

