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
 * @fileoverview Event deletion dialog component.
 */

goog.provide('bluemind.calendar.ui.event.PartStatNotificationDialog');

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
bluemind.calendar.ui.event.PartStatNotificationDialog =
  function(opt_class, opt_useIframeMask, opt_domHelper) {
  goog.ui.Dialog.call(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(bluemind.calendar.ui.event.PartStatNotificationDialog,
  goog.ui.Dialog);

/**
 * @param {text} part stat.
 * @private
 */
bluemind.calendar.ui.event.PartStatNotificationDialog.prototype.partStat_;

/**
 * Set participation status
 * @param {text} ps stat.
 */
bluemind.calendar.ui.event.PartStatNotificationDialog.prototype.setPartStat =
  function(ps) {
  this.partStat_ = ps;
};

/**
 * Get participation status
 * @return {text} part stat.
 */
bluemind.calendar.ui.event.PartStatNotificationDialog.prototype.getPartStat =
  function() {
  return this.partStat_;
};

/** @inheritDoc */
bluemind.calendar.ui.event.PartStatNotificationDialog.prototype.createDom =
  function() {
  var elem = goog.soy.renderAsElement(
    bluemind.calendar.event.template.partStatNotificationDialog);
  this.decorateInternal(elem);
};

/** @inheritDoc */
bluemind.calendar.ui.event.PartStatNotificationDialog.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(goog.dom.getElement('psn-btn-send'),
    goog.events.EventType.CLICK,
    this.send_, false, this);
};

/**
 * Clear textarea
 */
bluemind.calendar.ui.event.PartStatNotificationDialog.prototype.clear =
  function() {
  goog.dom.getElement('part-stat-note').value = '';
};

/**
 * Send the custom response.
 * @private
 */
bluemind.calendar.ui.event.PartStatNotificationDialog.prototype.send_ =
  function() {
  var note = goog.dom.forms.getValue(goog.dom.getElement('part-stat-note'));
  var isReccurring = this.getModel().getRepeatKind() != 'none';
  if (isReccurring) {
    bluemind.calendar.Controller.getInstance().updateReccurringPartChangeDialog(this.getModel(),
      bluemind.calendar.Controller.getInstance().setParticipation,
      this.partStat_, true, note);
  } else {
    bluemind.calendar.Controller.getInstance().setParticipation(
      this.getModel(), this.partStat_, true, note);

  }
};
