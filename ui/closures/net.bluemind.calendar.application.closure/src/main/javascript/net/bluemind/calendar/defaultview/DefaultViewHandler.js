/*
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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

goog.provide('net.bluemind.calendar.defaultview.DefaultViewHandler');

goog.require('goog.Promise');
goog.require('net.bluemind.mvp.handler.PresenterHandler');

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @extends {net.bluemind.mvp.handler.PresenterHandler}
 * @constructor
 */
net.bluemind.calendar.defaultview.DefaultViewHandler = function(ctx) {
  goog.base(this, ctx);
  this.presenter = this.createPresenter(ctx);
  this.registerDisposable(this.presenter);
};
goog.inherits(net.bluemind.calendar.defaultview.DefaultViewHandler,
  net.bluemind.mvp.handler.PresenterHandler);


/** @override */
net.bluemind.calendar.defaultview.DefaultViewHandler.prototype.createPresenter = function(ctx) {
  var v = ctx.session.get('defaultview');
  if (v == 'DAY') {
    return new net.bluemind.calendar.day.DayPresenter(ctx);
  } else if (v == 'MONTH') {
    return new net.bluemind.calendar.month.MonthPresenter(ctx);
  } else if (v == 'LIST') {
    return new net.bluemind.calendar.list.ListPresenter(ctx);
  }
  return new net.bluemind.calendar.day.DayPresenter(ctx);
};