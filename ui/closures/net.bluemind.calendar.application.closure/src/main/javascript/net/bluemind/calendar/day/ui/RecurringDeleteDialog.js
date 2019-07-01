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
 * @fileoverview Recurring event delete dialog component.
 */

goog.provide("net.bluemind.calendar.day.ui.RecurringDeleteDialog");

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
 * goog.ui.Component} for semantics.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.day.ui.RecurringDeleteDialog = function(opt_domHelper) {
  goog.base(this, undefined, undefined, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(net.bluemind.calendar.day.ui.RecurringDeleteDialog, goog.ui.Dialog);

/**
 * @type {Object}
 * @private
 */
net.bluemind.calendar.day.ui.RecurringDeleteDialog.prototype.vseries_;

/** @override */
net.bluemind.calendar.day.ui.RecurringDeleteDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(net.bluemind.calendar.day.templates.recurringDeleteDialog);
  this.decorateInternal(elem);
};

/** @override */
net.bluemind.calendar.day.ui.RecurringDeleteDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(goog.dom.getElement('rdd-btn-this-instance'), goog.events.EventType.CLICK,
      this.deleteInstance_, false, this);

  this.getHandler().listen(goog.dom.getElement('rdd-btn-all-the-following'), goog.events.EventType.CLICK,
      this.deleteFollowing_, false, this);

  this.getHandler().listen(goog.dom.getElement('rdd-btn-delete-serie'), goog.events.EventType.CLICK, this.deleteSerie_,
      false, this);

  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT, this.cancelDelete_, false, this);
};

/** @override */
net.bluemind.calendar.day.ui.RecurringDeleteDialog.prototype.setVSeries = function(vseries) {
  this.vseries_ = vseries;
};

/** @override */
net.bluemind.calendar.day.ui.RecurringDeleteDialog.prototype.setVisible = function(visible) {
  goog.base(this, 'setVisible', visible);
  if (visible) {
    var model = this.getModel();
    if (!goog.date.isSameDay(model.recurrenceId, model.dtstart)) {
      this.deleteInstance_();
    }
    var el = this.getDomHelper().getElement('rdd-btn-all-the-following');
    el = el.parentElement.parentElement;
    var following = !goog.date.isSameDay(this.vseries_.main.dtstart, model.recurrenceId) && model.states.master;
    goog.style.setElementShown(el, following)
  }
};

/**
 * delete this instance
 * 
 * @private
 */
net.bluemind.calendar.day.ui.RecurringDeleteDialog.prototype.deleteInstance_ = function() {
  this.setVisible(false);
  var model = this.getModel();
  this.vseries_.main.exdate.push(model.recurrenceId);
  this.vseries_.main.recurringDone = true;
  this.vseries_.main.sendNotification = model.sendNotification;
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.SAVE, this.vseries_.main);
  e.force = true;
  this.dispatchEvent(e);
};

/**
 * delete following
 * 
 * @private
 */
net.bluemind.calendar.day.ui.RecurringDeleteDialog.prototype.deleteFollowing_ = function() {
  this.setVisible(false);
  var model = this.getModel();
  var end = model.recurrenceId.clone();
  end.add(new goog.date.Interval(0, 0, -1));
  this.vseries_.main.rrule.until = end;
  this.vseries_.main.recurringDone = true;
  this.vseries_.main.sendNotification = model.sendNotification;
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.SAVE, this.vseries_.main);
  this.dispatchEvent(e);
};

/**
 * delete serie
 * 
 * @private
 */
net.bluemind.calendar.day.ui.RecurringDeleteDialog.prototype.deleteSerie_ = function() {
  this.setVisible(false);
  this.vseries_.main.recurringDone = true;
  this.vseries_.main.sendNotification = this.getModel().sendNotification;
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.REMOVE, this.vseries_.main);
  e.force = true;
  this.dispatchEvent(e);
};

/**
 * Cancel delete
 * 
 * @param {goog.ui.Dialog.Event} e dialog event.
 * @private
 */
net.bluemind.calendar.day.ui.RecurringDeleteDialog.prototype.cancelDelete_ = function(event) {
  event.stopPropagation();
  this.setVisible(false);
};
