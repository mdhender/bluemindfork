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
 * @fileoverview Provides a storage mechanism over the indexed db api.
 *
 */

goog.provide('bluemind.storage.mechanism.IndexedDbMechanism');

goog.require('goog.array');
goog.require('goog.async.Deferred');
goog.require('goog.async.Delay');
goog.require('goog.db');
goog.require('goog.db.IndexedDb');
goog.require('goog.db.Transaction.TransactionMode');
goog.require('goog.object');
goog.require('goog.storage.mechanism.IterableMechanism');
goog.require('goog.structs.Map');
goog.require('goog.userAgent.product');

/**
 * Provides a storage mechanism that get and store item from indexedDB.
 * This mechanism is a hack to make indexedDB look synchronous
 *
 * @param {!String || goog.db.IndexedDb} db Database name.
 * @param {String=} opt_store Optional object store name. Default is 'storage'.
 * @constructor
 * @extends {goog.storage.mechanism.IterableMechanism}
 */
bluemind.storage.mechanism.IndexedDbMechanism = function(db, opt_store) {
  goog.base(this);
  this.ds_ = opt_store || '__bm_storage';
  this.storage_ = new goog.structs.Map();
  this.readDelay_ = new goog.async.Delay(this.update_, 1000, this);
  this.ready_ = new goog.async.Deferred();
  this.init_(db);
}

goog.inherits(bluemind.storage.mechanism.IndexedDbMechanism,
              goog.storage.mechanism.IterableMechanism);


/**
 * Database.
 *
 * @type {goog.db.IndexedDb}
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.db_ = null;

/**
 * DataStore object.
 *
 * @type {String}
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.ds_ = null;

/**
 * The mechanism used to cache key-value pairs.
 *
 * @type {goog.structs.Map}
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.storage_;

/**
 * Current cache version
 * @type {number}
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.version_;

/**
 * Current storage version
 * @type {number}
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.storageVersion_;
/**
 * Crappy hack for asynchronous init
 * @type {goog.async.Deferred}
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.ready_;

/**
 * Timer to prevent to many refresh at the same time
 * @type {goog.async.Delay}
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.readDelay_;

/**
 * An integer value representing the number of milliseconds between midnight,
 * January 1, 1970 and the last cache update
 * @type {number}
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.lastupdate_ = 0;

/** @inheritDoc */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.set = function(key, value) {
  this.readDelay_.fireIfActive();
  value = goog.object.unsafeClone(value);
  this.storage_.set(key, value);
  this.write_();
};


/** @inheritDoc */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.get = function(key) {
  var value = goog.object.unsafeClone(this.storage_.get(key));
  return value;

};


/** @inheritDoc */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.remove = function(key) {
  this.readDelay_.fireIfActive();
  this.storage_.remove(key);
  this.write_();
};


/** @inheritDoc */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.getCount = function() {
  return this.storage_.getCount();
};


/** @inheritDoc */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.__iterator__ =
  function(opt_keys) {
  this.storage_.__iterator__(opt_keys);
};

/** @inheritDoc */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.clear = function() {
  this.storage_.clear();
  this.db_.setVersion(bluemind.version).addCallback(function(e) {
    if (!goog.array.contains(this.db_.getObjectStoreNames(), this.ds_)) {
      this.db_.deleteObjectStore(this.ds_);
    }
    this.db_.createObjectStore(this.ds_);
  }, this);  
};

/**
 * Check if the cache is up to date.
 * @return {boolean} Return if the cache is up to date.
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.upToDate_ = function() {
  if (!this.storage_ || !this.version_) {
    return false;
  }
  if ((goog.now - this.lastupdate_) > 500) {
    return false;
  }
  return true;
};

/**
 * Update cache with stored data.
 * @return {!goog.async.Deferred} The deferred transaction 
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.update_ = function() {
  if (!this.upToDate_()) {
    var transaction = this.db_.createTransaction(this.ds_);
    var store = transaction.objectStore(this.ds_);
    store.get(this.getVersionKey_()).addCallback(function(version) {
      this.storageVersion_ = version;
      if (this.version_ != this.storageVersion_) {
        this.read_();
      }
    }, this);
  }
  this.readDelay_.start();
};


/**
 * Update cache with stored data.
 * @return {!goog.async.Deferred} The deferred transaction 
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.read_ = function() {
  var transaction = this.db_.createTransaction(this.ds_);
  var store = transaction.objectStore(this.ds_);
  var d = store.get(this.getDataKey_()).addCallback(function(data) {
    this.storage_.clear();
    if (!data) data = {};
    this.storage_.addAll(data);
    this.version_ = this.storageVersion_;
    this.lastupdate_ = goog.now;
  }, this);
  return d;
};

/**
 * Update data with cached data.
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.write_ = function() {        
  var transaction = this.db_.createTransaction(this.ds_, 
    goog.db.Transaction.TransactionMode.READ_WRITE);
  var store = transaction.objectStore(this.ds_);
  this.version_++; 
  store.put(this.version_, this.getVersionKey_()).addCallback(function() {
    store.put(this.storage_.toObject(), this.getDataKey_());
  }, this);
};

/**
 * Prefix for storing data into the local storage.
 *
 * @param {!String} db Database name.
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.init_ = function(db) {
  goog.db.openDatabase(db).addCallback(function(db) { 
    this.db_ = db;
    if (this.db_.getVersion() != bluemind.version) {
      this.db_.setVersion(bluemind.version).addCallback(function(e) {
        if (goog.array.contains(this.db_.getObjectStoreNames(), this.ds_)) {
          this.db_.deleteObjectStore(this.ds_);
        }
        this.db_.createObjectStore(this.ds_);
        this.read_().addCallback(function() {
          this.ready_.callback(true);
          this.readDelay_.start();
        }, this).addErrback(function(e) {
          this.ready_.errback(true);
        });        
      }, this).addErrback(function(e) {
        this.ready_.errback(true);
      });        
    } else {
      this.read_().addCallback(function() {
        this.ready_.callback(true);
        this.readDelay_.start();
      }, this).addErrback(function(e) {
        this.ready_.errback(true);
      });        
    }
  }, this).addErrback(function(e) {
    this.ready_.errback(true);
  }, this);
};

/** 
 * Return the db ready trigger
 * @return {goog.async.Deferred} triggered when db is ready 
 */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.getReady = function() {
  return this.ready_;

};
/**
 * Get data storage key
 * @return {string} The key for data in the storage.
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.getDataKey_ = function() {
  return '_bm_storage-data';
};

/**
 * Get cache version key
 * @return {string} The key for cache version in the storage.
 * @private
 */
bluemind.storage.mechanism.IndexedDbMechanism.prototype.getVersionKey_ = function() {
  return '_bm_storage-version';
};
