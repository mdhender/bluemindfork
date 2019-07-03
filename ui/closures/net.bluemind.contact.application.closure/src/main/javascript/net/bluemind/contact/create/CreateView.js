/*
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
goog.provide("net.bluemind.contact.create.CreateView");

goog.require("goog.dom.classlist");
goog.require("goog.ui.Menu");
goog.require("goog.ui.MenuItem");
goog.require("net.bluemind.ui.ComboButton");
goog.require("bluemind.ui.style.PrimaryActionButtonRenderer");

/**
 * @constructor
 * 
 * @param {*} menu
 * @param {goog.ui.ButtonRenderer} opt_renderer
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {net.bluemind.ui.ComboButton}
 */
net.bluemind.contact.create.CreateView = function(menu, opt_domHelper) {
  var menu = new goog.ui.Menu();
  /** @meaning contact.contact.new */
  var MSG_NEW_CONTACT = goog.getMsg('New contact');
  var button = new goog.ui.MenuItem(MSG_NEW_CONTACT);
  menu.addChild(button, true);
  button.setId('individual');
  /** @meaning contact.distributionlist.new */
  var MSG_NEW_DISTRIBUTION_LIST = goog.getMsg('New distribution list')
  button = new goog.ui.MenuItem(MSG_NEW_DISTRIBUTION_LIST);
  menu.addChild(button, true);
  button.setId('group');
  goog.base(this, menu, bluemind.ui.style.PrimaryActionButtonRenderer.getInstance(), opt_domHelper);
  this.add
}
goog.inherits(net.bluemind.contact.create.CreateView, net.bluemind.ui.ComboButton);

/** @override */
net.bluemind.contact.create.CreateView.prototype.createDom = function() {
  goog.base(this, 'createDom');
};
