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

goog.provide('bluemind.ui.editor.LinkDialog');
goog.provide('bluemind.ui.editor.LinkDialog.BeforeTestLinkEvent');
goog.provide('bluemind.ui.editor.LinkDialog.EventType');
goog.provide('bluemind.ui.editor.LinkDialog.OkEvent');

goog.require('bluemind.ui.editor.messages');
goog.require('goog.dom');
goog.require('goog.dom.DomHelper');
goog.require('goog.dom.TagName');
goog.require('goog.dom.classes');
goog.require('goog.dom.selection');
goog.require('goog.editor.BrowserFeature');
goog.require('goog.editor.Link');
goog.require('goog.editor.focus');
goog.require('goog.events');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventType');
goog.require('goog.events.InputHandler');
goog.require('goog.events.InputHandler.EventType');
goog.require('goog.string');
goog.require('goog.style');
goog.require('goog.ui.Button');
goog.require('goog.ui.LinkButtonRenderer');
goog.require('goog.ui.editor.AbstractDialog');
goog.require('goog.ui.editor.AbstractDialog.Builder');
goog.require('goog.ui.editor.AbstractDialog.EventType');
goog.require('goog.ui.editor.TabPane');
goog.require('goog.userAgent');
goog.require('goog.window');



/**
 * A type of goog.ui.editor.AbstractDialog for editing/creating a link.
 * @param {goog.dom.DomHelper} domHelper DomHelper to be used to create the
 *     dialog's dom structure.
 * @param {goog.editor.Link} link The target link.
 * @constructor
 * @extends {goog.ui.editor.AbstractDialog}
 */
bluemind.ui.editor.LinkDialog = function(domHelper, link) {
  goog.base(this, domHelper);
  this.targetLink_ = link;

  /**
   * The event handler for this dialog.
   * @type {goog.events.EventHandler}
   * @private
   */
  this.eventHandler_ = new goog.events.EventHandler(this);
};
goog.inherits(bluemind.ui.editor.LinkDialog, goog.ui.editor.AbstractDialog);


/**
 * Events specific to the link dialog.
 * @enum {string}
 */
bluemind.ui.editor.LinkDialog.EventType = {
  BEFORE_TEST_LINK: 'beforetestlink'
};



/**
 * OK event object for the link dialog.
 * @param {string} linkText Text the user chose to display for the link.
 * @param {string} linkUrl Url the user chose for the link to point to.
 * @constructor
 * @extends {goog.events.Event}
 */
bluemind.ui.editor.LinkDialog.OkEvent = function(linkText, linkUrl) {
  goog.base(this, goog.ui.editor.AbstractDialog.EventType.OK);

  /**
   * The text of the link edited in the dialog.
   * @type {string}
   */
  this.linkText = linkText;

  /**
   * The url of the link edited in the dialog.
   * @type {string}
   */
  this.linkUrl = linkUrl;

};
goog.inherits(bluemind.ui.editor.LinkDialog.OkEvent, goog.events.Event);



/**
 * Event fired before testing a link by opening it in another window.
 * Calling preventDefault will stop the link from being opened.
 * @param {string} url Url of the link being tested.
 * @constructor
 * @extends {goog.events.Event}
 */
bluemind.ui.editor.LinkDialog.BeforeTestLinkEvent = function(url) {
  goog.base(this, bluemind.ui.editor.LinkDialog.EventType.BEFORE_TEST_LINK);

  /**
   * The url of the link being tested.
   * @type {string}
   */
  this.url = url;
};
goog.inherits(bluemind.ui.editor.LinkDialog.BeforeTestLinkEvent, goog.events.Event);


/** @override */
bluemind.ui.editor.LinkDialog.prototype.show = function() {
  goog.base(this, 'show');


  this.selectAppropriateTab_(this.textToDisplayInput_.value,
                             this.getTargetUrl_());
  this.syncOkButton_();

};


/** @override */
bluemind.ui.editor.LinkDialog.prototype.hide = function() {
  this.disableAutogenFlag_(false);
  goog.base(this, 'hide');
};


/**
 * Tells the dialog whether to show the 'text to display' div.
 * When the target element of the dialog is an image, there is no link text
 * to modify. This function can be used for this kind of situations.
 * @param {boolean} visible Whether to make 'text to display' div visible.
 */
bluemind.ui.editor.LinkDialog.prototype.setTextToDisplayVisible = function(
    visible) {
  if (this.textToDisplayDiv_) {
    goog.style.setStyle(this.textToDisplayDiv_, 'display',
                        visible ? 'block' : 'none');
  }
};


/**
 * Tells the dialog whether the autogeneration of text to display is to be
 * enabled.
 * @param {boolean} enable Whether to enable the feature.
 */
bluemind.ui.editor.LinkDialog.prototype.setAutogenFeatureEnabled = function(
    enable) {
  this.autogenFeatureEnabled_ = enable;
};

// *** Protected interface ************************************************** //


/** @override */
bluemind.ui.editor.LinkDialog.prototype.createDialogControl = function() {
  var builder = new goog.ui.editor.AbstractDialog.Builder(this);
  builder.setTitle(bluemind.ui.editor.messages.MSG_EDIT_LINK)
      .setContent(this.createDialogContent_());
  builder.addCancelButton(bluemind.ui.editor.messages.MSG_CANCEL);
  builder.addOkButton(bluemind.ui.editor.messages.MSG_OK);
  return builder.build();
};


/**
 * Creates and returns the event object to be used when dispatching the OK
 * event to listeners based on which tab is currently selected and the contents
 * of the input fields of that tab.
 * @return {bluemind.ui.editor.LinkDialog.OkEvent} The event object to be used when
 *     dispatching the OK event to listeners.
 * @protected
 * @override
 */
bluemind.ui.editor.LinkDialog.prototype.createOkEvent = function() {
  if (this.tabPane_.getCurrentTabId() ==
      bluemind.ui.editor.LinkDialog.Id_.EMAIL_ADDRESS_TAB) {
    return this.createOkEventFromEmailTab_();
  } else {
    return this.createOkEventFromWebTab_();
  }
};


/** @override */
bluemind.ui.editor.LinkDialog.prototype.disposeInternal = function() {
  this.eventHandler_.dispose();
  this.eventHandler_ = null;

  this.tabPane_.dispose();
  this.tabPane_ = null;

  this.urlInputHandler_.dispose();
  this.urlInputHandler_ = null;
  this.emailInputHandler_.dispose();
  this.emailInputHandler_ = null;

  goog.base(this, 'disposeInternal');
};


// *** Private implementation *********************************************** //

/**
 * The link being modified by this dialog.
 * @type {goog.editor.Link}
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.targetLink_;


/**
 * EventHandler object that keeps track of all handlers set by this dialog.
 * @type {goog.events.EventHandler}
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.eventHandler_;


/**
 * InputHandler object to listen for changes in the url input field.
 * @type {goog.events.InputHandler}
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.urlInputHandler_;


/**
 * InputHandler object to listen for changes in the email input field.
 * @type {goog.events.InputHandler}
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.emailInputHandler_;


/**
 * The tab bar where the url and email tabs are.
 * @type {goog.ui.editor.TabPane}
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.tabPane_;


/**
 * The div element holding the link's display text input.
 * @type {HTMLDivElement}
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.textToDisplayDiv_;


/**
 * The input element holding the link's display text.
 * @type {HTMLInputElement}
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.textToDisplayInput_;


/**
 * Whether or not the feature of automatically generating the display text is
 * enabled.
 * @type {boolean}
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.autogenFeatureEnabled_ = true;


/**
 * Whether or not we should automatically generate the display text.
 * @type {boolean}
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.autogenerateTextToDisplay_;


/**
 * Whether or not automatic generation of the display text is disabled.
 * @type {boolean}
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.disableAutogen_;

/**
 * Creates contents of this dialog.
 * @return {Element} Contents of the dialog as a DOM element.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.createDialogContent_ = function() {
  this.textToDisplayDiv_ = /** @type {HTMLDivElement} */(
      this.buildTextToDisplayDiv_());
  var content = this.dom.createDom(goog.dom.TagName.DIV, null,
      this.textToDisplayDiv_);

  this.tabPane_ = new goog.ui.editor.TabPane(this.dom,
      bluemind.ui.editor.messages.MSG_LINK_TO);
  this.tabPane_.addTab(bluemind.ui.editor.LinkDialog.Id_.ON_WEB_TAB,
      bluemind.ui.editor.messages.MSG_ON_THE_WEB,
      bluemind.ui.editor.messages.MSG_ON_THE_WEB_TIP,
      'link',
      this.buildTabOnTheWeb_());
  this.tabPane_.addTab(bluemind.ui.editor.LinkDialog.Id_.EMAIL_ADDRESS_TAB,
      bluemind.ui.editor.messages.MSG_EMAIL_ADDRESS,
      bluemind.ui.editor.messages.MSG_EMAIL_ADDRESS_TIP,
      'link',
      this.buildTabEmailAddress_());
  this.tabPane_.render(content);

  this.eventHandler_.listen(this.tabPane_, goog.ui.Component.EventType.SELECT,
      this.onChangeTab_);

  return content;
};


/**
 * Builds and returns the text to display section of the edit link dialog.
 * @return {Element} A div element to be appended into the dialog div.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.buildTextToDisplayDiv_ = function() {
  var table = this.dom.createTable(1, 2);
  table.cellSpacing = '0';
  table.cellPadding = '0';
  table.style.fontSize = '10pt';
  // Build the text to display input.
  var textToDisplayDiv = this.dom.createDom(goog.dom.TagName.DIV);
  table.rows[0].cells[0].innerHTML = '<span style="position: relative;' +
      ' bottom: 2px; padding-right: 1px; white-space: nowrap;">' +
      bluemind.ui.editor.messages.MSG_TEXT_TO_DISPLAY + '&nbsp;</span>';
  this.textToDisplayInput_ = /** @type {HTMLInputElement} */(
      this.dom.createDom(goog.dom.TagName.INPUT,
          {id: bluemind.ui.editor.LinkDialog.Id_.TEXT_TO_DISPLAY}));
  var textInput = this.textToDisplayInput_;
  // 98% prevents scroll bars in standards mode.
  // TODO(robbyw): Is this necessary for quirks mode?
  goog.style.setStyle(textInput, 'width', '98%');
  goog.style.setStyle(table.rows[0].cells[1], 'width', '100%');
  goog.dom.appendChild(table.rows[0].cells[1], textInput);

  textInput.value = this.targetLink_.getCurrentText();
  this.eventHandler_.listen(textInput,
                            goog.events.EventType.KEYUP,
                            goog.bind(this.onTextToDisplayEdit_, this));

  goog.dom.appendChild(textToDisplayDiv, table);
  return textToDisplayDiv;
};


/**
* Builds and returns the div containing the tab "On the web".
* @return {Element} The div element containing the tab.
* @private
*/
bluemind.ui.editor.LinkDialog.prototype.buildTabOnTheWeb_ = function() {
  var onTheWebDiv = this.dom.createElement(goog.dom.TagName.DIV);

  var headingDiv = this.dom.createDom(goog.dom.TagName.DIV,
      {innerHTML: '<b>' + bluemind.ui.editor.messages.MSG_WHAT_URL + '</b>'});
  var urlInput = this.dom.createDom(goog.dom.TagName.INPUT,
      {id: bluemind.ui.editor.LinkDialog.Id_.ON_WEB_INPUT,
       className: bluemind.ui.editor.LinkDialog.TARGET_INPUT_CLASSNAME_});
  // IE throws on unknown values for type.
  if (!goog.userAgent.IE) {
    // On browsers that support Web Forms 2.0, allow autocompletion of URLs.
    // (As of now, this is only supported by Opera 9)
    urlInput.type = 'url';
  }

  if (goog.editor.BrowserFeature.NEEDS_99_WIDTH_IN_STANDARDS_MODE &&
      goog.editor.node.isStandardsMode(urlInput)) {
    urlInput.style.width = '99%';
  }

  var inputDiv = this.dom.createDom(goog.dom.TagName.DIV, null, urlInput);

  this.urlInputHandler_ = new goog.events.InputHandler(urlInput);
  this.eventHandler_.listen(this.urlInputHandler_,
      goog.events.InputHandler.EventType.INPUT,
      this.onUrlOrEmailInputChange_);

  var testLink = new goog.ui.Button(bluemind.ui.editor.messages.MSG_TEST_THIS_LINK,
      goog.ui.LinkButtonRenderer.getInstance(),
      this.dom);
  testLink.render(inputDiv);
  testLink.getElement().style.marginTop = '1em';
  this.eventHandler_.listen(testLink,
      goog.ui.Component.EventType.ACTION,
      this.onWebTestLink_);
  onTheWebDiv.appendChild(headingDiv);
  onTheWebDiv.appendChild(inputDiv);

  return onTheWebDiv;
};


/**
 * Builds and returns the div containing the tab "Email address".
 * @return {Element} the div element containing the tab.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.buildTabEmailAddress_ = function() {
  var emailTab = this.dom.createDom(goog.dom.TagName.DIV);

  var headingDiv = this.dom.createDom(goog.dom.TagName.DIV,
      {innerHTML: '<b>' + bluemind.ui.editor.messages.MSG_WHAT_EMAIL + '</b>'});
  goog.dom.appendChild(emailTab, headingDiv);
  var emailInput = this.dom.createDom(goog.dom.TagName.INPUT,
      {id: bluemind.ui.editor.LinkDialog.Id_.EMAIL_ADDRESS_INPUT,
       className: bluemind.ui.editor.LinkDialog.TARGET_INPUT_CLASSNAME_});

  if (goog.editor.BrowserFeature.NEEDS_99_WIDTH_IN_STANDARDS_MODE &&
      goog.editor.node.isStandardsMode(emailInput)) {
    // Standards mode sizes this too large.
    emailInput.style.width = '99%';
  }

  goog.dom.appendChild(emailTab, emailInput);

  this.emailInputHandler_ = new goog.events.InputHandler(emailInput);
  this.eventHandler_.listen(this.emailInputHandler_,
      goog.events.InputHandler.EventType.INPUT,
      this.onUrlOrEmailInputChange_);

  goog.dom.appendChild(emailTab,
      this.dom.createDom(goog.dom.TagName.DIV,
          {id: bluemind.ui.editor.LinkDialog.Id_.EMAIL_WARNING,
           className: bluemind.ui.editor.LinkDialog.EMAIL_WARNING_CLASSNAME_,
           style: 'visibility:hidden'},
          bluemind.ui.editor.messages.MSG_INVALID_EMAIL));

  return emailTab;
};


/**
 * Returns the url that the target points to.
 * @return {string} The url that the target points to.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.getTargetUrl_ = function() {
  // Get the href-attribute through getAttribute() rather than the href property
  // because Google-Toolbar on Firefox with "Send with Gmail" turned on
  // modifies the href-property of 'mailto:' links but leaves the attribute
  // untouched.
  return this.targetLink_.getAnchor().getAttribute('href') || '';
};


/**
 * Selects the correct tab based on the URL, and fills in its inputs.
 * For new links, it suggests a url based on the link text.
 * @param {string} text The inner text of the link.
 * @param {string} url The href for the link.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.selectAppropriateTab_ = function(
    text, url) {
  if (this.isNewLink_()) {
    // Newly created non-empty link: try to infer URL from the link text.
    this.guessUrlAndSelectTab_(text);
  } else if (goog.editor.Link.isMailto(url)) {
    // The link is for an email.
    this.tabPane_.setSelectedTabId(
        bluemind.ui.editor.LinkDialog.Id_.EMAIL_ADDRESS_TAB);
    this.dom.getElement(bluemind.ui.editor.LinkDialog.Id_.EMAIL_ADDRESS_INPUT)
        .value = url.substring(url.indexOf(':') + 1);
    this.setAutogenFlagFromCurInput_();
  } else {
    // No specific tab was appropriate, default to on the web tab.
    this.tabPane_.setSelectedTabId(bluemind.ui.editor.LinkDialog.Id_.ON_WEB_TAB);
    this.dom.getElement(bluemind.ui.editor.LinkDialog.Id_.ON_WEB_INPUT)
        .value = this.isNewLink_() ? 'http://' : url;
    this.setAutogenFlagFromCurInput_();
  }
};


/**
 * Select a url/tab based on the link's text. This function is simply
 * the isNewLink_() == true case of selectAppropriateTab_().
 * @param {string} text The inner text of the link.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.guessUrlAndSelectTab_ = function(text) {
  if (goog.editor.Link.isLikelyEmailAddress(text)) {
    // The text is for an email address.
    this.tabPane_.setSelectedTabId(
        bluemind.ui.editor.LinkDialog.Id_.EMAIL_ADDRESS_TAB);
    this.dom.getElement(bluemind.ui.editor.LinkDialog.Id_.EMAIL_ADDRESS_INPUT)
        .value = text;
    this.setAutogenFlag_(true);
    // TODO(user): Why disable right after enabling? What bug are we
    // working around?
    this.disableAutogenFlag_(true);
  } else if (goog.editor.Link.isLikelyUrl(text)) {
    // The text is for a web URL.
    this.tabPane_.setSelectedTabId(bluemind.ui.editor.LinkDialog.Id_.ON_WEB_TAB);
    this.dom.getElement(bluemind.ui.editor.LinkDialog.Id_.ON_WEB_INPUT)
        .value = text;
    this.setAutogenFlag_(true);
    this.disableAutogenFlag_(true);
  } else {
    // No meaning could be deduced from text, choose a default tab.
    if (!this.targetLink_.getCurrentText()) {
      this.setAutogenFlag_(true);
    }
    this.tabPane_.setSelectedTabId(bluemind.ui.editor.LinkDialog.Id_.ON_WEB_TAB);
  }
};


/**
 * Called on a change to the url or email input. If either one of those tabs
 * is active, sets the OK button to enabled/disabled accordingly.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.syncOkButton_ = function() {
  var inputValue;
  if (this.tabPane_.getCurrentTabId() ==
      bluemind.ui.editor.LinkDialog.Id_.EMAIL_ADDRESS_TAB) {
    inputValue = this.dom.getElement(
        bluemind.ui.editor.LinkDialog.Id_.EMAIL_ADDRESS_INPUT).value;
    this.toggleInvalidEmailWarning_(inputValue != '' &&
        !goog.editor.Link.isLikelyEmailAddress(inputValue));
  } else if (this.tabPane_.getCurrentTabId() ==
      bluemind.ui.editor.LinkDialog.Id_.ON_WEB_TAB) {
    inputValue = this.dom.getElement(
        bluemind.ui.editor.LinkDialog.Id_.ON_WEB_INPUT).value;
  } else {
    return;
  }
  this.getOkButtonElement().disabled = goog.string.isEmpty(inputValue);
};


/**
 * Show/hide the Invalid Email Address warning.
 * @param {boolean} on Whether to show the warning.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.toggleInvalidEmailWarning_ = function(on) {
  this.dom.getElement(bluemind.ui.editor.LinkDialog.Id_.EMAIL_WARNING)
      .style.visibility = (on ? 'visible' : 'hidden');
};


/**
 * Changes the autogenerateTextToDisplay flag so that text to
 * display stops autogenerating.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.onTextToDisplayEdit_ = function() {
  var inputEmpty = this.textToDisplayInput_.value == '';
  if (inputEmpty) {
    this.setAutogenFlag_(true);
  } else {
    this.setAutogenFlagFromCurInput_();
  }
};


/**
 * The function called when hitting OK with the "On the web" tab current.
 * @return {bluemind.ui.editor.LinkDialog.OkEvent} The event object to be used when
 *     dispatching the OK event to listeners.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.createOkEventFromWebTab_ = function() {
  var input = /** @type {HTMLInputElement} */(
      this.dom.getElement(bluemind.ui.editor.LinkDialog.Id_.ON_WEB_INPUT));
  var linkURL = input.value;
  if (goog.editor.Link.isLikelyEmailAddress(linkURL)) {
    // Make sure that if user types in an e-mail address, it becomes "mailto:".
    return this.createOkEventFromEmailTab_(
        bluemind.ui.editor.LinkDialog.Id_.ON_WEB_INPUT);
  } else {
    if (linkURL.search(/:/) < 0) {
      linkURL = 'http://' + goog.string.trimLeft(linkURL);
    }
    return this.createOkEventFromUrl_(linkURL);
  }
};


/**
 * The function called when hitting OK with the "email address" tab current.
 * @param {string=} opt_inputId Id of an alternate input to check.
 * @return {bluemind.ui.editor.LinkDialog.OkEvent} The event object to be used when
 *     dispatching the OK event to listeners.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.createOkEventFromEmailTab_ = function(
    opt_inputId) {
  var linkURL = this.dom.getElement(
      opt_inputId || bluemind.ui.editor.LinkDialog.Id_.EMAIL_ADDRESS_INPUT).value;
  linkURL = 'mailto:' + linkURL;
  return this.createOkEventFromUrl_(linkURL);
};


/**
 * Function to test a link from the on the web tab.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.onWebTestLink_ = function() {
  var input = /** @type {HTMLInputElement} */(
      this.dom.getElement(bluemind.ui.editor.LinkDialog.Id_.ON_WEB_INPUT));
  var url = input.value;
  if (url.search(/:/) < 0) {
    url = 'http://' + goog.string.trimLeft(url);
  }
  if (this.dispatchEvent(
      new bluemind.ui.editor.LinkDialog.BeforeTestLinkEvent(url))) {
    var win = this.dom.getWindow();
    var size = goog.dom.getViewportSize(win);
    var openOptions = {
      target: '_blank',
      width: Math.max(size.width - 50, 50),
      height: Math.max(size.height - 50, 50),
      toolbar: true,
      scrollbars: true,
      location: true,
      statusbar: false,
      menubar: true,
      'resizable': true
    };
    goog.window.open(url, openOptions, win);
  }
};


/**
 * Called whenever the url or email input is edited. If the text to display
 * matches the text to display, turn on auto. Otherwise if auto is on, update
 * the text to display based on the url.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.onUrlOrEmailInputChange_ = function() {
  if (this.autogenerateTextToDisplay_) {
    this.setTextToDisplayFromAuto_();
  } else if (this.textToDisplayInput_.value == '') {
    this.setAutogenFlagFromCurInput_();
  }
  this.syncOkButton_();
};


/**
 * Called when the currently selected tab changes.
 * @param {goog.events.Event} e The tab change event.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.onChangeTab_ = function(e) {
  var tab = /** @type {goog.ui.Tab} */ (e.target);

  // Focus on the input field in the selected tab.
  var input = this.dom.getElement(tab.getId() +
      bluemind.ui.editor.LinkDialog.Id_.TAB_INPUT_SUFFIX);
  goog.editor.focus.focusInputField(input);

  // For some reason, IE does not fire onpropertychange events when the width
  // is specified as a percentage, which breaks the InputHandlers.
  input.style.width = '';
  input.style.width = input.offsetWidth + 'px';

  this.syncOkButton_();
  this.setTextToDisplayFromAuto_();
};


/**
 * If autogen is turned on, set the value of text to display based on the
 * current selection or url.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.setTextToDisplayFromAuto_ = function() {
  if (this.autogenFeatureEnabled_ && this.autogenerateTextToDisplay_) {
    var inputId = this.tabPane_.getCurrentTabId() +
        bluemind.ui.editor.LinkDialog.Id_.TAB_INPUT_SUFFIX;
    this.textToDisplayInput_.value =
        /** @type {HTMLInputElement} */(this.dom.getElement(inputId)).value;
  }
};


/**
 * Turn on the autogenerate text to display flag, and set some sort of indicator
 * that autogen is on.
 * @param {boolean} val Boolean value to set autogenerate to.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.setAutogenFlag_ = function(val) {
  // TODO(user): This whole autogen thing is very confusing. It needs
  // to be refactored and/or explained.
  this.autogenerateTextToDisplay_ = val;
};


/**
 * Disables autogen so that onUrlOrEmailInputChange_ doesn't act in cases
 * that are undesirable.
 * @param {boolean} autogen Boolean value to set disableAutogen to.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.disableAutogenFlag_ = function(autogen) {
  this.setAutogenFlag_(!autogen);
  this.disableAutogen_ = autogen;
};


/**
 * Creates an OK event from the text to display input and the specified link.
 * If text to display input is empty, then generate the auto value for it.
 * @return {bluemind.ui.editor.LinkDialog.OkEvent} The event object to be used when
 *     dispatching the OK event to listeners.
 * @param {string} url Url the target element should point to.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.createOkEventFromUrl_ = function(url) {
  // Fill in the text to display input in case it is empty.
  this.setTextToDisplayFromAuto_();
  return new bluemind.ui.editor.LinkDialog.OkEvent(this.textToDisplayInput_.value, url);
};


/**
 * If an email or url is being edited, set autogenerate to on if the text to
 * display matches the url.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.setAutogenFlagFromCurInput_ = function() {
  var autogen = false;
  if (!this.disableAutogen_) {
    var tabInput = this.dom.getElement(this.tabPane_.getCurrentTabId() +
        bluemind.ui.editor.LinkDialog.Id_.TAB_INPUT_SUFFIX);
    autogen = (tabInput.value == this.textToDisplayInput_.value);
  }
  this.setAutogenFlag_(autogen);
};


/**
 * @return {boolean} Whether the link is new.
 * @private
 */
bluemind.ui.editor.LinkDialog.prototype.isNewLink_ = function() {
  return this.targetLink_.isNew();
};


/**
 * IDs for relevant DOM elements.
 * @enum {string}
 * @private
 */
bluemind.ui.editor.LinkDialog.Id_ = {
  TEXT_TO_DISPLAY: 'linkdialog-text',
  ON_WEB_TAB: 'linkdialog-onweb',
  ON_WEB_INPUT: 'linkdialog-onweb-tab-input',
  EMAIL_ADDRESS_TAB: 'linkdialog-email',
  EMAIL_ADDRESS_INPUT: 'linkdialog-email-tab-input',
  EMAIL_WARNING: 'linkdialog-email-warning',
  TAB_INPUT_SUFFIX: '-tab-input'
};


/**
 * Class name for the url and email input elements.
 * @type {string}
 * @private
 */
bluemind.ui.editor.LinkDialog.TARGET_INPUT_CLASSNAME_ =
    goog.getCssName('tr-link-dialog-target-input');


/**
 * Class name for the email address warning element.
 * @type {string}
 * @private
 */
bluemind.ui.editor.LinkDialog.EMAIL_WARNING_CLASSNAME_ =
    goog.getCssName('tr-link-dialog-email-warning');


/**
 * Class name for the explanation text elements.
 * @type {string}
 * @private
 */
bluemind.ui.editor.LinkDialog.EXPLANATION_TEXT_CLASSNAME_ =
    goog.getCssName('tr-link-dialog-explanation-text');

