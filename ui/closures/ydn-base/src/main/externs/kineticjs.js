/**
 * @fileoverview Extens for KineticJS
 */


/**
 * @type {Object}
 * @const
 */
var Kinetic = {};


/**
 * @typedef {{
 *   data: (string|undefined),
 *   fill: (string|undefined),
 *   fontSize: (number|undefined),
 *   fontFamily: (string|undefined),
 *   lineCap: (string|undefined),
 *   lineJoin: (string|undefined),
 *   offsetX: (number|undefined),
 *   offsetY: (number|undefined),
 *   scale: (number|undefined),
 *   stroke: (string|undefined),
 *   strokeWidth: (number|undefined),
 *   text: (string|undefined),
 *   x: (number|undefined),
 *   y: (number|undefined)
 * }}
 */
Kinetic.Config;



/**
 * @param {Kinetic.Config=} params
 * @constructor
 */
Kinetic.Node = function(params) {};



/**
 * @param {Kinetic.Config=} params
 * @constructor
 * @extends {Kinetic.Node}
 */
Kinetic.Shape = function(params) {};


/**
 * get/set fill color
 * @param {string=} opt_color
 * @return {string}
 */
Kinetic.Shape.prototype.fill = function(opt_color) {};


/**
 * get/set fill green component
 * @param {number} opt_color
 * @return {number}
 */
Kinetic.Shape.prototype.fillGreen = function(opt_color) {};


/**
 * get/set fill red component
 * @param {number} opt_color
 * @return {number}
 */
Kinetic.Shape.prototype.fillRed = function(opt_color) {};


/**
 * get/set fill blue component
 * @param {number} opt_color
 * @return {number}
 */
Kinetic.Shape.prototype.fillBlue = function(opt_color) {};


/**
 * get/set fill blue component
 * @param {number=} x
 * @return {number}
 */
Kinetic.Shape.prototype.offsetX = function(x) {};


/**
 * get/set fill blue component
 * @param {number=} x
 * @return {number}
 */
Kinetic.Shape.prototype.offsetY = function(x) {};


/**
 * get/set fill blue component
 * @param {number=} x
 * @return {number}
 */
Kinetic.Shape.prototype.width = function(x) {};


/**
 * get/set fill blue component
 * @param {number=} x
 * @return {number}
 */
Kinetic.Shape.prototype.height = function(x) {};


/**
 * @param {Kinetic.Config=} params
 * @constructor
 * @extends {Kinetic.Shape}
 */
Kinetic.Container = function(params) {};



/**
 * @param {Kinetic.Config=} params
 * @constructor
 * @extends {Kinetic.Shape}
 */
Kinetic.Path = function(params) {};



/**
 * @param {Kinetic.Config=} params
 * @constructor
 * @extends {Kinetic.Shape}
 */
Kinetic.Circle = function(params) {};



/**
 * @param {Kinetic.Config=} params
 * @constructor
 * @extends {Kinetic.Shape}
 */
Kinetic.Rect = function(params) {};



/**
 * @param {Kinetic.Config=} params
 * @constructor
 * @extends {Kinetic.Shape}
 */
Kinetic.Text = function(params) {};


/**
 * Set/Get text.
 * @param {string=} text
 * @return {string}
 */
Kinetic.Text.prototype.text = function(text) {};



/**
 * @param {{scaleX: number, scaleY: number}=} params
 * @constructor
 * @extends {Kinetic.Container}
 */
Kinetic.Group = function(params) {};


/**
 * @param {Kinetic.Shape} path
 */
Kinetic.Group.prototype.add = function(path) {};


/**
 * @return {Array.<Kinetic.Shape>}
 */
Kinetic.Group.prototype.getChildren = function() {};



/**
 * @constructor
 * @param {Kinetic.Config=} opt_config
 * @extends {Kinetic.Container}
 */
Kinetic.Layer = function(opt_config) {};


/**
 * @param {Kinetic.Node} g
 */
Kinetic.Layer.prototype.add = function(g) {};


/**
 * Draw.
 */
Kinetic.Layer.prototype.draw = function() {};


/**
 * clear scene and hit canvas contexts tied to the layer
 */
Kinetic.Layer.prototype.clear = function() {};


/**
 * @return {Array.<Kinetic.Group>}
 */
Kinetic.Layer.prototype.getChildren = function() {};



/**
 * @param {{
 * container: (string|Element),
  * width: number,
  * height: number}=} params
 * @constructor
 */
Kinetic.Stage = function(params) {};


/**
 * @param {Kinetic.Layer} layer
 */
Kinetic.Stage.prototype.add = function(layer) {};



/**
 * @constructor
 */
Kinetic.AnimationFrame = function() {};


/**
 * @type {number}
 */
Kinetic.AnimationFrame.prototype.timeDiff;


/**
 * @type {number}
 */
Kinetic.AnimationFrame.prototype.lastTime;


/**
 * @type {number}
 */
Kinetic.AnimationFrame.prototype.time;


/**
 * @type {number}
 */
Kinetic.AnimationFrame.prototype.frameRate;



/**
 * @param {function(Kinetic.AnimationFrame)} frame
 * @param {Kinetic.Layer} layer
 * @constructor
 */
Kinetic.Animation = function(frame, layer) {};


/**
 */
Kinetic.Animation.prototype.start = function() {};


/**
 */
Kinetic.Animation.prototype.stop = function() {};

