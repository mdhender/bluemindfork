/**
 * @fileoverview A link with display its state.
 */


goog.provide('ydn.ui.ReportableLink');
goog.require('goog.ui.Component');
goog.require('ydn.ui.Reportable');



/**
 * A link with display its state.
 * @param {goog.dom.DomHelper=} opt_dom
 * @constructor
 * @extends {goog.ui.Component}
 * @struct
 */
ydn.ui.ReportableLink = function(opt_dom) {
  goog.base(this, opt_dom);
};
goog.inherits(ydn.ui.ReportableLink, goog.ui.Component);


/**
 * @const
 * @type {string}
 */
ydn.ui.ReportableLink.CSS_CLASS = 'reportable-link';


/**
 * @const
 * @type {string}
 */
ydn.ui.ReportableLink.CSS_CLASS_ERROR = 'error';


/**
 * @const
 * @type {string}
 */
ydn.ui.ReportableLink.HREF_ERROR = '#error';


/**
 * @inheritDoc
 */
ydn.ui.ReportableLink.prototype.createDom = function() {
  var el = this.getDomHelper().createDom('a');
  this.decorateInternal(el);
};


/**
 * @inheritDoc
 */
ydn.ui.ReportableLink.prototype.canDecorate = function(e) {
  return e.tagName == 'A';
};


/**
 * @inheritDoc
 */
ydn.ui.ReportableLink.prototype.decorateInternal = function(element) {
  goog.base(this, 'decorateInternal', element);
  goog.dom.classes.add(element, ydn.ui.ReportableLink.CSS_CLASS);
};


/**
 * @inheritDoc
 */
ydn.ui.ReportableLink.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getElement(), 'click', this.handleClick_, true);
};


/**
 * @param {Event} e
 * @private
 */
ydn.ui.ReportableLink.prototype.handleClick_ = function(e) {
  e.preventDefault();
  var a = this.getElement();
  if (this.getState() == ydn.ui.Reportable.State.ERROR) {
    a.textContent = 'Error reported, thanks.';
    a.removeAttribute('href');
    a.classList.remove(ydn.ui.ReportableLink.CSS_CLASS_ERROR);
  } else {
    this.dispatchEvent(e);
  }
};


/**
 * Get state
 * @return {ydn.ui.Reportable.State}
 */
ydn.ui.ReportableLink.prototype.getState = function() {
  var a = this.getContentElement();
  if (a.classList.contains(ydn.ui.ReportableLink.CSS_CLASS_ERROR)) {
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
ydn.ui.ReportableLink.prototype.setLink = function(label, href, opt_title) {
  var a = this.getElement();
  a.classList.remove(ydn.ui.ReportableLink.CSS_CLASS_ERROR);
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
 * @param {Object=} opt_payload Optional payload to send bug report.
 * @param {string=} opt_msg error message.
 */
ydn.ui.ReportableLink.prototype.setError = function(opt_payload, opt_msg) {
  var a = this.getElement().firstElementChild;
  a.classList.add(ydn.ui.ReportableLink.CSS_CLASS_ERROR);
  a.href = ydn.ui.ReportableLink.HREF_ERROR;
  a.textContent = 'error';
  var msg = opt_msg ? ' for ' + opt_msg : '';
  a.setAttribute('title', 'Click to report error' + msg);
  if (opt_payload) {
    ydn.msg.getChannel().send(ydn.crm.ch.Req.LOG_BUG, opt_payload);
  }
};
