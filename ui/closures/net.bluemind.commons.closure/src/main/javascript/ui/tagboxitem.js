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

goog.provide('bluemind.ui.TagBoxItem');

goog.require('bluemind.model.Tag');
goog.require('goog.ui.MenuItem');
goog.require('goog.ui.registry');

/**
 * Class for tag box items.
 * 
 * @param {bluemind.model.Tag=} opt_tag Identifying data for the menu item.
 * @param {goog.ui.ControlContent=} opt_content Text caption or DOM structure to
 *          display as the content of the item (use to add icons or styling to
 *          menus).
 * @param {goog.dom.DomHelper=} opt_domHelper Optional dom helper used for dom
 *          interactions.
 * @param {goog.ui.MenuItemRenderer=} opt_renderer Optional renderer.
 * @constructor
 * @extends {goog.ui.MenuItem}
 */
bluemind.ui.TagBoxItem = function(opt_tag, opt_content, opt_domHelper, opt_renderer) {
  var content = opt_tag != null ? opt_tag.label : opt_content || '';
  goog.base(this, content, opt_tag, opt_domHelper, opt_renderer);

};
goog.inherits(bluemind.ui.TagBoxItem, goog.ui.MenuItem);

// Register a decorator factory function for bluemind.ui.TagBoxItems.
goog.ui.registry.setDecoratorByClassName(goog.getCssName('bm-tagbox-item'), function() {
  return new bluemind.ui.TagBoxItem(null);
});

/**
 * Whether the menu item is sticky, non-sticky items will be hidden as the user
 * types.
 * 
 * @type {boolean}
 * @private
 */
bluemind.ui.TagBoxItem.prototype.isSticky_ = false;

/**
 * Sets the menu item to be sticky or not sticky.
 * 
 * @param {boolean} sticky Whether the menu item should be sticky.
 */
bluemind.ui.TagBoxItem.prototype.setSticky = function(sticky) {
  this.isSticky_ = sticky;
};

/**
 * @return {boolean} Whether the menu item is sticky.
 */
bluemind.ui.TagBoxItem.prototype.isSticky = function() {
  return this.isSticky_;
};

/**
 * Sets the format for a menu item based on a token, bolding the token.
 * 
 * @param {string} token The token.
 */
bluemind.ui.TagBoxItem.prototype.setFormatFromToken = function(token) {
  if (this.isEnabled()) {
    var caption = this.getCaption();
    var index = caption.toLowerCase().indexOf(token);
    if (index >= 0) {
      var domHelper = this.getDomHelper();
      this.setContent([ domHelper.createTextNode(caption.substr(0, index)),
          domHelper.createDom('b', null, caption.substr(index, token.length)),
          domHelper.createTextNode(caption.substr(index + token.length)) ]);
    }
  }
};
