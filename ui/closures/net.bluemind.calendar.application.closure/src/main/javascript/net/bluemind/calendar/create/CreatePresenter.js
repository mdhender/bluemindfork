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
goog.provide("net.bluemind.calendar.create.CreatePresenter");

goog.require("goog.Promise");
goog.require("goog.dom");
goog.require("goog.ui.Button");
goog.require("goog.ui.Component.EventType");
goog.require("net.bluemind.mvp.Presenter");
goog.require("bluemind.ui.style.PrimaryActionButtonRenderer");

/**
 * @constructor
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.calendar.create.CreatePresenter = function(ctx) {
  net.bluemind.mvp.Presenter.call(this, ctx);
  /** @meaning calendar.newEvent */
  var MSG_NEW_CONTACT = goog.getMsg('New event');
  this.view_ = new goog.ui.Button(MSG_NEW_CONTACT, bluemind.ui.style.PrimaryActionButtonRenderer.getInstance())
  this.registerDisposable(this.view_);
};
goog.inherits(net.bluemind.calendar.create.CreatePresenter, net.bluemind.mvp.Presenter);

/**
 * @type {goog.ui.Button}
 * @private
 */
net.bluemind.calendar.create.CreatePresenter.prototype.view_;

/** @override */
net.bluemind.calendar.create.CreatePresenter.prototype.init = function() {
  this.view_.addClassName(goog.getCssName('add-event'));
  this.view_.render(goog.dom.getElement('header'));
  this.handler.listen(this.view_, goog.ui.Component.EventType.ACTION, this.handleAction_);
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.calendar.create.CreatePresenter.prototype.setup = function() {
  return this.ctx.service('calendarsMgmt').list('calendar').then(function(cals) {
    var cal = goog.array.find(cals, function(cal) {
      return cal['writable'];
    });
    var enabled = cal || false;
    this.view_.setEnabled(enabled);

  }, null, this);
};

/** @override */
net.bluemind.calendar.create.CreatePresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * 
 */
net.bluemind.calendar.create.CreatePresenter.prototype.handleAction_ = function(e) {
  // only do it if a calendar is writable
  this.ctx.service('calendarsMgmt').list('calendar').then(function(cals) {

    // filter external cal
    cals = goog.array.filter(cals, function(c) {
      return !c['readOnly'];
    });

    // my calendar
    var cal = goog.array.find(cals, function(cal) {
      return cal['defaultContainer'] && cal['owner'] == this.ctx.user['uid'];
    }, this);

    if (cal == null) {
      // the 1st writable calendar
      cal = goog.array.find(cals, function(cal) {
        return cal['writable'];
      });
    }

    if (cal) {
      var url = '/vevent/?container=' + cal['uid'];
      this.ctx.helper('url').goTo(url);
     }

  }, null, this);
};
