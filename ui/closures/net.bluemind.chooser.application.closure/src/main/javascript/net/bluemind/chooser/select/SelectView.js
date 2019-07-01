/* BEGIN LICENSE
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
 * @fileoverview View class for application header (Logo + logo).
 */

goog.provide('net.bluemind.chooser.select.SelectView');

goog.require('bluemind.ui.style.PrimaryActionButtonRenderer');
goog.require('goog.dom');
goog.require('goog.dom.classlist');
goog.require('goog.events.KeyCodes');
goog.require('goog.events.KeyHandler');
goog.require('goog.events.KeyHandler.EventType');
goog.require('goog.ui.Button');
goog.require('goog.ui.Component');
goog.require('goog.ui.Component.EventType');
goog.require('goog.ui.LabelInput');
goog.require('goog.ui.style.app.ButtonRenderer');



/**
 * View class for application search form.
 *
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.chooser.select.SelectView = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  var terms = [];
  /** @meaning general.select */
  var MSG_SELECT = goog.getMsg('Select');
  var child = new goog.ui.Button(MSG_SELECT, bluemind.ui.style.PrimaryActionButtonRenderer.getInstance());
  child.setId('select');
  this.addChild(child, true);
  /** @meaning general.cancel */
  var MSG_CANCEL = goog.getMsg('Cancel');
  child = new goog.ui.Button(MSG_CANCEL, goog.ui.style.app.ButtonRenderer.getInstance());
  child.setId('cancel');
  this.addChild(child, true);

};
goog.inherits(net.bluemind.chooser.select.SelectView, goog.ui.Component);


/** @override */
net.bluemind.chooser.select.SelectView.prototype.createDom = function() {
  var el = this.getDomHelper().createDom('div', goog.getCssName('footer'));
  this.setElementInternal(el);
};
