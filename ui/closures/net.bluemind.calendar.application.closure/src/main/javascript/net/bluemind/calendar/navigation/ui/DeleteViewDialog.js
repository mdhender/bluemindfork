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

goog.provide('net.bluemind.calendar.navigation.ui.DeleteViewDialog');

goog.require('goog.ui.Dialog');
goog.require('net.bluemind.calendar.navigation.events.EventType');
goog.require('net.bluemind.calendar.navigation.templates');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper; see {@link
 *          goog.ui.Component} for semantics.
 * @constructor
 * @extends {goog.ui.Dialog}
 */
net.bluemind.calendar.navigation.ui.DeleteViewDialog = function(opt_domHelper) {
  goog.base(this, undefined, undefined, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(net.bluemind.calendar.navigation.ui.DeleteViewDialog, goog.ui.Dialog);

/** @override */
net.bluemind.calendar.navigation.ui.DeleteViewDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(net.bluemind.calendar.navigation.templates.deleteviewdialog);
  this.decorateInternal(elem);
};

/** @override */
net.bluemind.calendar.navigation.ui.DeleteViewDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getDomHelper().getElement('dvd-btn-ok'), goog.events.EventType.CLICK, this.delete_);
};

/**
 * delete current view
 * 
 * @private
 */
net.bluemind.calendar.navigation.ui.DeleteViewDialog.prototype.delete_ = function() {
  this.dispatchEvent(new net.bluemind.calendar.navigation.events.DeleteViewEvent(this.getModel()));
};
