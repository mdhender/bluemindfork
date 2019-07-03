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

/** @fileoverview Handler for the application banner */

goog.provide('net.bluemind.mvp.handler.PresenterHandler');

goog.require('goog.Promise');
goog.require('net.bluemind.mvp.banner.BannerPresenter');
goog.require('net.bluemind.mvp.Handler');

/**
 * Handler with a presenter.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @extends {net.bluemind.mvp.Handler}
 * @constructor
 */
net.bluemind.mvp.handler.PresenterHandler = function(ctx) {
  goog.base(this, ctx);
  this.presenter = this.createPresenter(ctx);
  if (this.presenter) {
    this.registerDisposable(this.presenter);
  }
};
goog.inherits(net.bluemind.mvp.handler.PresenterHandler, net.bluemind.mvp.Handler);

/**
 * Create the handler presenter
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @return {net.bluemind.mvp.Presenter}
 * @protected
 */
net.bluemind.mvp.handler.PresenterHandler.prototype.createPresenter = goog.abstractMethod;

/**
 * @type {net.bluemind.mvp.Presenter}
 * @protected
 */
net.bluemind.mvp.handler.PresenterHandler.prototype.presenter;

/** @override */
net.bluemind.mvp.handler.PresenterHandler.prototype.setup = function() {
  if (this.presenter) {
    return this.presenter.init().then(function() {
      return this.presenter.setup();
    }, null, this);
  } else {
    return goog.Promise.resolve();
  }
};

/** @override */
net.bluemind.mvp.handler.PresenterHandler.prototype.handle = function() {
  if (this.presenter) {
    return this.presenter.setup();
  } else {
    return goog.Promise.resolve();
  }
};

/** @override */
net.bluemind.mvp.handler.PresenterHandler.prototype.onNavigation = function(exit) {
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.mvp.handler.PresenterHandler.prototype.exit = function() {
  if (this.presenter) {
    return this.presenter.exit();
  } else {
    return goog.Promise.resolve();
  }
};
