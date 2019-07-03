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
 * @fileoverview Throttle object with a method that can take different argument
 *               at each calls.
 * @see ../demos/timers.html
 */

goog.provide('net.bluemind.async.Throttle');

goog.require('goog.async.Deferred');
goog.require('goog.Disposable');
goog.require('goog.Timer');

/**
 * Throttle will perform an action that is passed in no more than once per
 * interval (specified in milliseconds). If it gets multiple signals to perform
 * the action while it is waiting, it will only perform the action once at the
 * end of the interval.
 * <p>
 * This Throttle allow to call fire with arguments, the action will be called
 * with the given arguments. If the action is waiting for the interval, the
 * arguments used for the action will be the last arguments past to fire before
 * the timer end.
 * 
 * @param {function(this: T)} listener Function to callback when the action is
 *          triggered.
 * @param {number} interval Interval over which to throttle. The listener can
 *          only be called once per interval.
 * @param {T=} opt_handler Object in whose scope to call the listener.
 * @constructor
 * @extends {goog.Disposable}
 * @final
 * @template T
 */
net.bluemind.async.Throttle = function(listener, interval, opt_handler) {
  goog.Disposable.call(this);

  this.listener_ = listener;

  this.interval_ = interval;

  this.handler_ = opt_handler;

  this.callback_ = goog.bind(this.onTimer_, this);

  this.action_ = goog.async.Deferred.canceled();
};
goog.inherits(net.bluemind.async.Throttle, goog.Disposable);

/**
 * Function to callback
 * 
 * @type {function(this: T)}
 * @private
 */
net.bluemind.async.Throttle.listener_;

/**
 * pending action
 * 
 * @type {goog.async.Deferred}
 * @private
 */
net.bluemind.async.Throttle.action_;

/**
 * Interval for the throttle time
 * 
 * @type {number}
 * @private
 */
net.bluemind.async.Throttle.prototype.interval_;

/**
 * Cached callback function invoked after the throttle timeout completes
 * 
 * @type {Function}
 * @private
 */
net.bluemind.async.Throttle.prototype.callback_;

/**
 * "this" context for the listener
 * 
 * @type {Object|undefined}
 * @private
 */
net.bluemind.async.Throttle.prototype.handler_;

/**
 * Indicates the count of nested pauses currently in effect on the throttle.
 * When this count is not zero, fired actions will be postponed until the
 * throttle is resumed enough times to drop the pause count to zero.
 * 
 * @type {number}
 * @private
 */
net.bluemind.async.Throttle.prototype.pauseCount_ = 0;

/**
 * Timer for scheduling the next callback
 * 
 * @type {?number}
 * @private
 */
net.bluemind.async.Throttle.prototype.timer_ = null;

/**
 * Notifies the throttle that the action has happened. It will throttle the call
 * so that the callback is not called too often according to the interval
 * parameter passed to the constructor.
 * 
 * @param {...*} var_args Argument to pass to the action
 * @return {goog.async.Deferred} Action execution process
 */
net.bluemind.async.Throttle.prototype.fire = function(var_args) {
  if (!this.action_.hasFired()) {
    this.action_.cancel();
  }
  var fn = goog.bind(this.listener_, this.handler_, var_args);
  this.action_ = new goog.async.Deferred().addCallback(fn);

  if (!this.timer_ && !this.pauseCount_) {
    this.doAction_();
  }
  return this.action_;
};

/**
 * Cancels any pending action callback. The throttle can be restarted by calling
 * {@link #fire}.
 */
net.bluemind.async.Throttle.prototype.stop = function() {
  if (this.timer_) {
    goog.Timer.clear(this.timer_);
    this.timer_ = null;
    if (!!this.action_.hasFired()) {
      this.action_.cancel();
    }
  }
};

/**
 * Pauses the throttle. All pending and future action callbacks will be delayed
 * until the throttle is resumed. Pauses can be nested.
 */
net.bluemind.async.Throttle.prototype.pause = function() {
  this.pauseCount_++;
};

/**
 * Resumes the throttle. If doing so drops the pausing count to zero, pending
 * action callbacks will be executed as soon as possible, but still no sooner
 * than an interval's delay after the previous call. Future action callbacks
 * will be executed as normal.
 */
net.bluemind.async.Throttle.prototype.resume = function() {
  this.pauseCount_--;
  if (!this.pauseCount_ && !this.action_.hasFired() && !this.timer_) {
    this.doAction_();
  }
};

/** @override */
net.bluemind.async.Throttle.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.stop();
};

/**
 * Handler for the timer to fire the throttle
 * 
 * @private
 */
net.bluemind.async.Throttle.prototype.onTimer_ = function() {
  this.timer_ = null;

  if (!this.action_.hasFired() && !this.pauseCount_) {
    this.doAction_();
  }
};

/**
 * Calls the callback
 * 
 * @private
 */
net.bluemind.async.Throttle.prototype.doAction_ = function() {
  this.timer_ = goog.Timer.callOnce(this.callback_, this.interval_);
  this.action_.callback();
};