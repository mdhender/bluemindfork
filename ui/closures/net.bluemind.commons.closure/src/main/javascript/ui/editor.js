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
 * @fileoverview Wrapper arround goog.editor.Field.
 * This wrapper is meant to make Field usage easier and to be exported as a
 * standalone widget.
 *
 */

goog.provide('bluemind.ui.Editor');
goog.provide('bluemind.ui.editor.FeatureDescriptor');

goog.require('bluemind.ui.editor.IConfigurablePlugin');
goog.require('bluemind.ui.editor.messages');
goog.require('bluemind.ui.editor.Toolbar');
goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.editor.Command');
goog.require('goog.editor.Field');
goog.require('goog.editor.plugins.BasicTextFormatter');
goog.require('goog.editor.plugins.EnterHandler');
goog.require('goog.editor.plugins.HeaderFormatter');
goog.require('bluemind.ui.editor.plugins.ImageDropPlugin');
goog.require('bluemind.ui.editor.plugins.ImageBubblePlugin');
goog.require('bluemind.ui.editor.plugins.ImageDialogPlugin');
goog.require('bluemind.ui.editor.plugins.EditHTMLPlugin');
goog.require('bluemind.ui.editor.plugins.LinkBubble');
goog.require('bluemind.ui.editor.plugins.LinkDialogPlugin');
goog.require('goog.editor.plugins.ListTabHandler');
goog.require('goog.editor.plugins.RemoveFormatting');
goog.require('goog.editor.plugins.SpacesTabHandler');
goog.require('goog.editor.plugins.UndoRedo');
goog.require('goog.ui.editor.ToolbarController');
goog.require('goog.editor.ContentEditableField');


/**
 * Wysiwyg editor based on  {@link goog.editor.Field}
 * @param {!string} field Field element.
 * @param {?string|Element=} opt_toolbar Toolbar parent element.
 * @param {{string: *}=} opt_options Plugins options.
 * @extends {goog.events.EventTarget}
 * @constructor
 */
bluemind.ui.Editor = function(field, opt_toolbar, opt_options) {
  goog.base(this);
  this.field = new goog.editor.ContentEditableField(field);
  var toolbar;
  if (!opt_toolbar) {
    toolbar = goog.dom.createDom(goog.dom.TagName.DIV);
    goog.dom.insertSiblingBefore(toolbar, goog.dom.getElement(field));
  } else {
    toolbar = /** @type {!Element} */ (goog.dom.getElement(opt_toolbar));
  }
  this.field.setParentEventTarget(this);
  var options = goog.object.clone(bluemind.ui.Editor.DEFAULT_OPTIONS);
  goog.object.extend(options, (opt_options || {}));
  this.buildEditor_(toolbar, options);
  this.field.makeEditable();
};
goog.inherits(bluemind.ui.Editor, goog.events.EventTarget);


/**
 * Editor default options 
 * @type {Object} 
 */
bluemind.ui.Editor.DEFAULT_OPTIONS = {
  'dataUrlRpc': null,
  'buttons': {
    'bold': true,
    'italic': true,
    'underline': true,
    'strike': true,
    'unformat': true,
    'color': true,
    'bgcolor': true,
    'ulist': true,
    'olist': true,
    'align': true,
    'font': true,
    'size': true,
    'link': true,
    'image': true,
    'undo': true,
    'html': true
  }
};

/**
 * Editor editable field
 * @type {goog.editor.Field}
 */
bluemind.ui.Editor.prototype.field;

/**
 * Editor command toolbar.
 * @type {goog.ui.Toolbar}
 */
bluemind.ui.Editor.prototype.toolbar_;

/**
 * Editor command controller.
 * @type {goog.ui.editor.ToolbarController}
 */
bluemind.ui.Editor.prototype.controller_;


/**
 *
 * @type {Array.<{name: string, hideButtons: ?boolean, options: ?Object.<string, *>}>}
 */
bluemind.ui.Editor.prototype.features_;

/**
 * Button list.
 * @type {Array.<string>}
 */
bluemind.ui.Editor.prototype.buttons_;


/**
 * Build button list from the features.
 * @param {!Element} toolbar Toolbar parent element.
 * @param {Object.<string, *>} options Plugins options
 *
 * @private
 */
bluemind.ui.Editor.prototype.buildEditor_ = function(toolbar, options) {
  this.field.registerPlugin(new goog.editor.plugins.UndoRedo());
  this.field.registerPlugin(new goog.editor.plugins.SpacesTabHandler());
  this.field.registerPlugin(new goog.editor.plugins.EnterHandler());
    
  this.field.registerPlugin(new goog.editor.plugins.BasicTextFormatter());
  this.field.registerPlugin(new goog.editor.plugins.RemoveFormatting());
  this.field.registerPlugin(new goog.editor.plugins.HeaderFormatter());
  this.field.registerPlugin(new goog.editor.plugins.ListTabHandler());

  var plugin;
  plugin = new bluemind.ui.editor.plugins.LinkDialogPlugin();
  this.setPluginOptions_(plugin, options);
  this.field.registerPlugin(plugin);

  plugin = new bluemind.ui.editor.plugins.LinkBubble();
  this.setPluginOptions_(plugin, options);  
  this.field.registerPlugin(plugin);

  plugin = new bluemind.ui.editor.plugins.ImageDropPlugin();
  this.setPluginOptions_(plugin, options);
  this.field.registerPlugin(plugin);

  plugin = new bluemind.ui.editor.plugins.ImageDialogPlugin();
  this.setPluginOptions_(plugin, options);
  this.field.registerPlugin(plugin);

  plugin = new bluemind.ui.editor.plugins.ImageBubblePlugin();
  this.setPluginOptions_(plugin, options);
  this.field.registerPlugin(plugin);

  plugin = new bluemind.ui.editor.plugins.EditHTMLPlugin();
  this.setPluginOptions_(plugin, options);
  this.field.registerPlugin(plugin);

  // Specify the buttons to add to the toolbar, using built in default buttons.
  var buttons = [];
  if (options['buttons']['bold']) {
    buttons.push(goog.editor.Command.BOLD);
  }
  if (options['buttons']['italic']) {
    buttons.push(goog.editor.Command.ITALIC);
  }
  if (options['buttons']['underline']) {
    buttons.push(goog.editor.Command.UNDERLINE);
  }
  if (options['buttons']['strike']) {
    buttons.push(goog.editor.Command.STRIKE_THROUGH);
  }
  if (options['buttons']['unformat']) {
    buttons.push(goog.editor.Command.REMOVE_FORMAT);
  }
  buttons.push(new goog.ui.ToolbarSeparator());
  if (options['buttons']['color']) {
    buttons.push(goog.editor.Command.FONT_COLOR);
  }
  if (options['buttons']['bgcolor']) {
    buttons.push(goog.editor.Command.BACKGROUND_COLOR);
  }
  if (options['buttons']['ulist']) {
    buttons.push(goog.editor.Command.UNORDERED_LIST);
  }
  if (options['buttons']['olist']) {
    buttons.push(goog.editor.Command.ORDERED_LIST);
  }
  if (options['buttons']['align']) {
    buttons.push({
      id: 'alignOptions',
      tooltip: 'Option d\'alignement',
      classes: goog.getCssName('fa') + ' ' + goog.getCssName('fa-align-justify'),
      submenu:[
        goog.editor.Command.JUSTIFY_LEFT, 
        goog.editor.Command.JUSTIFY_CENTER,
        goog.editor.Command.JUSTIFY_RIGHT
      ]
    });
  }
  if (options['buttons']['font']) {
    buttons.push(goog.editor.Command.FONT_FACE);
  }
  if (options['buttons']['size']) {
    buttons.push(goog.editor.Command.FONT_SIZE);
  }
  buttons.push(new goog.ui.ToolbarSeparator());
  if (options['buttons']['link']) {
    buttons.push(goog.editor.Command.LINK);
  }
  if (options['buttons']['image']) {
    buttons.push(goog.editor.Command.IMAGE);
  }
  buttons.push(new goog.ui.ToolbarSeparator());
  if (options['buttons']['undo']) {
    buttons.push(goog.editor.Command.UNDO);
    buttons.push(goog.editor.Command.REDO);
  }

  buttons.push(new goog.ui.ToolbarSeparator());
  if (options['buttons']['html']) {
    buttons.push(goog.editor.Command.EDIT_HTML);
  }
  this.toolbar_ = bluemind.ui.editor.Toolbar.makeToolbar(buttons, toolbar);
  this.controller_ = new goog.ui.editor.ToolbarController(this.field, this.toolbar_);
};

/** 
 * Add a plugin to the Field
 * @param {bluemind.ui.editor.IConfigurablePlugin} plugin The plugin to add
 * @param {Object.<string, *>} options Plugins options.
 * @private
 */
bluemind.ui.Editor.prototype.setPluginOptions_ = function(plugin, options) {
  if (options[plugin.getTrogClassId()]) {
    plugin.setOptions(options[plugin.getTrogClassId()]);
  }
};
/**
 * Sets the contents of the field.
 * @param {?string} html html to insert.  If html=null, then this defaults
 *    to a nsbp for mozilla and an empty string for IE.
 */
bluemind.ui.Editor.prototype.setValue = function(html) {
  var plugin = (this.field.getPluginByClassId("EditHTMLPlugin"));
  if (!plugin) {
    this.field.setHtml(false, html);
  } else {
    plugin.setValue(html);
  }
};

/**
 * Retrieve the HTML contents of a field.
 * @return {string} The scrubbed contents of the field.
 */
bluemind.ui.Editor.prototype.getValue = function() {
  var content = goog.string.trim(this.field.getCleanContents());
  return content.replace(/^(<div>)?<br\/?>(<\/div>)?$/,'');
};

/**
 * Switch to the composer view
 */
bluemind.ui.Editor.prototype.composer = function() {
  if (this.field.queryCommandValue(goog.editor.Command.EDIT_HTML)) {
    this.field.execCommand(goog.editor.Command.EDIT_HTML);
  }
};

/**
 * Switch to the textarea view
 */
bluemind.ui.Editor.prototype.textarea = function() {
  if (!this.field.queryCommandValue(goog.editor.Command.EDIT_HTML)) {
    this.field.execCommand(goog.editor.Command.EDIT_HTML);
  }
};

/** @inheritDoc */
bluemind.ui.Editor.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.field.dispose();
  this.field = null;
};

goog.exportSymbol('bluemind.ui.Editor', bluemind.ui.Editor);
goog.exportProperty(bluemind.ui.Editor.prototype, 'getValue', bluemind.ui.Editor.prototype.getValue);
goog.exportProperty(bluemind.ui.Editor.prototype, 'setValue', bluemind.ui.Editor.prototype.setValue);
goog.exportProperty(bluemind.ui.Editor.prototype, 'textarea', bluemind.ui.Editor.prototype.textarea);
goog.exportProperty(bluemind.ui.Editor.prototype, 'composer', bluemind.ui.Editor.prototype.composer);

