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
 * @fileoverview A input control. This implementation extends {@link
 * goog.ui.Control}.
 *
 */

goog.provide('bluemind.ui.Input');

goog.require('bluemind.ui.style.InputRenderer');
goog.require('goog.events.KeyCodes');
goog.require('goog.ui.Control');
goog.require('goog.ui.ControlContent');
goog.require('goog.ui.registry');



/**
 * A input control, rendered as a native browser input by default.
 *
 * @param {goog.ui.ControlContent} content Text caption or existing DOM
 *     structure to display as the input's label.
 * @param {string=} opt_value Input value
 * @param {bluemind.ui.style.InputRenderer=} opt_renderer Renderer used to render or
 *     decorate the input; defaults to {@link bluemind.ui.style.InputRenderer}.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM hepler, used for
 *     document interaction.
 * @constructor
 * @extends {goog.ui.Control}
 */
bluemind.ui.Input = function(content, opt_value, opt_renderer, opt_domHelper) {
  content = content || '';
  var renderer =  opt_renderer || bluemind.ui.style.InputRenderer.getInstance();
  goog.base(this, content, renderer, opt_domHelper);
  this.setModel(opt_value);
  this.setAllowTextSelection(true);
};
goog.inherits(bluemind.ui.Input, goog.ui.Control);


/**
 * Tooltip text for the input, displayed on hover.
 * @type {string|undefined}
 * @private
 */
bluemind.ui.Input.prototype.tooltip_;


/**
 * Sets the input's value.
 * @param {*} value The value property for the input, will be cast to a
 *     string by the browser when setting input.value.
 */
bluemind.ui.Input.prototype.setValue = function(value) {
  this.getRenderer().setValue(this.getElement(), String(value));
  this.setValueInternal(String(value));
};

/**
 * Sets the value associated with the input.  Unlike {@link #setValue},
 * doesn't update the input's DOM.  Considered protected; to be called only
 * by renderer code during element decoration.
 * @param {*} value New input label.
 * @protected
 */
bluemind.ui.Input.prototype.setValueInternal = function(value) {
  this.setModel(String(value));
};

/**
 * Gets the input's value.
 * @return {string} value The value of the input.
 */
bluemind.ui.Input.prototype.getValue = function() {
  return /** @type {string } */ (this.getModel());
};

/**
 * Returns the tooltip for the input.
 * @return {string|undefined} Tooltip text (undefined if none).
 */
bluemind.ui.Input.prototype.getTooltip = function() {
  return this.tooltip_;
};


/**
 * Sets the tooltip for the input, and updates its DOM.
 * @param {string} tooltip New tooltip text.
 */
bluemind.ui.Input.prototype.setTooltip = function(tooltip) {
  this.setTooltipInternal(tooltip);
  this.getRenderer().setTooltip(this.getElement(), tooltip);
};


/**
 * Sets the tooltip for the input.  Unlike {@link #setTooltip}, doesn't update
 * the input's DOM.  Considered protected; to be called only by renderer code
 * during element decoration.
 * @param {string} tooltip New tooltip text.
 * @protected
 */
bluemind.ui.Input.prototype.setTooltipInternal = function(tooltip) {
  this.tooltip_ = tooltip;
};


/**
 * Sets the placeholder for the input, and updates its DOM.
 * @param {string} placeholder New tooltip text.
 */
bluemind.ui.Input.prototype.setPlaceholder = function(placeholder) {
  this.getChild('input').setLabel(placeholder);
};


/** @override */
bluemind.ui.Input.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getKeyEventTarget(), goog.events.EventType.KEYUP, this.handleKeyUpEvent);
};


/**
 * Attempts to handle a keyboard event; Dispatch changed if the content value have changed.
 * @param {goog.events.Event} e Key event to handle.
 * @protected
 */
bluemind.ui.Input.prototype.handleKeyUpEvent = function(e) {
  var value = this.getRenderer().getValue(this.getElement());
  if (this.getValue() != value) {
    this.setValueInternal(value);
    this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
  }
};

/** @override */
bluemind.ui.Input.prototype.handleMouseUp = function(e) {
  if (this.isEnabled()) {
    if (this.isAutoState(goog.ui.Component.State.HOVER)) {
      this.setHighlighted(true);
    }
  }
};

bluemind.ui.Input.prototype.setEnabled = function(enable) {
  goog.base(this, 'setEnabled', enable);
  this.getChild('input').setEnabled(enable);
}

// Register a decorator factory function for bluemind.ui.Inputs.
goog.ui.registry.setDecoratorByClassName(bluemind.ui.style.InputRenderer.CSS_CLASS,
    function() {
      return new bluemind.ui.Input(null);
    });
