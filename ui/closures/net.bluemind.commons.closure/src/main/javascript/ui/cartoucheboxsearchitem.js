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
 * @fileoverview Widget composed of a combo box of possible search fields, a search text and a close button
 * and with the form of a cartouche.
 **/


goog.provide('bluemind.ui.CartoucheBoxSearchItem');

goog.require('bluemind.ui.CartoucheBoxItem');
goog.require('bluemind.model.search.QueryPart');
goog.require('bluemind.model.search.SearchTerm');
goog.require('goog.ui.Select');
goog.require('goog.ui.FlatMenuButtonRenderer');
/**
 * Class representing an search term in a cartouche.
 *
 * @param {bluemind.model.search.QueryPart|string} content Box content.
 * @param {Array.<bluemind.model.search.SearchTerm>} scope Search scope. 
 * @param {bluemind.ui.style.CartoucheBoxItemRenderer=} opt_renderer Optional renderer.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper used for
 *     document interactions.
 * @constructor
 * @extends {bluemind.ui.CartoucheBoxItem}
 */
bluemind.ui.CartoucheBoxSearchItem =
  function(content, scope, opt_renderer, opt_domHelper) {
  if (goog.isString(content)) {
    content = bluemind.model.search.QueryPart.fromString(content);
  }
  this.setModel(content);
  goog.base(this, content.getValue(), content, opt_renderer, opt_domHelper);
  this.buildMenu_(scope);
  this.addClassName(goog.getCssName('searchitem'));

};
goog.inherits(bluemind.ui.CartoucheBoxSearchItem, bluemind.ui.CartoucheBoxItem);

/** @override */
bluemind.ui.CartoucheBoxSearchItem.prototype.getValue = function() {
  return this.getModel().toString();
};

/** @override */
bluemind.ui.CartoucheBoxSearchItem.prototype.getInputValue = function() {
  return this.getModel().toValue();
};

/** 
 * Reset select value
 */
bluemind.ui.CartoucheBoxSearchItem.prototype.reset = function() {
  this.getChild('select').setSelectedIndex(0);
};

/**
 * Build the menu part of the cartouche
 * @param {Array.<bluemind.model.search.SearchTerm>} scope Search scope.  
 * @private
 */
bluemind.ui.CartoucheBoxSearchItem.prototype.buildMenu_ = function(scope) {
  var select = new goog.ui.Select('', new goog.ui.Menu(), goog.ui.FlatMenuButtonRenderer.getInstance());
  select.setId('select');
  var selected;
  var term = this.getModel().getTerm();
  for(var i = 0; i < scope.length; i++) {
    var child = new goog.ui.MenuItem(scope[i].getDisplayname(), scope[i]);
    select.addItem(child);
    if (scope[i] == term) {
      selected = i;
    }
  }
  if (selected === undefined) {
    var child = new goog.ui.MenuItem(term.getDisplayname(), term.getTerm());
    selected = scope.length;
    select.addItem(child);
  }
  select.setSelectedIndex(selected || 0);
  this.addChild(select);   
  if (this.getElement()) {
    this.getChild('select').renderBefore(this.getContentElement());  
  }
  if (this.isInDocument()) {
    this.getHandler().listen(this.getChild('select'), goog.ui.Component.EventType.CHANGE, this.handleSelect_);    
  }
};

/** @override */
bluemind.ui.CartoucheBoxSearchItem.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  if (this.getChild('select')) {
    this.getHandler().listen(this.getChild('select'), goog.ui.Component.EventType.CHANGE, this.handleSelect_);
  }
};

/**
 * Handle search term change
 * @param {Event} e Change event.
 */
bluemind.ui.CartoucheBoxSearchItem.prototype.handleSelect_ = function(e) {
  var term = this.getChild('select').getSelectedItem().getValue();
  this.getModel().setTerm(term);
};

/** @override */
bluemind.ui.CartoucheBoxSearchItem.prototype.createDom = function() {
  goog.base(this, 'createDom');
};


goog.exportSymbol('bluemind.ui.CartoucheBoxSearchItem', bluemind.ui.CartoucheBoxSearchItem);

goog.exportProperty(bluemind.ui.CartoucheBoxSearchItem.prototype, 'setReadOnly', bluemind.ui.CartoucheBoxSearchItem.prototype.setReadOnly);
