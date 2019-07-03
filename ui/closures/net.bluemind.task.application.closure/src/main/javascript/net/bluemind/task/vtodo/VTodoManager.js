goog.provide("net.bluemind.task.vtodo.VTodoManager");
goog.require('net.bluemind.mvp.UID');
/**
 * @constructor
 * 
 * @param {net.bluemind.task.vtodo.edit.VTodoEditPresenter} ctx
 * @param {net.bluemind.task.vtodo.ModelAdaptor} adaptor
 */
net.bluemind.task.vtodo.VTodoManager = function(ctx, adaptor) {
  this.ctx_ = ctx;
  this.adaptor_ = adaptor;
};

/**
 * @type {net.bluemind.task.vtodo.ModelAdaptor}
 * @private
 */
net.bluemind.task.vtodo.VTodoManager.prototype.adaptor_;

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @private
 */
net.bluemind.task.vtodo.VTodoManager.prototype.ctx_;

/**
 * Save task
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.task.vtodo.VTodoManager.prototype.handleSave = function(event) {
  var model = event.target.getParent().getParent().getModel();
  var vtodo = this.adaptor_.vtodoFromModelView(model);
  var toCreate = goog.array.filter(vtodo['value']['categories'], function(c) {
    return c['itemUid'] == null;
  });

  var alreadyExists = !!vtodo['uid'] && vtodo['uid'] != 'undefinied';
  var promise = this.ctx_.service('tags').createTags(toCreate).then(function() {
    if (alreadyExists) {
      return this.ctx_.service('todolist').update(vtodo);
    } else {
      vtodo['uid'] = net.bluemind.mvp.UID.generate();
      return this.ctx_.service('todolist').create(vtodo);
    }
  }, null, this).then(function(item) {
    if (alreadyExists) {
      this.ctx_.notifyInfo(net.bluemind.task.Messages.successUpdate());
    } else {
      this.ctx_.notifyInfo(net.bluemind.task.Messages.successCreate());
    }
    // FIXME: location
    this.ctx_.helper('url').goTo('/vtodo/edit?uid=' + vtodo['uid'] + '&container=' + vtodo['container']);
  }, function(error) {
    if (alreadyExists) {
      this.ctx_.notifyError(net.bluemind.task.Messages.errorUpdate(error), error);
    } else {
      this.ctx_.notifyError(net.bluemind.task.Messages.errorCreate(error), error);
    }
  }, this);
};

/**
 * Move contact
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.task.vtodo.VTodoManager.prototype.handleMove = function(event) {
  var model = event.target.getParent().getParent().getParent().getParent().getModel();
  this.ctx_.service('todolist').moveItem(model.container, model.id, event.target.getId()).then(function(m) {
    this.ctx_.notifyInfo(net.bluemind.task.Messages.successMove());
    this.ctx_.helper('url').goTo('/vtodo/?container=' + event.target.getId() + '&uid=' + model.id);
  }, function(error) {
    this.ctx_.notifyError(net.bluemind.task.Messages.errorMove(error), error);
  }, this);
};

/**
 * Copy task
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.task.vtodo.VTodoManager.prototype.handleCopy = function(event) {
  var model = event.target.getParent().getParent().getParent().getParent().getModel();
  this.ctx_.service('todolist').copyItem(model.container, model.id, event.target.getId()).then(function(m) {
    this.ctx_.notifyInfo(net.bluemind.task.Messages.successCopy());
    this.ctx_.helper('url').goTo('/vtodo/?container=' + event.target.getId() + '&uid=' + model.id);
  }, function(error) {
    this.ctx_.notifyError(net.bluemind.task.Messages.errorCopy(error), error);
  }, this);
};


/**
 * load task history
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.task.vtodo.VTodoManager.prototype.loadItemHistory = function(event) {
  var model = event.target.getParent().getParent().getModel();
  if (model.id && model.container) {
    var todoUid = model.id;
    var container = model.container;
    var service = this.ctx_.service('todolist');
    return service.getItemHistory(container, todoUid);
  }
}

/**
 * Mark task as done
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.task.vtodo.VTodoManager.prototype.handleMarkAsDone = function(event) {
  var model = event.target.getParent().getParent().getModel();
  if (model.id && model.container) {
    var todoUid = model.id;
    var container = model.container;
    var service = this.ctx_.service('todolist');
    return service.getItem(container, todoUid).then(function(todo) {
      if (todo.value.status != "Completed") {
        todo.value.status = "Completed";
        return service.update(todo);
      }
    }, null, this).then(function() {
      this.ctx_.notifyInfo(net.bluemind.task.Messages.successUpdate());
      this.ctx_.helper('url').reload();
    }, function(error) {
      this.ctx_.notifyError(net.bluemind.task.Messages.errorUpdate(error), error);
    }, this);
    ;
  }
}

/**
 * Delete task
 * 
 * @protected
 * @param {goog.event.Event} event
 */
net.bluemind.task.vtodo.VTodoManager.prototype.handleDelete = function(event) {
  var model = event.target.getParent().getParent().getModel();
  if (model.id && model.container) {
    this.ctx_.service('todolist').deleteItem(model.container, model.id).then(function() {
      this.ctx_.notifyInfo(net.bluemind.task.Messages.successDelete());
      this.ctx_.helper('url').goTo('/', 'container');
    }, function(error) {
      this.ctx_.notifyError(net.bluemind.task.Messages.errorDelete(error), error);
    }, this);
  } else {
    this.ctx_.helper('url').goTo('/', 'container');
  }
};
