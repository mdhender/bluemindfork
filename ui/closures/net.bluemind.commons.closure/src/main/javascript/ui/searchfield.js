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
 * @fileoverview Widget for global search field.
 */

goog.provide('bluemind.ui.SearchField');
goog.provide('bluemind.ui.searchfield.TermMatcher');
goog.provide('bluemind.ui.searchfield.TermRenderer');

goog.require('bluemind.ui.CartoucheBoxSearchItem');
goog.require('bluemind.ui.MultiEntryField');
goog.require('goog.ui.ac');
goog.require('goog.ui.ac.Renderer.CustomRenderer');
goog.require('goog.array');
goog.require('goog.string');

/**
 * A search field control.
 * 
 * @param {Array.<bluemind.model.search.SearchTerm>=} opt_terms Default used
 * for cartouche.
 * @param {bluemind.ui.style.MultiEntryFieldRenderer=} opt_renderer Renderer
 * used to render or decorate the container; defaults to
 * {@link bluemind.ui.style.MultiEntryFieldRenderer}.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @extends {bluemind.ui.MultiEntryField}
 * @constructor
 */
bluemind.ui.SearchField = function(opt_terms, opt_renderer, opt_domHelper) {
  goog.base(this, null, opt_renderer, opt_domHelper);
  this.terms_ = opt_terms || [];
  this.setUniqueValue(false);
  this.setSeparators(',;');
  this.addClassName('bm-searchfield');

};
goog.inherits(bluemind.ui.SearchField, bluemind.ui.MultiEntryField);

/**
 * Default terms to build scope part of query terms.
 * 
 * @type {Array.<bluemind.model.search.SearchTerm>}
 * @private
 */
bluemind.ui.SearchField.prototype.terms_;

/**
 * Autocomplete mechanism
 * 
 * @type {goog.ui.ac.AutoComplete}
 * @private
 */
bluemind.ui.SearchField.prototype.autocomplete_;

/** @override */
bluemind.ui.SearchField.prototype.addValue = function(value) {
  if (value instanceof bluemind.ui.CartoucheBoxItem) {
    goog.base(this, 'addValue', value);
  } else {
    if (value != '') {// && (!this.unique_ || this.contains_(value)) {
      var entry = new bluemind.ui.CartoucheBoxSearchItem(value, this.terms_);
      goog.base(this, 'addValue', entry);
    }
  }
};

/** @override */
bluemind.ui.SearchField.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  var el = this.getDomHelper().getParentElement(this.getElement());
  var matcher = new bluemind.ui.searchfield.TermMatcher(this.terms_);
  var renderer = new goog.ui.ac.Renderer(el,
    new bluemind.ui.searchfield.TermRenderer());
  var handler = new goog.ui.ac.InputHandler(this.getSeparators());
  this.autocomplete_ = new goog.ui.ac.AutoComplete(matcher, renderer, handler);
  handler.attachAutoComplete(this.autocomplete_);
  this.autocomplete_.attachInputs(this.getRenderer().getField(this));

  handler = this.getHandler();
  handler.listen(this.autocomplete_, goog.ui.ac.AutoComplete.EventType.UPDATE,
    this.handleAutocomplete_);
};

/** @override */
bluemind.ui.SearchField.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  if (this.autocomplete_) {
    this.autocomplete_.dispose();
    this.autocomplete_ = null;
  }
};

/** @override */
bluemind.ui.SearchField.prototype.reset = function() {
  var children = [];
  this.getChild('entries').forEachChild(function(child) {
    if (!child.isReadOnly()) {
      children.push(child);
    } else {
      child.reset();
    }
  }, this);
  for (var i = 0; i < children.length; i++) {
    this.getChild('entries').deleteChild(children[i]);
  }
};

/**
 * Handle autocomplete value selected
 * 
 * @param {Object} e autocomple event
 * @private
 */
bluemind.ui.SearchField.prototype.handleAutocomplete_ = function(e) {
  var part = e.row;
  this.addValue(part);
  this.field.clear();
};

/**
 * Set value as a search query
 * 
 * @param {Array|string} query
 */
bluemind.ui.SearchField.prototype.setQuery = function(query) {
  var values = [];
  if (goog.isArray(query)) {
    values = query;
  } else if (goog.isString(query) && query != '') {
    var split = new RegExp('[' + this.getSeparators() + ']');
    goog.array.forEach(query.split(split), function(part) {
      values.push(part.replace(/:\(([^)]*)\)/, ':$1'));
    });
  }
  this.setValue(values);
};

/**
 * @return {string}
 */
bluemind.ui.SearchField.prototype.getQuery = function() {
  return this.getValue().join(';');
};
/**
 * Fake auto complete. It create match from the term list.
 * 
 * @param {Array.<bluemind.model.search.SearchTerm>} terms Term used for
 * autocomplete.
 * @constructor
 */
bluemind.ui.searchfield.TermMatcher = function(terms) {
  this.terms_ = terms;
};

/**
 * Term used for autocomplete.
 * 
 * @type {Array.<bluemind.model.search.SearchTerm>}
 * @private
 */
bluemind.ui.searchfield.TermMatcher.prototype.terms_;

/**
 * Function used to pass matches to the autocomplete
 * 
 * @param {string} token Token to match.
 * @param {number} max Max number of matches to return.
 * @param {Function} handler callback to execute after matching.
 * @param {string=} opt_fullString The full string from the input box.
 */
bluemind.ui.searchfield.TermMatcher.prototype.requestMatchingRows = function(
  token, max, handler, opt_fullString) {
  if (goog.string.trim(token) != '') {
    var def = bluemind.model.search.SearchTermFactory.getInstance()
      .getDefault();
    var list = [];
    var term = bluemind.model.search.SearchTerm.fromString(token);
    if (term == null) {
      for (var i = 0; i < this.terms_.length; i++) {
        var t = this.terms_[i];
        if (t == def) {
          list.unshift(new bluemind.model.search.QueryPart(token, t));
        } else {
          list.push(new bluemind.model.search.QueryPart(token, t));
        }
      }
    } else {
      list.push(bluemind.model.search.QueryPart.fromString(token));
    }
    handler(token, list);
  } else {
    handler(token, []);
  }
};

/**
 * Rendering the autocomplete box for terms.
 * 
 * @constructor
 * @extends goog.ui.ac.Renderer.CustomRenderer
 */
bluemind.ui.searchfield.TermRenderer = function() {
};
goog.inherits(bluemind.ui.searchfield.TermRenderer,
  goog.ui.ac.Renderer.CustomRenderer);

/** @override */
bluemind.ui.searchfield.TermRenderer.prototype.render = null;

/** @override */
bluemind.ui.searchfield.TermRenderer.prototype.renderRow = function(row, token,
  node) {
  var part = row.data;
  if (node && part) {
    node.innerHTML = part.getTerm().format(part.getValue());
  }
};

goog.exportSymbol('bluemind.ui.SearchField', bluemind.ui.SearchField);

goog.exportProperty(bluemind.ui.SearchField.prototype, 'render',
  bluemind.ui.SearchField.prototype.render);
goog.exportProperty(bluemind.ui.SearchField.prototype, 'getValue',
  bluemind.ui.SearchField.prototype.getValue);
goog.exportProperty(bluemind.ui.SearchField.prototype, 'addValue',
  bluemind.ui.SearchField.prototype.addValue);
goog.exportProperty(bluemind.ui.SearchField.prototype, 'setValue',
  bluemind.ui.SearchField.prototype.setValue);

goog.exportProperty(bluemind.ui.SearchField.prototype, 'reset',
  bluemind.ui.SearchField.prototype.reset);
goog.exportProperty(goog.events.EventTarget.prototype, 'addEventListener',
  bluemind.ui.SearchField.prototype.addEventListener);
