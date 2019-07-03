/**
 * @fileoverview Model message dialog box.
 */


goog.provide('ydn.ui.MessageBox');
goog.require('goog.async.Deferred');
goog.require('goog.ui.Dialog');



/**
 * Show a model message box. This is not instiatite directly but instead use static
 * <code>show</code> method.
 * <pre>
 *   var btns = goog.ui.Dialog.ButtonSet.createYesNo();
 *   ydn.ui.MessageBox.show('Test', 'Are you OK?', btns).addCallback(function(ans) {
 *     console.log(ans); // either 'yes' or 'no'
 *   });
 * </pre>
 * @param {string} title
 * @param {string} message
 * @param {goog.ui.Dialog.ButtonSet} btn_set
 * @param {goog.async.Deferred} df
 * @constructor
 * @extends {goog.ui.Dialog}
 */
ydn.ui.MessageBox = function(title, message, btn_set, df) {
  goog.base(this);
  this.df_ = df;
  this.setButtonSet(btn_set);
  this.setTitle(title);
  this.setContent(message);
  this.setEscapeToCancel(true);
  this.setHasTitleCloseButton(false);
  this.setModal(true);
  this.setDisposeOnHide(true);
  var hd = this.getHandler();
  hd.listen(this, goog.ui.Dialog.EventType.SELECT, this.handleSelect);
  hd.listen(this, goog.ui.Dialog.EventType.SELECT, this.handleCancel);
};
goog.inherits(ydn.ui.MessageBox, goog.ui.Dialog);


/**
 * @protected
 * @param {Event} e
 */
ydn.ui.MessageBox.prototype.handleSelect = function(e) {
  if (this.df_) {
    this.df_.callback(e.key);
    this.df_ = null;
  }
};


/**
 * @protected
 * @param {Event} e
 */
ydn.ui.MessageBox.prototype.handleCancel = function(e) {
  if (this.df_) {
    this.df_.callback(goog.ui.Dialog.DefaultButtonKeys.CANCEL);
    this.df_ = null;
  }
};


/**
 * Show a model message box.
 * <pre>
 *   var btns = goog.ui.Dialog.ButtonSet.createYesNo();
 *   ydn.ui.MessageBox.show('Test', 'Are you OK?', btns).addCallback(function(ans) {
 *     console.log(ans); // either 'yes' or 'no'
 *   });
 * </pre>
 * @param {string} title
 * @param {string} message
 * @param {goog.ui.Dialog.ButtonSet=} opt_btn_set default to 'ok' button only.
 * @return {!goog.async.Deferred}
 */
ydn.ui.MessageBox.show = function(title, message, opt_btn_set) {
  var df = new goog.async.Deferred();
  var btn_set = opt_btn_set || goog.ui.Dialog.ButtonSet.createOk();
  var dialog = new ydn.ui.MessageBox(title, message, btn_set, df);
  dialog.setVisible(true);
  return df;
};




