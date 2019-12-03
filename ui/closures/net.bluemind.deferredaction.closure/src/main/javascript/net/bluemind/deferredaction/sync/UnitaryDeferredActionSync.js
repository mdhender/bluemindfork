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

goog.provide("net.bluemind.deferredaction.sync.UnitaryDeferredActionSync");

goog.require("net.bluemind.deferredaction.sync.DeferredActionClientSync");

/**
 * @constructor
 * @extends {net.bluemind.container.sync.UnitaryContainerSync}
 */
net.bluemind.deferredaction.sync.UnitaryDeferredActionSync = function(ctx, containerUid) {
    goog.base(this, ctx, containerUid);
    this.logger = goog.log.getLogger("net.bluemind.deferredaction.sync.UnitaryDeferredActionSync");
};

goog.inherits(
    net.bluemind.deferredaction.sync.UnitaryDeferredActionSync,
    net.bluemind.container.sync.UnitaryContainerSync
);

/** @override */
net.bluemind.deferredaction.sync.UnitaryDeferredActionSync.prototype.getClient = function(uid) {
    return new net.bluemind.deferredaction.sync.DeferredActionClientSync(this.ctx, uid);
};

/** @override */
net.bluemind.deferredaction.sync.UnitaryDeferredActionSync.prototype.getContainerService = function() {
    return this.ctx.service("deferredaction").cs_;
};

/** @override */
net.bluemind.deferredaction.sync.UnitaryDeferredActionSync.prototype.getContainersService = function() {
    return this.ctx.service("deferredaction").css_;
};

/** @override */
net.bluemind.deferredaction.sync.UnitaryDeferredActionSync.prototype.getName = function() {
    return "DeferredAction (" + this.containerUid + ")";
};
