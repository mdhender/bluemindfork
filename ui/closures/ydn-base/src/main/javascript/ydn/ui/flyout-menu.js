// Copyright 2014 YDN Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


/**
 * @fileoverview CSS base Light weight popup menu that flyout on hovering over
 * the menu button.
 *
 * The main advantage for using FlyoutMenu is less listener is required and
 * does not require disposing if no handler are attached. Instead, event handler
 * can be attach to ancestor element.
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */


goog.provide('ydn.ui.FlyoutMenu');
goog.require('goog.asserts');
goog.require('goog.dom');
goog.require('goog.style');
goog.require('ydn.crm.ui');



/**
 * Light weight popup menu that flyout on hovering over the menu button.
 * <pre>
 *   var menu = new ydn.ui.FlyoutMenu();
 *   menu.setItems([{
 *     label: 'File'
 *   }, {
 *     label: 'Edit'
 *   ]);
 *   menu.render(root);
 *   root.onclick = function(e) {
 *     var cmds = menu.handleClick(e);
 *     if (cmds) {
 *       // handle menu click.
 *     }
 *   }
 * Simple rendering a menu.
 * <pre>
 *   var btn = document.createElement('span');
 *   btn.className = ydn.crm.ui.CSS_CLASS_MORE_MENU;
 *   ydn.ui.FlyoutMenu.decoratePopupMenu(btn, options);
 * </pre>
 * @param {ydn.ui.FlyoutMenu.Option=} opt_option
 * @param {Array.<?ydn.ui.FlyoutMenu.ItemOption>=} opt_menu_items
 * @constructor
 * @struct
 */
ydn.ui.FlyoutMenu = function(opt_option, opt_menu_items) {

  /**
   * @type {Element}
   * @private
   */
  this.el_ = null;
  /**
   * @type {ydn.ui.FlyoutMenu.Option}
   * @private
   */
  this.option_ = opt_option || /** @type {ydn.ui.FlyoutMenu.Option} */ ({});
  /**
   * @type {Array.<?ydn.ui.FlyoutMenu.ItemOption>}
   * @private
   */
  this.items_option_ = ydn.object.clone(opt_menu_items || []);
};


/**
 * @const
 * @type {string}
 */
ydn.ui.FlyoutMenu.CSS_CLASS = 'more-menu';


/**
 * @const
 * @type {string}
 */
ydn.ui.FlyoutMenu.CSS_CLASS_MENU = 'flyout-menu';


/**
 * @const
 * @type {string}
 */
ydn.ui.FlyoutMenu.CSS_CLASS_LEFT = 'left-menu';


/**
 * @const
 * @type {string}
 */
ydn.ui.FlyoutMenu.SVG_ICON_NAME = 'more-vert';


/**
 * @param {Element} el
 */
ydn.ui.FlyoutMenu.prototype.render = function(el) {
  goog.asserts.assert(!this.el_, 'Already rendered.');
  this.el_ = document.createElement('div');
  this.el_.className = ydn.ui.FlyoutMenu.CSS_CLASS;
  var icon_name = this.option_.iconName || ydn.ui.FlyoutMenu.SVG_ICON_NAME;
  var svg = ydn.crm.ui.createSvgIcon(icon_name);
  var button = document.createElement('div');
  button.className = 'svg-button';
  button.appendChild(svg);
  var menu = ydn.ui.FlyoutMenu.renderMenu(this.items_option_);
  menu.classList.add(ydn.ui.FlyoutMenu.CSS_CLASS_MENU);
  menu.classList.add(ydn.ui.FlyoutMenu.CSS_CLASS_LEFT);
  this.el_.appendChild(button);
  this.el_.appendChild(menu);
  el.appendChild(this.el_);
};


/**
 * @typedef {{
 *   name: string,
 *   checked: boolean
 * }}
 */
ydn.ui.FlyoutMenu.ItemState;


/**
 * @param {Element} el
 * @param {Array<ydn.ui.FlyoutMenu.ItemState>=} opt_states
 * @return {!Array<ydn.ui.FlyoutMenu.ItemState>}
 * @private
 */
ydn.ui.FlyoutMenu.listItemState_ = function(el, opt_states) {
  var states = opt_states || [];
  if (el && el instanceof Element) {
    if (el.classList.contains('goog-menuitem')) {
      var name = el.getAttribute('name');
      var val = el.classList.contains('goog-option-selected');
      states.push({name: name, checked: val});
      return ydn.ui.FlyoutMenu.listItemState_(el.parentElement, states);
    } else if (el.classList.contains('goog-menu')) {
      return ydn.ui.FlyoutMenu.listItemState_(el.parentElement, states);
    }
  }
  return states;
};


/**
 * @param {Element} el
 * @param {string} names
 * @return {string}
 * @private
 */
ydn.ui.FlyoutMenu.listItemName_ = function(el, names) {
  if (el && el instanceof Element) {
    if (el.classList.contains('goog-menuitem')) {
      var name = el.getAttribute('name');
      if (!names) {
        names = name;
      } else {
        names = name + ',' + names;
      }
      return ydn.ui.FlyoutMenu.listItemName_(el.parentElement, names);
    } else if (el.classList.contains('goog-menu')) {
      return ydn.ui.FlyoutMenu.listItemName_(el.parentElement, names);
    }
  }
  return names;
};


/**
 * Handle click event to root menu.
 * @param {goog.events.BrowserEvent|Event} e
 * @return {?string} if menu item is click, it will be return item name. For
 * hierarchical menu, item name are separated by comma from root to leave, i.e.,
 * 'sync,contact'.
 * @see ydn.ui.FlyoutMenu.handleClickState with check state.
 */
ydn.ui.FlyoutMenu.handleClick = function(e) {
  var item = null;

  if (e.target instanceof Element) {
    if (e.target.classList.contains('goog-menuitem')) {
      item = e.target;
    } else if (e.target.parentElement &&
        e.target.parentElement.classList.contains('goog-menuitem')) {
      item = e.target.parentElement;
    }
  }
  if (item) {
    e.preventDefault();
    e.stopPropagation();
    var is_disable = item.classList.contains('goog-menuitem-disabled');
    var el = goog.dom.getAncestorByClass(item, ydn.ui.FlyoutMenu.CSS_CLASS_MENU);
    goog.style.setElementShown(el, false);
    setTimeout(function() {
      // menu show/hide status is determine by hover state
      goog.style.setElementShown(el, true);
    }, 1000);
    return is_disable ? null : ydn.ui.FlyoutMenu.listItemName_(item, '');
  }
  return null;
};


/**
 * Handle click event to root menu.
 * @param {goog.events.BrowserEvent|Event} e
 * @return {Array<ydn.ui.FlyoutMenu.ItemState>} if menu item is click, it will be return item name. For
 * hierarchical menu, item name are separated by comma from root to leave, i.e.,
 * 'sync,contact'.
 * @see ydn.ui.FlyoutMenu.handleClick
 */
ydn.ui.FlyoutMenu.handleClickState = function(e) {
  var item = null;

  if (e.target instanceof Element) {
    if (e.target.classList.contains('goog-menuitem')) {
      item = e.target;
    } else if (e.target.parentElement &&
        e.target.parentElement.classList.contains('goog-menuitem')) {
      item = e.target.parentElement;
    }
  }
  if (item) {
    e.preventDefault();
    e.stopPropagation();
    var is_disable = item.classList.contains('goog-menuitem-disabled');
    var el = goog.dom.getAncestorByClass(item, ydn.ui.FlyoutMenu.CSS_CLASS_MENU);
    goog.style.setElementShown(el, false);
    setTimeout(function() {
      // menu show/hide status is determine by hover state
      goog.style.setElementShown(el, true);
    }, 1000);
    return is_disable ? null : ydn.ui.FlyoutMenu.listItemState_(item);
  }
  return null;
};


/**
 * Handle click event to root menu.
 * @param {goog.events.BrowserEvent} e
 * @return {?string} if menu item is click, it will be return item name.
 */
ydn.ui.FlyoutMenu.prototype.handleClick = function(e) {
  return ydn.ui.FlyoutMenu.handleClick(e);
};


/**
 * Set menu items.
 * @param {Array.<?ydn.ui.FlyoutMenu.ItemOption>} item
 */
ydn.ui.FlyoutMenu.prototype.setItems = function(item) {
  this.items_option_ = ydn.object.clone(item);
  if (this.el_) {
    var menu = this.el_.querySelector('.' + ydn.ui.FlyoutMenu.CSS_CLASS_MENU);
    this.el_.removeChild(menu);
    menu = ydn.ui.FlyoutMenu.renderMenu(this.items_option_);
    menu.classList.add(ydn.ui.FlyoutMenu.CSS_CLASS_MENU);
    menu.classList.add(ydn.ui.FlyoutMenu.CSS_CLASS_LEFT);
    this.el_.appendChild(menu);
  }
};


/**
 * @return {Element}
 */
ydn.ui.FlyoutMenu.prototype.getElement = function() {
  return this.el_;
};


/**
 * @typedef {{
 *   iconName: (string|undefined),
 *   className: (string|undefined),
 *   title: (string|undefined),
 *   isRightMenu: (boolean|undefined)
 * }}
 * iconName: svg icon name 'more-vert', 'more-horiz' or 'menu'
 * className: css class name.
 * isRightMenu: menu render to right side. By default, render to left.
 * title: div title for the menu item.
 */
ydn.ui.FlyoutMenu.Option;


/**
 * @typedef {{
 *   label: string,
 *   name: (string),
 *   value: *,
 *   title: (string|undefined),
 *   type: (string|undefined),
 *   disabled: (boolean|undefined),
 *   children: (Array.<ydn.ui.FlyoutMenu.ItemOption>|undefined)
 * }}
 * label: menu text.
 * name: commend in dispatching event.
 * type: SugarCrm.ModuleField#type for input type, default to type='text', for
 * 'bool', it becomes type='checkbox'
 * When type is 'bool', value can be 'true' or 'false'.
 */
ydn.ui.FlyoutMenu.ItemOption;


/**
 * @param {Array.<?ydn.ui.FlyoutMenu.ItemOption>} items_option
 * @return {Element}
 */
ydn.ui.FlyoutMenu.renderMenu = function(items_option) {
  // console.log(options);
  var dom = goog.dom.getDomHelper();
  var items = [];
  for (var i = 0; i < items_option.length; i++) {
    var opt = items_option[i];
    if (!opt) {
      // render as menu separator
      var rep = dom.createDom('div', {
        'class': 'goog-menuseparator',
        'role': 'separator'
      });
      items.push(rep);
      continue;
    }

    var menu_content = [dom.createDom('div', {
      'class': 'goog-menuitem-content'
    }, opt.label)];
    if (opt.type == 'bool') {
      var chk = dom.createDom('div', {
        'class': 'goog-menuitem-checkbox',
        'role': 'menuitem'
      });
      menu_content.unshift(chk);
    }
    if (opt.children) {
      var svg_arrow = ydn.crm.ui.createSvgIcon('arrow-drop-right');
      svg_arrow.classList.add('left-arrow');
      menu_content.unshift(svg_arrow);
    }
    var div_options = {
      'class': 'goog-menuitem',
      'role': opt.type == 'bool' ? 'goog-menuitem-checkbox' : 'menuitem'
    };
    if (opt.title) {
      div_options['title'] = opt.title;
    }
    var menuitem = dom.createDom('div', div_options, menu_content);
    menuitem.setAttribute('name', opt.name);
    if (opt.value) {
      menuitem.classList.add('goog-option-selected');
    }
    if (opt.children) {
      var sub_menu = ydn.ui.FlyoutMenu.renderMenu(opt.children);
      menuitem.appendChild(sub_menu);
    }
    if (opt.disabled) {
      menuitem.classList.add('goog-menuitem-disabled');
    }
    items.push(menuitem);
  }

  var menu = dom.createDom('div', {
    'class': 'goog-menu goog-menu-vertical',
    'role': 'menu'
  },
  items);
  return menu;
};


/**
 * <pre>
 *   var el = document.querySelector('#menu');
 *   var menu = ydn.ui.FlyoutMenu.decoratePopupMenu(el, items);
 *   el.onclick = function(e) {
 *     var name = ydn.ui.FlyoutMenu.handleClick(e);
 *     if (name) {
 *       // menu item with the name click.
 *     }
 *   }
 * </pre>
 * @param {Element} el
 * @param {Array<?ydn.ui.FlyoutMenu.ItemOption>} items_option
 * @param {ydn.ui.FlyoutMenu.Option=} opt_option
 */
ydn.ui.FlyoutMenu.decoratePopupMenu = function(el, items_option, opt_option) {
  // console.log(options);
  var icon_name = ydn.ui.FlyoutMenu.SVG_ICON_NAME;
  if (opt_option) {
    if (opt_option.iconName) {
      icon_name = opt_option.iconName;
    }
  }
  var button = document.createElement('span');
  button.className = 'flyout-button';
  var svg = ydn.crm.ui.createSvgIcon(icon_name);
  var menu = ydn.ui.FlyoutMenu.renderMenu(items_option);
  menu.classList.add(ydn.ui.FlyoutMenu.CSS_CLASS_MENU);
  if (!opt_option || !opt_option.isRightMenu) {
    menu.classList.add(ydn.ui.FlyoutMenu.CSS_CLASS_LEFT);
  }
  button.appendChild(svg);
  button.appendChild(menu);
  el.classList.add(ydn.ui.FlyoutMenu.CSS_CLASS);
  el.appendChild(button);
};


/**
 * Decorate as enable or disable to a given named menu item.
 * @param {Element} menu goog-menu element
 * @param {string} name menu item name.
 * @param {boolean} val true to enable, false to disable.
 */
ydn.ui.FlyoutMenu.setEnableMenuItem = function(menu, name, val) {
  var el = menu.querySelector('div.goog-menuitem[name="' + name + '"]');
  if (!el) {
    return;
  }
  if (val) {
    el.classList.remove('goog-menuitem-disabled');
  } else {
    el.classList.add('goog-menuitem-disabled');
  }
};


/**
 * Set menu label.
 * @param {Element} menu goog-menu element
 * @param {string} name menu item name.
 * @param {string} val true to enable, false to disable.
 * @param {string=} opt_tooltip if not provided, will clear existing tooltip.
 */
ydn.ui.FlyoutMenu.setMenuItemLabel = function(menu, name, val, opt_tooltip) {
  var el = menu.querySelector('div.goog-menuitem[name="' + name + '"]');
  if (!el) {
    return;
  }
  var content = el.querySelector('.goog-menuitem-content');
  content.textContent = val;
  content.setAttribute('title', opt_tooltip || '');
};


/**
 * Decorate as enable or disable to a given named menu item.
 * @param {string} name menu item name.
 * @param {boolean} val true to enable, false to disable.
 */
ydn.ui.FlyoutMenu.prototype.setEnableMenuItem = function(name, val) {
  var el = this.getElement();
  ydn.ui.FlyoutMenu.setEnableMenuItem(el, name, val);
};


/**
 * Set menu label.
 * @param {string} name menu item name.
 * @param {string} val true to enable, false to disable.
 * @param {string=} opt_tooltip if not provided, will clear existing tooltip.
 */
ydn.ui.FlyoutMenu.prototype.setMenuItemLabel = function(name, val, opt_tooltip) {
  var el = this.getElement();
  ydn.ui.FlyoutMenu.setMenuItemLabel(el, name, val, opt_tooltip);
};
