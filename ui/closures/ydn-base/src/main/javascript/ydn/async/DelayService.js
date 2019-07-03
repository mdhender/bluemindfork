/**
 * @fileoverview Delayed asynchronous service.
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */



goog.provide('ydn.async.DelayService');
goog.require('goog.async.Deferred');
goog.require('goog.async.Delay');



/**
 * Delayed asynchronous service.
 * @param {function(this: T, *): goog.async.Deferred} provider async service
 * provider.
 * @param {number} ms delay interval in milli seconds. Service will be invoked
 * after this set delay.
 * @param {T} scope scope to invoked on.
 * @constructor
 * @template T
 */
ydn.async.DelayService = function(provider, ms, scope) {

  /**
   * @type {*}
   */
  this.arg_;

  /**
   * @type {function(*): goog.async.Deferred}}
   * @private
   */
  this.provider_ = provider;

  this.scope_ = scope;

  /**
   * Last invocation.
   * @type {number}
   * @private
   */
  this.last_ = NaN;

  /**
   * last invocation result.
   * @type {goog.async.Deferred}
   * @private
   */
  this.df_ = null;

  /**
   * list of listener.
   * @type {Array<goog.async.Deferred>}
   * @private
   */
  this.dfs_ = [];

  /**
   * @type {goog.async.Delay}
   * @private
   */
  this.delay_ = new goog.async.Delay(this.listener_, ms, this);
};


/**
 * @private
 */
ydn.async.DelayService.prototype.listener_ = function() {
  if (this.df_) {
    return;
  }
  this.df_ = this.provider_.call(this.scope_, this.arg_);
  this.df_.addCallbacks(function(x) {
    for (var i = 0; i < this.dfs_.length; i++) {
      this.dfs_[i].callback(x);
    }
    this.dfs_.length = 0;
    this.df_ = null;
  }, function(e) {
    for (var i = 0; i < this.dfs_.length; i++) {
      this.dfs_[i].errback(e);
    }
    this.dfs_.length = 0;
    this.df_ = null;
  }, this);
};


/**
 * Purge all previous listener.
 * Note: this make previous deferred from {@link #invoke} neither invoke
 * success nor error.
 */
ydn.async.DelayService.prototype.purge = function() {
  goog.array.clear(this.dfs_);
};


/**
 * Invoke service with given argument.
 * @param {*} arg argument for service. Only the last invoked argument will
 * be used.
 * @return {!goog.async.Deferred}
 */
ydn.async.DelayService.prototype.invoke = function(arg) {
  this.arg_ = arg;
  this.delay_.start();
  var df = new goog.async.Deferred();
  this.dfs_.push(df);
  return df;
};

