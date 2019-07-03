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

/** @fileoverview Presenter for the application banner */

goog.provide('net.bluemind.mvp.banner.BannerPresenter');

goog.require('goog.Promise');
goog.require('net.bluemind.mvp.Presenter');
goog.require('net.bluemind.ui.banner.Banner');
goog.require('bluemind.ui.BannerModel');

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.mvp.banner.BannerPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.ui.banner.Banner();
  this.registerDisposable(this.view_);
};
goog.inherits(net.bluemind.mvp.banner.BannerPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.ui.banner.Banner}
 * @private
 */
net.bluemind.mvp.banner.BannerPresenter.prototype.view_;

/** @override */
net.bluemind.mvp.banner.BannerPresenter.prototype.init = function() {
  var model = bluemind.ui.BannerModel.buildModel(this.ctx.user, this.ctx.base);
  this.view_.setModel(model);
  this.view_.render(goog.dom.getElement('header'));
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.mvp.banner.BannerPresenter.prototype.setup = function() {
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.mvp.banner.BannerPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};