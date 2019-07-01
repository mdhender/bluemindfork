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
 * @fileoverview AutoCompleteRemote.
 */

goog.provide('bluemind.calendar.utils.AutoCompleteRemote');

goog.require('bluemind.calendar.utils.RemoteArrayMatcher');
goog.require('goog.ui.ac.Remote');

/**
 * @param {string} url Remote url.
 * @param {Element} input Input element.
 * @param {boolean} opt_multi Is the element multi-select.
 * @param {boolean} opt_useSimilar Use similar match.
 * @constructor
 * @extends {goog.ui.ac.Remote}
 */
bluemind.calendar.utils.AutoCompleteRemote =
  function(url, input, opt_multi, opt_useSimilar) {
  goog.base(this, url, input, opt_multi, opt_useSimilar);
  this.matcher_ = new bluemind.calendar.utils.RemoteArrayMatcher(url,
      !opt_useSimilar);
};
goog.inherits(bluemind.calendar.utils.AutoCompleteRemote,
  goog.ui.ac.Remote);
