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
 * @fileoverview Create / Update / Retrieve search terms.
 **/


goog.provide('bluemind.model.search.SearchTermFactory');

goog.require('bluemind.model.search.SearchTerm');
goog.require('goog.structs.Map');
/**
 * Factory for search terms.
 *
 * @constructor
 */
bluemind.model.search.SearchTermFactory = function() {
  this.map_ = new goog.structs.Map();
  this.reverse_ = new goog.structs.Map();
};

goog.addSingletonGetter(bluemind.model.search.SearchTermFactory);

/**
 * Map containing all search terms
 * @type {goog.structs.Map.<string, bluemind.model.search.SearchTerm>}
 * @private
 */
bluemind.model.search.SearchTermFactory.prototype.map_;

/**
 * Default term
 * @type {bluemind.model.search.SearchTerm}
 * @private
 */
bluemind.model.search.SearchTermFactory.prototype.default_;

/**
 * Reverse map containing all search terms
 * @type {goog.structs.Map.<string, bluemind.model.search.SearchTerm>}
 * @private
 */
bluemind.model.search.SearchTermFactory.prototype.reverse_;

/**
 * Create a term if does not already exist.
 * @param {bluemind.model.search.SearchTerm} term Term to create.
 * @param {boolean=} opt_default Is this term the default one
 * @return {bluemind.model.search.SearchTerm} Created search term.
 **/
bluemind.model.search.SearchTermFactory.prototype.store = function(term, opt_default) {
  var pattern = term.getTerm().toLowerCase();
  if (!this.map_.containsKey(pattern)) {
    this.map_.set(pattern, term);
    this.map_.set(term.getDisplayname().toLowerCase(), term);
  } else {
    var t = /** @type bluemind.model.search.SearchTerm */ (this.map_.get(pattern));
    if (t.getDisplayname() == t.getTerm() && term.getDisplayname() != t.getTerm()) {
      this.reverse_.remove(t.getDisplayname().toLowerCase());
      t.setDisplayname(term.getDisplayname());
      this.reverse_.set(t.getDisplayname().toLowerCase(), t);
    }
    term = t;
  }
  if (!!opt_default) {
    this.default_ = term;
  }
  return term;
};


/**
 * Get a search term. If it is not already present, it will be created. 
 * @param {string} term Term key to search
 * @return {bluemind.model.search.SearchTerm} Matching search term if present.
 */
bluemind.model.search.SearchTermFactory.prototype.get = function(term) {
  if (!this.map_.containsKey(term.toLowerCase())) {
    this.store(new bluemind.model.search.SearchTerm(term));
  }  
  return /** @type bluemind.model.search.SearchTerm */ (this.map_.get(term));
};

/**
 * Find a search term. Term will be search by term and by displayname.
 * @param {string} pattern Term key to search
 * @return {bluemind.model.search.SearchTerm} Matching search term if present.
 */
bluemind.model.search.SearchTermFactory.prototype.find = function(pattern) {
  pattern = pattern.toLowerCase();
  if (this.map_.containsKey(pattern)) {
    return /** @type bluemind.model.search.SearchTerm */ (this.map_.get(pattern));
  }
  if (this.reverse_.containsKey(pattern)) {
    return /** @type bluemind.model.search.SearchTerm */ (this.reverse_.get(pattern));
  }
  return null;
};

/**
 * Get a search term. If it is not already present, it will be created. 
 * @return {bluemind.model.search.SearchTerm} Default term.
 */
bluemind.model.search.SearchTermFactory.prototype.getDefault = function() {
  return this.default_;
};

/**
 * Get all search terms
 * @return {Array.<bluemind.model.search.SearchTerm>} All search term.
 */
bluemind.model.search.SearchTermFactory.prototype.getAll = function() {
  return this.map_.getValues();
};



goog.exportSymbol('bluemind.model.search.SearchTermFactory', bluemind.model.search.SearchTermFactory);
goog.exportProperty(bluemind.model.search.SearchTermFactory, 'getInstance', bluemind.model.search.SearchTermFactory.getInstance);
goog.exportProperty(bluemind.model.search.SearchTermFactory.prototype, 'store', bluemind.model.search.SearchTermFactory.prototype.store);

