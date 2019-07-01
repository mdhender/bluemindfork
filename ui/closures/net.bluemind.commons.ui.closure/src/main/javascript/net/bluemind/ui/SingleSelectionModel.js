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
 * @fileoverview A simple selection model that allows only one item to be
 *               selected a a time.
 */

goog.provide('net.bluemind.ui.SingleSelectionModel');

goog.require('net.bluemind.ui.SelectionModel');

/**
 * Handle single selection within a list. Dispatches a
 * {@link goog.events.EventType.SELECT} event when a selection is changed. Allow
 * void selection.
 * 
 * @param {function(Object): *=} opt_provider Optional function to extract key
 *                from object.
 * @extends {net.bluemind.ui.SelectionModel}
 * @constructor
 */
net.bluemind.ui.SingleSelectionModel = function(opt_provider) {
  goog.base(this, opt_provider);
};
goog.inherits(net.bluemind.ui.SingleSelectionModel, net.bluemind.ui.SelectionModel);

/** @override */
net.bluemind.ui.SingleSelectionModel.prototype.selectInternal = function(key, object, selected) {
  this.selection.forEach(function(item) {
    this.selectItem(item, false);
  }, this)
  this.selection.clear();
  if (selected) {
    this.selection.set(key, object);
  }
};
