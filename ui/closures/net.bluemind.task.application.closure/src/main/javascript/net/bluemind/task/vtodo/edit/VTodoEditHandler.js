goog.provide("net.bluemind.task.vtodo.edit.VTodoEditHandler");

goog.require("net.bluemind.mvp.handler.PresenterHandler");
goog.require("net.bluemind.task.vtodo.edit.VTodoEditPresenter");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.handler.PresenterHandler}
 */
net.bluemind.task.vtodo.edit.VTodoEditHandler = function(ctx) {
  net.bluemind.mvp.handler.PresenterHandler.call(this, ctx);
}
goog.inherits(net.bluemind.task.vtodo.edit.VTodoEditHandler, net.bluemind.mvp.handler.PresenterHandler);

/** @override */
net.bluemind.task.vtodo.edit.VTodoEditHandler.prototype.createPresenter = function(ctx) {
  return new net.bluemind.task.vtodo.edit.VTodoEditPresenter(ctx);
}

/** @override */
net.bluemind.task.vtodo.edit.VTodoEditHandler.prototype.onNavigation = function(exit) {
  return this.presenter.exit();
};