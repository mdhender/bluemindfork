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
goog.provide("net.bluemind.contact.vcard.VCardConsultPresenter");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.dom");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.addressbook.api.i18n.Address.Caption");
goog.require("net.bluemind.addressbook.api.i18n.Email.Caption");
goog.require("net.bluemind.addressbook.api.i18n.IMPP.Caption");
goog.require("net.bluemind.addressbook.api.i18n.Tel.Caption");
goog.require("net.bluemind.addressbook.api.i18n.URL.Caption");
goog.require("net.bluemind.contact.vcard.VCardModelAdapter");
/**
 * @constructor
 * @param {goog.ui.Component} view
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.contact.vcard.VCardConsultPresenter = function(ctx, view) {
  net.bluemind.mvp.Presenter.call(this, ctx);
  this.view = view;
  this.registerDisposable(this.view);
  this.handler.listen(this.view, 'copy', this.handleCopy);
  this.logger = goog.log.getLogger('net.bluemind.contact.vcard.VCardConsultPresenter');
};
goog.inherits(net.bluemind.contact.vcard.VCardConsultPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {goog.ui.Component}
 * @protected
 */
net.bluemind.contact.vcard.VCardConsultPresenter.prototype.view;

/** @override */
net.bluemind.contact.vcard.VCardConsultPresenter.prototype.init = function() {
  this.view.render(goog.dom.getElement('main'));
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.contact.vcard.VCardConsultPresenter.prototype.setup = function() {
  var container = this.ctx.params.get('container');
  var promise, uid = this.ctx.params.get('uid');
  var addressbook = goog.array.find(this.ctx.session.get('addressbooks'), function(adb) {
    return (adb['uid'] == container);
  });

  return this.ctx.service('addressbook').getItem(container, uid).then(function(vcard) {
    if (goog.isDefAndNotNull(vcard)) {
      this.view.setModel(this.toModelView(vcard, addressbook));
    } else {
      throw 'VCard ' + uid + ' not found';
    }
  }, null, this);
};

/** @override */
net.bluemind.contact.vcard.VCardConsultPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * Convert a vcard to model view
 * 
 * @protected
 * @param {Object} vcard VCard object
 * @param {Object} addressbook Container object
 * @return {Object}
 */
net.bluemind.contact.vcard.VCardConsultPresenter.prototype.toModelView = function(vcard, addressbook) {
  return new net.bluemind.contact.vcard.VCardModelAdapter(this.ctx).toModelView(vcard, addressbook);
};

/**
 * Copy contact
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.contact.vcard.VCardConsultPresenter.prototype.handleCopy = function(event) {
  var model = this.view.getModel();
  this.ctx.service('addressbook').copyItem(model.container.id, model.id, event.target.getId()).then(function(m) {
    this.ctx.notifyInfo(net.bluemind.contact.Messages.successCopy());
    this.ctx.helper('url').goTo('/vcard/?container=' + event.target.getId() + '&uid=' + model.id, 'vcontainer');
  }, function(error) {
    goog.log.error(this.logger, 'error during card ' + model.id + ' copy', error);
    this.ctx.notifyError(net.bluemind.contact.Messages.errorCopy(error), error);
  }, this);
};
