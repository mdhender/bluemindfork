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
 * @fileoverview Event notification dialog component.
 */

goog.provide('bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog');

goog.require('bluemind.calendar.event.template');
goog.require('goog.ui.Dialog');

/**
 * @param {string} opt_class CSS class name for the dialog element, also used
 *    as a class name prefix for related elements; defaults to modal-dialog.
 * @param {boolean} opt_useIframeMask Work around windowed controls z-index
 *     issue by using an iframe instead of a div for bg element.
 * @param {goog.dom.DomHelper} opt_domHelper Optional DOM helper; see {@link
 *    goog.ui.Component} for semantics.

 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog =
  function(opt_class, opt_useIframeMask, opt_domHelper) {
  goog.ui.Dialog.call(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog, goog.ui.Dialog);

/** @inheritDoc */
bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog.prototype.createDom =
  function() {
  var elem = goog.soy.renderAsElement(
    bluemind.calendar.event.template.updateReccurringPartWarningDialog);
  this.decorateInternal(elem);
};

/** action **/
bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog.prototype.action_;

/** participation status **/
bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog.prototype.partStat_;

/** notification **/
bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog.prototype.notification_;

/** note **/
bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog.prototype.note_;

/*
 * set the action to perform
 */
bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog.prototype.setAction = function(action) {
  this.action_ = action;
};

/*
 * set the part stat
 */
bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog.prototype.setPartStat = function(partStat) {
  this.partStat_ = partStat;
};

/*
 * set the notification
 */
bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog.prototype.setNotification = function(notification) {
  this.notification_ = notification;
};

/*
 * set the note:
 */
bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog.prototype.setNote = function(note) {
  this.note_ = note;
};

/** @inheritDoc */
bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(goog.dom.getElement('warning-btn-yes'),
    goog.events.EventType.CLICK, this.confirm_);

};

/**
 * Yay
 * @private
 */
bluemind.calendar.ui.event.UpdateReccurringPartChangeDialog.prototype.confirm_ = function() {
  this.action_(this.getModel(), this.partStat_, this.notification_, this.note_);
};

