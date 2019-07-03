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
goog.provide('net.bluemind.addressbook.sync.AddressBookSyncClient');

goog.require("net.bluemind.addressbook.api.AddressBookClient");
goog.require("net.bluemind.container.sync.ContainerSyncClient");


net.bluemind.addressbook.sync.AddressBookSyncClient = function(ctx, mailboxUid) {
  this.abClient = new net.bluemind.addressbook.api.AddressBookClient(ctx.rpc, '', mailboxUid);
};
goog.inherits(net.bluemind.addressbook.sync.AddressBookSyncClient, net.bluemind.container.sync.ContainerSyncClient);

net.bluemind.addressbook.sync.AddressBookSyncClient.prototype.changeset = function(version) {
  return this.abClient.changeset(version);
};

net.bluemind.addressbook.sync.AddressBookSyncClient.prototype.updates = function(updates) {
  return this.abClient.updates(updates);
};

net.bluemind.addressbook.sync.AddressBookSyncClient.prototype.retrieve = function(uids) {
  return this.abClient.multipleGet(uids);
};
