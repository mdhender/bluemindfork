/* BEGIN LICENSE
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
 */

goog.provide("net.bluemind.resource.service.ResourcesService");
goog.require("net.bluemind.resource.api.ResourcesClient");
goog.require("net.bluemind.i18n.AlphabetIndexSymbols");
goog.require("net.bluemind.string");
goog.require("net.bluemind.container.service.ContainerService");
goog.require("net.bluemind.mvp.helper.ServiceHelper");

/**
 * @constructor
 * 
 * @param {net.bluemind.mvp.ApplicationContext}
 *            ctx
 */
net.bluemind.resource.service.ResourcesService = function (ctx) {
    goog.base(this);
    this.ctx = ctx;
    this.cs_ = new net.bluemind.container.service.ContainerService(ctx,
        'resources');
    this.handler_ = new goog.events.EventHandler(this);

    this.handler_.listen(this.cs_,
        net.bluemind.container.service.ContainerService.EventType.CHANGE,
        function (e) {
            this.dispatchEvent(e);
        });
};
goog.inherits(net.bluemind.resource.service.ResourcesService,
    goog.events.EventTarget);

net.bluemind.resource.service.ResourcesService.prototype.isLocal = function () {
    return this.cs_.available();
};

net.bluemind.resource.service.ResourcesService.prototype.handleByState = function (
    states, containerId, params) {
    return this.ctx.service('folders').getFolder(containerId).then(
        function (folder) {
            var localState = [];
            if (this.cs_.available() && folder && folder['offlineSync']) {
                localState.push('local');
            }
            if (this.ctx.online) {
                localState.push('remote');
            }
            return net.bluemind.mvp.helper.ServiceHelper.handleByState(
                this.ctx, this, states, params, localState);
        }, null, this);
};

/**
 * Request the computing of a resource template if any and add it to the event
 * description if not already done.
 * 
 * @return the resource's template - if any - appended to eventDescription
 */
net.bluemind.resource.service.ResourcesService.prototype.addToEventDescription = function (
    domainUid, resourceUid, organizerUid, eventDescription) {
    return this.handleByState({
        'local,remote': function () {
            // nothing
        },
        'local': function () {
            // nothing
        },
        'remote': function (domainUid, resourceUid, organizer) {
            var client = new net.bluemind.resource.api.ResourcesClient(
                this.ctx.rpc, '', domainUid);
            return client.addToEventDescription(resourceUid, {
                "organizerUid": organizerUid,
                "description": eventDescription
            });
        }

    }, domainUid, [domainUid, resourceUid, organizerUid, eventDescription]);
};

/**
 * Request the removing of a resource template from the event description if
 * present.
 * 
 * @return eventDescription without the resource's template - if any
 */
net.bluemind.resource.service.ResourcesService.prototype.removeFromEventDescription = function (
    domainUid, resourceUid, eventDescription) {
    return this.handleByState({
        'local,remote': function () {
            // nothing
        },
        'local': function () {
            // nothing
        },
        'remote': function (domainUid, resourceUid, organizer) {
            var client = new net.bluemind.resource.api.ResourcesClient(
                this.ctx.rpc, '', domainUid);
            return client.removeFromEventDescription(resourceUid, {
                "description": eventDescription
            });
        }

    }, domainUid, [domainUid, resourceUid, eventDescription]);
};

net.bluemind.resource.service.ResourcesService.prototype.byType = function (
    resourceType) {
    var domainUid = this.ctx.user["domainUid"];
    return this.handleByState({
        'remote': this.byTypeRemote
    }, domainUid, [resourceType]);
};

net.bluemind.resource.service.ResourcesService.prototype.byTypeRemote = function(resourceType) {
  var client = new net.bluemind.resource.api.ResourcesClient(this.ctx.rpc, '', this.ctx.user["domainUid"]);
  return client.byType(resourceType);
};
