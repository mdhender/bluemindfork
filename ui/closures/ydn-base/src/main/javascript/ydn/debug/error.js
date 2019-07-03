// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


/**
 * @fileoverview Utilities function for debug.
 *
 * NOTE: these code are stripped using compiler prefix feature.
 * See more detail in tools/strip_debug.txt file.
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */



goog.provide('ydn.debug.error.ArgumentException');
goog.provide('ydn.debug.error.ConstraintError');
goog.provide('ydn.debug.error.InternalError');
goog.provide('ydn.debug.error.InvalidOperationException');
goog.provide('ydn.debug.error.NotImplementedException');
goog.provide('ydn.debug.error.NotSupportedException');
goog.provide('ydn.debug.error.TypeError');

goog.require('goog.debug.Error');



/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {goog.debug.Error}
 */
ydn.debug.error.ArgumentException = function(opt_msg) {
  goog.base(this, opt_msg);
  this.name = 'ydn.error.ArgumentException';
};
goog.inherits(ydn.debug.error.ArgumentException, goog.debug.Error);



/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {goog.debug.Error}
 */
ydn.debug.error.TypeError = function(opt_msg) {
  goog.base(this, opt_msg);
  this.name = 'ydn.error.TypeError';
};
goog.inherits(ydn.debug.error.TypeError, goog.debug.Error);



/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {goog.debug.Error}
 */
ydn.debug.error.NotSupportedException = function(opt_msg) {
  goog.base(this, opt_msg);
  this.name = 'ydn.error.NotSupportedException';
};
goog.inherits(ydn.debug.error.NotSupportedException, goog.debug.Error);



/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {goog.debug.Error}
 */
ydn.debug.error.NotImplementedException = function(opt_msg) {
  goog.base(this, opt_msg);
  this.name = 'ydn.error.NotImplementedException';
};
goog.inherits(ydn.debug.error.NotImplementedException, goog.debug.Error);



/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {goog.debug.Error}
 */
ydn.debug.error.InvalidOperationException = function(opt_msg) {
  goog.base(this, opt_msg);
  this.name = 'ydn.error.InvalidOperationException';
};
goog.inherits(ydn.debug.error.InvalidOperationException, goog.debug.Error);



/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {Error}
 */
ydn.debug.error.InternalError = function(opt_msg) {

  // Ensure there is a stack trace.
  if (Error.captureStackTrace) {
    Error.captureStackTrace(this, ydn.debug.error.InternalError);
  } else {
    this.stack = new Error().stack || '';
  }

  if (opt_msg) {
    this.message = String(opt_msg);
  }
  this.name = 'ydn.error.InternalError';
};
goog.inherits(ydn.debug.error.InternalError, Error);


/**
 * Name of error.
 * @type {string}
 */
ydn.debug.error.InternalError.prototype.name = 'ydn.error.InternalError';



/**
 * Base class for custom error objects.
 * @param {*=} opt_msg The message associated with the error.
 * @constructor
 * @extends {Error}
 */
ydn.debug.error.ConstraintError = function(opt_msg) {

  // Ensure there is a stack trace.
  if (Error.captureStackTrace) {
    Error.captureStackTrace(this, ydn.debug.error.ConstraintError);
  } else {
    this.stack = new Error().stack || '';
  }

  if (opt_msg) {
    this.message = String(opt_msg);
  }
  this.name = 'ydn.error.ConstraintError';
};
goog.inherits(ydn.debug.error.ConstraintError, Error);


/**
 * Name of error.
 * @type {string}
 */
ydn.debug.error.ConstraintError.prototype.name = 'ydn.error.ConstraintError';

