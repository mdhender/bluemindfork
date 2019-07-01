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
 * @fileoverview Widget similar to a combo-box which allow creation of new item,
 *               and which can hold multiple values.
 */

goog.provide('bluemind.ui.TagBox');

goog.require('bluemind.ui.TagBoxItem');
goog.require('bluemind.model.Tag');
goog.require('goog.Timer');
goog.require('goog.log');
goog.require('goog.dom.classlist');
goog.require('goog.events');
goog.require('goog.events.EventType');
goog.require('goog.events.InputHandler');
goog.require('goog.events.KeyCodes');
goog.require('goog.events.KeyHandler');
goog.require('goog.iter');
goog.require('goog.math.Coordinate');
goog.require('goog.math.Size');
goog.require('goog.positioning.Corner');
goog.require('goog.positioning.MenuAnchoredPosition');
goog.require('goog.string');
goog.require('goog.structs.Map');
goog.require('goog.style');
goog.require('goog.ui.Component');
goog.require('goog.ui.ItemEvent');
goog.require('goog.ui.LabelInput');
goog.require('goog.ui.Menu');
goog.require('goog.ui.MenuItem');
goog.require('goog.ui.registry');
goog.require('goog.userAgent');
goog.require('goog.style');



/**
 * A TagBox control.
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @param {goog.ui.Menu=} opt_menu Optional menu.
 * @extends {goog.ui.Component}
 * @constructor
 */
bluemind.ui.TagBox = function(opt_domHelper, opt_menu) {
  goog.base(this, opt_domHelper);

  this.labelInput_ = new goog.ui.LabelInput();
  this.enabled_ = true;

  this.menu_ = opt_menu || new goog.ui.Menu(this.getDomHelper());
  this.setupMenu_();
  var create = new bluemind.ui.TagBoxItem(null, 'Create Tag');
  create.setVisible(false);
  create.setSticky(true);
  create.setId('__create__');
  this.addItem(new goog.ui.MenuSeparator());
  this.addItem(create);
  this.selectedValues_ =  new goog.structs.Map();
  this.setModel([]);
};
goog.inherits(bluemind.ui.TagBox, goog.ui.Component);


/**
 * Number of milliseconds to wait before dismissing tagbox after blur.
 * 
 * @type {number}
 */
bluemind.ui.TagBox.BLUR_DISMISS_TIMER_MS = 250;


/**
 * A logger to help debugging of tag box behavior.
 * 
 * @type {goog.log.Logger}
 * @private
 */
bluemind.ui.TagBox.prototype.logger_ =
    goog.log.getLogger('bluemind.ui.TagBox');

    
/**
 * HashMap of the selected values
 * 
 * @type {goog.structs.Map}
 * @private
 */
bluemind.ui.TagBox.prototype.selectedValues_ = null;

/**
 * Characters that can be used to split multiple entries in an input string
 * 
 * @type {string}
 * @private
 */
bluemind.ui.TagBox.prototype.separators_ = ',; ';

/**
 * Whether the tag box is enabled.
 * 
 * @type {boolean}
 * @private
 */
bluemind.ui.TagBox.prototype.enabled_;


/**
 * Keyboard event handler to manage key events dispatched by the input element.
 * 
 * @type {goog.events.KeyHandler}
 * @private
 */
bluemind.ui.TagBox.prototype.keyHandler_;


/**
 * Input handler to take care of firing events when the user inputs text in the
 * input.
 * 
 * @type {goog.events.InputHandler?}
 * @private
 */
bluemind.ui.TagBox.prototype.inputHandler_ = null;


/**
 * The last input token.
 * 
 * @type {?string}
 * @private
 */
bluemind.ui.TagBox.prototype.lastToken_ = null;


/**
 * A LabelInput control that manages the focus/blur state of the input box.
 * 
 * @type {goog.ui.LabelInput?}
 * @private
 */
bluemind.ui.TagBox.prototype.labelInput_ = null;


/**
 * Drop down menu for the tag box. Will be created at construction time.
 * 
 * @type {goog.ui.Menu?}
 * @private
 */
bluemind.ui.TagBox.prototype.menu_ = null;


/**
 * The cached visible count.
 * 
 * @type {number}
 * @private
 */
bluemind.ui.TagBox.prototype.visibleCount_ = -1;


/**
 * The input element.
 * 
 * @type {Element}
 * @private
 */
bluemind.ui.TagBox.prototype.input_ = null;


/**
 * The match function. The first argument for the match function will be a
 * MenuItem's caption and the second will be the token to evaluate.
 * 
 * @type {Function}
 * @private
 */
bluemind.ui.TagBox.prototype.matchFunction_ = goog.string.startsWith;


/**
 * Element used as the tag boxes button.
 * 
 * @type {Element}
 * @private
 */
bluemind.ui.TagBox.prototype.button_ = null;


/**
 * Default text content for the input box when it is unchanged and unfocussed.
 * 
 * @type {string}
 * @private
 */
bluemind.ui.TagBox.prototype.defaultText_ = '';


/**
 * Name for the input box created
 * 
 * @type {string}
 * @private
 */
bluemind.ui.TagBox.prototype.fieldName_ = '';


/**
 * Timer identifier for delaying the dismissal of the tag menu.
 * 
 * @type {?number}
 * @private
 */
bluemind.ui.TagBox.prototype.dismissTimer_ = null;


/**
 * True if the unicode inverted triangle should be displayed in the dropdown
 * button. Defaults to false.
 * 
 * @type {boolean} useDropdownArrow
 * @private
 */
bluemind.ui.TagBox.prototype.useDropdownArrow_ = false;



/**
 * Container for selected tags.
 * 
 * @type {Element}
 * @private
 */
bluemind.ui.TagBox.prototype.bullets_ = null;

/**
 * Create the DOM objects needed for the tag box. A span and text input.
 * 
 * @override
 */
bluemind.ui.TagBox.prototype.createDom = function() {
  this.input_ = this.getDomHelper().createDom(
      'textarea', {'name': this.fieldName_, 'autocomplete': 'off', 'rows': '1'});
  this.button_ = this.getDomHelper().createDom('span',
      goog.getCssName('bm-tagbox-button'));
  this.bullets_ =  this.getDomHelper().createDom('ul',
      goog.getCssName('bm-tagbox-bullets'));
  this.setElementInternal(this.getDomHelper().createDom('div',
      [goog.getCssName('bm-tagbox'),  goog.getCssName('goog-inline-block')],
      this.bullets_, this.input_, this.button_));
    this.button_.innerHTML = '&#x25BC;';
    goog.style.setUnselectable(this.button_, true /* unselectable */);
  this.input_.setAttribute('label', this.defaultText_);
  this.labelInput_.decorate(this.input_);
  this.menu_.setFocusable(false);
  if (!this.menu_.isInDocument()) {
    this.addChild(this.menu_, true);
  }
  
};


/**
 * Enables/Disables the tag box.
 * 
 * @param {boolean} enabled Whether to enable (true) or disable (false) the tag
 *          box.
 */
bluemind.ui.TagBox.prototype.setEnabled = function(enabled) {
  this.enabled_ = enabled;
  this.labelInput_.setEnabled(enabled);
  goog.dom.classlist.enable(this.getElement(),
      goog.getCssName('bm-tagbox-disabled'), !enabled);
};


/** @override */
bluemind.ui.TagBox.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  var handler = this.getHandler();
  handler.listen(this.getElement(),
      goog.events.EventType.MOUSEDOWN, this.onTagMouseDown_);
  handler.listen(this.getDomHelper().getDocument(),
      goog.events.EventType.MOUSEDOWN, this.onDocClicked_);

  handler.listen(this.input_,
      goog.events.EventType.BLUR, this.onInputBlur_);

  this.keyHandler_ = new goog.events.KeyHandler(this.input_);
  handler.listen(this.keyHandler_,
      goog.events.KeyHandler.EventType.KEY, this.handleKeyEvent);

  this.inputHandler_ = new goog.events.InputHandler(this.input_);
  handler.listen(this.inputHandler_,
      goog.events.InputHandler.EventType.INPUT, this.onInputEvent_);

  handler.listen(this.menu_,
      goog.ui.Component.EventType.ACTION, this.onMenuSelected_);


  this.reloadValue_();
};


/** @override */
bluemind.ui.TagBox.prototype.exitDocument = function() {
  this.keyHandler_.dispose();
  delete this.keyHandler_;
  this.inputHandler_.dispose();
  this.inputHandler_ = null;
  goog.base(this, 'exitDocument');
};


/**
 * Tag box currently can't decorate elements.
 * 
 * @return {boolean} The value false.
 * @override
 */
bluemind.ui.TagBox.prototype.canDecorate = function() {
  return false;
};


/** @override */
bluemind.ui.TagBox.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');

  this.clearDismissTimer_();

  this.labelInput_.dispose();
  this.menu_.dispose();

  this.labelInput_ = null;
  this.menu_ = null;
  this.input_ = null;
  this.button_ = null;
};


/**
 * Dismisses the menu and resets the value of the edit field.
 */
bluemind.ui.TagBox.prototype.dismiss = function() {
  this.clearDismissTimer_();
  this.hideMenu_();
  this.menu_.setHighlightedIndex(-1);
};


/**
 * Adds a new menu item at the end of the menu.
 * 
 * @param {goog.ui.MenuItem | goog.ui.MenuSeparator} item Menu item to add to
 *          the menu.
 */
bluemind.ui.TagBox.prototype.addItem = function(item) {
  this.menu_.addChild(item, true);
  this.visibleCount_ = -1;
};


/**
 * Adds a new menu item at a specific index in the menu.
 * 
 * @param {goog.ui.MenuItem} item Menu item to add to the menu.
 * @param {number} n Index at which to insert the menu item.
 */
bluemind.ui.TagBox.prototype.addItemAt = function(item, n) {
  this.menu_.addChildAt(item, n, true);
  this.visibleCount_ = -1;
};


/**
 * Removes an item from the menu and disposes it.
 * 
 * @param {goog.ui.MenuItem} item The menu item to remove.
 */
bluemind.ui.TagBox.prototype.removeItem = function(item) {
  var child = this.menu_.removeChild(item, true);
  if (child) {
    child.dispose();
    this.visibleCount_ = -1;
  }
};


/**
 * Remove all of the items from the TagBox menu
 */
bluemind.ui.TagBox.prototype.removeAllItems = function() {
  for (var i = this.getItemCount() - 1; i >= 0; --i) {
    var item = this.getItemAt(i);
    if (item.getId() != '__create__' && !(item instanceof goog.ui.MenuSeparator)) {
      this.removeItem(item);
    }
  }
};


/**
 * Removes a menu item at a given index in the menu.
 * 
 * @param {number} n Index of item.
 */
bluemind.ui.TagBox.prototype.removeItemAt = function(n) {
  var child = this.menu_.removeChildAt(n, true);
  if (child) {
    child.dispose();
    this.visibleCount_ = -1;
  }
};


/**
 * Returns a reference to the menu item at a given index.
 * 
 * @param {string} id Id of menu item.
 * @return {goog.ui.MenuItem?} Reference to the menu item.
 */
bluemind.ui.TagBox.prototype.getItem = function(id) {
  return /** @type {goog.ui.MenuItem?} */(this.menu_.getChild(id));
};

/**
 * Returns a reference to the menu item at a given index.
 * 
 * @param {number} n Index of menu item.
 * @return {goog.ui.MenuItem?} Reference to the menu item.
 */
bluemind.ui.TagBox.prototype.getItemAt = function(n) {
  return /** @type {goog.ui.MenuItem?} */(this.menu_.getChildAt(n));
};


/**
 * Returns the number of items in the list, including non-visible items, such as
 * separators.
 * 
 * @return {number} Number of items in the menu for this tagbox.
 */
bluemind.ui.TagBox.prototype.getItemCount = function() {
  return this.menu_.getChildCount();
};


/**
 * @return {goog.ui.Menu} The menu that pops up.
 */
bluemind.ui.TagBox.prototype.getMenu = function() {
  return this.menu_;
};


/**
 * @return {Element} The input element.
 */
bluemind.ui.TagBox.prototype.getInputElement = function() {
  return this.input_;
};


/**
 * @return {number} The number of visible items in the menu.
 * @private
 */
bluemind.ui.TagBox.prototype.getNumberOfVisibleItems_ = function() {
  if (this.visibleCount_ == -1) {
    var count = 0;
    for (var i = 0, n = this.menu_.getChildCount(); i < n; i++) {
      var item = this.menu_.getChildAt(i);
      if (!(item instanceof goog.ui.MenuSeparator) && item.isVisible()) {
        count++;
      }
    }
    this.visibleCount_ = count;
  }

  goog.log.info(this.logger_,'getNumberOfVisibleItems() - ' + this.visibleCount_);
  return this.visibleCount_;
};


/**
 * Sets the match function to be used when filtering the tag box menu.
 * 
 * @param {Function} matchFunction The match function to be used when filtering
 *          the tag box menu.
 */
bluemind.ui.TagBox.prototype.setMatchFunction = function(matchFunction) {
  this.matchFunction_ = matchFunction;
};


/**
 * @return {Function} The match function for the tagx box.
 */
bluemind.ui.TagBox.prototype.getMatchFunction = function() {
  return this.matchFunction_;
};


/**
 * Sets the default text for the tag box.
 * 
 * @param {string} text The default text for the tag box.
 */
bluemind.ui.TagBox.prototype.setDefaultText = function(text) {
  this.defaultText_ = text;
  if (this.labelInput_) {
    this.labelInput_.setLabel(this.defaultText_);
  }
};


/**
 * @return {string} text The default text for the tagx box.
 */
bluemind.ui.TagBox.prototype.getDefaultText = function() {
  return this.defaultText_;
};


/**
 * Sets the field name for the tag box.
 * 
 * @param {string} fieldName The field name for the tag box.
 */
bluemind.ui.TagBox.prototype.setFieldName = function(fieldName) {
  this.fieldName_ = fieldName;
};


/**
 * @return {string} The field name for the tag box.
 */
bluemind.ui.TagBox.prototype.getFieldName = function() {
  return this.fieldName_;
};


/**
 * Set to true if a unicode inverted triangle should be displayed in the
 * dropdown button. This option defaults to false for backwards compatibility.
 * 
 * @param {boolean} useDropdownArrow True to use the dropdown arrow.
 */
bluemind.ui.TagBox.prototype.setUseDropdownArrow = function(useDropdownArrow) {
  this.useDropdownArrow_ = !!useDropdownArrow;
};


/**
 * Sets the current value of the tag box.
 * 
 * @param {string} value The new value.
 */
bluemind.ui.TagBox.prototype.setInputValue_ = function(value) {
  goog.log.info(this.logger_,'setValue() - ' + value);
  if (this.labelInput_.getValue() != value) {
    this.labelInput_.setValue(value);
    this.handleInputChange_();
  }
};


/**
 * @return {string} The current value of the tag box.
 */
bluemind.ui.TagBox.prototype.getInputValue_ = function() {
  return this.labelInput_.getValue();
};


/**
 * @return {string} HTML escaped token.
 */
bluemind.ui.TagBox.prototype.getToken = function() {
  return goog.string.htmlEscape(this.getTokenText_());
};


/**
 * @return {string} The token for the current cursor position in the input box,
 *         when multi-input is disabled it will be the full input value.
 * @private
 */
bluemind.ui.TagBox.prototype.getTokenText_ = function() {
  return goog.string.trim(this.labelInput_.getValue().toLowerCase());
};


/**
 * @private
 */
bluemind.ui.TagBox.prototype.setupMenu_ = function() {
  var sm = this.menu_;
  sm.setVisible(false);
  sm.setAllowAutoFocus(false);
  sm.setAllowHighlightDisabled(true);
};


/**
 * Shows the menu if it isn't already showing. Also positions the menu
 * correctly, resets the menu item visibilities and highlights the relevent
 * item.
 * 
 * @param {boolean} showAll Whether to show all items, with the first matching
 *          item highlighted.
 * @private
 */
bluemind.ui.TagBox.prototype.maybeShowMenu_ = function(showAll) {
  var isVisible = this.menu_.isVisible();
  var numVisibleItems = this.getNumberOfVisibleItems_();

  if (isVisible && numVisibleItems == 0) {
    goog.log.fine(this.logger_, 'no matching items, hiding');
    this.hideMenu_();

  } else if (!isVisible && numVisibleItems > 0) {
    if (showAll) {
      goog.log.fine(this.logger_, 'showing menu');
      this.setItemVisibilityFromToken_('');
      this.setItemHighlightFromToken_(this.getTokenText_());
    }
    goog.Timer.callOnce(this.clearDismissTimer_, 1, this);

    this.showMenu_();
  }

  this.positionMenu();
};


/**
 * Positions the menu.
 * 
 * @protected
 */
bluemind.ui.TagBox.prototype.positionMenu = function() {
  if (this.menu_ && this.menu_.isVisible()) {
    var position = new goog.positioning.MenuAnchoredPosition(this.getElement(),
        goog.positioning.Corner.BOTTOM_START, true);
    position.reposition(this.menu_.getElement(),
        goog.positioning.Corner.TOP_START);
  }
};


/**
 * Show the menu and add an active class to the tag box's element.
 * 
 * @private
 */
bluemind.ui.TagBox.prototype.showMenu_ = function() {
  this.menu_.setVisible(true);
  goog.dom.classlist.add(this.getElement(),
      goog.getCssName('bm-tagbox-active'));
};


/**
 * Hide the menu and remove the active class from the tag box's element.
 * 
 * @private
 */
bluemind.ui.TagBox.prototype.hideMenu_ = function() {
  this.menu_.setVisible(false);
  goog.dom.classlist.remove(this.getElement(),
      goog.getCssName('bm-tagbox-active'));
};


/**
 * Clears the dismiss timer if it's active.
 * 
 * @private
 */
bluemind.ui.TagBox.prototype.clearDismissTimer_ = function() {
  if (this.dismissTimer_) {
    goog.Timer.clear(this.dismissTimer_);
    this.dismissTimer_ = null;
  }
};


/**
 * Event handler for when the tag box area has been clicked.
 * 
 * @param {goog.events.BrowserEvent} e The browser event.
 * @private
 */
bluemind.ui.TagBox.prototype.onTagMouseDown_ = function(e) {
  // We only want this event on the element itself or the input or the button.
  if (this.enabled_ &&
      (e.target == this.getElement() || e.target == this.input_ ||
       goog.dom.contains(this.button_, /** @type {Node} */ (e.target)))) {
    if (this.menu_.isVisible()) {
      goog.log.fine(this.logger_, 'Menu is visible, dismissing');
      this.dismiss();
    } else {
      goog.log.fine(this.logger_, 'Opening dropdown');
      this.maybeShowMenu_(true);
      if (goog.userAgent.OPERA) {
        // select() doesn't focus <input> elements in Opera.
        this.input_.focus();
      }
      this.input_.select();
      this.menu_.setMouseButtonPressed(true);
      // Stop the click event from stealing focus
      e.preventDefault();
    }
  }
  // Stop the event from propagating outside of the tag box
  e.stopPropagation();
};


/**
 * Event handler for when the document is clicked.
 * 
 * @param {goog.events.BrowserEvent} e The browser event.
 * @private
 */
bluemind.ui.TagBox.prototype.onDocClicked_ = function(e) {
  if (!goog.dom.contains(
      this.menu_.getElement(), /** @type {Node} */ (e.target))) {
    goog.log.info(this.logger_,'onDocClicked_() - dismissing immediately');
    this.dismiss();
  }
};


/**
 * Handle the menu's select event.
 * 
 * @param {goog.events.Event} e The event.
 * @private
 */
bluemind.ui.TagBox.prototype.onMenuSelected_ = function(e) {
  goog.log.info(this.logger_,'onMenuSelected_()');
  var item = /** @type {!goog.ui.MenuItem} */ (e.target);
  var value = item.getValue();
  goog.log.fine(this.logger_, 'Menu selection: ' + value + '. Dismissing menu');
  if (item.getId() == '__create__') {
    var tag = this.addTag_(/** @type {string} */(value));
    if(tag ) {
      var e = new goog.events.Event('create-tag');
      tag.id =net.bluemind.mvp.UID.generate();
      e.tag= tag;
      this.dispatchEvent(e);
      this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
    }
  } else {
    if (this.addValue(/** @type {bluemind.model.Tag} */(value))) {
      this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
    }
  }
  this.dismiss();
  e.stopPropagation();
};


/**
 * Event handler for when the input box looses focus -- hide the menu
 * 
 * @param {goog.events.BrowserEvent} e The browser event.
 * @private
 */
bluemind.ui.TagBox.prototype.onInputBlur_ = function(e) {
  goog.log.info(this.logger_,'onInputBlur_() - delayed dismiss');
  this.clearDismissTimer_();
  this.dismissTimer_ = goog.Timer.callOnce(
      this.dismiss, bluemind.ui.TagBox.BLUR_DISMISS_TIMER_MS, this);
};


/**
 * Handles keyboard events from the input box. Returns true if the tag box was
 * able to handle the event, false otherwise.
 * 
 * @param {goog.events.KeyEvent} e Key event to handle.
 * @return {boolean} Whether the event was handled by the tag box.
 * @protected
 * @suppress {visibility} performActionInternal
 */
bluemind.ui.TagBox.prototype.handleKeyEvent = function(e) {
  var isMenuVisible = this.menu_.isVisible();

  if (isMenuVisible && this.menu_.handleKeyEvent(e)) {
    return true;
  }

  var handled = false;
  switch (e.keyCode) {
    case goog.events.KeyCodes.ESC:
      if (isMenuVisible) {
        goog.log.fine(this.logger_, 'Dismiss on Esc: ' + this.labelInput_.getValue());
        this.dismiss();
        handled = true;
      }
      break;
    case goog.events.KeyCodes.TAB:
      if (isMenuVisible) {
        var highlighted = this.menu_.getHighlighted();
        if (highlighted) {
          goog.log.fine(this.logger_, 'Select on Tab: ' + this.labelInput_.getValue());
          highlighted.performActionInternal(e);
          handled = true;
        }
      }
      break;
    case goog.events.KeyCodes.UP:
    case goog.events.KeyCodes.DOWN:
      if (!isMenuVisible) {
        goog.log.fine(this.logger_, 'Up/Down - maybe show menu');
        this.maybeShowMenu_(true);
        handled = true;
      }
      break;
    case goog.events.KeyCodes.ENTER:
      handled = true;
      break;
    case goog.events.KeyCodes.BACKSPACE:
      if (this.getInputValue_() == '') {
        if (this.deleteLastTag_()) {
          this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
        }
      }
      break;
  }
  if (handled) {
    e.preventDefault();
  }

  return handled;
};


/**
 * Handles the content of the input box changing.
 * 
 * @param {goog.events.Event} e The INPUT event to handle.
 * @private
 */
bluemind.ui.TagBox.prototype.onInputEvent_ = function(e) {
  goog.log.fine(this.logger_, 'Key is modifying: ' + this.labelInput_.getValue());
  this.handleInputChange_();
};


/**
 * Handles the content of the input box changing, either because of user
 * interaction or programmatic changes.
 * 
 * @private
 */
bluemind.ui.TagBox.prototype.handleInputChange_ = function() {
  var token = this.getTokenText_();
  this.setItemVisibilityFromToken_(token);
  if (goog.dom.getActiveElement(this.getDomHelper().getDocument()) ==
      this.input_) {
    this.maybeShowMenu_(false);
  }
  var highlighted = this.menu_.getHighlighted();
  if (token == '' || !highlighted || !highlighted.isVisible()) {
    this.setItemHighlightFromToken_(token);
  }
  this.lastToken_ = token;
};


/**
 * Loops through all menu items setting their visibility according to a token.
 * 
 * @param {string} token The token.
 * @private
 */
bluemind.ui.TagBox.prototype.setItemVisibilityFromToken_ = function(token) {
  goog.log.info(this.logger_,'setItemVisibilityFromToken_() - ' + token);
  var isVisibleItem = false;
  var count = 0;
  var recheckHidden = !this.matchFunction_(token, this.lastToken_);

  for (var i = 0, n = this.menu_.getChildCount(); i < n; i++) {
    var item = this.menu_.getChildAt(i);
    if (item.getId() == '__create__') {
      // TODO: I18N
      if (token == '' || goog.array.contains(this.getModel(), token)) {
        isVisibleItem = false;
        this.menu_.getChildAt(i - 1).setVisible(false);
      } else {
        item.setContent('Create Tag "' + this.getInputValue_() + '"...');
        item.setValue(this.getInputValue_());
        isVisibleItem = true;
      }
      item.setVisible(isVisibleItem);
    } else if (item instanceof goog.ui.MenuSeparator) {
      // Ensure that separators are only shown if there is at least one visible
      // item before them.
      item.setVisible(isVisibleItem);
      isVisibleItem = false;
    } else if (item instanceof goog.ui.MenuItem) {
      if (this.isItemSticky_(item) || (!item.isVisible() && !recheckHidden)) continue;
      var caption = item.getCaption();
      var visible = caption && this.matchFunction_(caption.toLowerCase(), token);
      if (typeof item.setFormatFromToken == 'function') {
        item.setFormatFromToken(token);
      }
      item.setVisible(!!visible);
      isVisibleItem = visible || isVisibleItem;

    } else {
      // Assume all other items are correctly using their visibility.
      isVisibleItem = item.isVisible() || isVisibleItem;
    }

    if (!(item instanceof goog.ui.MenuSeparator) && item.isVisible()) {
      count++;
    }
  }

  this.visibleCount_ = count;
};


/**
 * Highlights the first token that matches the given token.
 * 
 * @param {string} token The token.
 * @private
 */
bluemind.ui.TagBox.prototype.setItemHighlightFromToken_ = function(token) {
  goog.log.info(this.logger_,'setItemHighlightFromToken_() - ' + token);

  if (token == '') {
    this.menu_.setHighlightedIndex(-1);
    return;
  }

  for (var i = 0, n = this.menu_.getChildCount(); i < n; i++) {
    var item = this.menu_.getChildAt(i);
    if (item.isVisible() || !this.isItemSticky_(item)) {
      var caption = item.getCaption();
      if (caption && this.matchFunction_(caption.toLowerCase(), token)) {
        this.menu_.setHighlightedIndex(i);
        if (item.setFormatFromToken) {
          item.setFormatFromToken(token);
        }
        return;
      }
    }
  }
  this.menu_.setHighlightedIndex(-1);
};


/**
 * Returns true if the item has an isSticky method and the method returns true.
 * 
 * @param {goog.ui.Control} item The item.
 * @return {boolean} Whether the item has an isSticky method and the method
 *         returns true.
 * @private
 */
bluemind.ui.TagBox.prototype.isItemSticky_ = function(item) {
  return typeof item.isSticky == 'function' && item.isSticky();
};



/** @override */
bluemind.ui.TagBox.prototype.setModel = function(model) {
  if (this.getModel() != null && this.getModel().length > 0) {
    this.removeAllItems();
  }
  goog.base(this, 'setModel', model);
  goog.iter.forEach(this.getModel(), function(tag) {
    var item = new bluemind.ui.TagBoxItem(tag);
    item.setId(tag.label);
    this.addItemAt(item, this.menu_.getChildCount() - 2);
  }, this);
};

/**
 * @return {Array} The model.
 * @override
 */
bluemind.ui.TagBox.prototype.getModel = function() {
  return /** @type {Array} */ (goog.base(this, 'getModel'));
};

/**
 * Set focus on the last selected tag.
 * 
 * @private
 */
bluemind.ui.TagBox.prototype.deleteLastTag_ = function() {
  var last = this.getDomHelper().getLastElementChild(this.bullets_);
  if (last != null) {
    var tag = /** @type {bluemind.model.Tag} */ (this.selectedValues_.get(last.title));
    if (this.unselectTag_(tag)) {
      this.getDomHelper().removeNode(last);
      this.resizeInput_();
      this.dismiss();
      return true;
    }
  }
  return false;
};


/**
 * Add a new tag to the selected values.
 * 
 * @param {string} value Label of the tag to add.
 * @private
 */
bluemind.ui.TagBox.prototype.addTag_ = function(value) {
  if (value != '') {
    var tag = new bluemind.model.Tag();
    tag.label = value;
    if(this.addValue(tag)) {
      return tag;
    }
  }
  return null;
};

/**
 * Add a existing tag to the selected values.
 * 
 * @param {bluemind.model.Tag} tag Tag to select.
 * @private
 */
bluemind.ui.TagBox.prototype.selectTag_ = function(tag) {
  if(!this.selectedValues_.containsKey(tag.label)) {
    var item = this.getItem(tag.label);
    if (item != null) {
      item.setVisible(false);
      item.setSticky(true);
    }
    this.selectedValues_.set(tag.label, tag);
    return true;
  } 
  return false;
};

/**
 * Add a existing tag to the selected values.
 * 
 * @param {bluemind.model.Tag} tag Tag to remove.
 * @private
 */
bluemind.ui.TagBox.prototype.unselectTag_ = function(tag) {
  if(this.selectedValues_.containsKey(tag.label)) {
    var item = this.getItem(tag.label);
    if (item != null) {
      item.setSticky(false);
    }
    this.selectedValues_.remove(tag.label);    
    return true;
  }   
  return false;
};

/**
 * Add a existing tag to the list of the selected values.
 * 
 * @param {bluemind.model.Tag} tag Label to put in the bullet.
 * @private
 */
bluemind.ui.TagBox.prototype.createTagBullet_ = function(tag) {
  var close = this.getDomHelper().createDom('span', goog.getCssName('bm-tagbox-bullet-close'));
  var color = this.getDomHelper().createDom('span', goog.getCssName('bm-tagbox-bullet-color'));   
  var li = this.getDomHelper().createDom('li', 
      {'className': goog.getCssName('bm-tagbox-bullet'),
       'title': tag.label}, 
      color, tag.label, close);  
  if (tag.color) {
    color.style.backgroundColor = '#' + tag.color;
  }
  this.getDomHelper().appendChild(this.bullets_, li);
  this.getHandler().listenOnce(close, goog.events.EventType.CLICK, function(e) {
    if (this.unselectTag_(tag)) {
      this.getDomHelper().removeNode(li);
      this.resizeInput_();
      this.dismiss();
      this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
    }
  });
};


/**
 * Remove a existing tag to the list of the selected values.
 * 
 * @param {bluemind.model.Tag} tag Label to put in the bullet.
 * @private
 */
bluemind.ui.TagBox.prototype.removeTagBullet_ = function(tag) {
  var nodes = this.getDomHelper().getChildren(this.bullets_);
  for (var idx = 0; idx < nodes.length; idx++) {
    if (nodes[idx].title == tag.label) {
      var close = this.getDomHelper().getLastElementChild(nodes[idx]);
      this.getHandler().unlisten(close, goog.events.EventType.CLICK);
      this.getDomHelper().removeNode(nodes[idx]);
      break;
    }
  }
};



/**
 * Resize input and set cursor position.
 * 
 * @private
 */
bluemind.ui.TagBox.prototype.resizeInput_ = function() {
  var liSize, coords;
  var li = this.getDomHelper().getLastElementChild(this.bullets_);
  if (li != null) {
    liSize = goog.style.getSize(li);
    coords = goog.style.getPosition(li);
  } else {
    liSize = new goog.math.Size(0, 0);
    coords = new goog.math.Coordinate(0, 0);
  }
    coords.x += liSize.width;
    var textSize = goog.style.getSize(this.input_);
    if (textSize.width < (coords.x + 30)) {
      this.input_.style.paddingLeft = '4px';
      this.input_.style.paddingTop = (coords.y + liSize.height + 2) + 'px';
    } else {
      this.input_.style.paddingLeft = (coords.x + 4) + 'px';
      this.input_.style.paddingTop = (coords.y + 2) + 'px';
    }
};

/**
 * Add a tag to the selecteds values
 * 
 * @param {bluemind.model.Tag} tag Tag to add to the selectedValues
 */
bluemind.ui.TagBox.prototype.addValue = function(tag) {
  if (this.selectTag_(tag)) {
    if (this.isInDocument()) {
      this.createTagBullet_(tag);
      this.resizeInput_();
      this.dismiss();
      this.labelInput_.clear();
    }
    return true;
  }
  return false;
};

/**
 * remove a tag to the selecteds values
 * 
 * @param {bluemind.model.Tag} tag Tag to add to the selectedValues
 */
bluemind.ui.TagBox.prototype.removeValue = function(tag) {
  if (this.unselectTag_(tag)) {
    if (this.isInDocument()) {
      this.removeTagBullet_(tag);
      this.resizeInput_(); 
      this.dismiss();
      this.labelInput_.clear();
    }
    return true;
  }
  return false;
};

/**
 * Set selecteds values
 * 
 * @param {Array.<bluemind.model.Tag>} tags Tag to add to the selectedValues
 */
bluemind.ui.TagBox.prototype.setValue = function(tags) {
  this.resetValue();
  for (var i = 0; i < tags.length; i++) {
    var tag = tags[i];
    this.addValue(tag);
  }
};

/**
 * Reset widget values
 */
bluemind.ui.TagBox.prototype.resetValue = function() {
  var iter = this.selectedValues_.clone().getValueIterator();
  goog.iter.forEach(iter, function(tag) {
    this.removeValue(tag);
  }, this);
};

/**
 * return the selecteds values
 * 
 * @return {Array.<bluemind.model.Tag>} tags Tag to add to the selectedValues
 */
bluemind.ui.TagBox.prototype.getValue = function() {
  return this.selectedValues_.getValues();
};

/**
 * Reload widget values
 */
bluemind.ui.TagBox.prototype.reloadValue_ = function() {
  if (this.isInDocument()) {
    var nodes = this.getDomHelper().getChildren(this.bullets_);
    for (var idx = 0; idx < nodes.length; idx++) {
      var close = this.getDomHelper().getLastElementChild(nodes[idx]);
      this.getHandler().unlisten(close, goog.events.EventType.CLICK);
      this.getDomHelper().removeNode(nodes[idx]);
    }
    var iter = this.selectedValues_.getValueIterator();
    goog.iter.forEach(iter, function(tag) {
        this.createTagBullet_(tag);
    }, this);
    this.resizeInput_();
  }    
};
