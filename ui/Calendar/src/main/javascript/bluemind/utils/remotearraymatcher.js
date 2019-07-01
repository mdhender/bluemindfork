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
 * @fileoverview RemoteArrayMatcher.
 */

goog.provide('bluemind.calendar.utils.RemoteArrayMatcher');

goog.require('goog.ui.ac.RemoteArrayMatcher');

/**
 * @param {string} url Remote url.
 * @param {boolean} opt_noSimilar Use similar match.
 * @constructor
 * @extends {goog.ui.ac.RemoteArrayMatcher}
 */
bluemind.calendar.utils.RemoteArrayMatcher = function(url, opt_noSimilar) {
  goog.base(this, url, opt_noSimilar);
};
goog.inherits(bluemind.calendar.utils.RemoteArrayMatcher,
  goog.ui.ac.RemoteArrayMatcher);

/** @inheritDoc */
bluemind.calendar.utils.RemoteArrayMatcher.prototype.buildUrl = function(uri,
    token, maxMatches, useSimilar, opt_fullString) {
  var url = new goog.Uri(uri);
  return url.toString();
};

/** @inheritDoc */
bluemind.calendar.utils.RemoteArrayMatcher.prototype.requestMatchingRows =
    function(token, maxMatches, matchHandler, opt_fullString) {

  if (!this.shouldRequestMatches(this.url_, token, maxMatches, this.useSimilar_,
      opt_fullString)) {
    return;
  }
  // Set the query params on the URL.
  var url = this.buildUrl(this.url_, token, maxMatches, this.useSimilar_,
      opt_fullString);
  if (!url) {
    // Do nothing if there is no URL.
    return;
  }

  // The callback evals the server response and calls the match handler on
  // the array of matches.
  var callback = goog.bind(this.xhrCallback, this, token, matchHandler);

  // Abort the current request and issue the new one; prevent requests from
  // being queued up by the browser with a slow server
  if (this.xhr_.isActive()) {
    this.xhr_.abort();
  }
  // This ensures if previous XHR is aborted or ends with error, the
  // corresponding success-callbacks are cleared.
  if (this.lastListenerKey_) {
    goog.events.unlistenByKey(this.lastListenerKey_);
  }
  // Listen once ensures successful callback gets cleared by itself.
  this.lastListenerKey_ = goog.events.listenOnce(this.xhr_,
      goog.net.EventType.SUCCESS, callback);

  var content = this.content_ + '&max_matches=10&use_similar=0&token=' + token;
  this.xhr_.send(url, this.method_, content, this.headers_);
};
