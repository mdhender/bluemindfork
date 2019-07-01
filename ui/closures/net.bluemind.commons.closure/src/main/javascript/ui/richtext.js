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
 * @fileoverview A wrapper arround {@see bluemind.ui.Editor}.
 *
 */

goog.provide('bluemind.ui.RichText');

goog.require('bluemind.ui.Editor');
goog.require('goog.ui.Component');



/**
 *  A component wrapper for {@see bluemind.ui.Editor}.
 *
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM hepler, used for
 *     document interaction.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.ui.RichText = function( opt_domHelper) {
  goog.base(this, opt_domHelper);
};
goog.inherits(bluemind.ui.RichText, goog.ui.Component);

/**
 * Editor field
 * @type {bluemind.ui.Editor}
 */
bluemind.ui.RichText.prototype.editor_;

/** @override */
bluemind.ui.RichText.prototype.createDom = function() {
  var dom = this.getDomHelper();
  var element = dom.createDom('div', undefined,
    dom.createDom('div', { 'id': this.makeId('toolbar')}),
    dom.createDom('div', { 'id': this.makeId('editor'), 'class': goog.getCssName('bm-richtext')}));  
  this.setElementInternal(element);
};

/** @override */
bluemind.ui.RichText.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  if (! this.editor_) {
    var eid = this.makeId('editor');
    var tid = this.makeId('toolbar');    
    this.editor_ = new bluemind.ui.Editor(eid, tid);
  } else if (this.editor_.field.isUneditable()) {
    this.editor_.field.makeEditable();
  }
};

/**
 * Returns the current value of the text box, returning an empty string if the
 * search box is the default value
 * @return {string} The value of the input box.
 */
bluemind.ui.RichText.prototype.getValue = function() {
  if (this.editor_) {
    return this.editor_.getValue();
  }
  return '';
};


/**
 * Use this to set the value through script to ensure that the label state is
 * up to date
 * @param {string} s The new value for the input.
 */
bluemind.ui.RichText.prototype.setValue = function(s) {
  if (this.editor_) {
    this.editor_.setValue(s);
  } 
};

/** @override */
bluemind.ui.RichText.prototype.exitDocument = function() {
  goog.base(this, 'exitDocument');
  if (this.editor_ && !this.editor_.field.isUneditable()) {
    this.editor_.field.makeUneditable();
  }
};

/** @override */
bluemind.ui.RichText.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
}
