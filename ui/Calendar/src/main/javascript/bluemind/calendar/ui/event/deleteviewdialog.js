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
 * @fileoverview View deletion dialog component.
 */

goog.provide('bluemind.calendar.ui.event.DeleteViewDialog');

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
bluemind.calendar.ui.event.DeleteViewDialog =
  function(opt_class, opt_useIframeMask, opt_domHelper) {
  goog.ui.Dialog.call(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(bluemind.calendar.ui.event.DeleteViewDialog, goog.ui.Dialog);

/** @inheritDoc */
bluemind.calendar.ui.event.DeleteViewDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(
    bluemind.calendar.event.template.deleteViewDialog);
  this.decorateInternal(elem);
};

/** @inheritDoc */
bluemind.calendar.ui.event.DeleteViewDialog.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(goog.dom.getElement('dvd-btn-ok'),
    goog.events.EventType.CLICK,
    this.delete_, false, this);
};

/**
 * delete current view
 * @private
 */
bluemind.calendar.ui.event.DeleteViewDialog.prototype.delete_ = function() {
  var item = bluemind.view.getViewSelector().getSelected();
  bluemind.calendar.Controller.getInstance().deleteView(item);
};
