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
*/


/**
 * @fileoverview Provides a mechanism to bufferize the data.
 *
 */

goog.provide('bluemind.storage.mechanism.CacheMechanism');

goog.require('goog.async.Delay');
goog.require('goog.json');
goog.require('goog.json.Serializer');
goog.require('goog.object');
goog.require('goog.storage.mechanism.IterableMechanism');
goog.require('goog.structs.Map');



/**
 * Provides a storage mechanism that load data from a storage into memory.
 *
 * @param {!goog.storage.mechanism.Mechanism} mechanism The underlying
 *     storage mechanism.
 * @param {!string} prefix Prefix for storing data into the storage.
 * @constructor
 * @extends {goog.storage.mechanism.IterableMechanism}
 */
bluemind.storage.mechanism.CacheMechanism = function(mechanism, prefix) {
  goog.base(this);
  this.prefix_ = prefix;
  this.mechanism_ = mechanism;
  this.storage_ = new goog.structs.Map();
  this.serializer_ = new goog.json.Serializer();
  this.writeDelay_ = new goog.async.Delay(this.write_, 10, this);
  this.readDelay_ = new goog.async.Delay(this.read_, 5000, this);
  this.read_();

};
goog.inherits(bluemind.storage.mechanism.CacheMechanism,
              goog.storage.mechanism.IterableMechanism);


/**
 * The mechanism used to persist key-value pairs.
 *
 * @type {goog.storage.mechanism.Mechanism}
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.prototype.mechanism_;


/**
 * Prefix for storing data into the local storage.
 *
 * @type {string}
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.prototype.prefix_;

/**
 * The JSON serializer used to serialize values.
 *
 * @type {goog.json.Serializer}
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.prototype.serializer_;

/**
 * The mechanism used to cache key-value pairs.
 *
 * @type {goog.structs.Map}
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.prototype.storage_;

/**
 * Current cache version
 * @type {number | null}
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.version_ = null;

/**
 * Timer to prevent to many refresh at the same time
 * @type {goog.async.Delay}
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.writeDelay_;

/**
 * Timer to prevent to many refresh at the same time
 * @type {goog.async.Delay}
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.readDelay_;

/**
 * An integer value representing the number of milliseconds between midnight,
 * January 1, 1970 and the last cache update
 * @type {number}
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.lastupdate_ = 0;

/** @override */
bluemind.storage.mechanism.CacheMechanism.prototype.set = function(key, value) {
  this.readDelay_.fireIfActive();
  value = /** @type {string} */ (goog.object.unsafeClone(value));
  this.storage_.set(key, value);
  this.writeDelay_.start();
};


/** @override */
bluemind.storage.mechanism.CacheMechanism.prototype.get = function(key) {
  var value = /** @type {string} */ (goog.object.unsafeClone(this.storage_.get(key)));
  return value;

};


/** @override */
bluemind.storage.mechanism.CacheMechanism.prototype.remove = function(key) {
  this.readDelay_.fireIfActive();
  this.storage_.remove(key);
  this.writeDelay_.start();
};


/** @override */
bluemind.storage.mechanism.CacheMechanism.prototype.getCount = function() {
  return this.storage_.getCount();
};


/** @override */
bluemind.storage.mechanism.CacheMechanism.prototype.__iterator__ =
  function(opt_keys) {
  return this.storage_.__iterator__(opt_keys);
};

/** @override */
bluemind.storage.mechanism.CacheMechanism.prototype.clear = function() {
  this.mechanism_.clear();
  this.storage_.clear();
};

/**
 * Check if the cache is up to date.
 * @return {boolean} Return if the cache is up to date.
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.prototype.upToDate_ = function() {
  if ((goog.now() - this.lastupdate_) < 500) {
    return true;
  }
  if (!this.storage_ || this.version_ == null) {
    return false;
  }
  var version = this.mechanism_.get(this.getVersionKey_()) || 0;
  if (this.version_ < version) {
    return false;
  }
  return true;
};

/**
 * Update cache with stored data.
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.prototype.read_ = function() {
  if (!this.upToDate_()) {
    this.storage_.clear();
    var json = this.mechanism_.get(this.getDataKey_());
    var data = goog.json.parse(json);
    var version = this.mechanism_.get(this.getVersionKey_()) || 0;
    this.storage_.addAll(data);
    this.version_ = version;
    this.lastupdate_ = goog.now;
  }
  this.readDelay_.start();
};


/**
 * Update data with cached data.
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.prototype.write_ = function() {
  var json = this.serializer_.serialize(this.storage_.toObject());
  this.version_++;
  this.mechanism_.set(this.getVersionKey_(), this.version_);
  this.mechanism_.set(this.getDataKey_(), json);
};

/**
 * Get data storage key
 * @return {string} The key for data in the storage.
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.prototype.getDataKey_ = function() {
  return this.prefix_ + '-data';
};

/**
 * Get cache version key
 * @return {string} The key for cache version in the storage.
 * @private
 */
bluemind.storage.mechanism.CacheMechanism.prototype.getVersionKey_ = function() {
  return this.prefix_ + '-version';
};
