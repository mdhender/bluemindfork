goog.provide("net.bluemind.task.create.CreatePresenter");

goog.require("goog.Promise");
goog.require("goog.dom");
goog.require("goog.ui.Component.EventType");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.task.create.CreateView");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.task.create.CreatePresenter = function(ctx) {
  net.bluemind.mvp.Presenter.call(this, ctx);
  this.view_ = new net.bluemind.task.create.CreateView();
  this.registerDisposable(this.view_);
}
goog.inherits(net.bluemind.task.create.CreatePresenter,
    net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.task.create.CreateView}
 * @private
 */
net.bluemind.task.create.CreatePresenter.prototype.view_;

/** @override */
net.bluemind.task.create.CreatePresenter.prototype.init = function() {
  this.view_.addClassName(goog.getCssName('add-todo'));
  this.view_.render(goog.dom.getElement('header'));
  this.handler.listen(this.view_, goog.ui.Component.EventType.ACTION, this.goto_)
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.task.create.CreatePresenter.prototype.setup = function() {
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.task.create.CreatePresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * @param {goog.event.Event} e Event
 * @private
 */
net.bluemind.task.create.CreatePresenter.prototype.goto_ = function() {
  var loc = goog.dom.getWindow().location;
  loc.hash = '/vtodo/?container='+this.ctx.params.get('container')+'&ts=' + goog.now();
};
