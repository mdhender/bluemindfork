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
 * @fileoverview Class for matching attendees in an array.
 *
 */

goog.provide('bluemind.calendar.ui.autocomplete.AttendeeMatcher');

goog.require('goog.iter');
goog.require('goog.string');
goog.require('goog.ui.ac.ArrayMatcher');

/**
 * Basic class for matching words in an array
 * @constructor
 * @param {Array} rows Dictionary of items to match.  Can be objects if they
 *     have a toString method that returns the value to match against.
 * @param {boolean=} opt_noSimilar if true, do not do similarity matches for the
 *     input token against the dictionary.
 * @extends {goog.ui.ac.ArrayMatcher}
 */
bluemind.calendar.ui.autocomplete.AttendeeMatcher =
  function(rows, opt_noSimilar) {
  goog.base(this, rows, opt_noSimilar);
};
goog.inherits(bluemind.calendar.ui.autocomplete.AttendeeMatcher,
  goog.ui.ac.ArrayMatcher);

/**
 * Matches the token against the start of words in the row.
 * @param {string} token Token to match.
 * @param {number} maxMatches Max number of matches to return.
 * @return {Array} Rows that match.
 */
bluemind.calendar.ui.autocomplete.AttendeeMatcher.prototype.getPrefixMatches =
    function(token, maxMatches) {

  var matches = [];

  if (token != '') {
    var escapedToken = goog.string.regExpEscape(token);
    var matcher = new RegExp('(^|\\W+)' + escapedToken, 'i');

    goog.iter.some(this.rows_, function(row) {
      var string = row['displayName'] + ' ' + row['email'];
      if (string.match(matcher)) {
        matches.push(row);
      }
      return matches.length >= maxMatches;
    });
  }
  return matches;
};


/**
 * Matches the token against similar rows, by calculating "distance" between the
 * terms.
 * @param {string} token Token to match.
 * @param {number} maxMatches Max number of matches to return.
 * @return {Array} The best maxMatches rows.
 */
bluemind.calendar.ui.autocomplete.AttendeeMatcher.prototype.getSimilarRows =
    function(token, maxMatches) {

  var results = [];

  goog.iter.forEach(this.rows_, function(row, index) {
    var str = token.toLowerCase();
    var txt = row['displayName'] + ' ' + row['email'];
    txt = txt.toLowerCase();
    var score = 0;

    if (txt.indexOf(str) != -1) {
      score = parseInt((txt.indexOf(str) / 4).toString(), 10);

    } else {
      var arr = str.split('');

      var lastPos = -1;
      var penalty = 10;

      for (var i = 0, c; c = arr[i]; i++) {
        var pos = txt.indexOf(c);

        if (pos > lastPos) {
          var diff = pos - lastPos - 1;

          if (diff > penalty - 5) {
            diff = penalty - 5;
          }

          score += diff;

          lastPos = pos;
        } else {
          score += penalty;
          penalty += 5;
        }
      }
    }

    if (score < str.length * 6) {
      results.push({
        str: row,
        score: score,
        index: index
      });
    }
  });

  results.sort(function(a, b) {
    var diff = a.score - b.score;
    if (diff != 0) {
      return diff;
    }
    return a.index - b.index;
  });

  var matches = [];
  for (var i = 0; i < maxMatches && i < results.length; i++) {
    matches.push(results[i].str);
  }

  return matches;
};
