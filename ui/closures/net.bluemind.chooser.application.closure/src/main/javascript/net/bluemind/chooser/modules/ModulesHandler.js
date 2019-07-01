/* BEGIN LICENSE
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
goog.provide('net.bluemind.chooser.modules.ModulesHandler');

goog.require('net.bluemind.chooser.modules.ModulesPresenter');
goog.require('net.bluemind.mvp.handler.PresenterHandler');



/**
 * @constructor
 *
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {net.bluemind.mvp.handler.PresenterHandler}
 */
net.bluemind.chooser.modules.ModulesHandler = function(ctx) {
  net.bluemind.mvp.handler.PresenterHandler.call(this, ctx);
};
goog.inherits(net.bluemind.chooser.modules.ModulesHandler, net.bluemind.mvp.handler.PresenterHandler);


/** @override */
net.bluemind.chooser.modules.ModulesHandler.prototype.createPresenter = function(ctx) {
  return new net.bluemind.chooser.modules.ModulesPresenter(ctx);
};
