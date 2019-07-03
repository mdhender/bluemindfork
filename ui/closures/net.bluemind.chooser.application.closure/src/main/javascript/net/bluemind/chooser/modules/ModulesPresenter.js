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
goog.provide('net.bluemind.chooser.modules.ModulesPresenter');

goog.require('goog.Promise');
goog.require('goog.dom');
goog.require('net.bluemind.chooser.modules.ModulesView');
goog.require('net.bluemind.mvp.Presenter');



/**
 * @constructor
 *
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.chooser.modules.ModulesPresenter = function(ctx) {
  net.bluemind.mvp.Presenter.call(this, ctx);
  this.view_ = new net.bluemind.chooser.modules.ModulesView();
  this.registerDisposable(this.view_);
};
goog.inherits(net.bluemind.chooser.modules.ModulesPresenter, net.bluemind.mvp.Presenter);


/**
 * @type {net.bluemind.chooser.modules.ModulesView}
 * @private
 */
net.bluemind.chooser.modules.ModulesPresenter.prototype.view_;


/** @override */
net.bluemind.chooser.modules.ModulesPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('full'));
  return goog.Promise.resolve();

};


/** @override */
net.bluemind.chooser.modules.ModulesPresenter.prototype.setup = function() {
  return goog.Promise.resolve();
};


/** @override */
net.bluemind.chooser.modules.ModulesPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};
