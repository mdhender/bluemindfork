/**
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

/** @fileoverview Handler for the application Logo */

goog.provide('net.bluemind.mvp.logo.LogoHandler');

goog.require('goog.Promise');
goog.require('net.bluemind.mvp.logo.LogoPresenter');
goog.require('net.bluemind.mvp.handler.PresenterHandler');

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @extends {net.bluemind.mvp.handler.PresenterHandler}
 * @constructor
 */
net.bluemind.mvp.logo.LogoHandler = function(ctx) {
  goog.base(this, ctx);
};
goog.inherits(net.bluemind.mvp.logo.LogoHandler, net.bluemind.mvp.handler.PresenterHandler);

/** @override */
net.bluemind.mvp.logo.LogoHandler.prototype.createPresenter = function(ctx) {
  return new net.bluemind.mvp.logo.LogoPresenter(ctx);
};