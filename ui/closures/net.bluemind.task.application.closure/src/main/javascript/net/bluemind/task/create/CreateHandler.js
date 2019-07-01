goog.provide("net.bluemind.task.create.CreateHandler");

goog.require("net.bluemind.mvp.handler.PresenterHandler");
goog.require("net.bluemind.task.create.CreatePresenter");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.handler.PresenterHandler}
 */
net.bluemind.task.create.CreateHandler = function(ctx) {
  net.bluemind.mvp.handler.PresenterHandler.call(this, ctx);
}
goog.inherits(net.bluemind.task.create.CreateHandler,
    net.bluemind.mvp.handler.PresenterHandler);

/** @override */
net.bluemind.task.create.CreateHandler.prototype.createPresenter = function(ctx) {
  return new net.bluemind.task.create.CreatePresenter(ctx);
};