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

/**
 * @fileoverview Handler for the application navigation (calendars & tags
 *               selector)
 */

goog.provide('net.bluemind.calendar.tasks.TasksHandler');

goog.require('goog.Promise');
goog.require('net.bluemind.calendar.tasks.TasksPresenter');
goog.require('net.bluemind.mvp.handler.PresenterHandler');

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @extends {net.bluemind.mvp.handler.PresenterHandler}
 * @constructor
 */
net.bluemind.calendar.tasks.TasksHandler = function(ctx) {
  goog.base(this, ctx);
};
goog.inherits(net.bluemind.calendar.tasks.TasksHandler, net.bluemind.mvp.handler.PresenterHandler);

/** @override */
net.bluemind.calendar.tasks.TasksHandler.prototype.createPresenter = function(ctx) {
  return new net.bluemind.calendar.tasks.TasksPresenter(ctx);
};