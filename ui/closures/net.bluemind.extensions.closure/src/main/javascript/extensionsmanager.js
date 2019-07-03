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
 * @fileoverview A list control with a select option.
 * 
 */

goog.provide('bm.extensions.ExtensionsManager');

goog.provide('bm.extensions.ExtensionPoint');

goog.provide('bm.extensions.Extension');

/**
 * Creates a new id generator.
 * 
 * @constructor
 */
bm.extensions.ExtensionsManager = function() {
};

goog.addSingletonGetter(bm.extensions.ExtensionsManager);

/**
 * Get extension point
 * 
 * @param {string} id Extension point id
 */
bm.extensions.ExtensionsManager.prototype.getExtensionPoint = function(id) {
  var data = goog.global['bmExtensions_'][id];
  if (data) {
    return new bm.extensions.ExtensionPoint(data);
  } else {
    return null;
  }
}

/**
 * Creates a new id generator.
 * 
 * @constructor
 */
bm.extensions.ExtensionPoint = function(data) {
  this.data_ = data;
};

/**
 * 
 */
bm.extensions.ExtensionPoint.prototype.getExtensions = function() {
  return goog.array.map(this.data_, function(ext) {
    return new bm.extensions.Extension(ext);
  });
}

/**
 * Creates a new id generator.
 * 
 * @constructor
 */
bm.extensions.Extension = function(data) {
  this.data_ = data;
};

bm.extensions.Extension.prototype.resolve = function() {
  var resolver = goog.Promise.withResolver();

  goog.global['bundleResolve'](this.data_['bundle'], function() {
    resolver.resolve();
  });
  return resolver.promise;
}

/**
 * @param {string} id Method name
 * @param {Array} vargs Method argument
 */
bm.extensions.Extension.prototype.invoke = function(id, vargs) {
  return goog.getObjectByName(goog.getObjectByName(id, this.data_)).apply(null, vargs);
};

/**
 * @param {string} id Property id
 */
bm.extensions.Extension.prototype.data = function(id) {
  return goog.getObjectByName(id, this.data_);
};
