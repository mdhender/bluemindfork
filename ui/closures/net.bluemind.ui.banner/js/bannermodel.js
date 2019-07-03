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
 * @fileoverview
 * 
 * Bluemind application banner model
 */

goog.provide('bluemind.ui.BannerModel');
goog.require('bm.extensions.ExtensionsManager');

/**
 * 
 * @constructor
 */
bluemind.ui.BannerModel = function() {
}

bluemind.ui.BannerModel.orderItem_ = function(items) {

  goog.array.sort(items, function(a, b) {
    var aIndex = 0;
    var bIndex = 0;
    if (a["priority"] != null) {
      aIndex = goog.string.parseInt(a["priority"]);
    }

    if (b["priority"] != null) {
      bIndex = goog.string.parseInt(b["priority"]);
    }

    return bIndex - aIndex;
  });

}

bluemind.ui.BannerModel.loadEntries = function(userAuth) {
  var x = bm.extensions.ExtensionsManager.getInstance();
  x = x.getExtensionPoint('net.bluemind.ui.commons.banner');
  var entries = goog.array.map(x.getExtensions(), function(ext) {
    var e = ext.data('banner-entry');
    e.pendingActions = 0;
    return e;
  });
  bluemind.ui.BannerModel.orderItem_(entries);

  return goog.array.filter(entries, function(entry) {
    if (entry['role']) {
      if (!goog.array.contains(userAuth['roles'], entry['role'])) {
        return false;
      }
    }
    return true;
  });
}

bluemind.ui.BannerModel.loadWidgets = function(userAuth) {
  var w = bm.extensions.ExtensionsManager.getInstance();
  w = w.getExtensionPoint('net.bluemind.ui.commons.banner.widget');

  var exts = goog.array.filter(w.getExtensions(), function(ext) {
    var entry = ext.data('widget-entry');
    if (entry['role']) {
      if (!goog.array.contains(userAuth['roles'], entry['role'])) {
        console.log("widget " + ext.data('bundle') + "  not avalaible because NOT HAVE " + entry['role']);
        return false;
      }
    }
    return true;
  });

  var widgets_ = goog.array.map(exts, function(ext) {
    var entry = ext.data('widget-entry');
    entry.extension = ext;
    return entry;
  });
  bluemind.ui.BannerModel.orderItem_(widgets_);
  return widgets_;
}

bluemind.ui.BannerModel.buildModel = function(user, selectedEntry) {
  var model = {
    "entries" : bluemind.ui.BannerModel.loadEntries(user),
    "selectedEntry" : selectedEntry,
    "widgets" : bluemind.ui.BannerModel.loadWidgets(user),
    "user" : user
  };
  return model;
}
