goog.provide("net.bluemind.task.vtodo.edit.VTodoEditPresenter");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.dom");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.task.vtodo.ModelAdaptor");
goog.require("net.bluemind.task.vtodo.VTodoManager");
goog.require("net.bluemind.task.vtodo.edit.VTodoEditView");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.task.vtodo.edit.VTodoEditPresenter = function(ctx) {
  net.bluemind.mvp.Presenter.call(this, ctx);
  this.view_ = new net.bluemind.task.vtodo.edit.VTodoEditView(ctx);
  this.registerDisposable(this.view_);
  this.adaptor_ = new net.bluemind.task.vtodo.ModelAdaptor(ctx);
  this.manager_ = new net.bluemind.task.vtodo.VTodoManager(ctx, this.adaptor_);
  //
  this.handler.listenWithScope(this.view_, 'markAsDone', this.manager_.handleMarkAsDone, false, this.manager_);
  this.handler.listen(this.view_, 'save', function(e) {
    var model = e.target.getParent().getParent().getModel();
    var errors = this.validateModelView_(model);
    if (errors.length > 0) {
      model.errors = errors;
      this.view_.setModel(model);
      return;
    } else {
      this.manager_.handleSave(e);
    }
  });
  var that  = this;
  this.handler.listen(this.view_, 'history', function(e) {
    var history = this.manager_.loadItemHistory(e).then(function(history) {
      that.view_.showHistory(history['entries']);
    }); 
  });
  this.handler.listenWithScope(this.view_, 'delete', this.manager_.handleDelete, false, this.manager_);
  this.handler.listenWithScope(this.view_, 'move', this.manager_.handleMove, false, this.manager_);
  this.handler.listenWithScope(this.view_, 'copy', this.manager_.handleCopy, false, this.manager_);
}
goog.inherits(net.bluemind.task.vtodo.edit.VTodoEditPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {goog.ui.Component}
 * @private
 */
net.bluemind.task.vtodo.edit.VTodoEditPresenter.prototype.view_;

/**
 * @type {net.bluemind.task.vtodo.ModelAdaptor}
 * @private
 */
net.bluemind.task.vtodo.edit.VTodoEditPresenter.prototype.adaptor_;

/**
 * @type {net.bluemind.task.vtodo.VTodoManager}
 * @private
 */
net.bluemind.task.vtodo.edit.VTodoEditPresenter.prototype.manager_;

/** @override */
net.bluemind.task.vtodo.edit.VTodoEditPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('main'));
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.task.vtodo.edit.VTodoEditPresenter.prototype.setup = function() {
  var container = this.ctx.params.get('container');
  var promise, uid = this.ctx.params.get('uid');
  var todolist = goog.array.find(this.ctx.session.get('todolists'), function(tdl) {
    return (tdl['uid'] == container);
  });
  var todolists = this.ctx.session.get('todolists');
  if (uid) {
    promise = this.ctx.service('todolist').getItem(container, uid);
  } else {
    promise = goog.Promise.resolve(this.getEmptyVTodo_(container, todolist));
  }
  var vtodo;
  return promise.then(function(item) {
    vtodo = item;
    return this.ctx.service('todolist').getLocalChangeSet(container);
  }, null, this).then(function(changes) {
    if (goog.isDefAndNotNull(vtodo)) {
      this.view_.setModel(this.toModelView_(vtodo, todolist, changes));
      this.view_.setTodoLists(goog.array.map(todolists, this.adaptor_.todoListToModelView, this.adaptor_));
    } else {
      throw 'VTodo ' + uid + ' not found';
    }
  }, null, this).then(function() {
    return this.ctx.service('tags').getTags();
  }, null, this).then(function(tags) {
    var tags = goog.array.map(tags, function(tag) {
      var mv = {};
      mv.label = tag['label'];
      mv.color = tag['color'];
      mv.id = tag['itemUid'];
      mv.container = tag['containerUid'];
      return mv;
    }, this);
    this.view_.setTags(tags);
  }, null, this).thenCatch(function(error) {
    this.ctx.notifyError(net.bluemind.task.Messages.errorLoading(error), error);
  }, this);
};

/**
 * Convert a Vtodo to a model view
 * 
 * @param {Object} vtodo
 * @param {Object} todolist
 * @param {Array.<Object>} changes
 * @return {Object}
 * @private
 */
net.bluemind.task.vtodo.edit.VTodoEditPresenter.prototype.toModelView_ = function(vtodo, todolist, changes) {
  var mv = this.adaptor_.vtodoToModelView(vtodo, todolist);
  var change = goog.array.find(changes, function(change) {
    return change['itemId'] == vtodo['uid'];
  });
  mv.states.synced = !goog.isDefAndNotNull(change);
  mv.states.error = !mv.states.synced && change['type'] == 'error';
  mv.error = mv.states.error && {
    code : change['errorCode'],
    message : change['errorMessage']
  };
  return mv;
};

/** @override */
net.bluemind.task.vtodo.edit.VTodoEditPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * Task default form object.
 * 
 * @param {string} container Container uid
 * @private
 */
net.bluemind.task.vtodo.edit.VTodoEditPresenter.prototype.getEmptyVTodo_ = function(container) {
  return {
    'container' : container,
    'uid' : null,
    'value' : {
      'summary' : '',
      'description' : '',
      'location' : '',
      'status' : '',
      'priority' : 0,
      'percent' : '0'
    }
  };
};

net.bluemind.task.vtodo.edit.VTodoEditPresenter.prototype.validateModelView_ = function(mv) {
  var ret = [];
  if (!mv.summary || mv.summary.length == 0) {
    /** @meaning tasks.formError.summary */
    var MSG_NAME_IS_EMPTY = goog.getMsg('Summary is empty');
    ret.push({
      property : 'summary',
      msg : MSG_NAME_IS_EMPTY
    });
  }
  return ret;
}
