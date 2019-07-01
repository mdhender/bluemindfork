// Copyright 2012 YDN Authors. All Rights Reserved.
//
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
 * @fileoverview AVL tree suitable for async traversal and resume.
 *
 * Basically this is slight modification of @see goog.structs.AvlTree.
 * Unfortunately, the original class cannot be override since some functions
 * are not expose to superclass.
 *
 * @suppress {accessControls}
 */


goog.provide('ydn.structs.Buffer');
goog.provide('ydn.structs.Buffer.Iterator');
goog.require('goog.structs.AvlTree');
goog.require('ydn.db.KeyRange');



/**
 *
 * @param {Function=} opt_comparator Function used to order the tree's nodes.
 * @constructor
 * @extends {goog.structs.AvlTree}
 */
ydn.structs.Buffer = function(opt_comparator) {
  goog.base(this, opt_comparator);
};
goog.inherits(ydn.structs.Buffer, goog.structs.AvlTree);


/**
 * Performs an in-order traversal of the tree and calls {@code func} with each
 * traversed node, optionally starting from the smallest node with a value >= to
 * the specified start value. The traversal ends after traversing the tree's
 * maximum node or when {@code func} returns a value that evaluates to true.
 *
 * @param {function(goog.structs.AvlTree.Node)} func Function to call on each
 * traversed node.
 * @param {Object=} opt_startValue If
 * specified, traversal will begin on the node with the smallest
 * value >= opt_startValue. If AvlTree.Node, this will start immediately
 * from the node exclusive.
 */
ydn.structs.Buffer.prototype.traverse = function(func, opt_startValue) {
  // If our tree is empty, return immediately
  if (!this.root_) {
    func(null);
    return;
  }

  // Depth traverse the tree to find node to begin in-order traversal from
  var startNode;
  if (opt_startValue instanceof goog.structs.AvlTree.Node) {
    startNode = opt_startValue;
  } else if (opt_startValue) {
    this.traverse_(function(node) {
      var retNode = null;
      if (this.comparator_(node.value, opt_startValue) > 0) {
        retNode = node.left;
        startNode = node;
      } else if (this.comparator_(node.value, opt_startValue) < 0) {
        retNode = node.right;
      } else {
        startNode = node;
      }
      return retNode; // If null, we'll stop traversing the tree
    });
    if (!startNode) {
      func(null);
      return;
    }
  } else {
    startNode = this.getMinNode_();
  }

  // Traverse the tree and call func on each traversed node's value
  var node = startNode, prev = startNode.left ? startNode.left : startNode;
  while (node != null) {
    if (node.left != null && node.left != prev && node.right != prev) {
      node = node.left;
    } else {
      if (node.right != prev) {
        if (func(node)) {
          return;
        }
      }
      var temp = node;
      node = node.right != null && node.right != prev ?
          node.right :
          node.parent;
      prev = temp;
    }
  }
  func(null); // let know, no more traversal
};


/**
 * Performs a reverse-order traversal of the tree and calls {@code func} with
 * each traversed node, optionally starting from the largest node with a value
 * <= to the specified start value. The traversal ends after traversing the
 * tree's minimum node or when func returns a value that evaluates to true.
 *
 * @param {function(goog.structs.AvlTree.Node)} func Function to call on each
 * traversed node.
 * @param {Object=} opt_startValue If
 * specified, traversal will begin on the node with the smallest
 * value >= opt_startValue. If AvlTree.Node, this will start immediately
 * from the node exclusive.
 */
ydn.structs.Buffer.prototype.reverseTraverse = function(func, opt_startValue) {
  // If our tree is empty, return immediately
  if (!this.root_) {
    func(null);
    return;
  }

  // Depth traverse the tree to find node to begin reverse-order traversal from
  var startNode;
  if (opt_startValue instanceof goog.structs.AvlTree.Node) {
    startNode = opt_startValue;
  } else if (opt_startValue) {
    this.traverse_(goog.bind(function(node) {
      var retNode = null;
      if (this.comparator_(node.value, opt_startValue) > 0) {
        retNode = node.left;
      } else if (this.comparator_(node.value, opt_startValue) < 0) {
        retNode = node.right;
        startNode = node;
      } else {
        startNode = node;
      }
      return retNode; // If null, we'll stop traversing the tree
    }, this));
    if (!startNode) {
      func(null);
      return;
    }
  } else {
    startNode = this.getMaxNode_();
  }

  // Traverse the tree and call func on each traversed node's value
  var node = startNode, prev = startNode.right ? startNode.right : startNode;
  while (node != null) {
    if (node.right != null && node.right != prev && node.left != prev) {
      node = node.right;
    } else {
      if (node.left != prev) {
        if (func(node)) {
          return;
        }
      }
      var temp = node;
      node = node.left != null && node.left != prev ?
          node.left :
          node.parent;
      prev = temp;
    }
  }
  func(null); // let know, no more traversal
};


/**
 * Create a new iterator.
 * @param {ydn.db.KeyRange=} opt_key_range key range.
 * @param {boolean=} opt_reverse reverse iterating.
 * @param {boolean=} opt_unique unique traverse.
 * @return {!ydn.structs.Buffer.Iterator} iterator.
 */
ydn.structs.Buffer.prototype.iter = function(opt_key_range, opt_reverse,
                                        opt_unique) {
  return new ydn.structs.Buffer.Iterator(opt_key_range, opt_reverse,
      opt_unique);
};


/**
 * Key scanning.
 * @param {Function} func node visit callback.
 * @param {!ydn.structs.Buffer.Iterator|!ydn.db.KeyRange} iter iterator or key range.
 */
ydn.structs.Buffer.prototype.scan = function(func, iter) {
  var it = iter instanceof ydn.structs.Buffer.Iterator ? iter : this.iter(iter);

  // If our tree is empty, return immediately
  if (!this.root_) {
    func();
    return;
  }

  var startNode;
  var skip_first_node = false;
  if (it.current_) {
    startNode = it.current_;
  } else if (it.key_range_) {
    var start_value;
    if (it.reverse_ && goog.isDefAndNotNull(it.key_range_.upper)) {
      start_value = it.key_range_.upper;
      skip_first_node = !!it.key_range_.upperOpen;
    } else if (goog.isDefAndNotNull(it.key_range_.lower)) {
      start_value = it.key_range_.lower;
      skip_first_node = !!it.key_range_.lowerOpen;
    }
    this.traverse_(goog.bind(function(node) {
      var retNode = null;
      if (this.comparator_(node.value, start_value) > 0) {
        retNode = node.left;
      } else if (this.comparator_(node.value, start_value) < 0) {
        retNode = node.right;
        startNode = node;
      } else {
        startNode = node;
      }
      return retNode; // If null, we'll stop traversing the tree
    }, this));

  } else if (!startNode) {
    startNode = this.getMaxNode_();
  }
  if (!startNode) {
    it.current_ = null;
    func();
    return;
  }
  var node = startNode;
  var prev;
  // Traverse the tree and call func on each traversed node's value
  if (it.reverse_) {
    prev = startNode.right ? startNode.right : startNode;
    while (node != null) {
      if (node.right != null && node.right != prev && node.left != prev) {
        node = node.right;
      } else {
        if (node.left != prev) {
          it.current_ = node;
          if (skip_first_node) {
            skip_first_node = false;
            continue;
          }
          if (func(node.value)) {
            return;
          }
        }
        var temp = node;
        node = node.left != null && node.left != prev ?
            node.left :
            node.parent;
        prev = temp;
      }
    }
    it.current_ = null;
    func();
  } else {
    prev = startNode.left ? startNode.left : startNode;
    while (node != null) {
      if (node.left != null && node.left != prev && node.right != prev) {
        node = node.left;
      } else {
        if (node.right != prev) {
          it.current_ = node;
          if (skip_first_node) {
            skip_first_node = false;
            continue;
          }
          if (func(node.value)) {
            return;
          }
        }
        var temp = node;
        node = node.right != null && node.right != prev ?
            node.right :
            node.parent;
        prev = temp;
      }
    }
  }
};


/**
 * Union operation with other buffer.
 * @param {ydn.structs.Buffer} other
 */
ydn.structs.Buffer.prototype.union = function(other) {
  var this_node = this.getMinNode_();
  var that_node = other.getMinNode_();
  while (that_node) {

  }
};


/**
 * Iterator for buffer class.
 * @param {ydn.db.KeyRange=} opt_key_range key range.
 * @param {boolean=} opt_reverse reverse iterating.
 * @param {boolean=} opt_unique unique traverse.
 * @constructor
 */
ydn.structs.Buffer.Iterator = function(opt_key_range, opt_reverse, opt_unique) {
  /**
   * @final
   * @private
   * @type {?ydn.db.KeyRange}
   */
  this.key_range_ = opt_key_range || null;
  /**
   * @final
   * @private
   */
  this.reverse_ = !!opt_reverse;
  /**
   * @final
   * @private
   */
  this.unique_ = !!opt_unique;
  this.current_ = null;
};


/**
 * Reverse traversal.
 * @type {ydn.db.KeyRange}
 * @private
 */
ydn.structs.Buffer.Iterator.prototype.key_range_ = null;


/**
 * Reverse traversal.
 * @type {boolean}
 * @private
 */
ydn.structs.Buffer.Iterator.prototype.reverse_ = false;


/**
 * Traverse unique node only.
 * @type {boolean}
 * @private
 */
ydn.structs.Buffer.Iterator.prototype.unique_ = false;


/**
 * Current node.
 * @type {goog.structs.AvlTree.Node}
 * @private
 */
ydn.structs.Buffer.Iterator.prototype.current_ = null;
