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
 * @fileoverview Field that allow to type multiple entry.
 * Each time a separator char is entered the current value of the input 
 * is added to the entry list, and the value of the input is reset 
 */


goog.provide('bluemind.ui.MultiEntryField');


goog.require('bluemind.ui.CartoucheBox');
goog.require('bluemind.ui.CartoucheBoxItem');
goog.require('bluemind.ui.style.MultiEntryFieldRenderer');
goog.require('goog.array');
goog.require('goog.ui.Control');
goog.require('goog.ui.LabelInput');

/**
 * A MultiEntryField control.
 * @param {Array=} opt_model Option model with field values.
 * @param {bluemind.ui.style.MultiEntryFieldRenderer=} opt_renderer Renderer used to
 *   render or decorate the container; defaults to 
 *   {@link bluemind.ui.style.MultiEntryFieldRenderer}.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @extends {goog.ui.Control}
 * @constructor
 */
bluemind.ui.MultiEntryField = 
  function(opt_model, opt_renderer, opt_domHelper) {
  goog.base(this, null, opt_renderer ||
      bluemind.ui.style.MultiEntryFieldRenderer.getInstance(), opt_domHelper);
  this.entries_ = new bluemind.ui.CartoucheBox();
  this.entries_.setId('entries');
  this.addChild(this.entries_);
  this.field = new goog.ui.LabelInput();
  this.addChild(this.field);
  this.unique_ = false;
  this.setHandleMouseEvents(false);
  this.setAllowTextSelection(true);
};
goog.inherits(bluemind.ui.MultiEntryField, goog.ui.Control);


/**
 * Container for selected values.
 * @type {bluemind.ui.CartoucheBox}
 * @private
 */
bluemind.ui.MultiEntryField.prototype.entries_ = null;


/**
 * A LabelInput control that manages the focus/blur state of the input box.
 * @type {goog.ui.LabelInput?}
 * @protected
 */
bluemind.ui.MultiEntryField.prototype.field = null;

/**
 * Characters that can be used to split multiple entries in an input string
 * @type {string}
 * @private
 */
bluemind.ui.MultiEntryField.prototype.separators_ = ',; ';

/**
 * Determine if a value can be entered more than one time.
 * @type {boolean}
 * @private
 */
bluemind.ui.MultiEntryField.prototype.unique_;

/**
 * Set separators values
 * @param {string} separators new value to cut parts.
 */
bluemind.ui.MultiEntryField.prototype.setSeparators = function(separators) {
  this.separators_ = separators;
};

/**
 * Get separators values
 * @return {string} separators new value to cut parts.
 */
bluemind.ui.MultiEntryField.prototype.getSeparators = function() {
  return this.separators_;
};

/**
 * Set if a value can be added more than one time
 * @param {boolean} unique Is each field value unique.
 */
bluemind.ui.MultiEntryField.prototype.setUniqueValue = function(unique) {
  this.unique_ = unique;
  if (unique) {
    this.removeDuplicate();
  }
};

/** @override */
bluemind.ui.MultiEntryField.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var field = this.getRenderer().getField(this);
  var list = this.getRenderer().getEntries(this);
  this.field.decorate(field);
  this.entries_.renderBefore(field);
   
};

/** @override */
bluemind.ui.MultiEntryField.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  var handler = this.getHandler();
  handler.listen(this.entries_,
      goog.ui.Component.EventType.CHANGE, this.handleEntriesChange_);
  this.handleEntriesChange_();
};

/**
 * Handles keyboard events from the input box.  Returns true if the field
 * was able to handle the event, false otherwise.
 * @param {goog.events.KeyEvent} e Input event to handle.
 * @return {boolean} Whether the event was handled by the tag box.
 * @protected
 * @suppress {visibility} performActionInternal
 */
bluemind.ui.MultiEntryField.prototype.handleKeyEvent = function(e) {
  var handled = false;
  switch (e.keyCode) {
    case goog.events.KeyCodes.DELETE:
      var last = /** @type bluemind.ui.CartoucheBoxItem */ (this.entries_.getSelected());
      if (last != null) {
        this.entries_.deleteChild(last);
      }
      break;    
    case goog.events.KeyCodes.BACKSPACE:
      if (this.getInputValue() == '') {
        var last = /** @type bluemind.ui.CartoucheBoxItem */ (this.entries_.getLastChild());
        if (last != null) {
          if (last.isSelected()) {
            this.setInputValue(/** @type string */ (last.getInputValue()));
            this.entries_.deleteChild(last);
          } else {
            last.setSelected(true);
          }
        }
      }
      break;
    case goog.events.KeyCodes.ENTER:
      this.addValue(this.getInputValue());
      this.field.clear();
      this.performActionInternal(e);
      handled = true;
      break;      
    default :
      if (e.charCode && this.separators_.indexOf(String.fromCharCode(e.charCode)) != -1) {
         handled = true;
         this.addValue(this.getInputValue());
         this.field.clear();
      }
  }
  if (handled) {
    e.preventDefault();
  }

  return handled;
};

/**
 * Return value of the input field.
 * @return {string} The current value of the input field.
 * @protected
 */
bluemind.ui.MultiEntryField.prototype.getInputValue = function() {
  return this.field.getValue();
};


/**
 * Set value of the input field.
 * @param {string} value The current value of the input field.
 * @protected
 */
bluemind.ui.MultiEntryField.prototype.setInputValue = function(value) {
  this.field.setValue(value);
};

/**
 * Remove duplicate value.
 * @protected 
 **/
bluemind.ui.MultiEntryField.prototype.removeDuplicate = function() {
};

/**
 * Resize input and set cursor position.
 * @private
 */
bluemind.ui.MultiEntryField.prototype.handleEntriesChange_ = function() {
  this.getRenderer().adjust(this);
};

/**
 * Set all values.
 * @param {Array} values Value to set 
 */
bluemind.ui.MultiEntryField.prototype.setValue = function(values) {
  this.entries_.forEachChild(function(child) {
    child.dispose();
  });
  for (var i = 0; i < values.length; i++) {
    this.addValue(values[i]);
  }
};

/**
 * Get all values.
 * @return {Array} Selected values 
 */
bluemind.ui.MultiEntryField.prototype.getValue = function() {
  var values = [];
  this.entries_.forEachChild(function(child) {
    values.push(child.getValue());
  }, this);
  return values;
};

/**
 * Add a new entry to the selected values.
 * @param {string |  bluemind.ui.CartoucheBoxItem} value Value to add. 
 */
bluemind.ui.MultiEntryField.prototype.addValue = function(value) {
  if (value instanceof bluemind.ui.CartoucheBoxItem) {
    this.entries_.addChild(/** @type { bluemind.ui.CartoucheBoxItem} */ (value), true);
  } else {  
    if (value != '' ) {//&& (!this.unique_ || this.contains_(value)) {
      var entry = new bluemind.ui.CartoucheBoxItem(value);
      this.entries_.addChild(entry, true);
    }
  }
};

/**
 * Clear all writable values.
 */
bluemind.ui.MultiEntryField.prototype.reset = function() {
  var children = [];
  this.entries_.forEachChild(function(child, index) {
    if (!child.isReadOnly()) {
      children.push(child);
    }
  }, this);
  for (var i = 0; i < children.length; i++) {
    this.entries_.deleteChild(children[i]);
  }
};

