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

goog.provide("bm.ui.NewContainer");

goog.require("bluemind.IdGenerator");
goog.require('goog.events.KeyCodes');
goog.require('goog.events.KeyHandler');
goog.require('goog.events.KeyHandler.EventType');
goog.require("goog.ui.LabelInput");

bm.ui.NewContainer = function(kind, ctx, service, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.keyHandler_ = new goog.events.KeyHandler();
  this.inputText_ = new goog.ui.LabelInput(bm.ui.NewContainer.MSG_PLACEHOLDER, opt_domHelper);
  this.inputText_.setId('addContainer');
  this.addChild(this.inputText_, true);
  this.kind_ = kind;
  this.ctx_ = ctx;
  this.service_ = service;
};
goog.inherits(bm.ui.NewContainer, goog.ui.Component);
/** @meaning commons.add */
bm.ui.NewContainer.MSG_PLACEHOLDER = goog.getMsg('Add...');

/**
 * Context
 */
bm.ui.NewContainer.prototype.ctx_;

/**
 * Container kind (addressbook, calendar, todolist)
 */
bm.ui.NewContainer.prototype.kind_;

/**
 * goog.ui.LabelInput
 */
bm.ui.NewContainer.prototype.inputText_

/**
 * Keyboard event handler
 */
bm.ui.NewContainer.prototype.keyHandler_;

/** @override */
bm.ui.NewContainer.prototype.createDom = function() {
  this.setElementInternal(this.getDomHelper().createDom('div', goog.getCssName('newcontainer')));
};

/** @override */
bm.ui.NewContainer.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.keyHandler_.attach(this.getChild('addContainer').getElement());
  this.getHandler().listen(this.keyHandler_, goog.events.KeyHandler.EventType.KEY, this.handleInputKeyEvent_);
};

bm.ui.NewContainer.prototype.handleInputKeyEvent_ = function(e) {
  if (e.keyCode == goog.events.KeyCodes.ENTER && e.type == goog.events.KeyHandler.EventType.KEY) {
    var lbl = this.inputText_.getValue();
    if (lbl != null && lbl.trim() != '') {
      var uid = bluemind.IdGenerator.generate();
      var descriptor = {
        'uid' : uid,
        'type' : this.kind_,
        'name' : lbl.trim(),
        'owner' : this.ctx_.user['uid'],
        'domainUid' : this.ctx_.user['domainUid'],
        'writable' : true,
        'defaultContainer' : false
      };
      var ctx = this.ctx_;
      var inputText = this.inputText_;
      var op = this.service_.create(uid, descriptor);
      op.then(function() {
        inputText.setValue(null);
      });
    }
  }
};
