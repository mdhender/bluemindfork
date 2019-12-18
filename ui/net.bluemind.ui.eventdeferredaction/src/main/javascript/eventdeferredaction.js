/** BEGIN LICENSE
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
goog.provide("net.bluemind.ui.eventdeferredaction.EventDeferredAction");

goog.require("net.bluemind.mvp.ApplicationContext");
goog.require("net.bluemind.mvp.Application");
goog.require("net.bluemind.sync.SyncEngine");
goog.require("net.bluemind.deferredaction.sync.UnitaryDeferredActionSync");
goog.require("net.bluemind.deferredaction.service.DeferredActionService");
goog.require("net.bluemind.deferredaction.persistence.schema");
goog.require("net.bluemind.ui.eventdeferredaction.DeferredActionScheduler");
goog.require("net.bluemind.container.sync.UnitaryContainerSync");

/**
 * Calendar application
 *
 * @constructor
 * @extends {net.bluemind.mvp.Application}
 */
net.bluemind.ui.eventdeferredaction.EventDeferredAction = function() {
    goog.base(this, "EventDeferredAction");
};
goog.inherits(net.bluemind.ui.eventdeferredaction.EventDeferredAction, net.bluemind.mvp.Application);

/** @override */
net.bluemind.ui.eventdeferredaction.EventDeferredAction.prototype.bootstrap = function(ctx) {
    return goog.base(this, "bootstrap", ctx);
};

/** @override */
net.bluemind.ui.eventdeferredaction.EventDeferredAction.prototype.postBootstrap = function(ctx) {
    goog.base(this, "postBootstrap", ctx);
    var sync = net.bluemind.sync.SyncEngine.getInstance();
    var deferredaction = new net.bluemind.deferredaction.sync.UnitaryDeferredActionSync(
        ctx,
        "deferredaction-" + ctx.user["uid"]
    );
    sync.registerService(deferredaction);
    new net.bluemind.ui.eventdeferredaction.DeferredActionScheduler(ctx);
};

/** @override */
net.bluemind.ui.eventdeferredaction.EventDeferredAction.prototype.registerServices = function(ctx) {
    ctx.service("deferredaction", net.bluemind.deferredaction.service.DeferredActionService);
};

/** @override */
net.bluemind.ui.eventdeferredaction.EventDeferredAction.prototype.getDbSchemas = function(ctx) {
    var root = goog.base(this, "getDbSchemas", ctx);
    return goog.array.concat(root, [
        {
            name: "deferredaction",
            schema: net.bluemind.deferredaction.persistence.schema
        }
    ]);
};

new net.bluemind.ui.eventdeferredaction.EventDeferredAction();
