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
 * Provides a convenient API for data caching with expiration.
 */

goog.provide('bluemind.cache.Cache');

goog.require('relief.cache.Cache');


/**
 * Provides a convenient API for data caching with expiration.
 * Use goog.storage API instead of a Map.
 * TODO: The byValue feature is not overriden. It could be usefull (or not).
 * @param {number=} maxSize Only used when limitCacheSize is set to true.
 *
 * @constructor
 * @extends {relief.cache.Cache}
 */
bluemind.cache.Cache = function(maxSize) {
  goog.base(this, maxSize);
};

goog.inherits(bluemind.cache.Cache, relief.cache.Cache);

/** @override */
bluemind.cache.Cache.prototype.useNamespace = function(ns) {
};

/** @override */
bluemind.cache.Cache.prototype.get = function(key, value) {
  return this.storage_.get(key) || value;
};

/** @override */
bluemind.cache.Cache.prototype.nsGet = function(ns, key, value) {
};

/** @override */
bluemind.cache.Cache.prototype.set = function(key, value, opt_expire, opt_policy) {
};

/** @override */
bluemind.cache.Cache.prototype.nsSet = function(ns, key, value, opt_expire, opt_policy) {
};

/** @override */
bluemind.cache.Cache.prototype.setByValue = function(key, value, opt_expire, opt_policy) {
};

/** @override */
bluemind.cache.Cache.prototype.nsSetByValue = function(ns, key, value, opt_expire, opt_policy) {
};

bluemind.cache.Cache.prototype.remove = function(key) {
};

bluemind.cache.Cache.prototype.nsRemove = function(ns, key) {
};

bluemind.cache.Cache.prototype.containsKey = function(key) {
};

bluemind.cache.Cache.prototype.nsContainsKey = function(ns, key) {
};

bluemind.cache.Cache.prototype.containsValue = function(val) {
};

bluemind.cache.Cache.prototype.nsContainsValue = function(ns, val) {
};

bluemind.cache.Cache.prototype.increment = function(key, delta, initialValue,
                                                  decrementBelowZero) {


};


bluemind.cache.Cache.prototype.nsIncrement =
    function(ns, key, delta, initialValue, decrementBelowZero) {

  return this.increment(this.getNamespacedKey_(ns, key), delta, initialValue,
                        decrementBelowZero);
};


bluemind.cache.Cache.clone_ = function(obj) {
  return goog.object.unsafeClone(obj);
};
