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

/**
 * @fileoverview A combo button control.
 * 
 */

goog.provide("net.bluemind.ui.ComboButton");

goog.require("goog.dom.classlist");
goog.require("goog.positioning.Corner");
goog.require("goog.positioning.MenuAnchoredPosition");
goog.require("goog.ui.Button");
goog.require("goog.ui.ButtonSide");
goog.require("goog.ui.Component");
goog.require("goog.ui.MenuButton");
goog.require("goog.ui.style.app.ButtonRenderer");
goog.require("goog.ui.style.app.MenuButtonRenderer");

/**
 * A menu button control. Extends {@link goog.ui.Button} by composing a button
 * with a dropdown arrow and a popup menu.
 * 
 * @param {goog.ui.ControlContent} content Text caption or existing DOM
 *          structure to display as the button's caption (if any).
 * @param {goog.ui.Menu=} opt_menu Menu to render under the button when clicked.
 * @param {goog.ui.ButtonRenderer=} opt_renderer Renderer used to render or
 *          decorate the menu button; defaults to
 *          {@link goog.ui.MenuButtonRenderer}.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM hepler, used for
 *          document interaction.
 * @constructor
 * @extends {goog.ui.Button}
 */
net.bluemind.ui.ComboButton = function(menu, opt_renderer, opt_domHelper) {
  goog.base(this, opt_domHelper);
  var renderer = opt_renderer || goog.ui.style.app.ButtonRenderer.getInstance();
  var button = new goog.ui.Button(menu.getChildAt(0).getContent(), renderer);
  button.setId('button');
  this.addChild(button, true);
  button.setCollapsed(goog.ui.ButtonSide.END);

  renderer = goog.ui.style.app.MenuButtonRenderer.getInstance();
  var menu = new goog.ui.MenuButton(null, menu, renderer);
  menu.setId('menu');
  menu.setCollapsed(goog.ui.ButtonSide.START);
  if (opt_renderer) {
    menu.addClassName(opt_renderer.getCssClass());
  }
  this.addChild(menu, true);
  menu.setMenuPosition(new goog.positioning.MenuAnchoredPosition(this.getElement(),
      goog.positioning.Corner.BOTTOM_START));
};
goog.inherits(net.bluemind.ui.ComboButton, goog.ui.Component);

/** @override */
net.bluemind.ui.ComboButton.prototype.createDom = function() {
  var el = this.getDomHelper().createDom('div', [goog.getCssName('goog-combo-button'), goog.getCssName('goog-inline-block')],
    this.getDomHelper().createDom('div', goog.getCssName('goog-combo-button-inner-box')));
  this.setElementInternal(el)
};

/** @override */
net.bluemind.ui.ComboButton.prototype.getContentElement = function() {
  return this.getElementByClass(goog.getCssName('goog-combo-button-inner-box'));
};
