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
 * @fileoverview Event creation bubble graphic componnent.
 */

goog.provide('net.bluemind.calendar.vevent.ui.EventDetailsPopup');

goog.require('goog.dom');
goog.require('goog.events');
goog.require('goog.positioning');
goog.require('goog.positioning.AbsolutePosition');
goog.require('goog.positioning.AnchoredPosition');
goog.require('goog.positioning.Corner');
goog.require('goog.soy');
goog.require('goog.style');
goog.require('goog.ui.Component');
goog.require('goog.ui.Popup');
goog.require('goog.ui.PopupBase.EventType');
goog.require('net.bluemind.calendar.vevent.ui.templates');

/**
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.vevent.ui.EventDetailsPopup = function() {
  goog.base(this);

  this.popup_ = new goog.ui.Popup();
  this.popup_.setHideOnEscape(true);
  this.popup_.setAutoHide(true);
  this.popup_.setVisible(false);
  this.drawn_ = false;

};
goog.inherits(net.bluemind.calendar.vevent.ui.EventDetailsPopup, goog.ui.Component);

/**
 * The Popup element used to position and display the bubble.
 * 
 * @type {goog.ui.Popup}
 * @private
 */
net.bluemind.calendar.vevent.ui.EventDetailsPopup.prototype.popup_;

/** @override */
net.bluemind.calendar.vevent.ui.EventDetailsPopup.prototype.createDom = function() {
  var element = goog.soy.renderAsElement(net.bluemind.calendar.vevent.ui.templates.details, {
    events : this.getModel()
  });
  element.style.position = 'absolute';
  this.setElementInternal(element);
  this.popup_.setElement(element);
};

/** @override */
net.bluemind.calendar.vevent.ui.EventDetailsPopup.prototype.buildContent = function() {
  return goog.soy.renderAsElement(net.bluemind.calendar.vevent.ui.templates.details, {
    events : this.getModel()
  });
};

/** @override */
net.bluemind.calendar.vevent.ui.EventDetailsPopup.prototype.setModel = function(obj) {
  var model = this.getModel();
  if (model != null) {
    this.setVisible(false);
  }
  goog.base(this, 'setModel', obj);
};

/**
 * Unset all listener
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.EventDetailsPopup.prototype.unsetListeners_ = function() {
  this.getHandler().removeAll();
};

/**
 * Set listener
 */
net.bluemind.calendar.vevent.ui.EventDetailsPopup.prototype.setListeners = function() {
  // this.getHandler().listen(this.popup_, goog.ui.PopupBase.EventType.HIDE,
  // this.hide);
};

/**
 * Attaches the bubble to an anchor element. Computes the positioning and
 * orientation of the bubble. FIXME : Should be replaced by a
 * AnchorViewportPosition, but the AnchorViewportPositon does not tell the
 * coputed corner where the popup will be placed. So it's not possible to
 * correctly set the arrow.
 * 
 * @param {Element} anchor The element to which we are attaching.
 */
net.bluemind.calendar.vevent.ui.EventDetailsPopup.prototype.attach = function(event) {
  var position = this.computePosition_(event);
  this.popup_.setPosition(position);
};

/**
 * Turn the bubble visibility on or off.
 * 
 * @param {boolean} visible Desired visibility state.
 */
net.bluemind.calendar.vevent.ui.EventDetailsPopup.prototype.setVisible = function(visible) {
  if (visible && !this.drawn_) {
    this.setListeners();
    this.drawn_ = true;
  }
  this.popup_.setVisible(visible);
};

/**
 * Alias to setVisible(false)
 */
net.bluemind.calendar.vevent.ui.EventDetailsPopup.prototype.hide = function() {
  this.setVisible(false);
  this.dispatchEvent(goog.ui.PopupBase.EventType.HIDE);
};

/**
 * Computes position for the bubble.
 * 
 * @param {goog.event.Event} event
 * @return {goog.positioning.AbsolutePosition} position.
 * @private
 */
net.bluemind.calendar.vevent.ui.EventDetailsPopup.prototype.computePosition_ = function(event) {
  return new goog.positioning.AbsolutePosition(event.event_.pageX, event.event_.pageY);
};
