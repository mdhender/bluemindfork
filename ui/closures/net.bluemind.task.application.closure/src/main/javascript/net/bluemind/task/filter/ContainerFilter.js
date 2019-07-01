goog.provide("net.bluemind.task.filter.ContainerFilter");

goog.require("net.bluemind.mvp.Filter");

/**
 * @constructor
 * 
 * @extends {net.bluemind.mvp.Filter}
 */
net.bluemind.task.filter.ContainerFilter = function() {
  net.bluemind.mvp.Filter.call(this);
}
goog
    .inherits(net.bluemind.task.filter.ContainerFilter, net.bluemind.mvp.Filter);


/** @override */
net.bluemind.task.filter.ContainerFilter.prototype.filter = function(ctx) {
  if (ctx.params.get('container')) {
    ctx.session.set('container', ctx.params.get('container'))
  }
  if(!ctx.session.containsKey('container')) {
	  var userUid = ctx.user['uid'];  
	  ctx.session.set('container', 'todolist:default_'+userUid);
  }
};

