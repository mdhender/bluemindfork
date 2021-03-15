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

goog.provide("net.bluemind.videoconferencing.service.VideoConferencingUidsService");

goog.require("net.bluemind.videoconferencing.api.VideoConferenceUidsClient");
goog.require("net.bluemind.container.service.ContainerService");
goog.require("net.bluemind.mvp.helper.ServiceHelper");

net.bluemind.videoconferencing.service.VideoConferencingUidsService = function (ctx) {
    goog.base(this);
    this.ctx_ = ctx;   
};
goog.inherits(net.bluemind.videoconferencing.service.VideoConferencingUidsService,
    goog.events.EventTarget);

net.bluemind.videoconferencing.service.VideoConferencingUidsService.prototype.getResourceTypeUid = function(evt) {
    var client = new net.bluemind.videoconferencing.api.VideoConferenceUidsClient(
        this.ctx_.rpc, '', '');
    return client.getResourceTypeUid();
};

net.bluemind.videoconferencing.service.VideoConferencingUidsService.prototype.getProviderTypeUid = function(evt) {
    var client = new net.bluemind.videoconferencing.api.VideoConferenceUidsClient(
        this.ctx_.rpc, '', '');
    return client.getProviderTypeUid();
};
