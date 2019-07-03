/**
 * @fileoverview A link with display its state.
 */


goog.provide('ydn.ui.Reportable');
goog.require('goog.events.EventTarget');
goog.require('ydn.msg');



/**
 * A link with display its state.
 * @constructor
 * @extends {goog.events.EventTarget}
 * @struct
 */
ydn.ui.Reportable = function() {
  goog.base(this);
  /**
   * @type {goog.events.EventHandler}
   * @private
   */
  this.handler_ = null;
  /**
   * @type {Element}
   * @private
   */
  this.el_ = null;
};
goog.inherits(ydn.ui.Reportable, goog.events.EventTarget);


/**
 * @const
 * @type {string}
 */
ydn.ui.Reportable.CSS_CLASS = 'reportable-link';


/**
 * @const
 * @type {string}
 */
ydn.ui.Reportable.CSS_CLASS_ERROR = 'error';


/**
 * @const
 * @type {string}
 */
ydn.ui.Reportable.HREF_ERROR = '#error';


/**
 * @inheritDoc
 */
ydn.ui.Reportable.prototype.disposeInternal = function() {
  if (this.handler_) {
    this.handler_.dispose();
    this.handler_ = null;
    this.el_ = null;
  }
};


/**
 * @param {Element} el
 */
ydn.ui.Reportable.prototype.attachElement = function(el) {
  goog.asserts.assert(!this.el_, 'already attached?');
  this.el_ = el;
  this.handler_ = new goog.events.EventHandler(this);
  this.handler_.listen(el, 'click', this.handleClick_, true);
};


/**
 * @return {Element}
 */
ydn.ui.Reportable.prototype.getElement = function() {
  return this.el_;
};


/**
 * @param {Event} e
 * @private
 */
ydn.ui.Reportable.prototype.handleClick_ = function(e) {
  var a = this.getElement();
  if (this.getState() == ydn.ui.Reportable.State.ERROR) {
    e.preventDefault();
    // todo: really send data to server
    a.textContent = 'Error reported, thanks.';
    a.removeAttribute('href');
    a.removeAttribute('title');
    a.classList.remove(ydn.ui.Reportable.CSS_CLASS_ERROR);
  } else {
    this.dispatchEvent(e);
  }
};


/**
 * Link state.
 * @enum {string}
 */
ydn.ui.Reportable.State = {
  NORMAL: 'n',
  ERROR: 'e',
  NULL: 'v'
};


/**
 * Get state
 * @return {ydn.ui.Reportable.State}
 */
ydn.ui.Reportable.prototype.getState = function() {
  var a = this.getElement();
  if (a.classList.contains(ydn.ui.Reportable.CSS_CLASS_ERROR)) {
    return ydn.ui.Reportable.State.ERROR;
  } else if (a.href) {
    return ydn.ui.Reportable.State.NORMAL;
  } else {
    return ydn.ui.Reportable.State.NULL;
  }
};


/**
 * Set link.
 * @param {string?} label if null, this will hide.
 * @param {string=} href if no href, the status is set to NULL.
 * @param {string=} opt_title
 */
ydn.ui.Reportable.prototype.setLink = function(label, href, opt_title) {
  var a = this.getElement();
  a.classList.remove(ydn.ui.Reportable.CSS_CLASS_ERROR);
  if (href) {
    a.href = href;
  } else {
    a.removeAttribute('href');
  }
  if (label) {
    a.textContent = label;
    goog.style.setElementShown(a, true);
  } else {
    goog.style.setElementShown(a, false);
  }

  if (opt_title) {
    a.setAttribute('title', opt_title);
  } else {
    a.removeAttribute('title');
  }
};


/**
 * Set link.
 * @param {string|Error} msg error message.
 * @param {Object=} opt_payload Optional payload to send bug report.
 */
ydn.ui.Reportable.prototype.setError = function(msg, opt_payload) {
  var a = this.getElement();
  a.classList.add(ydn.ui.Reportable.CSS_CLASS_ERROR);
  a.href = ydn.ui.Reportable.HREF_ERROR;
  a.textContent = 'error';
  if (!!msg && msg instanceof Error) {
    msg = msg.name + ': ' + msg.message;
  }
  msg = msg ? ' for ' + msg : '';
  a.setAttribute('title', 'Click to report error' + msg);
  if (opt_payload) {
    ydn.msg.getChannel().send(ydn.crm.ch.Req.LOG, opt_payload);
  }
};
