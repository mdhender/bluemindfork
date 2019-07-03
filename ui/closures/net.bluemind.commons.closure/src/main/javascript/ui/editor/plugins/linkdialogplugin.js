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
 * @fileoverview A plugin for the LinkDialog.
 */

goog.provide('bluemind.ui.editor.plugins.LinkDialogPlugin');

goog.require('bluemind.ui.editor.IConfigurablePlugin');
goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.editor.Command');
goog.require('goog.editor.plugins.AbstractDialogPlugin');
goog.require('goog.events.EventHandler');
goog.require('goog.functions');
goog.require('goog.ui.editor.AbstractDialog.EventType');
goog.require('bluemind.ui.editor.LinkDialog');
goog.require('bluemind.ui.editor.LinkDialog.EventType');
goog.require('bluemind.ui.editor.LinkDialog.OkEvent');
goog.require('goog.uri.utils');



/**
 * A plugin that opens the link dialog.
 * @constructor
 * @implements {bluemind.ui.editor.IConfigurablePlugin}
 * @extends {goog.editor.plugins.AbstractDialogPlugin}
 */
bluemind.ui.editor.plugins.LinkDialogPlugin = function() {
  goog.base(this, goog.editor.Command.MODAL_LINK_EDITOR);

  /**
   * Event handler for this object.
   * @type {goog.events.EventHandler}
   * @private
   */
  this.eventHandler_ = new goog.events.EventHandler(this);


  /**
   * A list of whitelisted URL schemes which are safe to open.
   * @type {Array.<string>}
   * @private
   */
  this.safeToOpenSchemes_ = ['http', 'https', 'ftp'];
};
goog.inherits(bluemind.ui.editor.plugins.LinkDialogPlugin,
    goog.editor.plugins.AbstractDialogPlugin);


/**
 * Link object that the dialog is editing.
 * @type {goog.editor.Link}
 * @protected
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.currentLink_;


/**
 * Whether to block opening links with a non-whitelisted URL scheme.
 * @type {boolean}
 * @private
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.blockOpeningUnsafeSchemes_ =
    true;


/** @override */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.getTrogClassId =
    goog.functions.constant('LinkDialogPlugin');

/**
 * @type {*} Plugin's option
 * private
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.options_;

/** @override */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.getOptions = function() {
  return this.options_;
};

/** @override */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.setOptions = function(options) {
  this.options_ = options;
};

/**
 * Tells the plugin whether to block URLs with schemes not in the whitelist.
 * If blocking is enabled, this plugin will stop the 'Test Link' popup
 * window from being created. Blocking doesn't affect link creation--if the
 * user clicks the 'OK' button with an unsafe URL, the link will still be
 * created as normal.
 * @param {boolean} blockOpeningUnsafeSchemes Whether to block non-whitelisted
 *     schemes.
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.setBlockOpeningUnsafeSchemes =
    function(blockOpeningUnsafeSchemes) {
  this.blockOpeningUnsafeSchemes_ = blockOpeningUnsafeSchemes;
};


/**
 * Sets a whitelist of allowed URL schemes that are safe to open.
 * Schemes should all be in lowercase. If the plugin is set to block opening
 * unsafe schemes, user-entered URLs will be converted to lowercase and checked
 * against this list. The whitelist has no effect if blocking is not enabled.
 * @param {Array.<string>} schemes String array of URL schemes to allow (http,
 *     https, etc.).
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.setSafeToOpenSchemes =
    function(schemes) {
  this.safeToOpenSchemes_ = schemes;
};


/**
 * Handles execCommand by opening the dialog.
 * @param {string} command The command to execute.
 * @param {*=} opt_arg {@link A goog.editor.Link} object representing the link
 *     being edited.
 * @return {*} Always returns true, indicating the dialog was shown.
 * @protected
 * @override
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.execCommandInternal = function(
    command, opt_arg) {
  this.currentLink_ = /** @type {goog.editor.Link} */(opt_arg);
  return goog.base(this, 'execCommandInternal', command, opt_arg);
};


/**
 * Handles when the dialog closes.
 * @param {goog.events.Event} e The AFTER_HIDE event object.
 * @override
 * @protected
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.handleAfterHide = function(e) {
  goog.base(this, 'handleAfterHide', e);
  this.currentLink_ = null;
};


/**
 * @return {goog.events.EventHandler} The event handler.
 * @protected
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.getEventHandler = function() {
  return this.eventHandler_;
};


/**
 * @return {goog.editor.Link} The link being edited.
 * @protected
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.getCurrentLink = function() {
  return this.currentLink_;
};


/**
 * Creates a new instance of the dialog and registers for the relevant events.
 * @param {goog.dom.DomHelper} dialogDomHelper The dom helper to be used to
 *     create the dialog.
 * @param {*=} opt_link The target link (should be a goog.editor.Link).
 * @return {bluemind.ui.editor.LinkDialog} The dialog.
 * @override
 * @protected
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.createDialog = function(
    dialogDomHelper, opt_link) {
  var dialog = new bluemind.ui.editor.LinkDialog(dialogDomHelper,
      /** @type {goog.editor.Link} */ (opt_link));
  this.eventHandler_.
      listen(dialog, goog.ui.editor.AbstractDialog.EventType.OK,
          this.handleOk).
      listen(dialog, goog.ui.editor.AbstractDialog.EventType.CANCEL,
          this.handleCancel_).
      listen(dialog, bluemind.ui.editor.LinkDialog.EventType.BEFORE_TEST_LINK,
          this.handleBeforeTestLink);
  return dialog;
};


/** @override */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.eventHandler_.dispose();
};


/**
 * Handles the OK event from the dialog by updating the link in the field.
 * @param {bluemind.ui.editor.LinkDialog.OkEvent} e OK event object.
 * @protected
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.handleOk = function(e) {
  // We're not restoring the original selection, so clear it out.
  this.disposeOriginalSelection();

  this.currentLink_.setTextAndUrl(e.linkText, e.linkUrl);

  var anchor = this.currentLink_.getAnchor();
  var extraAnchors = this.currentLink_.getExtraAnchors();
  for (var i = 0; i < extraAnchors.length; ++i) {
    extraAnchors[i].href = anchor.href;
  }
  anchor.target = '_blank';

  // Place cursor to the right of the modified link.
  this.currentLink_.placeCursorRightOf();

  this.getFieldObject().focus();

  this.getFieldObject().dispatchSelectionChangeEvent();
  this.getFieldObject().dispatchChange();

  this.eventHandler_.removeAll();
};

/**
 * Handles the CANCEL event from the dialog by clearing the anchor if needed.
 * @param {goog.events.Event} e Event object.
 * @private
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.handleCancel_ = function(e) {
  if (this.currentLink_.isNew()) {
    goog.dom.flattenElement(this.currentLink_.getAnchor());
    var extraAnchors = this.currentLink_.getExtraAnchors();
    for (var i = 0; i < extraAnchors.length; ++i) {
      goog.dom.flattenElement(extraAnchors[i]);
    }
    // Make sure listeners know the anchor was flattened out.
    this.getFieldObject().dispatchChange();
  }

  this.eventHandler_.removeAll();
};


/**
 * Handles the BeforeTestLink event fired when the 'test' link is clicked.
 * @param {bluemind.ui.editor.LinkDialog.BeforeTestLinkEvent} e BeforeTestLink event
 *     object.
 * @protected
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.handleBeforeTestLink =
    function(e) {
  if (!this.shouldOpenUrl(e.url)) {
    alert(bluemind.ui.editor.messages.MSG_UNSAFE_LINK);
    e.preventDefault();
  }
};


/**
 * Checks whether the plugin should open the given url in a new window.
 * @param {string} url The url to check.
 * @return {boolean} If the plugin should open the given url in a new window.
 * @protected
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.shouldOpenUrl = function(url) {
  return !this.blockOpeningUnsafeSchemes_ || this.isSafeSchemeToOpen_(url);
};


/**
 * Determines whether or not a url has a scheme which is safe to open.
 * Schemes like javascript are unsafe due to the possibility of XSS.
 * @param {string} url A url.
 * @return {boolean} Whether the url has a safe scheme.
 * @private
 */
bluemind.ui.editor.plugins.LinkDialogPlugin.prototype.isSafeSchemeToOpen_ =
    function(url) {
  var scheme = goog.uri.utils.getScheme(url) || 'http';
  return goog.array.contains(this.safeToOpenSchemes_, scheme.toLowerCase());
};

