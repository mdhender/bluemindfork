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
 * @fileoverview Recurring event deletion dialog component.
 */

goog.provide('bluemind.calendar.ui.event.RecurringDeleteDialog');

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
bluemind.calendar.ui.event.RecurringDeleteDialog =
  function(opt_class, opt_useIframeMask, opt_domHelper) {
  goog.ui.Dialog.call(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(
  bluemind.calendar.ui.event.RecurringDeleteDialog, goog.ui.Dialog);

/** @inheritDoc */
bluemind.calendar.ui.event.RecurringDeleteDialog.prototype.createDom =
  function() {
  var elem = goog.soy.renderAsElement(
    bluemind.calendar.event.template.recurringDeleteDialog);
  this.decorateInternal(elem);
};

/** @inheritDoc */
bluemind.calendar.ui.event.RecurringDeleteDialog.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(goog.dom.getElement('rdd-btn-this-instance'),
    goog.events.EventType.CLICK,
    this.deleteInstance_, false, this);

  this.getHandler().listen(goog.dom.getElement('rdd-btn-all-the-following'),
    goog.events.EventType.CLICK,
    this.deleteFollowing_, false, this);

  this.getHandler().listen(goog.dom.getElement('rdd-btn-delete-serie'),
    goog.events.EventType.CLICK,
    this.deleteSerie_, false, this);

};

/**
 * delete this instance
 * @private
 */
bluemind.calendar.ui.event.RecurringDeleteDialog.prototype.deleteInstance_ =
  function() {
  bluemind.calendar.Controller.getInstance().
    insertEventException(this.getModel(), 'delete_occurrence');
  this.setVisible(false);
};

/**
 * delete following
 * @private
 */
bluemind.calendar.ui.event.RecurringDeleteDialog.prototype.deleteFollowing_ =
  function() {
  bluemind.calendar.Controller.getInstance().deleteFollowing(this.getModel());
  this.setVisible(false);
};

/**
 * delete serie
 * @private
 */
bluemind.calendar.ui.event.RecurringDeleteDialog.prototype.deleteSerie_ =
  function() {
  bluemind.calendar.Controller.getInstance().removeEvent(this.getModel());
  this.setVisible(false);
};
