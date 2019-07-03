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
 * @fileoverview Renderer for {@link bluemind.ui.MultiEntryField}s.
 *
 */

goog.provide('bluemind.ui.style.MultiEntryFieldRenderer');

goog.require('goog.dom');
goog.require('goog.a11y.aria');
goog.require('goog.a11y.aria.Role');
goog.require('goog.dom.classes');
goog.require('goog.ui.ControlContent');
goog.require('goog.ui.ControlRenderer');
goog.require('goog.ui.registry');



/**
 * Default renderer for {@link goog.ui.MultiEntryField}s.  
 * @constructor
 * @extends {goog.ui.ControlRenderer}
 */
bluemind.ui.style.MultiEntryFieldRenderer = function() {
  goog.base(this);
};
goog.inherits(bluemind.ui.style.MultiEntryFieldRenderer, goog.ui.ControlRenderer);
goog.addSingletonGetter(bluemind.ui.style.MultiEntryFieldRenderer);

/**
 * Default CSS class to be applied to the root element of components rendered
 * by this renderer.
 * @type {string}
 */
bluemind.ui.style.MultiEntryFieldRenderer.CSS_CLASS = goog.getCssName('multientryfield');

/** @override */
bluemind.ui.style.MultiEntryFieldRenderer.prototype.getCssClass = function() {
  return bluemind.ui.style.MultiEntryFieldRenderer.CSS_CLASS;
};

/** @override */
bluemind.ui.style.MultiEntryFieldRenderer.prototype.createDom = function(control) {
  var dom = control.getDomHelper();
  var classNames = this.getClassNames(control);
  classNames.push(goog.getCssName('goog-inline-block'));
  var baseClass = this.getCssClass();
  var element = dom.createDom('div', classNames.join(' '), 
    dom.createDom('textarea', 
      {'class': goog.getCssName(baseClass, 'field'), 'autocomplete': 'off', 'rows': '1'}
    )
  );
  
  return element;
};

/**
 * Get the element where the new entry value is entered.
 * @param {goog.ui.Control} control Control to render.
 * @return {Element} Root element for the field.
 */
bluemind.ui.style.MultiEntryFieldRenderer.prototype.getField = function(control) {
  return control.getElement().getElementsByTagName('textarea')[0];
};

/**
 * Get the element where the entries are stored.
 * @param {goog.ui.Control} control Control to render.
 * @return {Element} Root element for the entries.
 */
bluemind.ui.style.MultiEntryFieldRenderer.prototype.getEntries = function(control) {
  return control.getElement().getElementsByTagName('ul')[0];
};

/**
 * Resize field according to it's content.
 * @param {goog.ui.Control} control Control to render.
 */
bluemind.ui.style.MultiEntryFieldRenderer.prototype.adjust = function(control) {
  var dom = control.getDomHelper();
  var li = dom.getLastElementChild(this.getEntries(control));
  var field = this.getField(control);
  if (li != null) {
    var liSize = goog.style.getSize(li);
    var coords = goog.style.getPosition(li);
  } else {
    var liSize = new goog.math.Size(0, 0);
    var coords = new goog.math.Coordinate(0, 0);
  }
  coords.x += liSize.width;
  var textSize = goog.style.getSize(field);
  if (textSize.width < (coords.x + 30)) {
    field.style.paddingLeft = '4px';
    field.style.paddingTop = (coords.y + liSize.height + 2) + 'px';
  } else {
    field.style.paddingLeft = (coords.x + 4) + 'px';
    field.style.paddingTop = (coords.y + 2) + 'px';
  }
};
