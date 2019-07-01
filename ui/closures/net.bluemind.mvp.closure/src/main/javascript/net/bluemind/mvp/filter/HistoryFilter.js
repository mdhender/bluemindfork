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

/** @fileoverview Parse process date range */

goog.provide("net.bluemind.mvp.filter.HistoryFilter");

goog.require("goog.Promise");
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.date.DateRange");
goog.require("net.bluemind.mvp.Filter");

/**
 * Build an history of visited URI. This filter is necessary to use
 * URLHelper.back(), else it will fallback on history.back.
 * 
 * @param {Array.<string>} prune Parameter to prune before comparing url.
 * @constructor
 * @extends {net.bluemind.mvp.Filter}
 */
net.bluemind.mvp.filter.HistoryFilter = function(prune) {
  goog.base(this);
  this.prune_ = prune || [];
};
goog.inherits(net.bluemind.mvp.filter.HistoryFilter, net.bluemind.mvp.Filter);

/**
 * @type {Array.<string>}
 * @private
 */
net.bluemind.mvp.filter.HistoryFilter.prototype.prune_;

/**
 * Execute last. In fact it should even execute after handler.exit...
 * 
 * @override
 */
net.bluemind.mvp.filter.HistoryFilter.prototype.priority = 100;

/** @override */
net.bluemind.mvp.filter.HistoryFilter.prototype.filter = function(ctx) {
  var history = ctx.session.get('history') || [];
  var uri = ctx.uri.clone();
  uri = this.filterKeys(uri);
  if (history.length > 0) {
    var previous = history[history.length - 1].clone();
    previous = this.filterKeys(previous);
    if (previous.toString() == uri.toString()) {
      return;
    }
  }
  history.push(uri);
  ctx.session.set('history', history);
};

/**
 * Removes all keys that are in the provided list.
 * 
 * @param {goog.Uri} uri Uri to modify
 * @return {!goog.Uri} uri objec.
 */
net.bluemind.mvp.filter.HistoryFilter.prototype.filterKeys = function(uri) {
  var keys = uri.getQueryData().getKeys();
  goog.array.forEach(keys, function(key) {
    if (goog.array.contains(this.prune_, key)) {
      uri.getQueryData().remove(key);
    }
  }, this)
  return uri;
}
