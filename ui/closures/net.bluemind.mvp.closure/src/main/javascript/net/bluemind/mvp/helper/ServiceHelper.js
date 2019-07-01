goog.provide("net.bluemind.mvp.helper.ServiceHelper");

/**
 * @constructor
 */
net.bluemind.mvp.helper.ServiceHelper = function() {
}

/**
 * @static
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @param {Object} caller
 * @param {Object.<string, Function>} states
 * @param {Array.<*>} params
 * @param {Array=} opt_states
 * @return {*}
 */
net.bluemind.mvp.helper.ServiceHelper.handleByState = function(ctx, caller, states, params, opt_states) {
  var localState = [];
  if (!opt_states) {
    if (caller.isLocal()) {
      localState.push('local');
    }

    if (ctx.online) {
      localState.push('remote');
    }
  } else {
    localState = opt_states;
  }

  var callableStates = goog.array.map(goog.object.getKeys(states), function(state) {
    var t = goog.string.splitLimit(state, ',', 8);
    var ok = goog.array.reduce(t, function(r, s) {
      return r && goog.array.contains(localState, s);
    }, true);

    if (ok) {
      return states[state];
    } else {
      return null;
    }
  });

  callableStates = goog.array.filter(callableStates, goog.isDefAndNotNull);
  if (callableStates.length == 0) {
    throw 'NotAvailable';
  } else {
    var ret = callableStates[0].apply(caller, params);
    if (ret instanceof goog.async.Deferred) {
      return ret.addErrback(function(e) {
        if (e == "Offline") {
          return net.bluemind.mvp.helper.ServiceHelper.handleByState(ctx, caller, states, params, [ 'local' ]);
        } else {
          throw e;
        }
      });

    } else if (ret instanceof goog.Promise) {
      return ret.thenCatch(function(e) {
        if (e == "Offline") {
          return net.bluemind.mvp.helper.ServiceHelper.handleByState(ctx, caller, states, params, [ 'local' ]);
        } else {
          throw e;
        }
      });
    } else {
      return ret;
    }
  }
};