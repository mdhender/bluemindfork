goog.provide('relief.ServiceProvider');

goog.require('goog.Disposable');
goog.require('goog.dispose');



/**
 * Service Provider
 *
 * The service provider class is the enabler for dependency injection.  It
 * provides a mechanism for injecting arbitrary objects and values into other
 * systems at construction.  If a subsystem requires specific things be
 * injected into them, developers are encouraged to subclass ServiceProvider
 * with one that requires the needed subsystems as constructor arguments.
 * See relief.sp.NavServiceProvider for an example.
 *
 * @constructor
 * @extends {goog.Disposable}
 */
relief.ServiceProvider = function() {
  goog.base(this);

  /**
   * @type {Object.<*>}
   * @private
   */
  this.resources_ = {};
};
goog.inherits(relief.ServiceProvider, goog.Disposable);


/**
 * This method (and the corresponding getter) allow for injecting arbitrary
 * values via the ServiceProvider object.  If something was already inserted
 * with the same name, the existing value is passed to goog.dispose() and then
 * overwritten with the new value.
 *
 * @param {string} name The name of the resource to store.
 * @param {*} value The thing to store.
 */
relief.ServiceProvider.prototype.setResource = function(name, value) {
  var resources = this.resources_;

  if (resources[name]) {
    // Dispose the existing value.
    goog.dispose(resources[name]);
  }

  resources[name] = value;
};


/**
 * @param {string} name The name of the resource to get.
 * @return {*} Returns the requested object.
 */
relief.ServiceProvider.prototype.getResource = function(name) {
  return this.resources_[name];
};


/** @inheritDoc */
relief.ServiceProvider.prototype.disposeInternal = function() {
  var resources = this.resources_;

  for (var key in resources) {
    goog.dispose(resources[key]);
    delete resources[key];
  }
};


/**
 * Throws an error if called after disposal.  This way, subclasses can assert
 * that the provider has not yet been disposed, and if no error is thrown, the
 * resource can safely be cast to non-null and returned.
 *
 * @protected
 */
relief.ServiceProvider.prototype.assertNotDisposed = function() {
  if (this.isDisposed()) {
    throw Error('Attempting to retrieve resource after disposal.');
  }
};
