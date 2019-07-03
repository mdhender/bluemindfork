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
 * @fileoverview Factory functions for creating a bluemind editing toolbar.
 *
 */

goog.provide('bluemind.ui.editor.Toolbar');
goog.provide('bluemind.ui.editor.Toolbar.ToolbarSelect');


goog.require('bluemind.ui.editor.messages');
goog.require('goog.dom');
goog.require('goog.dom.TagName');
goog.require('goog.dom.classlist');
goog.require('goog.editor.Command');
goog.require('goog.style');
goog.require('goog.ui.ControlContent');
goog.require('goog.ui.editor.ToolbarFactory');
goog.require('goog.ui.editor.messages');
goog.require('goog.ui.style.app.ButtonRenderer');
goog.require('goog.ui.style.app.MenuButtonRenderer');
goog.require('goog.ui.ToolbarSeparator');
goog.require('goog.ui.FlatButtonRenderer');
goog.require('goog.ui.FlatMenuButtonRenderer');

/**
 * Common font descriptors for all locales.  Each descriptor has the following
 * attributes:
 * <ul>
 *   <li>{@code caption} - Caption to show in the font menu (e.g. 'Tahoma')
 *   <li>{@code value} - Value for the corresponding 'font-family' CSS style
 *       (e.g. 'Tahoma, Arial, sans-serif')
 * </ul>
 * @type {!Array.<{caption:string, value:string}>}
 * @private
 */
bluemind.ui.editor.Toolbar.FONTS_ = [
  {
    caption: bluemind.ui.editor.messages.MSG_FONT_NORMAL,
    value: 'arial,sans-serif'
  },
  {
    caption: bluemind.ui.editor.messages.MSG_FONT_NORMAL_SERIF,
    value: 'times new roman,serif'
  },
  {caption: 'Courier New', value: 'courier new,monospace'},
  {caption: 'Georgia', value: 'georgia,serif'},
  {caption: 'Trebuchet', value: 'trebuchet ms,sans-serif'},
  {caption: 'Verdana', value: 'verdana,sans-serif'}
];


/**
 * Initializes the given font menu button by adding default fonts to the menu.
 * @param {!goog.ui.Select} button Font menu button.
 */
bluemind.ui.editor.Toolbar.addDefaultFonts = function(button) {
  goog.ui.editor.ToolbarFactory.addFonts(button,
      bluemind.ui.editor.Toolbar.FONTS_);
};


/**
 * Font size descriptors, each with the following attributes:
 * <ul>
 *   <li>{@code caption} - Caption to show in the font size menu (e.g. 'Huge')
 *   <li>{@code value} - Value for the corresponding HTML font size (e.g. 6)
 * </ul>
 * @type {!Array.<{caption:string, value:number}>}
 * @private
 */
bluemind.ui.editor.Toolbar.FONT_SIZES_ = [
  {caption: bluemind.ui.editor.messages.MSG_FONT_SIZE_SMALL, value: 1},
  {caption: bluemind.ui.editor.messages.MSG_FONT_SIZE_NORMAL, value: 2},
  {caption: bluemind.ui.editor.messages.MSG_FONT_SIZE_LARGE, value: 4},
  {caption: bluemind.ui.editor.messages.MSG_FONT_SIZE_HUGE, value: 6}
];


/**
 * Initializes the given font size menu button by adding default font sizes to
 * it.
 * @param {!goog.ui.Select} button Font size menu button.
 */
bluemind.ui.editor.Toolbar.addDefaultFontSizes = function(button) {
  goog.ui.editor.ToolbarFactory.addFontSizes(button,
      bluemind.ui.editor.Toolbar.FONT_SIZES_);
};


/**
 * Format option descriptors, each with the following attributes:
 * <ul>
 *   <li>{@code caption} - Caption to show in the menu (e.g. 'Minor heading')
 *   <li>{@code command} - Corresponding {@link goog.dom.TagName} (e.g.
 *       'H4')
 * </ul>
 * @type {!Array.<{caption: string, command: !goog.dom.TagName}>}
 * @private
 */
bluemind.ui.editor.Toolbar.FORMAT_OPTIONS_ = [
  {
    caption: bluemind.ui.editor.messages.MSG_FORMAT_HEADING,
    command: goog.dom.TagName.H2
  },
  {
    caption: bluemind.ui.editor.messages.MSG_FORMAT_SUBHEADING,
    command: goog.dom.TagName.H3
  },
  {
    caption: bluemind.ui.editor.messages.MSG_FORMAT_MINOR_HEADING,
    command: goog.dom.TagName.H4
  },
  {
    caption: bluemind.ui.editor.messages.MSG_FORMAT_NORMAL,
    command: goog.dom.TagName.P
  }
];


/**
 * Initializes the given "Format block" menu button by adding default format
 * options to the menu.
 * @param {!goog.ui.Select} button "Format block" menu button.
 */
bluemind.ui.editor.Toolbar.addDefaultFormatOptions = function(button) {
  goog.ui.editor.ToolbarFactory.addFormatOptions(button,
      bluemind.ui.editor.Toolbar.FORMAT_OPTIONS_);
};


/**
 * Creates a {@link goog.ui.Toolbar} containing a default set of editor
 * toolbar buttons, and renders it into the given parent element.
 * @param {!Element} elem Toolbar parent element.
 * @param {boolean=} opt_isRightToLeft Whether the editor chrome is
 *     right-to-left; defaults to the directionality of the toolbar parent
 *     element.
 * @return {!goog.ui.Toolbar} Default editor toolbar, rendered into the given
 *     parent element.
 * @see bluemind.ui.editor.Toolbar.DEFAULT_BUTTONS
 */
bluemind.ui.editor.Toolbar.makeDefaultToolbar = function(elem,
    opt_isRightToLeft) {
  var buttons =  bluemind.ui.editor.Toolbar.DEFAULT_BUTTONS;
  return bluemind.ui.editor.Toolbar.makeToolbar(buttons, elem);
};


/**
 * Creates a {@link goog.ui.Toolbar} containing the specified set of
 * toolbar buttons, and renders it into the given parent element.  Each
 * item in the {@code items} array must either be a
 * {@link goog.editor.Command} (to create a built-in button) or a subclass
 * of {@link goog.ui.Control} (to create a custom control).
 * @param {!Array.<string|goog.ui.Control|Array.<string|goog.ui.Control>>} items Toolbar items; each must
 *     be a {@link goog.editor.Command} or a {@link goog.ui.Control}.
 * @param {!Element} elem Toolbar parent element.
 * @return {!goog.ui.Toolbar} Editor toolbar, rendered into the given parent
 *     element.
 */
bluemind.ui.editor.Toolbar.makeToolbar = function(items, elem) {
  var domHelper = goog.dom.getDomHelper(elem);
  var buttons = bluemind.ui.editor.Toolbar.buildButtons_(items, domHelper);
  return goog.ui.editor.ToolbarFactory.makeToolbar(buttons, elem);
};


/**
 * Creates a buttons from button description 
 * @param {!Array.<string|bluemind.ui.editor.ButtonDescriptor|goog.ui.Control|Array.<string|goog.ui.Control>>} entries Toolbar items; each must
 *     be a {@link goog.editor.Command} or a {@link goog.ui.Control}.
 * @param {goog.dom.DomHelper} domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {!Array.<goog.ui.Control>} Button set.
 */
bluemind.ui.editor.Toolbar.buildButtons_ = function(entries, domHelper) {
  var controls = [];
  for (var i = 0, entry; entry = entries[i]; i++) {
    if (!goog.isArray(entry) && goog.isObject(entry) && entry.submenu) {
      var menu = bluemind.ui.editor.Toolbar.makeMenuButton(/** @type {bluemind.ui.editor.ButtonDescriptor} */ (entry), domHelper);
      var buttons = bluemind.ui.editor.Toolbar.buildButtons_(entry.submenu, domHelper);
      goog.array.forEach(buttons, menu.addItem, menu);
      controls.push(menu);
    } else {
      var items = entry;
      if (!goog.isArray(items)) {
        items = [items];
      } 
      for (var j = 0, button; button = items[j]; j++) {
        if (goog.isString(button)) {
          button = bluemind.ui.editor.Toolbar.makeButton(button, domHelper);
        }
        if (button) {
          controls.push(button);
        }
        
      }      
    } 
  }
  return controls;

};


/**
 * Creates an instance of a subclass of {@link goog.ui.MenuButton} for the given
 * {@link bluemind.ui.editor.ButtonDescriptor}.
 * @param {bluemind.ui.editor.ButtonDescriptor} menu Menu description.
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {goog.ui.MenuButton} Toolbar button (null if no built-in button exists
 *     for the command).
 */
bluemind.ui.editor.Toolbar.makeMenuButton = function(menu, opt_domHelper) {
  var button;
  var factory = menu.factory ||
      goog.ui.editor.ToolbarFactory.makeMenuButton;
  var id = menu.id;
  var tooltip = menu.tooltip;
  var caption = menu.caption;
  var renderer = menu.renderer ||
      goog.ui.FlatMenuButtonRenderer.getInstance();
  var classNames = menu.classes;
  var domHelper = opt_domHelper || goog.dom.getDomHelper();
  button = factory(id, tooltip, caption, classNames, renderer, domHelper);
  
  return button;
};

/**
 * Creates a select button with the given ID, tooltip, and caption. Applies
 * any custom CSS class names to the button's root element.  The button
 * returned doesn't have an actual menu attached; use {@link
 * goog.ui.Select#setMenu} to attach a {@link goog.ui.Menu} containing
 * {@link goog.ui.Option}s to the select button.
 * @param {string} id Button ID; must equal a {@link goog.editor.Command} for
 *     built-in buttons, anything else for custom buttons.
 * @param {string} tooltip Tooltip to be shown on hover.
 * @param {goog.ui.ControlContent} caption Button caption; used as the
 *     default caption when nothing is selected.
 * @param {string=} opt_classNames CSS class name(s) to apply to the button's
 *     root element.
 * @param {goog.ui.MenuButtonRenderer=} opt_renderer Button renderer;
 *     defaults to {@link goog.ui.ToolbarMenuButtonRenderer} if unspecified.
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {!goog.ui.Select} A select button.
 */
bluemind.ui.editor.Toolbar.makeSelectButton = function(id, tooltip, caption,
    opt_classNames, opt_renderer, opt_domHelper) {
  caption = bluemind.ui.editor.Toolbar.createContent_(caption, opt_classNames, opt_domHelper);
  var button = new bluemind.ui.editor.Toolbar.ToolbarSelect(caption, null,
      opt_renderer,
      opt_domHelper);
  button.addClassName(goog.getCssName('goog-toolbar-select'));
  button.setId(id);
  button.setTooltip(tooltip);
  return button;
};

/**
 * Creates a new DIV that wraps a button caption, optionally applying CSS
 * class names to it.  Used as a helper function in button factory methods.
 * @param {goog.ui.ControlContent} caption Button caption.
 * @param {string=} opt_classNames CSS class name(s) to apply to the DIV that
 *     wraps the caption (if any).
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {!Element} DIV that wraps the caption.
 * @private
 */
bluemind.ui.editor.Toolbar.createContent_ = function(caption, opt_classNames,
    opt_domHelper) {
  // FF2 doesn't like empty DIVs, especially when rendered right-to-left.
  if ((!caption || caption == '') && goog.userAgent.GECKO &&
      !goog.userAgent.isVersionOrHigher('1.9a')) {
    caption = goog.string.Unicode.NBSP;
  }
  return (opt_domHelper || goog.dom.getDomHelper()).createDom(
      goog.dom.TagName.DIV,
      opt_classNames ? {'class' : opt_classNames} : null, caption);
};

/**
 * Creates an instance of a subclass of {@link goog.ui.Button} for the given
 * {@link goog.editor.Command}, or null if no built-in button exists for the
 * command.  Note that this function is only intended to create built-in
 * buttons; please don't try to hack it!
 * @param {string} command Editor command ID.
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {goog.ui.Button} Toolbar button (null if no built-in button exists
 *     for the command).
 */
bluemind.ui.editor.Toolbar.makeButton = function(command, opt_domHelper) {
  var button = null;
  var descriptor = bluemind.ui.editor.Toolbar.buttons_[command];
  if (descriptor) {
    var factory = descriptor.factory ||
        goog.ui.editor.ToolbarFactory.makeToggleButton;
    var id = descriptor.id;
    var tooltip = descriptor.tooltip;
    var caption = descriptor.caption;
    var renderer = descriptor.renderer ||
        goog.ui.FlatButtonRenderer.getInstance();
    var classNames = descriptor.classes;
    // Default the DOM helper to the one for the current document.
    var domHelper = opt_domHelper || goog.dom.getDomHelper();
    // Instantiate the button based on the descriptor.
    button = factory(id, tooltip, caption, classNames, renderer, domHelper);
    // If this button's state should be queried when updating the toolbar,
    // set the button object's queryable property to true.
    if (descriptor.queryable) {
      button.queryable = true;
    }
  }
  return button;
};


/**
 * A set of built-in buttons to display in the default editor toolbar.
 * @type {!Array.<string>}
 */
bluemind.ui.editor.Toolbar.DEFAULT_BUTTONS = [
  goog.editor.Command.IMAGE,
  goog.editor.Command.LINK,
  goog.editor.Command.BOLD,
  goog.editor.Command.ITALIC,
  goog.editor.Command.UNORDERED_LIST,
  goog.editor.Command.FONT_COLOR,
  goog.editor.Command.FONT_FACE,
  goog.editor.Command.FONT_SIZE,
  goog.editor.Command.JUSTIFY_LEFT,
  goog.editor.Command.JUSTIFY_CENTER,
  goog.editor.Command.JUSTIFY_RIGHT,
  goog.editor.Command.EDIT_HTML
];


/**
 * Creates a toolbar button with the given ID, tooltip, and caption.  Applies
 * any custom CSS class names to the button's caption element.  This button
 * is designed to be used as the RTL button.
 * @param {string} id Button ID; must equal a {@link goog.editor.Command} for
 *     built-in buttons, anything else for custom buttons.
 * @param {string} tooltip Tooltip to be shown on hover.
 * @param {goog.ui.ControlContent} caption Button caption.
 * @param {string=} opt_classNames CSS class name(s) to apply to the caption
 *     element.
 * @param {goog.ui.ButtonRenderer=} opt_renderer Button renderer; defaults to
 *     {@link goog.ui.ToolbarButtonRenderer} if unspecified.
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {!goog.ui.Button} A toolbar button.
 * @private
 */
bluemind.ui.editor.Toolbar.rtlButtonFactory_ = function(id, tooltip,
    caption, opt_classNames, opt_renderer, opt_domHelper) {
  var button = goog.ui.editor.ToolbarFactory.makeToggleButton(id, tooltip,
      caption, opt_classNames, opt_renderer, opt_domHelper);
  button.updateFromValue = function(value) {
    // Enable/disable right-to-left text editing mode in the toolbar.
    var isRtl = !!value;
    // Enable/disable a marker class on the toolbar's root element; the rest is
    // done using CSS scoping in editortoolbar.css.  This changes
    // direction-senitive toolbar icons (like indent/outdent)
    goog.dom.classlist.enable(
        button.getParent().getElement(), goog.getCssName('tr-rtl-mode'), isRtl);
    button.setChecked(isRtl);
  };
  return button;
};


/**
 * Creates a toolbar button with the given ID, tooltip, and caption.  Applies
 * any custom CSS class names to the button's caption element.  Designed to
 * be used to create undo and redo buttons.
 * @param {string} id Button ID; must equal a {@link goog.editor.Command} for
 *     built-in buttons, anything else for custom buttons.
 * @param {string} tooltip Tooltip to be shown on hover.
 * @param {goog.ui.ControlContent} caption Button caption.
 * @param {string=} opt_classNames CSS class name(s) to apply to the caption
 *     element.
 * @param {goog.ui.ButtonRenderer=} opt_renderer Button renderer; defaults to
 *     {@link goog.ui.ToolbarButtonRenderer} if unspecified.
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {!goog.ui.Button} A toolbar button.
 * @private
 */
bluemind.ui.editor.Toolbar.undoRedoButtonFactory_ = function(id, tooltip,
    caption, opt_classNames, opt_renderer, opt_domHelper) {
  var button = goog.ui.editor.ToolbarFactory.makeButton(id, tooltip,
      caption, opt_classNames, opt_renderer, opt_domHelper);
  button.updateFromValue = function(value) {
    if (!goog.isDef(button.wasEnabled)) {
      button.setEnabled(value);
    }
  };
  return button;
};


/**
 * Creates a toolbar button with the given ID, tooltip, and caption.  Applies
 * any custom CSS class names to the button's caption element.  Used to create
 * a font face button, filled with default fonts.
 * @param {string} id Button ID; must equal a {@link goog.editor.Command} for
 *     built-in buttons, anything else for custom buttons.
 * @param {string} tooltip Tooltip to be shown on hover.
 * @param {goog.ui.ControlContent} caption Button caption.
 * @param {string=} opt_classNames CSS class name(s) to apply to the caption
 *     element.
 * @param {goog.ui.MenuButtonRenderer=} opt_renderer Button renderer; defaults
 *     to {@link goog.ui.ToolbarMenuButtonRenderer} if unspecified.
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {!goog.ui.Button} A toolbar button.
 * @private
 */
bluemind.ui.editor.Toolbar.fontFaceFactory_ = function(id, tooltip,
    caption, opt_classNames, opt_renderer, opt_domHelper) {
  var button = bluemind.ui.editor.Toolbar.makeSelectButton(id, tooltip,
      caption, opt_classNames, opt_renderer, opt_domHelper);
  bluemind.ui.editor.Toolbar.addDefaultFonts(button);
  // Font options don't have keyboard accelerators.
  goog.dom.classlist.add(button.getMenu().getContentElement(),
      goog.getCssName('goog-menu-noaccel'));

  return button;
};


/**
 * Creates a toolbar button with the given ID, tooltip, and caption.  Applies
 * any custom CSS class names to the button's caption element. Use to create a
 * font size button, filled with default font sizes.
 * @param {string} id Button ID; must equal a {@link goog.editor.Command} for
 *     built-in buttons, anything else for custom buttons.
 * @param {string} tooltip Tooltip to be shown on hover.
 * @param {goog.ui.ControlContent} caption Button caption.
 * @param {string=} opt_classNames CSS class name(s) to apply to the caption
 *     element.
 * @param {goog.ui.MenuButtonRenderer=} opt_renderer Button renderer; defaults
 *     to {@link goog.ui.ToolbarMebuButtonRenderer} if unspecified.
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {!goog.ui.Button} A toolbar button.
 * @private
 */
bluemind.ui.editor.Toolbar.fontSizeFactory_ = function(id, tooltip,
    caption, opt_classNames, opt_renderer, opt_domHelper) {
  var button = bluemind.ui.editor.Toolbar.makeSelectButton(id, tooltip,
      caption, opt_classNames, opt_renderer, opt_domHelper);
  bluemind.ui.editor.Toolbar.addDefaultFontSizes(button);
  // Font size options don't have keyboard accelerators.
  goog.dom.classlist.add(button.getMenu().getContentElement(),
      goog.getCssName('goog-menu-noaccel'));

  return button;
};


/**
 * Function to update the state of a color menu button.
 * @param {goog.ui.ToolbarColorMenuButton} button The button to which the
 *     color menu is attached.
 * @param {number} color Color value to update to.
 * @private
 */
bluemind.ui.editor.Toolbar.colorUpdateFromValue_ = function(button, color) {
  var value = color;
  /** @preserveTry */
  try {
    if (goog.userAgent.IE) {
      // IE returns a number that, converted to hex, is a BGR color.
      // Convert from decimal to BGR to RGB.
      var hex = '000000' + value.toString(16);
      var bgr = hex.substr(hex.length - 6, 6);
      value = '#' + bgr.substring(4, 6) + bgr.substring(2, 4) +
          bgr.substring(0, 2);
    }
    if (value != button.getValue()) {
      button.setValue(/** @type {string} */ (value));
    }
  } catch (ex) {
    // TODO(attila): Find out when/why this happens.
  }
};


/**
 * Creates a toolbar button with the given ID, tooltip, and caption.  Applies
 * any custom CSS class names to the button's caption element. Use to create
 * a font color button.
 * @param {string} id Button ID; must equal a {@link goog.editor.Command} for
 *     built-in buttons, anything else for custom buttons.
 * @param {string} tooltip Tooltip to be shown on hover.
 * @param {goog.ui.ControlContent} caption Button caption.
 * @param {string=} opt_classNames CSS class name(s) to apply to the caption
 *     element.
 * @param {goog.ui.ColorMenuButtonRenderer=} opt_renderer Button renderer;
 *     defaults to {@link goog.ui.ToolbarColorMenuButtonRenderer} if
 *     unspecified.
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {!goog.ui.Button} A toolbar button.
 * @private
 */
bluemind.ui.editor.Toolbar.fontColorFactory_ = function(id, tooltip,
    caption, opt_classNames, opt_renderer, opt_domHelper) {
  var button = goog.ui.editor.ToolbarFactory.makeColorMenuButton(id, tooltip,
      caption, opt_classNames, opt_renderer, opt_domHelper);
  // Initialize default foreground color.
  button.setSelectedColor('#000');
  button.updateFromValue = goog.partial(
      bluemind.ui.editor.Toolbar.colorUpdateFromValue_, button);
  return button;
};


/**
 * Creates a toolbar button with the given ID, tooltip, and caption.  Applies
 * any custom CSS class names to the button's caption element. Use to create
 * a font background color button.
 * @param {string} id Button ID; must equal a {@link goog.editor.Command} for
 *     built-in buttons, anything else for custom buttons.
 * @param {string} tooltip Tooltip to be shown on hover.
 * @param {goog.ui.ControlContent} caption Button caption.
 * @param {string=} opt_classNames CSS class name(s) to apply to the caption
 *     element.
 * @param {goog.ui.ColorMenuButtonRenderer=} opt_renderer Button renderer;
 *     defaults to {@link goog.ui.ToolbarColorMenuButtonRenderer} if
 *     unspecified.
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {!goog.ui.Button} A toolbar button.
 * @private
 */
bluemind.ui.editor.Toolbar.backgroundColorFactory_ = function(id, tooltip,
    caption, opt_classNames, opt_renderer, opt_domHelper) {
  var button = goog.ui.editor.ToolbarFactory.makeColorMenuButton(id,
      tooltip, caption, opt_classNames, opt_renderer, opt_domHelper);
  // Initialize default background color.
  button.setSelectedColor('#FFF');
  button.updateFromValue = goog.partial(
      bluemind.ui.editor.Toolbar.colorUpdateFromValue_, button);
  return button;
};


/**
 * Creates a toolbar button with the given ID, tooltip, and caption.  Applies
 * any custom CSS class names to the button's caption element. Use to create
 * the format menu, prefilled with default formats.
 * @param {string} id Button ID; must equal a {@link goog.editor.Command} for
 *     built-in buttons, anything else for custom buttons.
 * @param {string} tooltip Tooltip to be shown on hover.
 * @param {goog.ui.ControlContent} caption Button caption.
 * @param {string=} opt_classNames CSS class name(s) to apply to the caption
 *     element.
 * @param {goog.ui.MenuButtonRenderer=} opt_renderer Button renderer;
 *     defaults to
 *     {@link goog.ui.ToolbarMenuButtonRenderer} if unspecified.
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {!goog.ui.Button} A toolbar button.
 * @private
 */
bluemind.ui.editor.Toolbar.formatBlockFactory_ = function(id, tooltip,
    caption, opt_classNames, opt_renderer, opt_domHelper) {
  var button = bluemind.ui.editor.Toolbar.makeSelectButton(id, tooltip,
      caption, opt_classNames, opt_renderer, opt_domHelper);
  bluemind.ui.editor.Toolbar.addDefaultFormatOptions(button);
  button.setDefaultCaption(bluemind.ui.editor.messages.MSG_FORMAT_NORMAL);
  // Format options don't have keyboard accelerators.
  goog.dom.classlist.add(button.getMenu().getContentElement(),
      goog.getCssName('goog-menu-noaccel'));
  // How to update this button.
  button.updateFromValue = function(value) {
    // Normalize value to null or a nonempty string (sometimes we get
    // the empty string, sometimes we get false...)
    value = value && value.length > 0 ? value : null;
    if (value != button.getValue()) {
      button.setValue(value);
     }
  };
  return button;
};

/**
 * Creates a toolbar button with the given ID, tooltip, and caption.  Applies
 * any custom CSS class names to the button's caption element. Use to create
 * the format menu, prefilled with default formats.
 * @param {string} id Button ID; must equal a {@link goog.editor.Command} for
 *     built-in buttons, anything else for custom buttons.
 * @param {string} tooltip Tooltip to be shown on hover.
 * @param {goog.ui.ControlContent} caption Button caption.
 * @param {string=} opt_classNames CSS class name(s) to apply to the caption
 *     element.
 * @param {goog.ui.MenuButtonRenderer=} opt_renderer Button renderer;
 *     defaults to
 *     {@link goog.ui.ToolbarMenuButtonRenderer} if unspecified.
 * @param {goog.dom.DomHelper=} opt_domHelper DOM helper, used for DOM
 *     creation; defaults to the current document if unspecified.
 * @return {!goog.ui.Button} A toolbar button.
 * @private
 */
bluemind.ui.editor.Toolbar.editHTMLFactory_ = function(id, tooltip,
    caption, opt_classNames, opt_renderer, opt_domHelper) {
  var button = goog.ui.editor.ToolbarFactory.makeButton(id, tooltip,
      caption, opt_classNames, opt_renderer, opt_domHelper);
  button.updateFromValue = function(value) {
    button.getParent().forEachChild(function(child) {
      if (child.getId() != this.getId()) {
        if (value) {
          child.wasEnabled = child.isEnabled();
          child.setEnabled(false);
        } else {
          child.setEnabled(child.wasEnabled);
          delete child.wasEnabled;
        }
      }
    }, button);
  };
  return button;
};

/**
 * Map of {@code goog.editor.Command}s to toolbar button descriptor objects,
 * each of which has the following attributes:
 * <ul>
 *   <li>{@code command} - The command corresponding to the
 *       button (mandatory)
 *   <li>{@code tooltip} - Tooltip text (optional); if unspecified, the button
 *       has no hover text
 *   <li>{@code caption} - Caption to display on the button (optional); if
 *       unspecified, the button has no text caption
 *   <li>{@code classes} - CSS class name(s) to be applied to the button's
 *       element when rendered (optional); if unspecified, defaults to
 *       'tr-icon'
 *       plus 'tr-' followed by the command ID, but without any leading '+'
 *       character (e.g. if the command ID is '+undo', then {@code classes}
 *       defaults to 'tr-icon tr-undo')
 *   <li>{@code factory} - factory function used to create the button, which
 *       must accept {@code id}, {@code tooltip}, {@code caption}, and
 *       {@code classes} as arguments, and must return an instance of
 *       {@link goog.ui.Button} or an appropriate subclass (optional); if
 *       unspecified, defaults to
 *       {@link goog.ui.editor.ToolbarFactory.makeToggleButton},
 *       since most built-in toolbar buttons are toggle buttons
 *   <li>(@code queryable} - Whether the button's state should be queried
 *       when updating the toolbar (optional).
 * </ul>
 * Note that this object is only used for creating toolbar buttons for
 * built-in editor commands; custom buttons aren't listed here.  Please don't
 * try to hack this!
 * @type {Object.<!bluemind.ui.editor.ButtonDescriptor>}.
 * @private
 */
bluemind.ui.editor.Toolbar.buttons_ = {};


/**
 * @typedef {{
 *   id: string, tooltip: ?string,
 *   caption: ?goog.ui.ControlContent, classes: ?string,
 *   factory: ?function(string, string, goog.ui.ControlContent, ?string,
 *       goog.ui.ButtonRenderer, goog.dom.DomHelper):goog.ui.Button,
 *   renderer: ?goog.ui.ControlRenderer,
 *   queryable:?boolean,
 *   submenu: ?Array.<string>
 *   }}
 */
bluemind.ui.editor.ButtonDescriptor;


/**
 * Built-in toolbar button descriptors.  See
 * {@link bluemind.ui.editor.Toolbar.buttons_} for details on button
 * descriptor objects.  This array is processed at JS parse time; each item is
 * inserted into {@link bluemind.ui.editor.Toolbar.buttons_}, and the array
 * itself is deleted and (hopefully) garbage-collected.
 * @type {Array.<!bluemind.ui.editor.ButtonDescriptor>}.
 * @private
 */
bluemind.ui.editor.Toolbar.BUTTONS_ = [{
  id: goog.editor.Command.UNDO,
  tooltip: bluemind.ui.editor.messages.MSG_UNDO_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-undo'),
  factory: bluemind.ui.editor.Toolbar.undoRedoButtonFactory_,
  queryable: true
}, {
  id: goog.editor.Command.REDO,
  tooltip: bluemind.ui.editor.messages.MSG_REDO_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-repeat'),
  factory: bluemind.ui.editor.Toolbar.undoRedoButtonFactory_,
  queryable: true
}, {
  id: goog.editor.Command.FONT_FACE,
  tooltip: bluemind.ui.editor.messages.MSG_FONT_FACE_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-font'),  
  factory: bluemind.ui.editor.Toolbar.fontFaceFactory_,
  renderer:  goog.ui.FlatMenuButtonRenderer.getInstance(),
  queryable: true
}, {
  id: goog.editor.Command.FONT_SIZE,
  tooltip: bluemind.ui.editor.messages.MSG_FONT_SIZE_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-text-height'),  
  factory: bluemind.ui.editor.Toolbar.fontSizeFactory_,
  renderer:  goog.ui.FlatMenuButtonRenderer.getInstance(),
  queryable: true
}, {
  id: goog.editor.Command.BOLD,
  tooltip: bluemind.ui.editor.messages.MSG_BOLD_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-bold'),
  queryable: true
}, {
  id: goog.editor.Command.ITALIC,
  tooltip: bluemind.ui.editor.messages.MSG_ITALIC_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-italic'),
  queryable: true
}, {
  id: goog.editor.Command.UNDERLINE,
  tooltip: bluemind.ui.editor.messages.MSG_UNDERLINE_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-underline'),
  queryable: true
}, {
  id: goog.editor.Command.FONT_COLOR,
  tooltip: bluemind.ui.editor.messages.MSG_FONT_COLOR_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fontcolorbutton') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-font'), 
  factory: bluemind.ui.editor.Toolbar.fontColorFactory_,
  queryable: true
}, {
  id: goog.editor.Command.BACKGROUND_COLOR,
  tooltip: bluemind.ui.editor.messages.MSG_BACKGROUND_COLOR_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-tint'),
  factory: bluemind.ui.editor.Toolbar.backgroundColorFactory_,
  queryable: true
}, {
  id: goog.editor.Command.LINK,
  tooltip: bluemind.ui.editor.messages.MSG_LINK_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-chain'),
  queryable: true
}, {
  id: goog.editor.Command.ORDERED_LIST,
  tooltip: bluemind.ui.editor.messages.MSG_ORDERED_LIST_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-list-ol'),
  queryable: true
}, {
  id: goog.editor.Command.UNORDERED_LIST,
  tooltip: bluemind.ui.editor.messages.MSG_UNORDERED_LIST_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-list-ul'),
  queryable: true
}, {
  id: goog.editor.Command.OUTDENT,
  tooltip: bluemind.ui.editor.messages.MSG_OUTDENT_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-outdent'),
  factory: goog.ui.editor.ToolbarFactory.makeButton
}, {
  id: goog.editor.Command.INDENT,
  tooltip: bluemind.ui.editor.messages.MSG_INDENT_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-indent'),
  factory: goog.ui.editor.ToolbarFactory.makeButton
}, {
  id: goog.editor.Command.JUSTIFY_LEFT,
  tooltip: bluemind.ui.editor.messages.MSG_ALIGN_LEFT_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-align-left'),
  queryable: true,
  factory: goog.ui.editor.ToolbarFactory.makeButton
}, {
  id: goog.editor.Command.JUSTIFY_CENTER,
  tooltip: bluemind.ui.editor.messages.MSG_ALIGN_CENTER_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-align-center'),
  queryable: true,
  factory: goog.ui.editor.ToolbarFactory.makeButton
}, {
  id: goog.editor.Command.JUSTIFY_RIGHT,
  tooltip: bluemind.ui.editor.messages.MSG_ALIGN_RIGHT_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-align-right'),
  queryable: true,
  factory: goog.ui.editor.ToolbarFactory.makeButton
}, {
  id: goog.editor.Command.JUSTIFY_FULL,
  tooltip: bluemind.ui.editor.messages.MSG_JUSTIFY_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-align-justify'),
  queryable: true,
  factory: goog.ui.editor.ToolbarFactory.makeButton
}, {
  id: goog.editor.Command.REMOVE_FORMAT,
  tooltip: bluemind.ui.editor.messages.MSG_REMOVE_FORMAT_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-times'),
  factory: goog.ui.editor.ToolbarFactory.makeButton
}, {
  id: goog.editor.Command.IMAGE,
  tooltip: bluemind.ui.editor.messages.MSG_IMAGE_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-picture-o'),
  factory: goog.ui.editor.ToolbarFactory.makeButton
}, {
  id: goog.editor.Command.STRIKE_THROUGH,
  tooltip: bluemind.ui.editor.messages.MSG_STRIKE_THROUGH_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-strikethrough'),
  queryable: true
}, {
  id: goog.editor.Command.SUBSCRIPT,
  tooltip: bluemind.ui.editor.messages.MSG_SUBSCRIPT,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-subscript'),
  queryable: true
} , {
  id: goog.editor.Command.SUPERSCRIPT,
  tooltip: bluemind.ui.editor.messages.MSG_SUPERSCRIPT,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-superscript'),
  queryable: true
}, {
  id: goog.editor.Command.DIR_LTR,
  tooltip: bluemind.ui.editor.messages.MSG_DIR_LTR_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-angle-double-right'),
  queryable: true
}, {
  id: goog.editor.Command.DIR_RTL,
  tooltip: bluemind.ui.editor.messages.MSG_DIR_RTL_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-angle-double-left'),
  factory: bluemind.ui.editor.Toolbar.rtlButtonFactory_,
  queryable: true
}, {
  id: goog.editor.Command.BLOCKQUOTE,
  tooltip: bluemind.ui.editor.messages.MSG_BLOCKQUOTE_TITLE,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-quote-right'),
  queryable: true
}, {
  id: goog.editor.Command.FORMAT_BLOCK,
  tooltip: bluemind.ui.editor.messages.MSG_FORMAT_BLOCK_TITLE,
  caption: bluemind.ui.editor.messages.MSG_FORMAT_BLOCK_CAPTION,
  classes: goog.getCssName('tr-icon') + ' ' + goog.getCssName('tr-formatBlock'),
  factory: bluemind.ui.editor.Toolbar.formatBlockFactory_,
  renderer:  goog.ui.style.app.MenuButtonRenderer.getInstance(),
  queryable: true
}, {
  id: goog.editor.Command.EDIT_HTML,
  tooltip: bluemind.ui.editor.messages.MSG_EDIT_HTML_TITLE,
  caption: bluemind.ui.editor.messages.MSG_EDIT_HTML_CAPTION,
  classes: goog.getCssName('codebutton') + ' ' + goog.getCssName('tr-icon') + ' ' + goog.getCssName('fa-lg') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-code'),
  factory: bluemind.ui.editor.Toolbar.editHTMLFactory_
}];


(function() {
// Create the bluemind.ui.editor.Toolbar.buttons_ map from
// bluemind.ui.editor.Toolbar.BUTTONS_.
for (var i = 0, button;
    button = bluemind.ui.editor.Toolbar.BUTTONS_[i]; i++) {
  bluemind.ui.editor.Toolbar.buttons_[button.id] = button;
}

// bluemind.ui.editor.Toolbar.BUTTONS_ is no longer needed
// once the map is ready.
delete bluemind.ui.editor.Toolbar.BUTTONS_;

})();




/**
 * A select control for a toolbar.
 *
 * @param {goog.ui.ControlContent} caption Default caption or existing DOM
 *     structure to display as the button's caption when nothing is selected.
 * @param {goog.ui.Menu=} opt_menu Menu containing selection options.
 * @param {goog.ui.MenuButtonRenderer=} opt_renderer Renderer used to
 *     render or decorate the control; defaults to
 *     {@link goog.ui.ToolbarMenuButtonRenderer}.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM hepler, used for
 *     document interaction.
 * @constructor
 * @extends {goog.ui.ToolbarSelect}
 */
bluemind.ui.editor.Toolbar.ToolbarSelect = function(
    caption, opt_menu, opt_renderer, opt_domHelper) {
  goog.base(this, caption, opt_menu, opt_renderer, opt_domHelper);
};
goog.inherits(bluemind.ui.editor.Toolbar.ToolbarSelect ,goog.ui.ToolbarSelect);

/** @override */
bluemind.ui.editor.Toolbar.ToolbarSelect.prototype.updateCaption = function() {
  this.setContent(this.getDefaultCaption());
};

