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
 * @fileoverview Term that can be search on. A search term is composed a 
 * technical name and a display name.
 **/

goog.provide('bluemind.model.search.SearchTerm');

goog.require('goog.string');

/**
 * Class representing an search term.
 *
 * @param {string} term Search term.
 * @param {string=} opt_dname Display name of the term.
 * @param {string=} opt_format Long description, a place holder should be inserted to add the pattern search for. 
 * @param {boolean=} opt_expert True if this term enable expert mode.
 * @constructor
 */
bluemind.model.search.SearchTerm = function(term, opt_dname, opt_format, opt_expert) {
  this.term_ = term;
  this.dname_ = opt_dname || null;
  this.format_ = opt_format || bluemind.model.search.SearchTerm.DEFAULT_FORMAT;
  this.mustEscape_ = !opt_expert;
};


/**
 * Default formating
 * @type {string}
 * @const
 */
bluemind.model.search.SearchTerm.DEFAULT_FORMAT = '<strong>%0</strong> <i>contains</i> "<span>%1</span>"';

/**
 * Technical name
 * @type {string}
 * @private
 */
bluemind.model.search.SearchTerm.prototype.term_;

/**
 * Display name
 * @type {?string}
 * @private
 */
bluemind.model.search.SearchTerm.prototype.dname_;

/**
 * Description
 * @type {string}
 * @private
 */
bluemind.model.search.SearchTerm.prototype.format_;


/**
 * True if the value associated with this term in the query part must be
 * escaped.
 * @type {boolean}
 * @private
 */
bluemind.model.search.SearchTerm.prototype.mustEscape_;

/**
 * Return term technical string
 * @return {string} term technical string
 */
bluemind.model.search.SearchTerm.prototype.getTerm = function() {
  return this.term_;
};

/**
 * Return term display name
 * @return {string} term display name
 */
bluemind.model.search.SearchTerm.prototype.getDisplayname = function() {
  return this.dname_ || this.term_;
};


/**
 * Return Term format 
 * @return {string} term format string
 */
bluemind.model.search.SearchTerm.prototype.format = function() {
  var string = this.format_.replace('%0', this.dname_ || this.term_);
  for (var i = 0; i < arguments.length; i++) {
    string = string.replace('%' + (i+1), arguments[i]);
  }
  return string;
};


/**
 * Return term technical string
 * @param {string} term technical string
 */
bluemind.model.search.SearchTerm.prototype.setTerm = function(term) {
  this.term_ = term;
};

/**
 * Return term display name
 * @param {string} dname term display name
 */
bluemind.model.search.SearchTerm.prototype.setDisplayname = function(dname) {
  this.dname_ = dname;
};

/**
 * Return true if the term is used in the query part
 * @param {string} queryPart query to compar with.
 */
bluemind.model.search.SearchTerm.prototype.match = function(queryPart) {
  return goog.string.caseInsensitiveStartsWith(queryPart, this.term_);
};

/**
 * Return true if the value associated with this term must be escaped
 * @return {boolean} Term must beescaped.
 */
bluemind.model.search.SearchTerm.prototype.mustEscape = function() {
  return this.mustEscape_;
};

/**
 * Get a search term from query part if possible.
 * @param {string} string query to compar with.
 * @return {bluemind.model.search.SearchTerm} Search term if possible
 */
bluemind.model.search.SearchTerm.fromString = function(string) {
  var s = bluemind.model.search.QueryPart.SEPARATOR;
  var count = goog.string.countOf(string, s);
  switch (count) {
    case 0:
      return null;
    case 1:
      var parts = string.split(s);
      return bluemind.model.search.SearchTermFactory.getInstance().find(parts[0]);
    default:
      var i = string.indexOf(':');
      var pattern = string.substring(0, i);
      return bluemind.model.search.SearchTermFactory.getInstance().find(pattern);
  }
};

goog.exportSymbol('bluemind.model.search.SearchTerm', bluemind.model.search.SearchTerm);

goog.exportProperty(bluemind.model.search.SearchTerm, 'bluemind.model.search.SearchTerm.DEFAULT_FORMAT', bluemind.model.search.SearchTerm.DEFAULT_FORMAT);
