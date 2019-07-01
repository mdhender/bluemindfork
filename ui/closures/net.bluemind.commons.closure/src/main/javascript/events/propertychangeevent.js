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
 * @fileoverview
 * Easy api to observe object changes and act according to that change.
 * Usefull for a beanlike view update (when model property changes, the ui
 * is updated)
 */

goog.provide('bluemind.events.PropertyChangeEvent');

goog.require('goog.events.Event');
goog.require('goog.events.EventType');

/**
 * Object representing a property change event. 
 * @param {string} property Property name. What that property mean and how to
 *   read it is the work of the listener
 * @param {*} oldValue Value before change. 
 * @param {*} newValue Value after change.
 * @constructor
 * @extends {goog.events.Event}
 */
bluemind.events.PropertyChangeEvent = function(property, oldValue, newValue) {
  goog.base(this, goog.events.EventType.PROPERTYCHANGE);
  this.property_ = property;
  this.oldValue_ = oldValue;
  this.newValue_ = newValue;
};

goog.inherits(bluemind.events.PropertyChangeEvent, goog.events.Event);

/**
 * Changed property.
 * @type {string}
 */
bluemind.events.PropertyChangeEvent.prototype.property_;

/**
 * Value of the property after change
 * @type {*}
 */
bluemind.events.PropertyChangeEvent.prototype.newValue_;

/**
 * Value of the property before change
 * @type {*}
 */
bluemind.events.PropertyChangeEvent.prototype.oldValue_;

/**
 * Return the changed property
 * return {string} Changed property.
 */
bluemind.events.PropertyChangeEvent.prototype.getProperty = function() {
  return this.property_;
};

/**
 * Return the value of the property before change
 * return {*} the value of the property before change.
 */
bluemind.events.PropertyChangeEvent.prototype.getOldValue = function() {
  return this.oldValue_;
};

/**
 * Return the value of the property adter change
 * return {*} the value of the property after change.
 */
bluemind.events.PropertyChangeEvent.prototype.getNewValue = function() {
  return this.newValue_;
};
