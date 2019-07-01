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
goog.provide("net.bluemind.contact.individual.consult.IndividualConsultHandler");

goog.require("net.bluemind.contact.individual.consult.IndividualConsultPresenter");
goog.require("net.bluemind.mvp.handler.PresenterHandler");

/**
 * @constructor
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {net.bluemind.mvp.handler.PresenterHandler}
 */
net.bluemind.contact.individual.consult.IndividualConsultHandler = function(ctx) {
  net.bluemind.mvp.handler.PresenterHandler.call(this, ctx);
}
goog.inherits(net.bluemind.contact.individual.consult.IndividualConsultHandler,
    net.bluemind.mvp.handler.PresenterHandler);

/** @override */
net.bluemind.contact.individual.consult.IndividualConsultHandler.prototype.createPresenter = function(ctx) {
  return new net.bluemind.contact.individual.consult.IndividualConsultPresenter(ctx);
};
