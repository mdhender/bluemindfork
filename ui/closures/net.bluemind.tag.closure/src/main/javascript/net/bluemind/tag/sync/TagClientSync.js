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
goog.provide('net.bluemind.tag.sync.TagSyncClient');

goog.require('net.bluemind.tag.api.TagsClient');
goog.require("net.bluemind.container.sync.ContainerSyncClient");


net.bluemind.tag.sync.TagSyncClient = function(ctx, mailboxUid) {
  this.tagClient = new net.bluemind.tag.api.TagsClient(ctx.rpc, '', mailboxUid);
};
goog.inherits(net.bluemind.tag.sync.TagSyncClient, net.bluemind.container.sync.ContainerSyncClient);

net.bluemind.tag.sync.TagSyncClient.prototype.changeset = function(version) {
  return this.tagClient.changeset(version);
};

net.bluemind.tag.sync.TagSyncClient.prototype.updates = function(updates) {
  return this.tagClient.updates(updates);
};

net.bluemind.tag.sync.TagSyncClient.prototype.retrieve = function(uids) {
  return this.tagClient.multipleGet(uids);
};