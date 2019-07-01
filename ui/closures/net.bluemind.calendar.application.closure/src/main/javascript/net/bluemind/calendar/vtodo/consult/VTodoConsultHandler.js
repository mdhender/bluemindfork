goog.provide("net.bluemind.calendar.vtodo.consult.VTodoConsultHandler");

goog.require("net.bluemind.mvp.handler.PresenterHandler");
goog.require("net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.handler.PresenterHandler}
 */
net.bluemind.calendar.vtodo.consult.VTodoConsultHandler = function(ctx) {
  net.bluemind.mvp.handler.PresenterHandler.call(this, ctx);
}
goog.inherits(net.bluemind.calendar.vtodo.consult.VTodoConsultHandler, net.bluemind.mvp.handler.PresenterHandler);

/** @override */
net.bluemind.calendar.vtodo.consult.VTodoConsultHandler.prototype.createPresenter = function(ctx) {
  return new net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter(ctx);
}

/** @override */
net.bluemind.calendar.vtodo.consult.VTodoConsultHandler.prototype.onNavigation = function(exit) {
  return this.presenter.exit();
};