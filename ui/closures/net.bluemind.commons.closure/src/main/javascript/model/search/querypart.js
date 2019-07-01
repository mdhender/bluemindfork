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
 * @fileoverview represent a part of a search query.
 * A query part is composed of a search term that define the scope of the query
 * and a value.
 **/

goog.provide('bluemind.model.search.QueryPart');

goog.require('bluemind.model.search.SearchTerm');
goog.require('bluemind.model.search.SearchTermFactory');
goog.require('goog.string');

/**
 * Class representing an query part
 *
 * @param {string} value Value to search on.
 * @param {bluemind.model.search.SearchTerm=} opt_term Search term.
 * @constructor
 */
bluemind.model.search.QueryPart = function(value, opt_term) {
  this.value_ = value;
  this.term_ = opt_term || bluemind.model.search.SearchTermFactory.getInstance().getDefault();
};

/**
 * Reserved chars.
 * @type {RegExp}
 * @const
 */
bluemind.model.search.QueryPart.SPECIAL = /(\\|\+|-|&&|\|\||!|\(|\)|\{|\}|\[|\]\^|~|\?|:|\/)/g



/**
 * Technical char.
 * @type {string}
 * @const
 */
bluemind.model.search.QueryPart.VERY_SPECIAL = ';';

/**
 * Standard term/value separator.
 * @type {string}
 * @const
 */
bluemind.model.search.QueryPart.SEPARATOR = ':';


/**
 * Search term 
 * @type {bluemind.model.search.SearchTerm}
 * @private
 */
bluemind.model.search.QueryPart.prototype.term_;

/**
 * Search value
 * @type {string}
 * @private
 */
bluemind.model.search.QueryPart.prototype.value_;


/**
 * Return term technical string
 * @param {bluemind.model.search.SearchTerm} term technical string
 */
bluemind.model.search.QueryPart.prototype.setTerm = function(term ) {
  this.term_ = term;
};

/**
 * Return term technical string
 * @return {bluemind.model.search.SearchTerm} term technical string
 */
bluemind.model.search.QueryPart.prototype.getTerm = function() {
  return this.term_;
};

/**
 * Return query part value
 * @return {string} value
 */
bluemind.model.search.QueryPart.prototype.getValue = function() {
  return this.value_;
};


/**
 * Return query part search string form
 * @return {string} value
 */
bluemind.model.search.QueryPart.prototype.toString = function() {
  if (this.value_) {
    var val = goog.string.trim(this.value_.replace(bluemind.model.search.QueryPart.VERY_SPECIAL, ' '));
    if (this.term_.mustEscape()) {
      val = '(' + val.replace(bluemind.model.search.QueryPart.SPECIAL, ' ') + ')';
    }
    return this.term_.getTerm() + bluemind.model.search.QueryPart.SEPARATOR + val + ' ';
  } else {
    return this.term_.getTerm();
  }
};


/**
 * Return a string that will create the same QueryPart when input in "fromQuery" methid
 * @return {string} value
 */
bluemind.model.search.QueryPart.prototype.toValue = function() {
  var string = [];
  if (this.term_ != bluemind.model.search.SearchTermFactory.getInstance().getDefault()) {
    string.push(this.term_.getTerm());
  }
  if (this.value_) {
    string.push(this.value_);
  }
   
  return string.join(bluemind.model.search.QueryPart.SEPARATOR);
};

/**
 * Get a query part a string if possible.
 * @param {string} string query to compare with.
 * @return {bluemind.model.search.QueryPart} Query part
 */
bluemind.model.search.QueryPart.fromString = function(string) {
  var s = bluemind.model.search.QueryPart.SEPARATOR;
  var count = goog.string.countOf(string, s);
  switch (count) {
    case 0:
      return new bluemind.model.search.QueryPart(string);
    case 1:
      var parts = string.split(s);
      var term = bluemind.model.search.SearchTermFactory.getInstance().find(parts[0]);
      term = term || bluemind.model.search.SearchTermFactory.getInstance().get(parts[0]);
      return  new bluemind.model.search.QueryPart(parts[1], term);
    default:
      var i = string.indexOf(':');
      var pattern = string.substring(0, i++);
      var value = string.substring(i).replace(/^\(([^)]*)\)$/, '$1'); 
      var term = bluemind.model.search.SearchTermFactory.getInstance().find(pattern);
      term = term || bluemind.model.search.SearchTermFactory.getInstance().get(pattern);
      return new bluemind.model.search.QueryPart(value, term);
  }
};


/**
 * Build a query from query parts.
 * @param {Array.<bluemind.model.search.QueryPart>} parts query parts to build query from.
 * @return {string} Search query.
 */
bluemind.model.search.QueryPart.buildQuery = function(parts) {
  var query = '';
  for(var i = 0; i < parts.length; i++) {
    var part = parts[i];
    query += part.toString() + ' ';
  }
  return query;
};

goog.exportSymbol('bluemind.model.search.QueryPart', bluemind.model.search.QueryPart);

