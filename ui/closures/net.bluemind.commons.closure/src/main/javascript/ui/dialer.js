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
 * @fileoverview Dialer widget.
 */

goog.provide('bluemind.ui.Dialer');
goog.provide('bluemind.ui.dialer.CallEvent');
goog.provide('bluemind.ui.dialer.RowRenderer');
goog.provide('bluemind.ui.dialer.Matcher');

goog.require('bluemind.ui.Input');
goog.require('bluemind.ui.dialer.template.rowrenderer');
goog.require('bluemind.string');
goog.require('goog.dom.classlist');
goog.require('goog.events.Event');
goog.require('goog.events.EventType');
goog.require('goog.ui.ac');
goog.require('goog.ui.ac.Renderer.CustomRenderer');
goog.require('goog.ui.ac.RemoteArrayMatcher');
goog.require('goog.ui.ac.Renderer');
goog.require('goog.ui.ac.InputHandler');
goog.require('goog.ui.ac.AutoComplete');
goog.require('goog.ui.Component');
goog.require('goog.ui.Button');
goog.require('goog.ui.Popup');
goog.require('goog.positioning.Corner');
goog.require('goog.positioning.AnchoredPosition');
goog.require('goog.style');
goog.require('goog.ui.style.app.ButtonRenderer');
goog.require('goog.Timer');

/**
 * A dialer component.
 * 
 * @param {string=} opt_content Optional label for input field.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM hepler, used for
 *          document interaction.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.ui.Dialer = function(opt_content, opt_domHelper) {
  goog.base(this, opt_domHelper);
  this.input_ = new bluemind.ui.Input('');
  this.addChild(this.input_, true);
  this.button_ = new goog.ui.Button(this.getDomHelper().createDom('div',
      goog.getCssName('dial-button') + ' ' + goog.getCssName('goog-inline-block')), goog.ui.style.app.ButtonRenderer
      .getInstance());
  this.addChild(this.button_, true);
};

goog.inherits(bluemind.ui.Dialer, goog.ui.Component);

/**
 * Url for autocompletion.
 * 
 * @type {string}
 * @expose
 * @public
 */
bluemind.ui.Dialer.prototype.url;

/**
 * Autocomplete mechanism
 * 
 * @type {goog.ui.ac.AutoComplete}
 * @private
 */
bluemind.ui.Dialer.prototype.autocomplete_;

/**
 * Component visibility.
 * 
 * @type {boolean}
 * @private
 */
bluemind.ui.Dialer.prototype.visible_ = true;

/**
 * The Popup element used to position and display the bubble.
 * 
 * @type {goog.ui.Popup}
 * @private
 */
bluemind.ui.Dialer.prototype.popup_;

/**
 * Dialer input.
 * 
 * @type {bluemind.ui.Input}
 * @private
 */
bluemind.ui.Dialer.prototype.input_;

/**
 * Dialer call button.
 * 
 * @type {goog.ui.Button}
 * @private
 */
bluemind.ui.Dialer.prototype.button_;

/** @override */
bluemind.ui.Dialer.prototype.createDom = function() {
  goog.base(this, 'createDom');
  goog.dom.classlist.add(this.getElement(), goog.getCssName('dialer'));
  this.input_.addClassName(goog.getCssName('goog-inline-block'));
  this.popup_ = new goog.ui.Popup();
  this.popup_.setElement(this.getElement());
  this.popup_.setAutoHide(false);
  this.popup_.setHideOnEscape(false);
  this.popup_.setPinnedCorner(goog.positioning.Corner.TOP_RIGHT);
  this.popup_.setVisible(false);
};

/**
 * Set position depending on an element. If a parent is set and no element is
 * provided the parent element will be used.
 * 
 * @private
 */
bluemind.ui.Dialer.prototype.setPosition_ = function() {
  if (this.getParent()) {
    var el = this.getParent().getElement();
  } else {
    var el = this.getDomHelper().getParentElement(this.getElement());
  }
  var position = new goog.positioning.AnchoredPosition(el, goog.positioning.Corner.BOTTOM_RIGHT);
  this.popup_.setPosition(position);
};

/** @override */
bluemind.ui.Dialer.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  if (this.popup_) {
    this.popup_.dispose();
    this.popup_ = null;
  }
  if (this.autocomplete_) {
    this.autocomplete_.dispose();
    this.autocomplete_ = null;
  }
};

/** @override */
bluemind.ui.Dialer.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.button_, goog.ui.Component.EventType.ACTION, this.performActionInternal);
  this.getHandler().listen(this.input_, goog.ui.Component.EventType.ACTION, this.performActionInternal);
  if (this.url) {
    var matcher = new bluemind.ui.dialer.Matcher(this.url, false);
    var renderer = new goog.ui.ac.Renderer(null, new bluemind.ui.dialer.RowRenderer());
    renderer.setWidthProvider(this.input_.getElement());
    renderer.className = goog.getCssName('dialer-renderer');
    var handler = new goog.ui.ac.InputHandler();
    handler.setUpdateDuringTyping(false);
    handler.setThrottleTime(500);
    this.autocomplete_ = new goog.ui.ac.AutoComplete(matcher, renderer, handler);
    handler.attachAutoComplete(this.autocomplete_);
    this.autocomplete_.attachInputs(this.input_.getKeyEventTarget());
    this.getHandler().listen(this.autocomplete_, goog.ui.ac.AutoComplete.EventType.UPDATE, this.handleAutocomplete_);
    this.getHandler().listen(this.input_.getKeyEventTarget(), goog.events.EventType.KEYDOWN, this.handleKeyDown);
  }
};

/**
 * Shows or hides the component. Does nothing if the component already has the
 * requested visibility.
 * 
 * @param {boolean} visible Whether to show or hide the component.
 */
bluemind.ui.Dialer.prototype.setVisible = function(visible) {
  if (!this.popup_.getPosition()) {
    this.setPosition_();
  }
  this.popup_.setVisible(visible);
};

/**
 * Shows or hides the component. already has the requested visibility, and
 * doesn't dispatch any events.
 */
bluemind.ui.Dialer.prototype.toggleVisibility = function() {
  this.setVisible(!this.popup_.isVisible());
};

/**
 * Attempts to handle a keyboard event.
 * 
 * @param {goog.events.Event} e Key event to handle.
 * @protected
 */
bluemind.ui.Dialer.prototype.handleKeyDown = function(e) {
  if (e.keyCode == goog.events.KeyCodes.ENTER && this.autocomplete_.hasHighlight()) {
    this.autocomplete_.selectHilited();
  }
  if (e.keyCode == goog.events.KeyCodes.ESC && !this.autocomplete_.isOpen()) {
    this.setVisible(false);
  }
};

/**
 * Performs the appropriate action when the call action is performed.
 * 
 * @param {goog.events.Event} e Event that triggered the action.
 * @return {boolean} Whether the action is allowed to proceed.
 * @protected
 */
bluemind.ui.Dialer.prototype.performActionInternal = function(e) {
  e.stopPropagation();
  if (goog.string.trim(this.input_.getValue()) != '') {
    this.dialing_();
    var actionEvent = new bluemind.ui.dialer.CallEvent(goog.ui.Component.EventType.ACTION, this.input_.getValue(), this);
    if (e) {
      actionEvent.altKey = e.altKey;
      actionEvent.ctrlKey = e.ctrlKey;
      actionEvent.metaKey = e.metaKey;
      actionEvent.shiftKey = e.shiftKey;
      actionEvent.platformModifierKey = e.platformModifierKey;
    }
    return this.dispatchEvent(actionEvent);
  }
  return false;
};

/**
 * Fake dialing effect
 */
bluemind.ui.Dialer.prototype.dialing_ = function() {
  this.input_.setEnabled(false);
  this.button_.setEnabled(false);
  var value = this.input_.getValue();
  var count = 0;
  var timer = new goog.Timer(300);
  this.getHandler().listen(timer, goog.Timer.TICK, function(e) {
    count++;
    if (count > 10) {
      this.input_.setValue('');
      this.input_.setPlaceholder(value);
      this.input_.setEnabled(true);
      this.button_.setEnabled(true);
      timer.stop();
    } else {
      var v = value;
      for (var i = 0; i < (count % 4); i++) {
        v += '.';
      }
      this.input_.setValue(v)
    }
  });
  timer.start();
};

/**
 * Handle autocomplete value selected
 * 
 * @param {Object} e autocomple event
 * @private
 */
bluemind.ui.Dialer.prototype.handleAutocomplete_ = function(e) {
  var contact = e.row;
  this.input_.setValue(contact['phone']);
};

/**
 * Object representing a property change event.
 * 
 * @param {string} type Event Type.
 * @param {string} number Call number.
 * @param {Object=} opt_target Reference to the object that is the target of
 *          this event.
 * @constructor
 * @extends {goog.events.Event}
 */
bluemind.ui.dialer.CallEvent = function(type, number, opt_target) {
  goog.base(this, type, opt_target);
  this.number = number;
};

goog.inherits(bluemind.ui.dialer.CallEvent, goog.events.Event);

/**
 * called number.
 * 
 * @type {string}
 * @public
 * @expose
 */
bluemind.ui.dialer.CallEvent.prototype.number;

/**
 * Rendering the autocomplete box for contact to dial.
 * 
 * @constructor
 * @extends goog.ui.ac.Renderer.CustomRenderer
 */
bluemind.ui.dialer.RowRenderer = function() {
};
goog.inherits(bluemind.ui.dialer.RowRenderer, goog.ui.ac.Renderer.CustomRenderer);

/** @override */
bluemind.ui.dialer.RowRenderer.prototype.render = null;

/** @override */
bluemind.ui.dialer.RowRenderer.prototype.renderRow = function(row, token, node) {
  node.innerHTML = bluemind.ui.dialer.template.rowrenderer.main({
    entry : row.data
  });
};

/**
 * Matcher the autocomplete box for contact to dial.
 * 
 * @constructor
 * @extends goog.ui.ac.RemoteArrayMatcher
 */
bluemind.ui.dialer.Matcher = function(url, opt_noSimilar) {
  goog.base(this, url, !!opt_noSimilar);
};
goog.inherits(bluemind.ui.dialer.Matcher, goog.ui.ac.RemoteArrayMatcher);

/** @override */
bluemind.ui.dialer.Matcher.prototype.buildUrl = function(uri, token, maxMatches, useSimilar, opt_fullString) {
  token = bluemind.string.normalize(token) + "*";
  return goog.base(this, 'buildUrl', uri, token, maxMatches, useSimilar, opt_fullString);
};

/** @override */
bluemind.ui.dialer.Matcher.prototype.requestMatchingRows = function(token, maxMatches, matchHandler, opt_fullString) {
  if (token.length < 3) {
    matchHandler(token, []);
  } else {
    goog.base(this, 'requestMatchingRows', token, maxMatches, matchHandler, opt_fullString);
  }
};

goog.exportSymbol('bluemind.ui.Dialer', bluemind.ui.Dialer);

goog.exportProperty(bluemind.ui.Dialer.prototype, 'render', bluemind.ui.Dialer.prototype.render);
goog.exportProperty(bluemind.ui.Dialer.prototype, 'toggleVisibility', bluemind.ui.Dialer.prototype.toggleVisibility);
goog.exportProperty(goog.events.EventTarget.prototype, 'addEventListener',
    bluemind.ui.Dialer.prototype.addEventListener);
