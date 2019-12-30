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
goog.require("net.bluemind.deferredaction.api.DeferredActionClient");
goog.require("net.bluemind.mvp.helper.ServiceHelper");
goog.require("goog.array");

var CACHE_EXPIRATION = 9 * 60 * 1000;
/**
 * @constructor
 * @extends {goog.events.EventTarget}
 */
net.bluemind.deferredaction.service.DeferredActionService = function(ctx) {
    goog.base(this);
    this.ctx = ctx;
    this.cache_ = { ts: 0, values: [] };
    this.cs_ = new net.bluemind.container.service.ContainerService(ctx, "deferredaction");
    this.css_ = new net.bluemind.container.service.ContainersService(ctx, "deferredaction");
};

goog.inherits(net.bluemind.deferredaction.service.DeferredActionService, goog.events.EventTarget);

net.bluemind.deferredaction.service.DeferredActionService.prototype.isLocal = function() {
    return this.cs_.available();
};

/**
 * Execute the right method depending on application state.
 *
 * @param {Object.<string, Function>} states
 * @param {Array.<*>} params Array of function parameters
 * @return {!goog.Promise<*>}
 */
net.bluemind.deferredaction.service.DeferredActionService.prototype.handleByState = function(states, params) {
    var localState = [];
    if (this.cs_.available()) {
        localState.push("local");
    }
    if (this.ctx.online) {
        localState.push("remote");
    }
    try {
        return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params, localState);
    } catch (e) {
        return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params, ["remote"]);
    }
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.getItemsByDate = function(executionDate) {
    return this.handleByState(
        {
            "local": this.getItemsByDateLocal,
            "remote": this.getItemsByDateRemote
        },
        [executionDate]
    );
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.getItemsByDateRemote = function(executionDate) {
    var promise;
    if (this.cache_.ts < executionDate) {
        var container = "deferredaction-" + this.ctx.user["uid"];
        var client = new net.bluemind.deferredaction.api.DeferredActionClient(this.ctx.rpc, "", container);
        promise = client.getByActionId("EVENT", executionDate + CACHE_EXPIRATION).then(
            function(items) {
                this.cache_.ts = executionDate + CACHE_EXPIRATION;
                this.cache_.values = items;
                return items;
            },
            null,
            this
        );
    } else {
        promise = goog.Promise.resolve(this.cache_.values);
    }
    return promise.then(function(items) {
        return goog.array.filter(items, function(item) {
            return item["value"]["executionDate"] < executionDate;
        });
    });
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.getItemsByDateLocal = function(executionDate) {
    var query = [["value.executionDate", "<", executionDate]];
    return this.css_.searchItems(query).then(function(items) {
        return items.filter(function(item) {
            return item && item["value"]["actionId"] === "EVENT";
        });
    });
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.getItem = function(id) {
    return this.handleByState(
        {
            "local": this.getItemLocal,
            "remote": function() {
                return goog.Promise.resolve(null);
            }
        },
        [id]
    );
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.getItemLocal = function(id) {
    var containerId = "deferredaction-" + this.ctx.user["uid"];
    return this.cs_.getItem(containerId, id);
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.deleteItem = function(item) {
    return this.handleByState(
        {
            "local": this.deleteItemLocal,
            "remote": this.deleteItemRemote
        },
        [item]
    );
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.deleteItemLocal = function(item) {
    var containerId = "deferredaction-" + this.ctx.user["uid"];
    return this.cs_.deleteItem(containerId, item["uid"]);
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.deleteItemRemote = function(item) {
    var container = "deferredaction-" + this.ctx.user["uid"];
    var client = new net.bluemind.deferredaction.api.DeferredActionClient(this.ctx.rpc, "", container);
    goog.array.removeIf(this.cache_.values, function(value) {
        return value["uid"] === item["uid"];
    });
    return client.delete_(item["uid"]);
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.createItem = function(item) {
    return this.handleByState(
        {
            "local": this.createItemLocal,
            "remote": this.createItemRemote
        },
        [item]
    );
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.createItemLocal = function(item) {
    item["container"] = "deferredaction-" + this.ctx.user["uid"];
    return this.cs_.storeItem(item);
};

net.bluemind.deferredaction.service.DeferredActionService.prototype.createItemRemote = function(item) {
    var container = "deferredaction-" + this.ctx.user["uid"];
    var client = new net.bluemind.deferredaction.api.DeferredActionClient(this.ctx.rpc, "", container);
    return client.create(item["uid"], item["value"]).then(
        function() {},
        function() {}
    );
};
