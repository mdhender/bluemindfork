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

/**
 * @fileoverview An MVP-style presenter. Presenter is mainly in charge of making
 *               a view from model to build the UI. It can also catch some
 *               action that will trigger a command, and can make change to
 *               model sometime, even though this is mainly the job of the model
 *               view. Presenter will also build and dispose the ui.
 */

goog.provide('net.bluemind.mvp.Presenter');

goog.require('goog.Disposable');
goog.require('goog.events.EventHandler');
goog.require('goog.async.Deferred');

/**
 * An MVP-style presenter class.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {goog.Disposable}
 */
net.bluemind.mvp.Presenter = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.handler = new goog.events.EventHandler(this);
};
goog.inherits(net.bluemind.mvp.Presenter, goog.Disposable);

/**
 * Event handler to easily manage event listening
 * 
 * @type {goog.events.EventHandler}
 * @protected
 */
net.bluemind.mvp.Presenter.prototype.handler;

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.mvp.Presenter.prototype.ctx;

/**
 * Initialize presenter. This method is not called on transition, only on
 * handle.
 * 
 * @return {goog.Promise} Async execution sequence.
 */
net.bluemind.mvp.Presenter.prototype.init = goog.abstractMethod;

/**
 * Setup presenter action. This method is called on transition, and on handle.
 * 
 * @return {goog.Promise} Async execution sequence.
 */
net.bluemind.mvp.Presenter.prototype.setup = goog.abstractMethod;

/**
 * Tear down presenter action. This method is called on exit.
 * 
 * @return {goog.Promise} Async execution sequence.
 */
net.bluemind.mvp.Presenter.prototype.exit = goog.abstractMethod;

/** @override */
net.bluemind.mvp.Presenter.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.handler.dispose();
  this.handler = null;
};
