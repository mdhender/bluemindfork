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
goog.provide("net.bluemind.contact.individual.consult.IndividualConsultPresenter");

goog.require("net.bluemind.contact.individual.consult.IndividualConsultView");
goog.require("net.bluemind.contact.vcard.VCardConsultPresenter");
goog.require("goog.Uri");

/**
 * @constructor
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends { net.bluemind.contact.vcard.VCardConsultPresenter}
 */
net.bluemind.contact.individual.consult.IndividualConsultPresenter = function(ctx) {
  var view = new net.bluemind.contact.individual.consult.IndividualConsultView();
  net.bluemind.contact.vcard.VCardConsultPresenter.call(this, ctx, view);
}
goog.inherits(net.bluemind.contact.individual.consult.IndividualConsultPresenter,
    net.bluemind.contact.vcard.VCardConsultPresenter);

/** @override */
net.bluemind.contact.individual.consult.IndividualConsultPresenter.prototype.toModelView = function(vcard, addressbook) {
  var mv = goog.base(this, 'toModelView', vcard, addressbook);
  goog.array.forEach(mv.urls, function(url) {
    if (!goog.Uri.parse(url.value).hasScheme()) {
      url.value = 'http://' + url.value; 
    }
  });
  return mv;
};
