/**
 * Copyright (c) 2011, Sun Ning.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

/**
 * @fileoverview Geohash.
 *
 * http://en.wikipedia.org/wiki/Geohash
 */

// https://github.com/sunng87/node-geohash.git

goog.provide('ydn.geohash');


/**
 * @const
 * @type {string}
 */
ydn.geohash.BASE32_CODES = '0123456789bcdefghjkmnpqrstuvwxyz';


/**
 * Encode coordinate into hash.
 * @param {Array.<number>} args an array representing [latitude,
 * longitude, opt_precision]
 * Latitude starts from -90 to 90.
 * Longitude starts from -180 to 180.
 * Precision is number of char to use for encoding. Default to 12 char. One char
 * encode 4 bits of latitude and longitude pair. The error is, thus,
 * 1 / Math.pow(2, 4 * precision/2) * 180. For default precision of 12 char,
 * the error is 0.0000107288 or 1.190 meter at equator.
 * @return {string}
 */
ydn.geohash.encode = function(args) {
  // NOTE: asm annotation for double and int.
  var lat = +args[0];
  var lon = +args[1];
  var numberOfChars = (args[2] || 12) | 0;
  var chars = [];

  var maxlat = 90;
  var minlat = -90;
  var maxlon = 180;
  var minlon = -180;

  var mid;
  var islon = true; // the first bit is longitude
  for (var i = 0; i < numberOfChars; i++) {
    var hash_value = 0;
    for (var bits = 0; bits < 5; bits++) {
      if (islon) {
        mid = (maxlon + minlon) / 2;
        if (lon > mid) {
          hash_value = (hash_value << 1) + 1;
          minlon = mid;
        } else {
          hash_value = (hash_value << 1) + 0;
          maxlon = mid;
        }
      } else {
        mid = (maxlat + minlat) / 2;
        if (lat > mid) {
          hash_value = (hash_value << 1) + 1;
          minlat = mid;
        } else {
          hash_value = (hash_value << 1) + 0;
          maxlat = mid;
        }
      }
      islon = !islon; // alternate longitude and longitude
    }

    chars[i] = ydn.geohash.BASE32_CODES[hash_value];
  }

  return chars.join('');
};


/**
 * Decode box hash.
 * @param {string} hash_string geohash in lower case.
 * @return {Array.<number>} [minlat, minlon, maxlat, maxlon]
 * @private
 */
ydn.geohash.decodeBbox_ = function(hash_string) {
  var islon = true;
  var maxlat = 90, minlat = -90;
  var maxlon = 180, minlon = -180;

  var hash_value = 0;
  for (var i = 0, l = hash_string.length; i < l; i++) {
    var code = hash_string.charAt(i);
    hash_value = ydn.geohash.BASE32_CODES.indexOf(code);

    for (var bits = 4; bits >= 0; bits--) {
      var bit = (hash_value >> bits) & 1;
      if (islon) {
        var mid = (maxlon + minlon) / 2;
        if (bit == 1) {
          minlon = mid;
        } else {
          maxlon = mid;
        }
      } else {
        var mid = (maxlat + minlat) / 2;
        if (bit == 1) {
          minlat = mid;
        } else {
          maxlat = mid;
        }
      }
      islon = !islon;
    }
  }
  return [+minlat, +minlon, +maxlat, +maxlon];
};


/**
 * Decode geo hash string into coordinate.
 * @param {string} hash_string
 * @return {Array.<number>} [latitude, longitude: number,
 *   error_latitude, error_longitude]
 * }}
 */
ydn.geohash.decode = function(hash_string) {
  var bbox = ydn.geohash.decodeBbox_(hash_string);
  var lat = (bbox[0] + bbox[2]) / 2;
  var lon = (bbox[1] + bbox[3]) / 2;
  var laterr = bbox[2] - lat;
  var lonerr = bbox[3] - lon;
  return [+lat, +lon, +laterr, +lonerr];
};


/**
 * Unit rectangular vector for relative direction representation.
 * @enum {Array}
 */
ydn.geohash.Direction = {
  NORTH: [1, 0],
  NORTH_EAST: [1, 1],
  NORTH_WEST: [1, -1],
  EAST: [0, 1],
  SOUTH: [-1, 0],
  SOUTH_EAST: [0, 1],
  SOUTH_WEST: [-1, -1],
  WEST: [0, -1]
};


/**
 * Return neighbor geohash of a given center position.
 * @param {string} hashstring
 * @param {ydn.geohash.Direction} direction Relative direction.
 * @return {string} neighbor geohash.
 */
ydn.geohash.neighbor = function(hashstring, direction) {
  var lonlat = ydn.geohash.decode(hashstring);
  var lat = lonlat[0];
  var lon = lonlat[1];
  var lat_err = lonlat[2];
  var lon_err = lonlat[3];
  var neighbor_lat = lat + direction[0] * lat_err * 2;
  var neighbor_lon = lon + direction[1] * lon_err * 2;
  return ydn.geohash.encode([neighbor_lat, neighbor_lon, hashstring.length]);
};


