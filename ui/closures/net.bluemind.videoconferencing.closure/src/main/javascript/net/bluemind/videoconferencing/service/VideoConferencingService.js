/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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

goog.provide("net.bluemind.videoconferencing.service.VideoConferencingService");

goog.require("net.bluemind.videoconferencing.api.VideoConferencingClient");
goog.require("net.bluemind.container.service.ContainerService");
goog.require("net.bluemind.mvp.helper.ServiceHelper");
goog.require("net.bluemind.authentication.api.UserAccessTokenClient")

net.bluemind.videoconferencing.service.VideoConferencingService = function (ctx) {
    goog.base(this);
    this.ctx_ = ctx;
    this.cs_ = new net.bluemind.container.service.ContainerService(ctx, 'calendar');
   
};
goog.inherits(net.bluemind.videoconferencing.service.VideoConferencingService,
    goog.events.EventTarget);

net.bluemind.videoconferencing.service.VideoConferencingService.prototype.resources_ = [];

net.bluemind.videoconferencing.service.VideoConferencingService.prototype.setVideoConferencingResources = function(resources) {
    goog.array.sort(resources, function(a, b) {
      if (a.displayName < b.displayName) {
        return -1;
      }
      if (a.displayName > b.displayName) {
        return 1;
      }
      return 0;
    });

    this.resources_ = resources;
};

net.bluemind.videoconferencing.service.VideoConferencingService.prototype.getVideoConferencingResources = function() {
    return this.resources_;
};

net.bluemind.videoconferencing.service.VideoConferencingService.prototype.add = function(evt) {
    var client = new net.bluemind.videoconferencing.api.VideoConferencingClient(
        this.ctx_.rpc, '', this.ctx_.user['domainUid']);
    return client.add(evt);
};

net.bluemind.videoconferencing.service.VideoConferencingService.prototype.remove = function(evt) {
    var client = new net.bluemind.videoconferencing.api.VideoConferencingClient(
        this.ctx_.rpc, '', this.ctx_.user['domainUid']);
    return client.remove(evt);
};

net.bluemind.videoconferencing.service.VideoConferencingService.prototype.auth = function(ressourceUid) {
    return this.ctx_.service('resources').get(ressourceUid).then(function (resource) {
        var systemProp = goog.array.find(resource['properties'], function (elem, index, arr){
            return elem['propertyId'] = 'bm-videoconferencing-type';
        }, this);
        var system = systemProp.value;
        var client = new net.bluemind.authentication.api.UserAccessTokenClient(this.ctx_.rpc, '');
        return client.getTokenInfo(system);
    }, null, this);
    
};

