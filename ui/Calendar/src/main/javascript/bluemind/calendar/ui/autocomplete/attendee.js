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
 * @fileoverview Factory class to create an autocomplete that will match
 * from an array of attendees.
 *
 */

goog.provide('bluemind.calendar.ui.autocomplete.Attendee');

goog.require('bluemind.calendar.ui.autocomplete.AttendeeMatcher');
goog.require('goog.ui.ac');
goog.require('goog.ui.ac.InputHandler');
goog.require('goog.ui.ac.Renderer');



/**
 * Factory class for building a basic autocomplete widget that autocompletes
 * an inputbox or text area from an attendee array.
 * @param {Array} data Attendees array.
 * @param {Element} input Input element or text area.
 * @param {boolean=} opt_multi Whether to allow multiple entries separated with
 * semi-colons or commas.
 * @param {boolean=} opt_useSimilar use similar matches. e.g. "gost" => "ghost".
 * @constructor
 * @extends {goog.ui.ac}
 */
bluemind.calendar.ui.autocomplete.Attendee =
  function(data, input, opt_multi, opt_useSimilar) {
  var matcher = new bluemind.calendar.ui.autocomplete.
    AttendeeMatcher(data, !opt_useSimilar);
  var renderer = new goog.ui.ac.Renderer();
  var inputhandler =
    new goog.ui.ac.InputHandler(null, null, !!opt_multi);

  goog.base(this, matcher, renderer, inputhandler);

  inputhandler.attachAutoComplete(this);
  inputhandler.attachInputs(input);
};
goog.inherits(bluemind.calendar.ui.autocomplete.Attendee,
  goog.ui.ac.AutoComplete);

