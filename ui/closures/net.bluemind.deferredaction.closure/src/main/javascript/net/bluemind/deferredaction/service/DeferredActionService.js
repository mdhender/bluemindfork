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

goog.provide("net.bluemind.deferredaction.service.DeferredActionService");

goog.require("net.bluemind.container.service.ContainerService");
goog.require("net.bluemind.mvp.helper.ServiceHelper");

/**
 * @constructor
 * @extends {goog.events.EventTarget}
 */
net.bluemind.deferredaction.service.DeferredActionService = function(ctx) {
    goog.base(this);
    this.ctx = ctx;

    this.cs_ = new net.bluemind.container.service.ContainerService(ctx, "deferredaction");
    this.css_ = new net.bluemind.container.service.ContainersService(ctx, "deferredaction");
};

goog.inherits(net.bluemind.deferredaction.service.DeferredActionService, goog.events.EventTarget);

net.bluemind.deferredaction.service.DeferredActionService.prototype.isLocal = function() {
    return this.cs_.available();
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.handleByState = function(states, params) {
    return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params);
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.getItems = function(executionDate) {
    var containerId = "deferredaction-" + this.ctx.user["uid"];
    return this.handleByState(
        {
            local: this.getItemsLocal,
            remote: this.getItemsRemote
        },
        [containerId, executionDate]
    );
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.getItemsLocal = function(
    containerId,
    executionDate
) {
    return this.cs_.getItems(containerId).then(function(items) {
        return items.filter(function(item) {
            return item["value"]["executionDate"] <= executionDate;
        });
    });
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.getItemsRemote = function(
    containerId,
    executionDate
) {
    var client = new net.bluemind.deferredaction.api.DeferredActionClient(this.ctx.rpc, "", containerId);
    return client.getByActionId("EVENT", executionDate).then(normalize(containerId));
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.deleteItem = function(item) {
    var containerId = "deferredaction-" + this.ctx.user["uid"];
    return this.handleByState(
        {
            local: this.deleteItemLocal,
            remote: this.deleteItemRemote
        },
        [containerId, item]
    );
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.deleteItemLocal = function(containerId, item) {
    this.cs_.deleteItem(containerId, item["uid"]);
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.deleteItemRemote = function(containerId, item) {
    var client = new net.bluemind.deferredaction.api.DeferredActionClient(this.ctx.rpc, "", containerId);
    client.delete_(item["uid"]);
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.createItem = function(item) {
    var containerId = "deferredaction-" + this.ctx.user["uid"];
    return this.handleByState(
        {
            local: this.createItemLocal,
            remote: this.createItemRemote
        },
        [containerId, item]
    );
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.createItemLocal = function(containerId, item) {
    item["container"] = containerId;
    return this.cs_.storeItem(item);
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.createItemRemote = function(containerId, item) {
    var value = item.value;
    var client = new net.bluemind.deferredaction.api.DeferredActionClient(this.ctx.rpc, "", containerId);
    return client.create(item["uid"], value);
};

function normalize(containerId) {
    return function(items) {
        return items
            .filter(function(item) {
                return item["value"] !== null;
            })
            .map(function(item) {
                item["name"] = item["displayName"];
                item["container"] = containerId;
                return item;
            });
    };
}
