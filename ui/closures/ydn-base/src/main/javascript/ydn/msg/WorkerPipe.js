/**
 * @fileoverview Channel provider for web worker.
 */


goog.provide('ydn.msg.WorkerPipe');
goog.require('ydn.msg.Pipe');



/**
 * Channel provider for web worker.
 * @param {string} name
 * @param {string} fn worker file name;
 * @constructor
 * @extends {ydn.msg.Pipe}
 */
ydn.msg.WorkerPipe = function(name, fn) {
  ydn.msg.WorkerPipe.base(this, 'constructor', name);
  this.worker_ = null;
  /**
   * @type {string}
   * @private
   */
  this.fn_ = fn;
};
goog.inherits(ydn.msg.WorkerPipe, ydn.msg.Pipe);


/**
 * @type {Worker}
 * @private
 */
ydn.msg.WorkerPipe.prototype.worker_ = null;


/**
 * @return {Worker}
 * @protected
 */
ydn.msg.WorkerPipe.prototype.getWorker = function() {
  if (!this.worker_) {
    this.worker_ = new Worker(this.fn_);
    this.worker_.onmessage = (function(ev) {
      this.defaultListener(ev.data);
    }).bind(this);
    this.worker_.onerror = function(e) {
      goog.global.console.log(e);
      goog.global.console.error(e.message);
    }
  }
  return this.worker_;
};


/**
 * @override
 */
ydn.msg.WorkerPipe.prototype.postMessage = function(msg) {
  this.getWorker().postMessage(msg);
};
