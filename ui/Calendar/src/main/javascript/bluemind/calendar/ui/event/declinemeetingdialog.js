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
 * @fileoverview Meeting decline (silently) dialog component.
 */

goog.provide('bluemind.calendar.ui.event.DeclineMeetingDialog');

goog.require('bluemind.calendar.event.template');
goog.require('goog.ui.Dialog');

/**
 * @param {boolean} opt_recurring is event recurring.

 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.event.DeclineMeetingDialog =
  function(opt_recurring) {
  goog.ui.Dialog.call(this);
  this.recurring_ = !!opt_recurring;
  this.setDraggable(false);
};
goog.inherits(bluemind.calendar.ui.event.DeclineMeetingDialog, goog.ui.Dialog);

/** @inheritDoc */
bluemind.calendar.ui.event.DeclineMeetingDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(
    bluemind.calendar.event.template.declineMeetingDialog, {warning: this.recurring_});
  this.decorateInternal(elem);
};

/** @inheritDoc */
bluemind.calendar.ui.event.DeclineMeetingDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(goog.dom.getElement('dm-btn-ok'),
    goog.events.EventType.CLICK,
    this.decline_, false, this);
};

/**
 * decline the meeting 
 * @private
 */
bluemind.calendar.ui.event.DeclineMeetingDialog.prototype.decline_ = function() {
  bluemind.calendar.Controller.getInstance().setParticipation(
    this.getModel(), 'DECLINED', false);
};
