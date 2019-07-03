/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

/**
 * @fileoverview Contact synchronization service.
 */

goog.provide('net.bluemind.rrule.VEventUtilsTest');
goog.require('net.bluemind.rrule.VEventUtils');
goog.require('goog.debug.Console');
goog.require('goog.testing.jsunit');

function setUp() {

}

function testReccurenceFreq() {
  var evt = {
    "dtstart" : {
      "iso8601" : new Date(2012, 11, 21).toISOString(), // 21/12/2012
      "timezone" : "Asia/Ho_Chi_Minh"
    },
    "rrule" : {
      frequency : "DAILY",
      "until" : {
        "iso8601" : new Date(2013, 11, 21).toISOString(), // 21/12/2013
        "timezone" : "Asia/Ho_Chi_Minh"
      },
      "interval" : 1
    }
  };

  // DAILY
  var resp = new net.bluemind.rrule.OccurencesHelper().occurenceBetween(evt, new Date(2012, 11, 20), new Date(
      2012, 11, 23));
  // 21 and 22
  assertEquals(2,resp.length);
  assertEquals(21, resp[0].getDate());
  assertEquals(22, resp[1].getDate());
  assertTrue(true);
  
  // HOURLY
  evt.rrule.frequency = "HOURLY";
  resp = new net.bluemind.rrule.OccurencesHelper().occurenceBetween(evt, new Date(2012, 11, 21), new Date(
      2012, 11, 22, 1));
  assertEquals(24,resp.length);
  
  
  // WEEKLY
  evt.rrule.frequency = "WEEKLY";
  resp = new net.bluemind.rrule.OccurencesHelper().occurenceBetween(evt, new Date(2012, 11, 21), new Date(
      2012, 11, 21 + 7 + 7 +1));
  assertEquals(2,resp.length);
  
  // MONTHLY
  evt.rrule.frequency = "MONTHLY";
  resp = new net.bluemind.rrule.OccurencesHelper().occurenceBetween(evt, new Date(2012, 11, 21), new Date(
      2012, 11+2, 21 +1));
  assertEquals(2,resp.length);
  
  //  YEARLY
  evt.rrule.frequency = "YEARLY";
  resp = new net.bluemind.rrule.OccurencesHelper().occurenceBetween(evt, new Date(2012, 11, 21), new Date(
      2012+2, 11, 21 +1));
  assertEquals(2,resp.length);

};

function testReccurenceByXXX() {
  var evt = {
      "dtstart" : {
        "timestamp" : new Date(2012, 11, 21), // 21/12/2012 Friday
        "timezone" : "Asia/Ho_Chi_Minh"
      },
      "rrule" : {
        frequency : "WEEKLY",
        "until" : {
          "iso8601" : new Date(2013, 11, 21).toISOString(), // 21/12/2013
          "timezone" : "Asia/Ho_Chi_Minh"
        },
        "interval" : 1
      }
    };

  // byHour
  evt.rrule.byHour = [1, 12, 14];
  var resp = new net.bluemind.rrule.OccurencesHelper().occurenceBetween(evt, new Date(2012, 11, 21), new Date(
      2012, 11, 22, 1));
  assertEquals(3,resp.length);
  
  // byweekday
  evt.rrule.byHour = null;
  evt.rrule.byDay = [{"day":"MO"}, { "day":"WE"}];
  resp = new net.bluemind.rrule.OccurencesHelper().occurenceBetween(evt, new Date(2012, 11, 21), new Date(
      2012, 11, 21+7, 1));
  assertEquals(2,resp.length);
  
  //bymonthday
  evt.rrule.byDay = null;
  evt.rrule.byMonthDay = [22];
  resp = new net.bluemind.rrule.OccurencesHelper().occurenceBetween(evt, new Date(2012, 11, 21), new Date(
      2012, 11+1, 22+1, 1));
  assertEquals(2,resp.length);
  
  // byyearday
  evt.rrule.byMonthDay = null;
  evt.rrule.byYearDay = [ 5, 20 , 200];
  resp = new net.bluemind.rrule.OccurencesHelper().occurenceBetween(evt, new Date(2012, 11, 21), new Date(
      2012+1, 11, 22, 1));
  assertEquals(3,resp.length);
  
  // byWeekNo
  evt.rrule.byYearDay = null;
  evt.rrule.byDay = [{"day":"MO"}];
  evt.rrule.byWeekNo = [1,5,53];
  resp = new net.bluemind.rrule.OccurencesHelper().occurenceBetween(evt, new Date(2012, 11, 21), new Date(
      2012+1, 11+1, 22, 1));
  assertEquals(3,resp.length);
  
  // bymonth
  evt.rrule.byWeekNo = null;
  evt.rrule.byDay = [{"day":"MO"}];
  evt.rrule.byMonth = [1];
  resp = new net.bluemind.rrule.OccurencesHelper().occurenceBetween(evt, new Date(2012, 11, 21), new Date(
      2012+1, 11, 22, 1));
  
  // 4 mondays of january 2013
  assertEquals(4,resp.length);
  
}
