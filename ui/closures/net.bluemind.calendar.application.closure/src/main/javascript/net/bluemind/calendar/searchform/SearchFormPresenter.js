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

goog.provide("net.bluemind.calendar.searchform.SearchFormPresenter");

goog.require("goog.Promise");
goog.require("goog.dom");
goog.require("goog.structs.Set");
goog.require("goog.ui.Component.EventType");
goog.require("net.bluemind.calendar.searchform.SearchFormView");
goog.require("net.bluemind.mvp.Presenter");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.calendar.searchform.SearchFormPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.calendar.searchform.SearchFormView();
  this.registerDisposable(this.view_);
  this.handler.listen(this.view_, goog.ui.Component.EventType.ACTION, this.handleAction_);
};
goog.inherits(net.bluemind.calendar.searchform.SearchFormPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.calendar.searchform.SearchFormView}
 * @private
 */
net.bluemind.calendar.searchform.SearchFormPresenter.prototype.view_;

/** @override */
net.bluemind.calendar.searchform.SearchFormPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('header'));
  return goog.Promise.resolve();

};

/** @override */
net.bluemind.calendar.searchform.SearchFormPresenter.prototype.setup = function() {
  var vcontainer = this.ctx.params.get('pattern');
  // if (vcontainer && vcontainer['settings'] &&
  // vcontainer['settings']['query']) {
  // this.view_.setModel(vcontainer['settings']['query']);
  this.view_.setModel(vcontainer || '');

  return goog.Promise.resolve();
};

/** @override */
net.bluemind.calendar.searchform.SearchFormPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * @param {goog.events.Event} e
 * @private
 */
net.bluemind.calendar.searchform.SearchFormPresenter.prototype.handleAction_ = function(e) {
  // FIXME
  var pattern = this.view_.getChild('search').getValue();
  if (pattern.trim() != '') {
    var uri = new goog.Uri('/search/');
    uri.getQueryData().set('range', 'day');
    uri.getQueryData().set('pattern', pattern);
    this.ctx.helper('url').goTo(uri);
  }
};
