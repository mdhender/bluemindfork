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
 * @fileoverview Base class for bubble plugins.
 *
 */

goog.provide('bluemind.ui.editor.plugins.LinkBubble');
goog.provide('bluemind.ui.editor.plugins.LinkBubble.Action');

goog.require('bluemind.ui.editor.IConfigurablePlugin');
goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.editor.BrowserFeature');
goog.require('goog.editor.Command');
goog.require('goog.editor.Link');
goog.require('goog.editor.plugins.AbstractBubblePlugin');
goog.require('goog.editor.range');
goog.require('goog.string');
goog.require('goog.style');
goog.require('goog.ui.editor.messages');
goog.require('goog.uri.utils');
goog.require('goog.window');



/**
 * Property bubble plugin for links.
 * @param {...!bluemind.ui.editor.plugins.LinkBubble.Action} var_args List of
 *     extra actions supported by the bubble.
 * @implements {bluemind.ui.editor.IConfigurablePlugin}
 * @constructor
 * @extends {goog.editor.plugins.AbstractBubblePlugin}
 */
bluemind.ui.editor.plugins.LinkBubble = function(var_args) {
  goog.base(this);

  /**
   * List of extra actions supported by the bubble.
   * @type {Array.<!bluemind.ui.editor.plugins.LinkBubble.Action>}
   * @private
   */
  this.extraActions_ = goog.array.toArray(arguments);

  /**
   * List of spans corresponding to the extra actions.
   * @type {Array.<!Element>}
   * @private
   */
  this.actionSpans_ = [];

  /**
   * A list of whitelisted URL schemes which are safe to open.
   * @type {Array.<string>}
   * @private
   */
  this.safeToOpenSchemes_ = ['http', 'https', 'ftp'];
};
goog.inherits(bluemind.ui.editor.plugins.LinkBubble,
    goog.editor.plugins.AbstractBubblePlugin);


/**
 * Element id for the link text.
 * type {string}
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.LINK_TEXT_ID_ = 'tr_link-text';


/**
 * Element id for the test link span.
 * type {string}
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.TEST_LINK_SPAN_ID_ = 'tr_test-link-span';


/**
 * Element id for the test link.
 * type {string}
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.TEST_LINK_ID_ = 'tr_test-link';


/**
 * Element id for the change link span.
 * type {string}
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.CHANGE_LINK_SPAN_ID_ = 'tr_change-link-span';


/**
 * Element id for the link.
 * type {string}
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.CHANGE_LINK_ID_ = 'tr_change-link';


/**
 * Element id for the delete link span.
 * type {string}
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.DELETE_LINK_SPAN_ID_ = 'tr_delete-link-span';


/**
 * Element id for the delete link.
 * type {string}
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.DELETE_LINK_ID_ = 'tr_delete-link';


/**
 * Element id for the link bubble wrapper div.
 * type {string}
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.LINK_DIV_ID_ = 'tr_link-div';


/**
 * Whether to block opening links with a non-whitelisted URL scheme.
 * @type {boolean}
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.blockOpeningUnsafeSchemes_ =
    true;

/**
 * @type {*} Plugin's option
 * private
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.options_;

/** @override */
bluemind.ui.editor.plugins.LinkBubble.prototype.getOptions = function() {
  return this.options_;
};

/** @override */
bluemind.ui.editor.plugins.LinkBubble.prototype.setOptions = function(options) {
  this.options_ = options;
};

/**
 * Tells the plugin whether to block URLs with schemes not in the whitelist.
 * If blocking is enabled, this plugin will not linkify the link in the bubble
 * popup.
 * @param {boolean} blockOpeningUnsafeSchemes Whether to block non-whitelisted
 *     schemes.
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.setBlockOpeningUnsafeSchemes =
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
bluemind.ui.editor.plugins.LinkBubble.prototype.setSafeToOpenSchemes =
    function(schemes) {
  this.safeToOpenSchemes_ = schemes;
};


/** @override */
bluemind.ui.editor.plugins.LinkBubble.prototype.getTrogClassId = function() {
  return 'LinkBubble';
};


/** @override */
bluemind.ui.editor.plugins.LinkBubble.prototype.isSupportedCommand =
    function(command) {
  return command == goog.editor.Command.UPDATE_LINK_BUBBLE;
};


/** @override */
bluemind.ui.editor.plugins.LinkBubble.prototype.execCommandInternal =
    function(command, var_args) {
  if (command == goog.editor.Command.UPDATE_LINK_BUBBLE) {
    this.updateLink_();
  }
};


/**
 * Updates the href in the link bubble with a new link.
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.updateLink_ = function() {
  var targetEl = this.getTargetElement();
  this.closeBubble();
  this.createBubble(targetEl);
};


/** @override */
bluemind.ui.editor.plugins.LinkBubble.prototype.getBubbleTargetFromSelection =
    function(selectedElement) {
  var bubbleTarget = goog.dom.getAncestorByTagNameAndClass(selectedElement,
      goog.dom.TagName.A);

  if (!bubbleTarget) {
    // See if the selection is touching the right side of a link, and if so,
    // show a bubble for that link.  The check for "touching" is very brittle,
    // and currently only guarantees that it will pop up a bubble at the
    // position the cursor is placed at after the link dialog is closed.
    // NOTE(robbyw): This assumes this method is always called with
    // selected element = range.getContainerElement().  Right now this is true,
    // but attempts to re-use this method for other purposes could cause issues.
    // TODO(robbyw): Refactor this method to also take a range, and use that.
    var range = this.getFieldObject().getRange();
    if (range && range.isCollapsed() && range.getStartOffset() == 0) {
      var startNode = range.getStartNode();
      var previous = startNode.previousSibling;
      if (previous && previous.tagName == goog.dom.TagName.A) {
        bubbleTarget = previous;
      }
    }
  }

  return /** @type {Element} */ (bubbleTarget);
};


/**
 * Set the optional function for getting the "test" link of a url.
 * @param {function(string) : string} func The function to use.
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.setTestLinkUrlFn = function(func) {
  this.testLinkUrlFn_ = func;
};


/**
 * Returns the target element url for the bubble.
 * @return {string} The url href.
 * @protected
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.getTargetUrl = function() {
  // Get the href-attribute through getAttribute() rather than the href property
  // because Google-Toolbar on Firefox with "Send with Gmail" turned on
  // modifies the href-property of 'mailto:' links but leaves the attribute
  // untouched.
  return this.getTargetElement().getAttribute('href') || '';
};


/** @override */
bluemind.ui.editor.plugins.LinkBubble.prototype.getBubbleType = function() {
  return goog.dom.TagName.A;
};


/** @override */
bluemind.ui.editor.plugins.LinkBubble.prototype.getBubbleTitle = function() {
  return bluemind.ui.editor.messages.MSG_LINK_CAPTION;
};


/** @override */
bluemind.ui.editor.plugins.LinkBubble.prototype.createBubbleContents = function(
    bubbleContainer) {
  var linkObj = this.getLinkToTextObj_();

  // Create linkTextSpan, show plain text for e-mail address or truncate the
  // text to <= 48 characters so that property bubbles don't grow too wide and
  // create a link if URL.  Only linkify valid links.
  // TODO(robbyw): Repalce this color with a CSS class.
  var color = linkObj.valid ? 'black' : 'red';
  var shouldOpenUrl = this.shouldOpenUrl(linkObj.linkText);
  var linkTextSpan;
  if (goog.editor.Link.isLikelyEmailAddress(linkObj.linkText) ||
      !linkObj.valid || !shouldOpenUrl) {
    linkTextSpan = this.dom_.createDom(goog.dom.TagName.SPAN,
        {
          id: bluemind.ui.editor.plugins.LinkBubble.LINK_TEXT_ID_,
          style: 'color:' + color
        }, this.dom_.createTextNode(linkObj.linkText));
  } else {
    var testMsgSpan = this.dom_.createDom(goog.dom.TagName.SPAN,
        {id: bluemind.ui.editor.plugins.LinkBubble.TEST_LINK_SPAN_ID_},
        bluemind.ui.editor.messages.MSG_LINK_BUBBLE_TEST_LINK);
    linkTextSpan = this.dom_.createDom(goog.dom.TagName.SPAN,
        {
          id: bluemind.ui.editor.plugins.LinkBubble.LINK_TEXT_ID_,
          style: 'color:' + color
        }, '');
    var linkText = goog.string.truncateMiddle(linkObj.linkText, 48);
    this.createLink(bluemind.ui.editor.plugins.LinkBubble.TEST_LINK_ID_,
                    this.dom_.createTextNode(linkText).data,
                    this.testLink,
                    linkTextSpan);
  }

  var changeLinkSpan = this.createLinkOption(
      bluemind.ui.editor.plugins.LinkBubble.CHANGE_LINK_SPAN_ID_);
  this.createLink(bluemind.ui.editor.plugins.LinkBubble.CHANGE_LINK_ID_,
      bluemind.ui.editor.messages.MSG_BUBBLE_CHANGE, this.showLinkDialog_, changeLinkSpan);

  // This function is called multiple times - we have to reset the array.
  this.actionSpans_ = [];
  for (var i = 0; i < this.extraActions_.length; i++) {
    var action = this.extraActions_[i];
    var actionSpan = this.createLinkOption(action.spanId_);
    this.actionSpans_.push(actionSpan);
    this.createLink(action.linkId_, action.message_,
        function() {
          action.actionFn_(this.getTargetUrl());
        },
        actionSpan);
  }

  var removeLinkSpan = this.createLinkOption(
      bluemind.ui.editor.plugins.LinkBubble.DELETE_LINK_SPAN_ID_);
  this.createLink(bluemind.ui.editor.plugins.LinkBubble.DELETE_LINK_ID_,
      bluemind.ui.editor.messages.MSG_BUBBLE_REMOVE, this.deleteLink_, removeLinkSpan);

  this.onShow();

  var bubbleContents = this.dom_.createDom(goog.dom.TagName.DIV,
      {id: bluemind.ui.editor.plugins.LinkBubble.LINK_DIV_ID_},
      testMsgSpan || '', linkTextSpan, changeLinkSpan);

  for (i = 0; i < this.actionSpans_.length; i++) {
    bubbleContents.appendChild(this.actionSpans_[i]);
  }
  bubbleContents.appendChild(removeLinkSpan);

  goog.dom.appendChild(bubbleContainer, bubbleContents);
};


/**
 * Tests the link by opening it in a new tab/window. Should be used as the
 * click event handler for the test pseudo-link.
 * @protected
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.testLink = function() {
  goog.window.open(this.getTestLinkAction_(),
      {
        'target': '_blank'
      }, this.getFieldObject().getAppWindow());
};


/**
 * Returns whether the URL should be considered invalid.  This always returns
 * false in the base class, and should be overridden by subclasses that wish
 * to impose validity rules on URLs.
 * @param {string} url The url to check.
 * @return {boolean} Whether the URL should be considered invalid.
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.isInvalidUrl = goog.functions.FALSE;


/**
 * Gets the text to display for a link, based on the type of link
 * @return {Object} Returns an object of the form:
 *     {linkText: displayTextForLinkTarget, valid: ifTheLinkIsValid}.
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.getLinkToTextObj_ = function() {
  var isError;
  var targetUrl = this.getTargetUrl();

  if (this.isInvalidUrl(targetUrl)) {
    /**
     * @desc Message shown in a link bubble when the link is not a valid url.
     */
    targetUrl = bluemind.ui.editor.messages.MSG_INVALID_URL_LINK_BUBBLE;
    isError = true;
  } else if (goog.editor.Link.isMailto(targetUrl)) {
    targetUrl = targetUrl.substring(7); // 7 == "mailto:".length
  }

  return {linkText: targetUrl, valid: !isError};
};


/**
 * Shows the link dialog.
 * @param {goog.events.BrowserEvent} e The event.
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.showLinkDialog_ = function(e) {
  // Needed when this occurs due to an ENTER key event, else the newly created
  // dialog manages to have its OK button pressed, causing it to disappear.
  e.preventDefault();

  this.getFieldObject().execCommand(goog.editor.Command.MODAL_LINK_EDITOR,
      new goog.editor.Link(
          /** @type {HTMLAnchorElement} */ (this.getTargetElement()),
          false));
  this.closeBubble();
};


/**
 * Deletes the link associated with the bubble
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.deleteLink_ = function() {
  this.getFieldObject().dispatchBeforeChange();

  var link = this.getTargetElement();
  var child = link.lastChild;
  goog.dom.flattenElement(link);
  goog.editor.range.placeCursorNextTo(child, false);

  this.closeBubble();

  this.getFieldObject().dispatchChange();
  this.getFieldObject().focus();
};


/**
 * Sets the proper state for the action links.
 * @protected
 * @override
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.onShow = function() {
  var linkDiv = this.dom_.getElement(
      bluemind.ui.editor.plugins.LinkBubble.LINK_DIV_ID_);
  if (linkDiv) {
    var testLinkSpan = this.dom_.getElement(
        bluemind.ui.editor.plugins.LinkBubble.TEST_LINK_SPAN_ID_);
    if (testLinkSpan) {
      var url = this.getTargetUrl();
      goog.style.setElementShown(testLinkSpan, !goog.editor.Link.isMailto(url));
    }

    for (var i = 0; i < this.extraActions_.length; i++) {
      var action = this.extraActions_[i];
      var actionSpan = this.dom_.getElement(action.spanId_);
      if (actionSpan) {
        goog.style.setElementShown(actionSpan, action.toShowFn_(
            this.getTargetUrl()));
      }
    }
  }
};


/**
 * Gets the url for the bubble test link.  The test link is the link in the
 * bubble the user can click on to make sure the link they entered is correct.
 * @return {string} The url for the bubble link href.
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.getTestLinkAction_ = function() {
  var targetUrl = this.getTargetUrl();
  return this.testLinkUrlFn_ ? this.testLinkUrlFn_(targetUrl) : targetUrl;
};


/**
 * Checks whether the plugin should open the given url in a new window.
 * @param {string} url The url to check.
 * @return {boolean} If the plugin should open the given url in a new window.
 * @protected
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.shouldOpenUrl = function(url) {
  return !this.blockOpeningUnsafeSchemes_ || this.isSafeSchemeToOpen_(url);
};


/**
 * Determines whether or not a url has a scheme which is safe to open.
 * Schemes like javascript are unsafe due to the possibility of XSS.
 * @param {string} url A url.
 * @return {boolean} Whether the url has a safe scheme.
 * @private
 */
bluemind.ui.editor.plugins.LinkBubble.prototype.isSafeSchemeToOpen_ =
    function(url) {
  var scheme = goog.uri.utils.getScheme(url) || 'http';
  return goog.array.contains(this.safeToOpenSchemes_, scheme.toLowerCase());
};



/**
 * Constructor for extra actions that can be added to the link bubble.
 * @param {string} spanId The ID for the span showing the action.
 * @param {string} linkId The ID for the link showing the action.
 * @param {string} message The text for the link showing the action.
 * @param {function(string):boolean} toShowFn Test function to determine whether
 *     to show the action for the given URL.
 * @param {function(string):void} actionFn Action function to run when the
 *     action is clicked.  Takes the current target URL as a parameter.
 * @constructor
 */
bluemind.ui.editor.plugins.LinkBubble.Action = function(spanId, linkId, message,
    toShowFn, actionFn) {
  this.spanId_ = spanId;
  this.linkId_ = linkId;
  this.message_ = message;
  this.toShowFn_ = toShowFn;
  this.actionFn_ = actionFn;
};

