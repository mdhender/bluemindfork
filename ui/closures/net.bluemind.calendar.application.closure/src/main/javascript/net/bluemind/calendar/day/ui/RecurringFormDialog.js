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

/**
 * @fileoverview Recurring event form dialog component.
 */

goog.provide("net.bluemind.calendar.day.ui.RecurringFormDialog");

goog.require("goog.dom");
goog.require("goog.soy");
goog.require("goog.events.EventType");
goog.require("goog.ui.Dialog");
goog.require("goog.ui.Dialog.EventType");
goog.require("net.bluemind.calendar.day.templates");
goog.require("net.bluemind.calendar.vevent.EventType");
goog.require("net.bluemind.calendar.vevent.VEventEvent");

/**
 * @param {goog.dom.DomHelper} opt_domHelper Optional DOM helper; see {@link
 *          goog.ui.Component} for semantics.
 * 
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.day.ui.RecurringFormDialog = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(net.bluemind.calendar.day.ui.RecurringFormDialog, goog.ui.Dialog);

/**
 * 
 * @type {Object}
 * @private
 */
net.bluemind.calendar.day.ui.RecurringFormDialog.prototype.vseries_;

/** @override */
net.bluemind.calendar.day.ui.RecurringFormDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(net.bluemind.calendar.day.templates.recurringFormDialog);
  this.decorateInternal(elem);
};

/** @override */
net.bluemind.calendar.day.ui.RecurringFormDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(goog.dom.getElement('rfd-btn-this-instance'), goog.events.EventType.CLICK,
      this.gotoInstance_, false, this);

  this.getHandler().listen(goog.dom.getElement('rfd-btn-goto-serie'), goog.events.EventType.CLICK, this.gotoSerie_,
      false, this);

  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT, this.cancelForm_, false, this);
};

/** @override */
net.bluemind.calendar.day.ui.RecurringFormDialog.prototype.setVSeries = function(vseries) {
  this.vseries_ = vseries;
};

/** @override */
net.bluemind.calendar.day.ui.RecurringFormDialog.prototype.setVisible = function(visible) {
  goog.base(this, 'setVisible', visible);
  if (visible) {
    var model = this.getModel();
    if (!goog.date.isSameDay(model.recurrenceId, model.dtstart)) {
      this.gotoInstance_();
    }
    if (!model.states.updatable && !model.states.exception) {
      this.gotoSerie_();
    }
  }
};

/**
 * goto this instance
 * 
 * @private
 */
net.bluemind.calendar.day.ui.RecurringFormDialog.prototype.gotoInstance_ = function() {
  this.setVisible(false);
  var model = this.getModel();
  model.recurringDone = true;
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.DETAILS, this.getModel());
  this.dispatchEvent(e);
};

/**
 * goto serie
 * 
 * @private
 */
net.bluemind.calendar.day.ui.RecurringFormDialog.prototype.gotoSerie_ = function() {
  this.setVisible(false);
  this.vseries_.main.summary = this.getModel().summary;
  this.vseries_.main.recurringDone = true;
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.DETAILS, this.vseries_.main);
  this.dispatchEvent(e);
};

/**
 * Cancel goto
 * 
 * @param {goog.ui.Dialog.Event} e dialog event.
 * @private
 */
net.bluemind.calendar.day.ui.RecurringFormDialog.prototype.cancelForm_ = function(event) {
  event.stopPropagation();
  this.setVisible(false);
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.REFRESH, this.getModel());
  this.dispatchEvent(e);
};
