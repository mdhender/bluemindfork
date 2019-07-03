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
goog.provide("net.bluemind.contact.individual.edit.IndividualEditPresenter");

goog.require("net.bluemind.contact.individual.edit.IndividualEditView");
goog.require("net.bluemind.contact.vcard.VCardEditPresenter");

/**
 * @constructor
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {net.bluemind.contact.vcard.VCardEditPresenter}
 */
net.bluemind.contact.individual.edit.IndividualEditPresenter = function(ctx) {
  var view = new net.bluemind.contact.individual.edit.IndividualEditView(ctx);
  net.bluemind.contact.vcard.VCardEditPresenter.call(this, ctx, view);
}
goog.inherits(net.bluemind.contact.individual.edit.IndividualEditPresenter,
    net.bluemind.contact.vcard.VCardEditPresenter);

/** @override */
net.bluemind.contact.individual.edit.IndividualEditPresenter.prototype.toModelView = function(vcard, addressbook,
    changes) {
  var mv = goog.base(this, 'toModelView', vcard, addressbook, changes);
  mv.fullname = mv.fullname || {};
  return mv;
};

/** @override */
net.bluemind.contact.individual.edit.IndividualEditPresenter.prototype.fromModelView = function(mv) {
  var vcard = goog.base(this, 'fromModelView', mv)
  vcard['name'] = mv.fullname.value;
  vcard['value']['kind'] = 'individual';
  return vcard;
};