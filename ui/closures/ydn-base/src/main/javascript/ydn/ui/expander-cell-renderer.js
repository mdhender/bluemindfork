// Copyright 2012 YDN Authors. All Rights Reserved.
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
 * @fileoverview Cell renderer for Closure Grid.
 *
 * @link https://github.com/dekajp/google-closure-grid
 * @author kyawtun@yathit.com (Kyaw Tun)
 */


goog.provide('ydn.ui.ExpanderCellRenderer');
goog.require('pear.ui.GridCellRenderer');



/**
 * Cell renderer for Closure Grid.
 * @constructor
 * @extends {pear.ui.GridCellRenderer}
 */
ydn.ui.ExpanderCellRenderer = function() {

};
goog.inherits(ydn.ui.ExpanderCellRenderer, pear.ui.GridCellRenderer);
goog.addSingletonGetter(ydn.ui.ExpanderCellRenderer);


/**
 * @const
 * @type {string}
 */
ydn.ui.ExpanderCellRenderer.CSS_CLASS = 'expander';


/**
 * @override
 */
ydn.ui.ExpanderCellRenderer.prototype.createDom = function(gridcell) {
  var el = goog.base(this, 'createDom', gridcell);
  el.textContent = '►';
  el.classList.add(ydn.ui.ExpanderCellRenderer.CSS_CLASS);
  el.classList.add('no-border');
  el.setAttribute('title', 'Show detail');
  return el;
};


/**
 * Set open/close expander state.
 * @param {pear.ui.GridCell} cell
 * @param {boolean} open
 */
ydn.ui.ExpanderCellRenderer.setOpen = function(cell, open) {
  var el = cell.getElement().querySelector('.' + ydn.ui.ExpanderCellRenderer.CSS_CLASS);
  el.textContent = open ? '▼' : '►';
  if (open) {
    el.setAttribute('title', 'Hide detail');
  } else {
    el.setAttribute('title', 'Show detail');
  }
};


/**
 * Set open/close expander state.
 * @param {pear.ui.GridCell} cell
 * @return {boolean} open
 */
ydn.ui.ExpanderCellRenderer.isOpen = function(cell) {
  var el = cell.getElement().querySelector(ydn.ui.ExpanderCellRenderer.CSS_CLASS);
  return el.textContent == '▼';
};
