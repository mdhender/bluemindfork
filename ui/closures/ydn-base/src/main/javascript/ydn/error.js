/**
 * @fileoverview About this file.
 *
 * User: kyawtun
 * Date: 7/9/12
 */

goog.provide('ydn.error');
goog.provide('ydn.error.ArgumentException');
goog.provide('ydn.error.NotImplementedException');
goog.provide('ydn.error.ConstraintError');
goog.provide('ydn.error.NotSupportedException');
goog.provide('ydn.error.InternalError');
goog.provide('ydn.error.InvalidOperationException');
goog.provide('ydn.error.InvalidOperationError');




/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {Error}
 */
ydn.error.ArgumentException = function(opt_msg) {

  // Ensure there is a stack trace.
  if (Error.captureStackTrace) {
    Error.captureStackTrace(this, ydn.error.ArgumentException);
  } else {
    this.stack = new Error().stack || '';
  }

  if (opt_msg) {
    this.message = String(opt_msg);
  }
  this.name = 'ydn.error.ArgumentException';
};
goog.inherits(ydn.error.ArgumentException, Error);

/// FIXME: how come compiler strip out prototype name ?

//
///** @override */
//ydn.error.ArgumentException.prototype.name = 'ydn.ArgumentException';



/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {Error}
 */
ydn.error.NotSupportedException = function(opt_msg) {

  // Ensure there is a stack trace.
  if (Error.captureStackTrace) {
    Error.captureStackTrace(this, ydn.error.NotSupportedException);
  } else {
    this.stack = new Error().stack || '';
  }

  if (opt_msg) {
    this.message = String(opt_msg);
  }
  this.name = 'ydn.error.NotSupportedException';
};
goog.inherits(ydn.error.ArgumentException, Error);

/** @override */
ydn.error.NotSupportedException.prototype.name = 'ydn.error.NotSupportedException';


/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {Error}
 */
ydn.error.NotImplementedException = function(opt_msg) {

  // Ensure there is a stack trace.
  if (Error.captureStackTrace) {
    Error.captureStackTrace(this, ydn.error.NotImplementedException);
  } else {
    this.stack = new Error().stack || '';
  }

  if (opt_msg) {
    this.message = String(opt_msg);
  }
  this.name = 'ydn.error.NotImplementedException';
};
goog.inherits(ydn.error.NotImplementedException, Error);

///** @override */
//ydn.error.NotImplementedException.prototype.name = 'ydn.NotImplementedException';



/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {Error}
 */
ydn.error.InternalError = function(opt_msg) {

  // Ensure there is a stack trace.
  if (Error.captureStackTrace) {
    Error.captureStackTrace(this, ydn.error.InternalError);
  } else {
    this.stack = new Error().stack || '';
  }

  if (opt_msg) {
    this.message = String(opt_msg);
  }
  this.name = 'ydn.error.InternalError';
};
goog.inherits(ydn.error.InternalError, Error);

ydn.error.InternalError.prototype.name = 'ydn.InternalError';


/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {Error}
 */
ydn.error.ConstraintError = function(opt_msg) {

  // Ensure there is a stack trace.
  if (Error.captureStackTrace) {
    Error.captureStackTrace(this, ydn.error.ConstraintError);
  } else {
    this.stack = new Error().stack || '';
  }

  if (opt_msg) {
    this.message = String(opt_msg);
  }
  this.name = 'ydn.error.ConstraintError';
};
goog.inherits(ydn.error.ConstraintError, Error);

ydn.error.ConstraintError.prototype.name = 'ydn.error.ConstraintError';



/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {Error}
 */
ydn.error.InvalidOperationException = function(opt_msg) {

  // Ensure there is a stack trace.
  if (Error.captureStackTrace) {
    Error.captureStackTrace(this, ydn.error.InvalidOperationException);
  } else {
    this.stack = new Error().stack || '';
  }

  if (opt_msg) {
    this.message = String(opt_msg);
  }
  this.name = 'ydn.error.InvalidOperationException';
};
goog.inherits(ydn.error.ArgumentException, Error);

///** @override */
//ydn.error.InvalidOperationException.prototype.name = 'ydn.InvalidOperationException';



/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {Error}
 */
ydn.error.InvalidOperationError = function(opt_msg) {

  // Ensure there is a stack trace.
  if (Error.captureStackTrace) {
    Error.captureStackTrace(this, ydn.error.InvalidOperationError);
  } else {
    this.stack = new Error().stack || '';
  }

  if (opt_msg) {
    this.message = String(opt_msg);
  }
  this.name = 'ydn.error.InvalidOperationError';
};
goog.inherits(ydn.error.InvalidOperationError, Error);

//
///** @override */
//ydn.error.InvalidOperationError.prototype.name = 'ydn.InvalidOperationError';