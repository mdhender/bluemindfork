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
goog.provide("net.bluemind.ui.Link");

goog.require("goog.events.EventType");
goog.require("goog.events.KeyCodes");
goog.require("goog.events.KeyHandler.EventType");
goog.require("goog.ui.Control");
goog.require("goog.ui.Component.State");
goog.require("net.bluemind.ui.ButtonLinkRenderer");

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent} opt_content
 * @param {goog.ui.ControlRenderer} opt_renderer
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {goog.ui.Control}
 */
net.bluemind.ui.Link = function(opt_content, opt_renderer, opt_domHelper) {
  goog.ui.Control.call(this, opt_content, opt_renderer || net.bluemind.ui.ButtonLinkRenderer.getInstance(),
      opt_domHelper);
}
goog.inherits(net.bluemind.ui.Link, goog.ui.Control);

/**
 * Href associated with the link.
 * 
 * @type {goog.Uri}
 * @private
 */
net.bluemind.ui.Link.prototype.href_;

/**
 * Tooltip text for the link, displayed on hover.
 * 
 * @type {string|undefined}
 * @private
 */
net.bluemind.ui.Link.prototype.tooltip_;

/**
 * Returns the href associated with the link.
 * 
 * @return {goog.Uri} Link href (undefined if none).
 */
net.bluemind.ui.Link.prototype.getHref = function() {
  return this.href_;
};

/**
 * Sets the href associated with the link's, and updates its DOM.
 * 
 * @param {goog.Uri} href New link's href.
 */
net.bluemind.ui.Link.prototype.setHref = function(href) {
  this.href_ = href;
  var renderer = this.getRenderer();
  renderer.setHref(this.getElement(), href.toString());
};

/**
 * Sets the href associated with the link's. Unlike {@link #setHref}, doesn't
 * update the link's DOM. Considered protected; to be called only by renderer
 * code during element decoration.
 * 
 * @param {goog.Uri} href New link's href.
 * @protected
 */
net.bluemind.ui.Link.prototype.setValueInternal = function(href) {
  this.href_ = href;
};

/**
 * Returns the tooltip for the link.
 * 
 * @return {string|undefined} Tooltip text (undefined if none).
 */
net.bluemind.ui.Link.prototype.getTooltip = function() {
  return this.tooltip_;
};

/**
 * Sets the tooltip for the link, and updates its DOM.
 * 
 * @param {string} tooltip New tooltip text.
 */
net.bluemind.ui.Link.prototype.setTooltip = function(tooltip) {
  this.tooltip_ = tooltip;
  this.getRenderer().setTooltip(this.getElement(), tooltip);
};

/**
 * Sets the tooltip for the link. Unlike {@link #setTooltip}, doesn't update
 * the link's DOM. Considered protected; to be called only by renderer code
 * during element decoration.
 * 
 * @param {string} tooltip New tooltip text.
 * @protected
 */
net.bluemind.ui.Link.prototype.setTooltipInternal = function(tooltip) {
  this.tooltip_ = tooltip;
};

/** @override */
net.bluemind.ui.Link.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  delete this.href_;
  delete this.tooltip_;
};

/** @override */
net.bluemind.ui.Link.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  if (this.isSupportedState(goog.ui.Component.State.FOCUSED)) {
    var keyTarget = this.getKeyEventTarget();
    if (keyTarget) {
      this.getHandler().listen(keyTarget, goog.events.EventType.KEYUP, this.handleKeyEventInternal);
    }
  }
};

/**
 * Attempts to handle a keyboard event; returns true if the event was handled,
 * false otherwise. If the link is enabled and the Enter/Space key was pressed,
 * handles the event by dispatching an {@code ACTION} event, and returns true.
 * Overrides {@link goog.ui.Control#handleKeyEventInternal}.
 * 
 * @param {goog.events.KeyEvent} e Key event to handle.
 * @return {boolean} Whether the key event was handled.
 * @protected
 * @override
 */
net.bluemind.ui.Link.prototype.handleKeyEventInternal = function(e) {
  if (e.keyCode == goog.events.KeyCodes.ENTER && e.type == goog.events.KeyHandler.EventType.KEY
      || e.keyCode == goog.events.KeyCodes.SPACE && e.type == goog.events.EventType.KEYUP) {
    return this.performActionInternal(e);
  }
  // Return true for space keypress (even though the event is handled on keyup)
  // as preventDefault needs to be called up keypress to take effect in IE and
  // WebKit.
  return e.keyCode == goog.events.KeyCodes.SPACE;
};
