/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
 *
 * @format
 */

goog.provide("net.bluemind.deferredaction.sync.DeferredActionClientSync");

goog.require("net.bluemind.deferredaction.api.DeferredActionClient");
goog.require("net.bluemind.container.sync.ContainerSyncClient");

/**
 * @constructor
 * @extends {net.bluemind.container.sync.ContainerSyncClient}
 */
net.bluemind.deferredaction.sync.DeferredActionClientSync = function(ctx, mailboxUid) {
    this.client = new net.bluemind.deferredaction.api.DeferredActionClient(ctx.rpc, "", mailboxUid);
};

goog.inherits(
    net.bluemind.deferredaction.sync.DeferredActionClientSync,
    net.bluemind.container.sync.ContainerSyncClient
);

/**
 * @return {goog.Promise|null|undefined}
 */
net.bluemind.deferredaction.sync.DeferredActionClientSync.prototype.changeset = function(version) {
    return /** @type {goog.Promise|null|undefined} */ (this.client.changeset(version));
};

/**
 * @return {goog.Promise|null|undefined}
 */
net.bluemind.deferredaction.sync.DeferredActionClientSync.prototype.updates = function(updates) {
    var result = { "updated": [], "removed": [] };
    updates["delete"].forEach(
        function(item) {
            result["removed"].push(item["uid"]);
            this.client.delete_(item["uid"]);
        }.bind(this)
    );
    updates["modify"].forEach(
        function(item) {
            result["updated"].push(item["uid"]);
            this.client.create(item["uid"], item["value"]);
        }.bind(this)
    );
    result["errors"] = [];
    return /** @type {goog.Promise|null|undefined} */ (goog.Promise.resolve(result));
};

/**
 * @return {goog.Promise|null|undefined}
 */
net.bluemind.deferredaction.sync.DeferredActionClientSync.prototype.retrieve = function(uids) {
    return /** @type {goog.Promise|null|undefined} */ (this.client.multipleGet(uids));
};
