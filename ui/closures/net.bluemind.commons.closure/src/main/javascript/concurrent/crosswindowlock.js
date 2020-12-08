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
 * @fileoverview Cross window locking.
 */

goog.provide('net.bluemind.concurrent.CrossWindowLock');

goog.require('bluemind.storage.StorageHelper');
goog.require('goog.Disposable');
/**
 * A lock is a tool for controlling access to a shared resource by
 * multiple concurrent process.
 * This lock is mean to be shared between all instances of all applications
 * from a same domain, opened in one browser.
 * 
 * @param {string} name Lock name.
 * @constructor
 * @extends {goog.Disposable}
 */
net.bluemind.concurrent.CrossWindowLock = function(name) {
  this.name_ = name;
  this.uid_ = goog.now() + '-' + goog.getUid(this);
  this.key_ = this.name_ +  net.bluemind.concurrent.CrossWindowLock.SUFFIX;
  this.storage_ = bluemind.storage.StorageHelper.getExpiringStorage();
};
goog.inherits(net.bluemind.concurrent.CrossWindowLock, goog.Disposable);

/**
 * Lock timeout if not refresh or unlock.
 * Should be as small as possible. Javascript timer precision is the key
 * problem.
 * @type {number}
 * @const
 * @private
 */
net.bluemind.concurrent.CrossWindowLock.TIMEOUT = 300;

/**
 * Prefix to store lock into the local storage
 * @type {string}
 * @const
 * @private
 */
net.bluemind.concurrent.CrossWindowLock.SUFFIX = '.lock';

/**
 * Lock name, used to share the lock between all applications
 * @type {string}
 * @private
 */
net.bluemind.concurrent.CrossWindowLock.prototype.name_;

/**
 * Lock object uid, used to identify the lock instance owner of
 * the database lock
 * @type {string}
 * @private
 */
net.bluemind.concurrent.CrossWindowLock.prototype.uid_;

/**
 * Handler object
 * @type {goog.storage.ExpiringStorage}
 * @private
 */
net.bluemind.concurrent.CrossWindowLock.prototype.storage_;

/**
 * Acquires the lock.
 *
 * If the lock is not available then the current thread lies dormant
 * until the lock has been acquired.
 * //TODO: Better than trylock for our mean but it mean that the lock must 
 * run asynchronously, and so, must execute the critical section.
 */
//net.bluemind.concurrent.CrossWindowLock.prototype.lock = function() {
//
//};

/**
 * Acquires the lock only if it is free at the time of invocation.
 *
 * Acquires the lock if it is available and returns immediately
 * with the value {@code true}.
 * If the lock is not available then this method will return
 * immediately with the value {@code false}.
 *
 * @param {number=} opt_timeout Optional timeout for lock validity.
 * @return {boolean} Is the lock acquired.
 */
net.bluemind.concurrent.CrossWindowLock.prototype.tryLock =
  function(opt_timeout) {
  var lock = this.storage_.get(this.key_);
  if (! lock  || lock == this.uid_) {
    var t = opt_timeout || net.bluemind.concurrent.CrossWindowLock.TIMEOUT;
    t += goog.now();
    this.storage_.set(this.key_, this.uid_, t);
    return true;
  }
  return false;
};


/**
 * Releases the lock.
 */
net.bluemind.concurrent.CrossWindowLock.prototype.unlock = function() {
  var lock = this.storage_.get(this.key_);
  if (lock == this.uid_) {
    this.storage_.remove(this.key_);
  }
};

/** @override */
net.bluemind.concurrent.CrossWindowLock.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.unlock();  
};
