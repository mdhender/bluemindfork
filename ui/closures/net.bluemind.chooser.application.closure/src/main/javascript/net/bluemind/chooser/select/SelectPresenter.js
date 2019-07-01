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

/** @fileoverview Presenter for the application search bar */

goog.provide('net.bluemind.chooser.select.SelectPresenter');

goog.require('goog.Promise');
goog.require('goog.dom');
goog.require('goog.events.EventType');
goog.require('goog.log');
goog.require('goog.ui.Component.EventType');
goog.require('net.bluemind.chooser.select.SelectView');
goog.require('net.bluemind.mvp.Presenter');

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.chooser.select.SelectPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.chooser.select.SelectView();
  this.registerDisposable(this.view_);
};
goog.inherits(net.bluemind.chooser.select.SelectPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.chooser.select.SelectView}
 * @private
 */
net.bluemind.chooser.select.SelectPresenter.prototype.view_;

/**
 * @type {goog.debug.Logger}
 * @private
 */
net.bluemind.chooser.select.SelectPresenter.prototype.logger_ = goog.log
    .getLogger('net.bluemind.chooser.select.SelectPresenter');

/** @override */
net.bluemind.chooser.select.SelectPresenter.prototype.init = function() {
  this.handler.listen(this.ctx.handler('selection'), goog.events.EventType.CHANGE, this.handleSelectionChanged_);
  this.view_.render(goog.dom.getElement('full'));
  this.handleSelectionChanged_(null);
  this.handler.listen(this.view_, goog.ui.Component.EventType.ACTION, this.handleAction_);
  return goog.Promise.resolve();

};

/** @override */
net.bluemind.chooser.select.SelectPresenter.prototype.setup = function() {
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.chooser.select.SelectPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * @param {goog.events.Event} e
 * @private
 */
net.bluemind.chooser.select.SelectPresenter.prototype.handleSelectionChanged_ = function(e) {
  var selection = this.ctx.session.get('selection') || [];
  this.view_.getChild('select').setEnabled((selection.length > 0));
};

/**
 * @param {goog.events.ActionEvent} e
 * @private
 */
net.bluemind.chooser.select.SelectPresenter.prototype.handleAction_ = function(e) {
  if (e.target.getId() == 'cancel') {
    goog.log.info(this.logger_, 'Cancel chooser selection');
    if (this.ctx.session.containsKey('onCancel')) {
      var cancel = this.ctx.session.get('onCancel');
      goog.log.fine(this.logger_, 'Cancel action detected. Calling it');
      cancel();
    }
  } else if (e.target.getId() == 'select') {
    var selection = this.ctx.session.get('selection') || [];
    goog.log.info(this.logger_, 'Select ' + selection.length + ' item(s)');
    if (this.ctx.session.containsKey('onSuccess')) {
      var success = this.ctx.session.get('onSuccess');
      goog.log.fine(this.logger_, 'Success action detected. Calling it');
      success(selection);
    }
  }
  if (this.ctx.session.get('closeAfterAction')) {
    try {
      goog.global.close();
    } catch (err) {
      /** @meaning chooser.error.unearchableOpener */
      var MSG_UNREACHABLE_OPENER = goog.getMsg('Cannot communicate with parent window');
      throw MSG_UNREACHABLE_OPENER;
    }
  } else {
    this.ctx.helper('url').reload();
  }
};
