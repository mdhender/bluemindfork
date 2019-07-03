/**
 * @fileoverview Expression.
 *
 * User: kyawtun
 * Date: 28/10/12
 */


goog.provide('ydn.math.Expression');
goog.require('ydn.string');
goog.require('goog.string');
goog.require('ydn.object');
goog.require('ydn.error');


/**
 * Create a expression by tokesn.
 * @param {Array} tokens
 * @constructor
 */
ydn.math.Expression = function(tokens) {
  /**
   * @final
   * @type {Array}
   */
  this.tokens = tokens;
};


/**
 *
 * @type {Array}
 */
ydn.math.Expression.prototype.tokens = [];


/**
 * Evaluate the expression.
 * @param {Object} with_object
 * @param {...} var_args
 * @return {*}
 */
ydn.math.Expression.prototype.evaluate = function (with_object, var_args) {
  var stack = [];

  for (var i = 0; i < this.tokens.length; i++) {
    var tok = this.tokens[i];
    var is_field_name = tok[0] === '"' && tok[tok.length - 1] === '"';
    var is_string_literal = tok[0] === "'" && tok[tok.length - 1] === "'";

    if (is_field_name) {
      var keys = goog.string.stripQuotes(tok, '"').split('.');
      goog.asserts.assertObject(with_object);
      var value = goog.object.getValueByKeys(with_object, keys);
      stack.push(value);
    } else if (is_string_literal) {
      stack.push(goog.string.stripQuotes(tok, "'"));
    } else if (goog.isString(tok)) {
      if (tok === 'true') {
        stack.push(true);
      } else if (tok === 'false') {
        stack.push(false);
      } else if (tok === 'Date') {
        stack.push(new Date(parseInt(stack.pop(), 10)));
      } else if (tok === 'now') {
        stack.push(new Date());
      } else if (tok === '!') {
        stack[stack.length - 1] = !stack[stack.length - 1];
      } else if (tok === '==') {
        stack.push(stack.pop() == stack.pop());
      } else if (tok === '===') {
        stack.push(stack.pop() === stack.pop());
      } else if (tok === '!=') {
        stack.push(stack.pop() != stack.pop());
      } else if (tok === '!==') {
        stack.push(stack.pop() !== stack.pop());
      } else if (tok === '<=') {
        stack.push(stack.pop() <= stack.pop());
      } else if (tok === '<') {
        stack.push(stack.pop() < stack.pop());
      } else if (tok === '>=') {
        stack.push(stack.pop() >= stack.pop());
      } else if (tok === '>') {
        stack.push(stack.pop() > stack.pop());
      } else if (tok === '&') {
        stack.push(stack.pop() & stack.pop());
      } else if (tok === '|') {
        stack.push(stack.pop() | stack.pop());
      } else if (tok === '?') {
        var ok = !!stack.pop();
        var a = stack.pop();
        var b = stack.pop();
        var v = ok ? a : b;
        //console.log([this.tokens.join(','), ok, a, b, v]);
        stack.push(v);
      } else if (tok === '+') {
        stack.push(stack.pop() + stack.pop());
      } else if (tok === '-') {
        stack.push(stack.pop() - stack.pop());
      } else if (tok === '*') {
        stack.push(stack.pop() * stack.pop());
      } else if (tok === '/') {
        stack.push(stack.pop() / stack.pop());
      } else if (tok === '%') {
        stack.push(stack.pop() % stack.pop());
      } else if (tok === 'at') {
        var at = stack.pop();
        var arr = stack.pop();
        stack.push(arr[at]);
      } else if (tok === 'of') {
        var ele = stack.pop();
        var arr = stack.pop();
        var v = goog.isArray(arr) ? arr.indexOf(ele) : -1;
        stack.push(v);
      } else if (tok === 'in') {
        var ele = stack.pop();
        var arr = stack.pop();
        var v = goog.isArray(arr) ? arr.indexOf(ele) >= 0 : false;
        stack.push(v);
      } else if (tok === 'abs') {
        stack[stack.length - 1] = Math.abs(stack[stack.length - 1]);
      } else if (tok[0] == '$' && /^\$\d$/.test(tok)) {
        var pos = parseInt(tok.match(/^\$(\d)$/)[1], 10);
        stack.push(arguments[pos]);
      } else {
        stack.push(parseFloat(tok));
      }
    } else {
      stack.push(tok);
    }
  }
  return stack[0];
};


/**
 * Compile into function.
 * @return {Function}
 */
ydn.math.Expression.prototype.compile = function() {
  var tokens = this.tokens;
  return function() {
    var args = arguments.length > 2 ?
      Array.prototype.slice(arguments, 2) : undefined;
    return ydn.math.Expression.prototype.evaluate.apply(null, args);
  }
};


/**
 * @override
 * @return {Object}
 */
ydn.math.Expression.prototype.toJSON = function() {
  return {'Tokens': ydn.object.clone(this.tokens)};
};


/**
 * Parse reverse polish notation statement into expression.
 * @param {string} expression
 * @return {!ydn.math.Expression}
 */
ydn.math.Expression.parseRpn = function(expression) {
  return new ydn.math.Expression(ydn.string.split_space(expression));
};

/**
 * Infix notation statement into expression.
 * @param {string} expression
 * @return {!ydn.math.Expression}
 */
ydn.math.Expression.parseInfix = function(expression) {
  throw new ydn.error.NotImplementedException(
    'Too lazy to learn Dutch in Shunting Yard station, ' +
      'we speak Polish here.');
};

