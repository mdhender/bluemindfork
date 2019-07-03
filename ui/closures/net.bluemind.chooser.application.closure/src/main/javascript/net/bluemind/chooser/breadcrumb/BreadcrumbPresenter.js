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
goog.provide('net.bluemind.chooser.breadcrumb.BreadcrumbPresenter');

goog.require('goog.Promise');
goog.require('goog.dom');
goog.require('net.bluemind.mvp.Presenter');
goog.require('net.bluemind.ui.Breadcrumb');



/**
 * @constructor
 *
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.chooser.breadcrumb.BreadcrumbPresenter = function(ctx) {
  net.bluemind.mvp.Presenter.call(this, ctx);
  this.view_ = new net.bluemind.ui.Breadcrumb();
  this.registerDisposable(this.view_);
};
goog.inherits(net.bluemind.chooser.breadcrumb.BreadcrumbPresenter, net.bluemind.mvp.Presenter);


/**
 * @type {net.bluemind.ui.Breadcrumb}
 * @private
 */
net.bluemind.chooser.breadcrumb.BreadcrumbPresenter.prototype.view_;


/** @override */
net.bluemind.chooser.breadcrumb.BreadcrumbPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('full'));
  this.handler.listen(this.view_, goog.ui.Component.EventType.ACTION, this.handleAction_);
  return goog.Promise.resolve();

};


/** @override */
net.bluemind.chooser.breadcrumb.BreadcrumbPresenter.prototype.setup = function() {
  var path = (
      /** @type {string}) */
      (this.ctx.params.get('path'))) || '/';
  this.view_.setPath(path);
  return goog.Promise.resolve();
};


/** @override */
net.bluemind.chooser.breadcrumb.BreadcrumbPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};


/**
 * @param {goog.events.ActionEvent} e
 * @private
 */
net.bluemind.chooser.breadcrumb.BreadcrumbPresenter.prototype.handleAction_ = function(e) {
  var path = e.target;
  this.ctx.helper('url').goTo('?path=' + path.getModel());
};
