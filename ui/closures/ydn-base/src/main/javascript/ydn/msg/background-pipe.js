/**
 * @fileoverview Channel provider for background iframe.
 */


goog.provide('ydn.msg.BackgroundPipe');
goog.require('ydn.msg.Pipe');



/**
 * Channel provider for background iframe.
 * @param {string} name
 * @param {string} fn worker file name;
 * @constructor
 * @extends {ydn.msg.Pipe}
 */
ydn.msg.BackgroundPipe = function(name, fn) {
  ydn.msg.BackgroundPipe.base(this, 'constructor', name);
  this.worker_ = null;
  /**
   * @type {string}
   * @private
   */
  this.fn_ = fn;
};
goog.inherits(ydn.msg.BackgroundPipe, ydn.msg.Pipe);


/**
 * @type {Window}
 * @private
 */
ydn.msg.BackgroundPipe.prototype.worker_ = null;


/**
 * @return {Window}
 * @protected
 */
ydn.msg.BackgroundPipe.prototype.getWorker = function() {
  if (!this.worker_) {
    var iframe = document.createElement("IFRAME");
    iframe.setAttribute("src", this.fn_);
    iframe.style.width = '1px';
    iframe.style.height = '1px';
    iframe.style.position = 'absolute';
    iframe.style.marginTop = '-99px';
    iframe.id = 'yathit-background';
    document.body.appendChild(iframe);
    this.worker_ = iframe.contentWindow;

    var origin = window.location.origin;
    window.onmessage = (function(ev) {
      if (ydn.msg.Pipe.DEBUG) {
        window.console.log(ev);
      }
      if (ev.origin == origin) {
        this.defaultListener(ev.data);
      } else if (goog.DEBUG) {
        window.console.error(ev);
      }
    }).bind(this);
    window.onerror = function(e) {
      goog.global.console.log(e);
      goog.global.console.error(e.message);
    }
  }
  return this.worker_;
};


/**
 * @override
 */
ydn.msg.BackgroundPipe.prototype.postMessage = function(msg) {
  if (ydn.msg.Pipe.DEBUG) {
    window.console.log(msg);
  }
  var origin = window.location.origin;
  this.getWorker().postMessage(msg, origin);
};
