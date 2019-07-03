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

goog.provide('bluemind.calendar.ui.event.RecurringFormDialog');

goog.require('bluemind.calendar.event.template');
goog.require('bluemind.calendar.model.EventHome');
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
bluemind.calendar.ui.event.RecurringFormDialog =
  function(opt_class, opt_useIframeMask, opt_domHelper) {
  goog.ui.Dialog.call(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(
  bluemind.calendar.ui.event.RecurringFormDialog, goog.ui.Dialog);

/** @inheritDoc */
bluemind.calendar.ui.event.RecurringFormDialog.prototype.createDom =
  function() {
  var elem = goog.soy.renderAsElement(
    bluemind.calendar.event.template.recurringFormDialog);
  this.decorateInternal(elem);
};

/** @inheritDoc */
bluemind.calendar.ui.event.RecurringFormDialog.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(goog.dom.getElement('rfd-btn-this-instance'),
    goog.events.EventType.CLICK,
    this.updateInstance_, false, this);

  this.getHandler().listen(goog.dom.getElement('rfd-btn-update-serie'),
    goog.events.EventType.CLICK,
    this.updateSerie_, false, this);

};

/**
 * update this instance
 * @private
 */
bluemind.calendar.ui.event.RecurringFormDialog.prototype.updateInstance_ =
  function() {
  bluemind.view.updateEventForm(this.getModel());
  this.setVisible(false);
};

/**
 * update serie
 * @private
 */
bluemind.calendar.ui.event.RecurringFormDialog.prototype.updateSerie_ =
  function() {
  bluemind.calendar.model.EventHome.getInstance()
    .get(this.getModel().getExtId()).addCallback(function(evt) {
    bluemind.view.updateEventForm(evt);
    this.setVisible(false);
  }, this).addErrback(function(err) {
    this.setVisible(false);
  }, this);
};

