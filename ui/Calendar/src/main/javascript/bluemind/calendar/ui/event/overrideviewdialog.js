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
 * @fileoverview save view dialog component.
 */

goog.provide('bluemind.calendar.ui.event.OverrideViewDialog');

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
bluemind.calendar.ui.event.OverrideViewDialog =
  function(opt_class, opt_useIframeMask, opt_domHelper) {
  goog.ui.Dialog.call(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(bluemind.calendar.ui.event.OverrideViewDialog, goog.ui.Dialog);

/** @inheritDoc */
bluemind.calendar.ui.event.OverrideViewDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(
    bluemind.calendar.event.template.overrideViewDialog);
  this.decorateInternal(elem);
};

/**
 * View label
 * @type {text}
 * @private
 */
bluemind.calendar.ui.event.OverrideViewDialog.prototype.item_;

/** @inheritDoc */
bluemind.calendar.ui.event.OverrideViewDialog.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(goog.dom.getElement('view-override-btn-ok'),
    goog.events.EventType.CLICK, function(e) {
      e.stopPropagation();
      this.save_();
    }, false, this);

  this.getHandler().listen(goog.dom.getElement('view-override-btn-cancel'),
    goog.events.EventType.CLICK, function(e) {
      e.stopPropagation();
      this.cancel_();
    }, false, this);
};

/**
 * @param {Object} item to set.
 */
bluemind.calendar.ui.event.OverrideViewDialog.prototype.setItem =
  function(item) {
  this.item_ = item;
};

/** @inheritDoc */
bluemind.calendar.ui.event.OverrideViewDialog.prototype.setVisible =
  function(visible) {
  if (visible) {
    var lbl = goog.dom.getElement('view-override-label');
    lbl.innerHTML =
      bluemind.calendar.template.i18n.overrideViewMsg(
        {v: this.item_.getModel().getLabel()});
  }
  bluemind.calendar.ui.event.OverrideViewDialog.superClass_.setVisible.call(
    this, visible);
};

/**
 * save current view
 * @private
 */
bluemind.calendar.ui.event.OverrideViewDialog.prototype.save_ = function() {
  bluemind.calendar.Controller.getInstance().updateView(this.item_);
  this.setVisible(false);
};

/**
 * back to new view dialog
 * @private
 */
bluemind.calendar.ui.event.OverrideViewDialog.prototype.cancel_ = function() {
  var l = this.item_.getModel().getLabel();
  this.setVisible(false);
  bluemind.calendar.Controller.getInstance().saveViewDialog(l);
};

