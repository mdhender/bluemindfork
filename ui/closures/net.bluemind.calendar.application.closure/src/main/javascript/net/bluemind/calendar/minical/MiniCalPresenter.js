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

/** @fileoverview Presenter for the application search bar */

goog.provide("net.bluemind.calendar.minical.MiniCalPresenter");

goog.require("goog.Promise");
goog.require("goog.Uri");
goog.require("goog.dom");
goog.require("goog.ui.DatePicker.Events");
goog.require("net.bluemind.calendar.minical.MiniCalView");
goog.require("net.bluemind.mvp.Presenter");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.calendar.minical.MiniCalPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.calendar.minical.MiniCalView();
  this.registerDisposable(this.view_);
};
goog.inherits(net.bluemind.calendar.minical.MiniCalPresenter,
  net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.calendar.minical.MiniCalView}
 * @private
 */
net.bluemind.calendar.minical.MiniCalPresenter.prototype.view_;

/** @override */
net.bluemind.calendar.minical.MiniCalPresenter.prototype.init = function() {
  this.handler.listen(this.view_, goog.ui.DatePicker.Events.CHANGE, this.goTo_);
  this.view_.render(goog.dom.getElement('content-menu'));
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.calendar.minical.MiniCalPresenter.prototype.setup = function() {
  this.view_.range = this.ctx.session.get('range');
  this.view_.setDateInternal(this.ctx.session.get('date'));
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.calendar.minical.MiniCalPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};


/**
 * Called when the date is changed.
 * @param {goog.ui.DatePickerEvent} e The date change event.
 * @private
 */
net.bluemind.calendar.minical.MiniCalPresenter.prototype.goTo_ = function(e) {
  var date = e.date;
  //FIXME : in view
  var loc = window.location;
  var uri = new goog.Uri(loc.hash.replace('#', ''));
  uri.getQueryData().set('date', date.toIsoString());
  loc.hash = uri.toString();
};
