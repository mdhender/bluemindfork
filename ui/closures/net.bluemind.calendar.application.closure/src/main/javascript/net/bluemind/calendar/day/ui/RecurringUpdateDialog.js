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
 * @fileoverview Recurring event update dialog component.
 */

goog.provide("net.bluemind.calendar.day.ui.RecurringUpdateDialog");

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
net.bluemind.calendar.day.ui.RecurringUpdateDialog = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(net.bluemind.calendar.day.ui.RecurringUpdateDialog, goog.ui.Dialog);

/**
 * 
 * @type {Object}
 * @private
 */
net.bluemind.calendar.day.ui.RecurringUpdateDialog.prototype.vseries_;

/** @override */
net.bluemind.calendar.day.ui.RecurringUpdateDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(net.bluemind.calendar.day.templates.recurringUpdateDialog);
  this.decorateInternal(elem);
};

/** @override */
net.bluemind.calendar.day.ui.RecurringUpdateDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(goog.dom.getElement('rud-btn-this-instance'), goog.events.EventType.CLICK,
      this.updateInstance_, false, this);

  this.getHandler().listen(goog.dom.getElement('rud-btn-all-the-following'), goog.events.EventType.CLICK,
      this.updateFollowing_, false, this);

  this.getHandler().listen(goog.dom.getElement('rud-btn-update-serie'), goog.events.EventType.CLICK, this.updateSerie_,
      false, this);

  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT, this.cancelUpdate_, false, this);
};

/** @override */
net.bluemind.calendar.day.ui.RecurringUpdateDialog.prototype.setVSeries = function(vseries_) {
  this.vseries_ = vseries_;
};

/** @override */
net.bluemind.calendar.day.ui.RecurringUpdateDialog.prototype.setVisible = function(visible) {
  goog.base(this, 'setVisible', visible);
  if (visible) {
    var model = this.getModel();
    if (!goog.date.isSameDay(model.recurrenceId, model.dtstart) || model.states.exception) {
      this.updateInstance_();
    }
    var el = this.getDomHelper().getElement('rud-btn-all-the-following');
    el = el.parentElement.parentElement;
    var following = !goog.date.isSameDay(this.vseries_.main.dtstart, model.recurrenceId) && model.states.master;
    goog.style.setElementShown(el, following)
  }
};

/**
 * update this instance
 * 
 * @private
 */
net.bluemind.calendar.day.ui.RecurringUpdateDialog.prototype.updateInstance_ = function() {
  this.setVisible(false);
  var model = this.getModel();
  model.recurringDone = true;
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.SAVE, this.getModel());
  this.dispatchEvent(e);
};

/**
 * update following
 * 
 * @private
 */
net.bluemind.calendar.day.ui.RecurringUpdateDialog.prototype.updateFollowing_ = function() {
  this.setVisible(false);
  var model = this.getModel();
  var main = this.vseries_.main;
  main.recurringDone = true;
  main.updateFollowing = true;
  main.dtstart = model.dtstart;
  main.dtend = model.dtend;
  main.summary = model.summary;
  main.attendee = model.attendee;
  main.participation = model.participation;
  main.sendNotification = model.sendNotification;
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.SAVE, main);
  this.dispatchEvent(e);
};

/**
 * update serie
 * 
 * @private
 */
net.bluemind.calendar.day.ui.RecurringUpdateDialog.prototype.updateSerie_ = function() {
  this.setVisible(false);
  var model = this.getModel();
  var main = this.vseries_.main;
  if (model.dtstart instanceof goog.date.DateTime) {
    main.dtstart.setHours(model.dtstart.getHours());
    main.dtstart.setMinutes(model.dtstart.getMinutes());
    main.dtstart.setSeconds(model.dtstart.getSeconds());
    main.dtstart.setMilliseconds(model.dtstart.getMilliseconds());
    var duration = model.dtend.getTime() - model.dtstart.getTime();
    main.dtend.setTime(main.dtstart.getTime() + duration);
  }
  main.summary = model.summary;
  main.attendee = model.attendee;
  main.participation = model.participation;
  main.sendNotification = model.sendNotification;
  main.recurringDone = true;
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.SAVE, main);
  this.dispatchEvent(e);
};

/**
 * Cancel update
 * 
 * @param {goog.ui.Dialog.Event} e dialog event.
 * @private
 */
net.bluemind.calendar.day.ui.RecurringUpdateDialog.prototype.cancelUpdate_ = function(event) {
  event.stopPropagation();
  this.setVisible(false);
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.REFRESH, this.getModel());
  this.dispatchEvent(e);
};
