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

goog.provide("net.bluemind.filehosting.ui.FileHostingExplorer");

goog.require("goog.style");
goog.require("goog.ui.Container");
goog.require("goog.ui.Container.Orientation");
goog.require("net.bluemind.filehosting.ui.FileHostingExplorerRenderer");
goog.require("net.bluemind.ui.SelectionModel");
goog.require("net.bluemind.ui.SingleSelectionModel");

/**
 * @constructor
 * 
 * @param {goog.ui.ContainerRenderer=} opt_renderer
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {goog.ui.Container}
 */
net.bluemind.filehosting.ui.FileHostingExplorer = function(opt_renderer, opt_domHelper) {
  var renderer = opt_renderer || net.bluemind.filehosting.ui.FileHostingExplorerRenderer.getInstance();
  goog.base(this, goog.ui.Container.Orientation.HORIZONTAL, renderer, opt_domHelper);
  this.setFocusable(false);
  this.setFocusableChildrenAllowed(true);
  this.setMultipleSelectionAllowed(true);
}
goog.inherits(net.bluemind.filehosting.ui.FileHostingExplorer, goog.ui.Container);

/**
 * The selection model controlling the items in the menu.
 * 
 * @type {net.bluemind.ui.SelectionModel}
 * @private
 */
net.bluemind.filehosting.ui.FileHostingExplorer.prototype.selectionModel_ = null;

/** @override */
net.bluemind.filehosting.ui.FileHostingExplorer.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getRenderer().setEmpty(this, (this.getChildCount() == 0));
  this.getHandler().listen(this, goog.ui.Component.EventType.ACTION, this.handleAction);
};

/** @override */

/** @override */
net.bluemind.filehosting.ui.FileHostingExplorer.prototype.removeChild = function(child, opt_unrender) {
  var removed = goog.base(this, 'removeChild', child, opt_unrender);
  if (removed) {
    this.selectionModel_.setSelected(removed, false);
    removed.dispose();
  }
  if (this.getChildCount() == 0 && this.isInDocument()) {
    this.getRenderer().setEmpty(this, true);
  }
  return removed;
};

/** @override */
net.bluemind.filehosting.ui.FileHostingExplorer.prototype.addChildAt = function(child, index, opt_render) {
  goog.base(this, 'addChildAt', child, index, opt_render);
  child.setSelected(false);
  if (this.getChildCount() == 1 && this.isInDocument()) {
    this.getRenderer().setEmpty(this, false);
  }
};

/** @override */
net.bluemind.filehosting.ui.FileHostingExplorer.prototype.setHighlightedIndex = function(index) {
  goog.base(this, 'setHighlightedIndex', index);
  var child = this.getChildAt(index);
  if (child) {
    goog.style.scrollIntoContainerView(child.getElement(), this.getElement());
  }
};

/**
 * Set if you can select more than one item
 * 
 * @param {boolean} allowed
 */
net.bluemind.filehosting.ui.FileHostingExplorer.prototype.setMultipleSelectionAllowed = function(allowed) {
  if (this.selectionModel_) {
    this.selectionModel_.dispose();
    this.selectionModel_ = null;
  }
  if (allowed) {
    this.createMultiSelectionModel_()
  } else {
    this.createSingleSelectionModel_()
  }
};

/**
 * Creates a new multiple selection model and sets up an event listener to
 * handle {@link goog.events.EventType.SELECT} events dispatched by it.
 * 
 * @private
 */
net.bluemind.filehosting.ui.FileHostingExplorer.prototype.createMultiSelectionModel_ = function() {
  this.selectionModel_ = new net.bluemind.ui.SelectionModel();
  this.forEachChild(function(child, index) {
    child.setSelected(false);
  }, this);
  this.registerDisposable(this.selectionModel_);
  this.selectionModel_.setParentEventTarget(this);
};

/**
 * Creates a new single selection model and sets up an event listener to handle
 * {@link goog.events.EventType.SELECT} events dispatched by it.
 * 
 * @private
 */
net.bluemind.filehosting.ui.FileHostingExplorer.prototype.createSingleSelectionModel_ = function() {
  this.selectionModel_ = new net.bluemind.ui.SingleSelectionModel();
  this.forEachChild(function(child, index) {
    child.setSelected(false);
  }, this);
  this.registerDisposable(this.selectionModel_);
  this.selectionModel_.setParentEventTarget(this);
};

/**
 * Handles {@code ACTION} events dispatched by an activated filehosting item.
 * 
 * @param {goog.events.Event} e Action event to handle.
 * @protected
 */
net.bluemind.filehosting.ui.FileHostingExplorer.prototype.handleAction = function(e) {
  /** @type {net.bluemind.filehosting.ui.FileHostingItem} */
  var item = (
  /** @type {net.bluemind.filehosting.ui.FileHostingItem} */
  (e.target));
  if (item.isSupportedState(goog.ui.Component.State.SELECTED)) {
    this.selectionModel_.toggle(item);
  }
};

/**
 * Get selected children
 * 
 * @return {Array.<goog.ui.Control>} selected children
 */
net.bluemind.filehosting.ui.FileHostingExplorer.prototype.getSelectedChildren = function() {
  return (
  /** @type {Array.<goog.ui.Control>} */
  (this.selectionModel_.getSelection()));
};
