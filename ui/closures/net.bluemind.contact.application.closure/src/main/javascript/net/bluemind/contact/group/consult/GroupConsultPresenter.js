/*
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
goog.provide("net.bluemind.contact.group.consult.GroupConsultPresenter");

goog.require("net.bluemind.contact.group.consult.GroupConsultView");
goog.require("net.bluemind.contact.group.ui.MemberDetails.EventType");
goog.require("net.bluemind.contact.vcard.VCardConsultPresenter");

/**
 * @constructor
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends { net.bluemind.contact.vcard.VCardConsultPresenter}
 */
net.bluemind.contact.group.consult.GroupConsultPresenter = function(ctx) {
  var view = new net.bluemind.contact.group.consult.GroupConsultView()
  net.bluemind.contact.vcard.VCardConsultPresenter.call(this, ctx, view);
  this.handler.listen(view, net.bluemind.contact.group.ui.MemberDetails.EventType.GOTO, this.handleGoToMember_);
}
goog.inherits(net.bluemind.contact.group.consult.GroupConsultPresenter,
    net.bluemind.contact.vcard.VCardConsultPresenter);

/**
 * Go to a contact
 * 
 * @param {goog.event.Event} event
 */
net.bluemind.contact.group.consult.GroupConsultPresenter.prototype.handleGoToMember_ = function(event) {
  var model = event.model;
  this.ctx.helper('url').goTo('/vcard/?uid=' + model.id + '&container=' + model.container);
};
