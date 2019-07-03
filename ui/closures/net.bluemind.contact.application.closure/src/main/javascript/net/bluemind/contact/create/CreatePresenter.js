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
goog.provide("net.bluemind.contact.create.CreatePresenter");

goog.require("goog.Promise");
goog.require("goog.dom");
goog.require("goog.ui.Component.EventType");
goog.require("net.bluemind.contact.create.CreateView");
goog.require("net.bluemind.mvp.Presenter");

/**
 * @constructor
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.contact.create.CreatePresenter = function(ctx) {
  net.bluemind.mvp.Presenter.call(this, ctx);
  this.view_ = new net.bluemind.contact.create.CreateView(ctx);
  this.registerDisposable(this.view_);
};
goog.inherits(net.bluemind.contact.create.CreatePresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.contact.create.CreateView}
 * @private
 */
net.bluemind.contact.create.CreatePresenter.prototype.view_;

/** @override */
net.bluemind.contact.create.CreatePresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('header'));
  this.handler.listen(this.view_, goog.ui.Component.EventType.ACTION, this.handleAction_);
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.contact.create.CreatePresenter.prototype.setup = function() {
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.contact.create.CreatePresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * 
 */
net.bluemind.contact.create.CreatePresenter.prototype.handleAction_ = function(e) {
  var container = this.ctx.params.get('container');
  var url = '/';
  if (e.target.getId() != 'group') {
    url += 'individual/';
  } else {
    url += 'group/'
  }
  if (container) {
    url += '?container=' + container;
  }
  this.ctx.helper('url').goTo(url);
};
