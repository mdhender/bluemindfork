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

goog.provide('bluemind.calendar.ui.event.RecurringUpdateDialog');

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
bluemind.calendar.ui.event.RecurringUpdateDialog =
  function(opt_class, opt_useIframeMask, opt_domHelper) {
  goog.ui.Dialog.call(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(
  bluemind.calendar.ui.event.RecurringUpdateDialog, goog.ui.Dialog);

/** @inheritDoc */
bluemind.calendar.ui.event.RecurringUpdateDialog.prototype.createDom =
  function() {
  var elem = goog.soy.renderAsElement(
    bluemind.calendar.event.template.recurringUpdateDialog);
  this.decorateInternal(elem);
};

/** @inheritDoc */
bluemind.calendar.ui.event.RecurringUpdateDialog.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(goog.dom.getElement('rud-btn-this-instance'),
    goog.events.EventType.CLICK,
    this.updateInstance_, false, this);

  this.getHandler().listen(goog.dom.getElement('rud-btn-all-the-following'),
    goog.events.EventType.CLICK,
    this.updateFollowing_, false, this);

  this.getHandler().listen(goog.dom.getElement('rud-btn-update-serie'),
    goog.events.EventType.CLICK,
    this.updateSerie_, false, this);

  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT,
    this.cancelUpdate_, false, this);
};

/** @inheritDoc */
bluemind.calendar.ui.event.RecurringUpdateDialog.prototype.setModel = function(m) {
  goog.base(this, 'setModel', m); 
  var evt = this.getModel();
  var tr = goog.dom.getElement('rud-tr-update-serie');
  if (!evt.getInitialDate().equals(evt.getDate())) {
    tr.setAttribute('style', 'display:none;');
  } else {
    tr.setAttribute('style', 'display:table-row;');
  }
};

/**
 * update this instance
 * @private
 */
bluemind.calendar.ui.event.RecurringUpdateDialog.prototype.updateInstance_ =
  function() {
  if (this.getModel().onlyMyselfAsAttendee()) {
    bluemind.calendar.Controller.getInstance().updateInstance(this.getModel());
    this.setVisible(false);
  } else {
    this.setVisible(false);
    bluemind.calendar.Controller.getInstance().eventSendUpdateDialog(
      this.getModel(),
      bluemind.calendar.Controller.getInstance().updateInstance);
  }
};

/**
 * update following
 * @private
 */
bluemind.calendar.ui.event.RecurringUpdateDialog.prototype.updateFollowing_ =
  function() {
  if (this.getModel().onlyMyselfAsAttendee()) {
    bluemind.calendar.Controller.getInstance().updateFollowing(this.getModel());
    this.setVisible(false);
  } else {
    this.setVisible(false);
    bluemind.calendar.Controller.getInstance().eventSendUpdateDialog(
      this.getModel(),
      bluemind.calendar.Controller.getInstance().updateFollowing);
  }
};

/**
 * update serie
 * @private
 */
bluemind.calendar.ui.event.RecurringUpdateDialog.prototype.updateSerie_ =
  function() {

  // Quick update from UI, update series, DO NOT TOUCH event start date
  // Only duration or title can be changed here
  bluemind.calendar.Controller.getInstance().getEventFromUid(
    this.getModel().getExtId())
    .addCallback(function(orig) {
    orig.setDuration(this.getModel().getDuration());
    orig.setTitle(this.getModel().getTitle());

    if (this.getModel().onlyMyselfAsAttendee()) {
      bluemind.calendar.Controller.getInstance().updateSeries(orig);
      this.setVisible(false);
    } else {
      this.setVisible(false);
      bluemind.calendar.Controller.getInstance().eventSendUpdateDialog(orig,
        bluemind.calendar.Controller.getInstance().updateSeries);
    }
  }, this);
};

/**
 * Cancel update
 * @param {goog.ui.Dialog.Event} e dialog event.
 * @private
 */
bluemind.calendar.ui.event.RecurringUpdateDialog.prototype.cancelUpdate_ =
  function(e) {
  if (e.key == 'cancel') {
    bluemind.calendar.Controller.getInstance().cancelUpdate(this.getModel());
  }
};
