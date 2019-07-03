goog.provide('net.bluemind.webbundle.BundlesManager');
goog.require('goog.array');
goog.require('goog.object');
/**
 * @constructor
 */
net.bluemind.webbundle.BundlesManager = function() {
};

net.bluemind.webbundle.BundlesManager.prototype.bundles = {};
net.bluemind.webbundle.BundlesManager.prototype.listeners = {};

net.bluemind.webbundle.BundlesManager.prototype.loadBundle = function(bundle, url, async) {
  this.bundles[bundle] = {
    'id' : bundle,
    'state' : 'NONE',
    'async-loading' : async,
    'time': goog.now()
  };

  document.write('<script type="text/javascript" src="' + url + '" ><\/script>');
  if (!async) {
    document.write('<script type="text/javascript">window.bundleListener("' + bundle + '");<\/script>');
  }
  this.bundleStateChanged(bundle, 'INSTALLED');
}
net.bluemind.webbundle.BundlesManager.prototype.started = false;
net.bluemind.webbundle.BundlesManager.prototype.bundleStateChanged = function(bundleId, state) {
  var bundle = this.bundles[bundleId];
  if( bundle == null) {
    console.error('could not find bundle '+bundleId);
    return;
  }

  console.log('bundle ' + bundleId + " changed state: from " + bundle.state + " to " + state+' in '+( goog.now() - bundle.time));
  bundle.state = state;
  if (state == 'RESOLVED') {
    var listeners = this.listeners[bundleId];
    if (listeners) {
      goog.array.forEach(listeners, function(l) {
        l.apply(null, []);
      });
    }
  }

  var start = goog.array.reduce(goog.object.getValues(this.bundles), function(p, v, i) {
    return p && v['state'] == 'RESOLVED';
  }, true);
  
  if (start && this.started == false) {
    // prevent double start
    this.started = true;
    goog.array.forEach(goog.object.getValues(this.bundles), function(bundle) {
      if (bundle.start) {
        console.log('bundle ' + bundleId + " , call activator");
        bundle.start();
      }
    });
  }

}

net.bluemind.webbundle.BundlesManager.prototype.onResolve = function(bundleId, func) {
  if(! this.bundles[bundleId]) {
    console.log('bundle '+bundleId +' doesnt exists');
    func.apply(null, []);
    return;
  }
  if (this.bundles[bundleId]['state'] == 'RESOLVED' ) {
    func.apply(null, []);
  } else {
    var listeners = this.listeners[bundleId];
    if (!listeners) {
      listeners = [];
      this.listeners[bundleId] = listeners;
    }

    listeners.push(func);
  }
};

var bm = new net.bluemind.webbundle.BundlesManager();

goog.global['bmLoadBundle'] = goog.bind(bm.loadBundle, bm);
goog.global['__gwtStatsEvent'] = function(event) {
  if (event['evtGroup'] == 'moduleStartup' && event['type'] == 'end') {
    bm.bundleStateChanged(event['moduleName'], 'RESOLVED');
  }
};

goog.global['bundles'] = bm.bundles;

goog.global['bundleResolve'] = function(bundleId, func) {
  bm.onResolve(bundleId, func);
}

goog.global['bundleListener'] = function(bundleId) {
  bm.bundleStateChanged(bundleId, 'RESOLVED');
};