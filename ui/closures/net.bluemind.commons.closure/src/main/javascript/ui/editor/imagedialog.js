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
 * @fileoverview A dialog for editing/creating a link.
 *
 */

goog.provide('bluemind.ui.editor.ImageDialog');

goog.require('bluemind.ui.editor.Image');
goog.require('bluemind.ui.editor.imagedialog.template') ;
goog.require('goog.async.Deferred');
goog.require('goog.dom');
goog.require('goog.dom.DomHelper');
goog.require('goog.dom.TagName');
goog.require('goog.dom.classes');
goog.require('goog.dom.selection');
goog.require('goog.editor.BrowserFeature');
goog.require('goog.editor.focus');
goog.require('goog.events');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventType');
goog.require('goog.events.InputHandler');
goog.require('goog.events.InputHandler.EventType');
goog.require('goog.fs.FileReader');
goog.require('goog.string');
goog.require('goog.style');
goog.require('goog.ui.Button');
goog.require('goog.ui.Dialog.Event');
goog.require('goog.ui.LinkButtonRenderer');
goog.require('goog.ui.editor.AbstractDialog');
goog.require('goog.ui.editor.AbstractDialog.Builder');
goog.require('goog.ui.editor.AbstractDialog.EventType');
goog.require('goog.ui.editor.TabPane');
goog.require('goog.ui.editor.messages');
goog.require('goog.window');
goog.require('goog.soy');



/**
 * A type of goog.ui.editor.AbstractDialog for editing/creating an image.
 * @param {goog.dom.DomHelper} domHelper DomHelper to be used to create the
 *     dialog's dom structure.
 * @param {bluemind.ui.editor.Image} image The target image.
 * @param {?string} opt_rpc Optional rpc fallback to encode image as data-url
 * @constructor
 * @extends {goog.ui.editor.AbstractDialog}
 */
bluemind.ui.editor.ImageDialog = function(domHelper, image, opt_rpc) {
  goog.base(this, domHelper);
  this.image_ = image;
  this.inputHandlers_ = [];
  /**
   * fallback rpc for data encoding.
   * @type {string|null}
   * @private
   */
  this.fallback_ = opt_rpc;
  /**
   * The event handler for this dialog.
   * @type {goog.events.EventHandler}
   * @private
   */
  this.eventHandler_ = new goog.events.EventHandler(this);
};
goog.inherits(bluemind.ui.editor.ImageDialog, goog.ui.editor.AbstractDialog);




/** @override */
bluemind.ui.editor.ImageDialog.prototype.show = function() {
  goog.base(this, 'show');

  this.selectAppropriateTab_(this.getTargetSrc_());
  this.syncOkButton_();
};


// *** Protected interface ************************************************** //


/** @override */
bluemind.ui.editor.ImageDialog.prototype.createDialogControl = function() {
  var builder = new goog.ui.editor.AbstractDialog.Builder(this);
  builder.setTitle(bluemind.ui.editor.messages.MSG_IMAGE_EDIT)
      .setContent(this.createDialogContent_());
  builder.addCancelButton(bluemind.ui.editor.messages.MSG_CANCEL);
  builder.addOkButton(bluemind.ui.editor.messages.MSG_OK);
  return builder.build();
};

/** @override */
bluemind.ui.editor.ImageDialog.prototype.createOkEvent = function(e) {
  return new goog.ui.Dialog.Event(goog.ui.Dialog.DefaultButtonKeys.OK, null);
};

/**
 * Creates and returns the event object to be used when dispatching the OK
 * event to listeners based on which tab is currently selected and the contents
 * of the input fields of that tab.
 * @return {goog.async.Deferred} The event object to be used when
 *     dispatching the OK event to listeners.
 * @protected
 */
bluemind.ui.editor.ImageDialog.prototype.generateImgSrc= function() {
  var current = this.tabPane_.getCurrentTabId();
  var tab = this.dom.getElement(current + '-tab');
  var input = /** @type {HTMLInputElement} */ (this.dom.getElementByClass(goog.getCssName('imagedialog-input'), tab));
  switch(current) {
    case bluemind.ui.editor.ImageDialog.Id_.ON_WEB_TAB:
      var url = input.value;
      if (url.search(/:/) < 0) {
        url = 'http://' + goog.string.trimLeft(url);
      }
      var d = new goog.async.Deferred();
      d.callback(url);
      return d;
      break;
    case bluemind.ui.editor.ImageDialog.Id_.INLINE_TAB:
      return bluemind.ui.editor.Image.toDataURL(input, this.fallback_); 
      break;
    default:
      return null;
  }
};


/** @override */
bluemind.ui.editor.ImageDialog.prototype.handleOk = function(e) {
  this.image_.loading(true);
  this.generateImgSrc().addCallback(function(url) {
    if (url) {
      this.image_.setSrc(url);
    }
  }, this);
  return goog.base(this, 'handleOk', e);
};

/** @override */
bluemind.ui.editor.ImageDialog.prototype.disposeInternal = function() {
  this.eventHandler_.dispose();
  this.eventHandler_ = null;

  this.tabPane_.dispose();
  this.tabPane_ = null;
  for (var i = 0; i < this.inputHandlers_.length; i++) {
    this.inputHandlers_[i].dispose();
    this.inputHandlers_[i] = null;
  }
  this.inputHandler_ = [];

  goog.base(this, 'disposeInternal');
};


// *** Private implementation *********************************************** //


/**
 * The image being modified by this dialog.
 * @type {bluemind.ui.editor.Image}
 * @private
 */
bluemind.ui.editor.ImageDialog.prototype.image_;


/**
 * EventHandler object that keeps track of all handlers set by this dialog.
 * @type {goog.events.EventHandler}
 * @private
 */
bluemind.ui.editor.ImageDialog.prototype.eventHandler_;


/**
 * InputHandler object to listen for changes in the input field.
 * @type {Array.<goog.events.InputHandler>}
 * @private
 */
bluemind.ui.editor.ImageDialog.prototype.inputHandlers_;



/**
 * The tab bar where the url and email tabs are.
 * @type {goog.ui.editor.TabPane}
 * @private
 */
bluemind.ui.editor.ImageDialog.prototype.tabPane_;

/**
 * Creates contents of this dialog.
 * @return {Element} Contents of the dialog as a DOM element.
 * @private
 */
bluemind.ui.editor.ImageDialog.prototype.createDialogContent_ = function() {
  var content = this.dom.createDom(goog.dom.TagName.DIV);
  this.tabPane_ = new goog.ui.editor.TabPane(this.dom,
    bluemind.ui.editor.messages.MSG_IMAGE_SOURCE);
  this.tabPane_.addTab(bluemind.ui.editor.ImageDialog.Id_.ON_WEB_TAB,
    bluemind.ui.editor.messages.MSG_IMAGE_FROM_URL,
    bluemind.ui.editor.messages.MSG_IMAGE_FROM_WEB,
    'image',
    this.buildTabOnTheWeb_());
  this.tabPane_.addTab(bluemind.ui.editor.ImageDialog.Id_.INLINE_TAB,
    bluemind.ui.editor.messages.MSG_IMAGE_FROM_COMPUTER,
    bluemind.ui.editor.messages.MSG_IMAGE_UPLOAD,
    'image',
    this.buildTabInline_());
  this.tabPane_.render(content);

  this.eventHandler_.listen(this.tabPane_, goog.ui.Component.EventType.SELECT,
      this.onChangeTab_);

  return content;
};


/**
* Builds and returns the div containing the tab "On the web".
* @return {Element} The div element containing the tab.
* @private
*/
bluemind.ui.editor.ImageDialog.prototype.buildTabOnTheWeb_ = function() {
  var renderer = goog.ui.LinkButtonRenderer.getInstance();
  var div = goog.soy.renderAsElement(bluemind.ui.editor.imagedialog.template.onTheWeb);
  var i = this.dom.getElementByClass(goog.getCssName('imagedialog-input'), div);
  var h = new goog.events.InputHandler(i);
  this.inputHandlers_.push(h);
  this.eventHandler_.listen(h,
      goog.events.InputHandler.EventType.INPUT,
      this.onInputChange_);
  return div;
};


/**
* Builds and returns the div containing the tab "Inline".
* @return {Element} The div element containing the tab.
* @private
*/
bluemind.ui.editor.ImageDialog.prototype.buildTabInline_ = function() {
 
  var div = goog.soy.renderAsElement(bluemind.ui.editor.imagedialog.template.inline);
  var i = this.dom.getElementByClass(goog.getCssName('imagedialog-input'), div);
  this.eventHandler_.listen(i,
      goog.events.EventType.CHANGE,
      this.onInputChange_);
  return div;
};

/**
 * Returns the source the target .
 * @return {string} The source of the target.
 * @private
 */
bluemind.ui.editor.ImageDialog.prototype.getTargetSrc_ = function() {
  return this.image_.getImage().getAttribute('src') || '';
};


/**
 * Selects the correct tab based on the src, and fills in its inputs.
 * For new links, it suggests a url based on the link text.
 * @param {string} url The href for the link.
 * @private
 */
bluemind.ui.editor.ImageDialog.prototype.selectAppropriateTab_ = function(url) {
  if (bluemind.ui.editor.Image.isInline(url)) {
    this.tabPane_.setSelectedTabId(bluemind.ui.editor.ImageDialog.Id_.INLINE_TAB);
  } else {
    // No specific tab was appropriate, default to on the web tab.
    this.tabPane_.setSelectedTabId(bluemind.ui.editor.ImageDialog.Id_.ON_WEB_TAB);
    var current = this.tabPane_.getCurrentTabId();
    var tab = this.dom.getElement(current + '-tab');
    var input = this.dom.getElementByClass(goog.getCssName('imagedialog-input'), tab);  
    input.value = this.isNewImage_() ? '' : url;
  }
};

/**
 * Called on a change to the url or email input. If either one of those tabs
 * is active, sets the OK button to enabled/disabled accordingly.
 * @private
 */
bluemind.ui.editor.ImageDialog.prototype.syncOkButton_ = function() {
  var inputValue;
  var current = this.tabPane_.getCurrentTabId();
  var tab = this.dom.getElement(current + '-tab');
  var input = this.dom.getElementByClass(goog.getCssName('imagedialog-input'), tab);
  switch(current) {
    case bluemind.ui.editor.ImageDialog.Id_.ON_WEB_TAB:
    case bluemind.ui.editor.ImageDialog.Id_.INLINE_TAB:
      inputValue = input.value;
      break;
    default:
      return;
  }
  this.getOkButtonElement().disabled = goog.string.isEmpty(inputValue);
};


/**
 * Called whenever the url or email input is edited. If the text to display
 * matches the text to display, turn on auto. Otherwise if auto is on, update
 * the text to display based on the url.
 * @private
 */
bluemind.ui.editor.ImageDialog.prototype.onInputChange_ = function() {
  this.syncOkButton_();
};


/**
 * Called when the currently selected tab changes.
 * @param {goog.events.Event} e The tab change event.
 * @private
 */
bluemind.ui.editor.ImageDialog.prototype.onChangeTab_ = function(e) {
  var tab = /** @type {goog.ui.Tab} */ (e.target);

  // Focus on the input field in the selected tab.
  //var input = this.dom.getElement(tab.getId() +
  //    bluemind.ui.editor.ImageDialog.Id_.TAB_INPUT_SUFFIX);
  //goog.editor.focus.focusInputField(input);

  //// For some reason, IE does not fire onpropertychange events when the width
  //// is specified as a percentage, which breaks the InputHandlers.
  //input.style.width = '';
  //input.style.width = input.offsetWidth + 'px';

  this.syncOkButton_();
};


/**
 * @return {boolean} Whether the image is new.
 * @private
 */
bluemind.ui.editor.ImageDialog.prototype.isNewImage_ = function() {
  return this.image_.isNew();
};


/**
 * IDs for relevant DOM elements.
 * @enum {string}
 * @private
 */
bluemind.ui.editor.ImageDialog.Id_ = {
  INLINE_TAB: 'imagedialog-inline',
  ON_WEB_TAB: 'imagedialog-onweb'
};
