/**
 * @fileoverview Model message dialog box using native.
 */


goog.provide('ydn.ui.MessageDialog');
goog.require('goog.events.EventHandler');



/**
 * Model message dialog box using native.
 * <pre>
 *   ydn.ui.MessageDialog.showModal('Dialog demo', 'Here is a message');
 * </pre>
 * @param {string} title
 * @param {string|Node} message message or message content DOM node.
 * @param {Array<ydn.ui.MessageDialog.ButtonDef>} buttons button set.
 * @constructor
 * @struct
 */
ydn.ui.MessageDialog = function(title, message, buttons) {
  /**
   * @protected
   * @type {goog.events.EventHandler}
   */
  this.handler = new goog.events.EventHandler(this);
  var dialog = document.createElement('dialog');
  /**
   * @type {HTMLDialogElement}
   * @protected
   */
  this.dialog = /** @type {HTMLDialogElement} */ (dialog);
  var header = document.createElement('div');
  header.className = 'ydn-header';
  var content = document.createElement('div');
  content.className = 'ydn-content';
  var button_bar = document.createElement('div');
  button_bar.className = 'button-bar';

  var title_el = document.createElement('h3');
  title_el.textContent = title;
  header.appendChild(title_el);

  if (goog.isString(message)) {
    var msg_el = document.createElement('div');
    msg_el.textContent = message;
    content.appendChild(msg_el);
  } else if (message) {
    content.appendChild(message);
  }

  for (var i = 0; i < buttons.length; i++) {
    var btn_def = buttons[i];
    var button = document.createElement('button');
    if (btn_def.isDefault) {
      button.classList.add('default');
      button.setAttribute('autofocus', 'autofocus');
    } else {
      button.classList.add('maia-button-secondary');
    }
    button.classList.add('maia-button');
    button.classList.add('blue');
    button.value = btn_def.name;
    button.textContent = btn_def.label;
    this.handler.listen(button, 'click', this.onButtonClick);
    button_bar.appendChild(button);
  }

  this.handler.listen(this.dialog, 'cancel', this.onCancelClick);

  this.dialog.appendChild(header);
  this.dialog.appendChild(content);
  this.dialog.appendChild(button_bar);
  this.dialog.className = ydn.ui.MessageDialog.CSS_CLASS_NAME;
  document.body.appendChild(this.dialog);

};


/**
 * @enum {string}
 */
ydn.ui.MessageDialog.Button = {
  OK: 'ok',
  CANCEL: 'cancel'
};


/**
 * @typedef {{
 *   name: string,
 *   label: string,
 *   isDefault: (boolean|undefined),
 *   isCancel: (boolean|undefined)
 * }}
 */
ydn.ui.MessageDialog.ButtonDef;


/**
 * @define {string} default class name
 */
ydn.ui.MessageDialog.CSS_CLASS_NAME = 'ydn-crm';


/**
 * @protected
 * @param {goog.events.BrowserEvent} e
 */
ydn.ui.MessageDialog.prototype.onCancelClick = function(e) {
  this.dialog.close('cancel');
};


/**
 * @protected
 * @param {goog.events.BrowserEvent} e
 */
ydn.ui.MessageDialog.prototype.onButtonClick = function(e) {
  this.dialog.close(e.target.value);
};


/**
 * Clean up reference.
 * @protected
 */
ydn.ui.MessageDialog.prototype.dispose = function() {
  if (!this.dialog) {
    return;
  }
  this.handler.dispose();
  this.handler = null;
  this.dialog.onclose = null;
  document.body.removeChild(this.dialog);
  this.dialog = null;
};


/**
 * @return {Element}
 */
ydn.ui.MessageDialog.prototype.getContentElement = function() {
  return this.dialog.querySelector('.' + ydn.crm.ui.CSS_CLASS_CONTENT);
};


/**
 * @param {string=} opt_ok_label optional OK label.
 * @return {Array<ydn.ui.MessageDialog.ButtonDef>}
 */
ydn.ui.MessageDialog.createOKButtonSet = function(opt_ok_label) {
  return [{
    name: ydn.ui.MessageDialog.Button.OK,
    label: opt_ok_label || 'OK',
    isDefault: true
  }];
};


/**
 * @param {string=} opt_ok_label optional OK label.
 * @param {string=} opt_cancel_label optional Cancel label.
 * @return {Array<ydn.ui.MessageDialog.ButtonDef>}
 */
ydn.ui.MessageDialog.createOKCancelButtonSet = function(opt_ok_label, opt_cancel_label) {
  return [{
    name: ydn.ui.MessageDialog.Button.OK,
    label: opt_ok_label || 'OK',
    isDefault: true
  }, {
    name: ydn.ui.MessageDialog.Button.CANCEL,
    label: opt_cancel_label || 'Cancel',
    isCancel: true
  }];
};


/**
 * Show model message dialog.
 * @param {string} title
 * @param {string|Node} message message or message content.
 * @param {Array.<ydn.ui.MessageDialog.ButtonDef>=} opt_btn button set. Default to
 * OK button.
 * @return {!goog.async.Deferred.<ydn.ui.MessageDialog.Button>} promise on
 * dialog close, resolved with button click.
 */
ydn.ui.MessageDialog.showModal = function(title, message, opt_btn) {
  var buttons = opt_btn || ydn.ui.MessageDialog.createOKButtonSet();
  var dialog = new ydn.ui.MessageDialog(title, message, buttons);
  var df = new goog.async.Deferred();
  dialog.dialog.onclose = function(event) {
    df.callback(dialog.dialog.returnValue);
    dialog.dispose();
  };
  dialog.dialog.showModal();
  return df;
};


