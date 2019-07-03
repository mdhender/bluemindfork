/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
using System;
using System.Collections.Generic;
using System.Configuration;
using System.IO;
using System.Web.Script.Serialization;
using core2client;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using net.bluemind.authentication.api;
using net.bluemind.calendar.api;
using net.bluemind.core.api;
using net.bluemind.core.api.date;
using net.bluemind.core.container.api;
using net.bluemind.core.container.model;
using net.bluemind.core.container.model.acl;
using net.bluemind.icalendar.api;

namespace TestCore2client
{
    /// <summary>
    ///This is a test class for CalendarTest and is intended
    ///to contain all CalendarTest Unit Tests
    ///</summary>
    [TestClass()]
    public class CalendarTest
    {
        private static AuthenticationClient _authentication;
        private static String _authKey;

        /// <summary>
        ///Gets or sets the test context which provides
        ///information about and functionality for the current test run.
        ///</summary>
        public TestContext TestContext { get; set; }

        #region Additional test attributes
        // 
        //You can use the following additional attributes as you write your tests:
        //
        //Use ClassInitialize to run code before running the first test in the class
        [ClassInitialize()]
        public static void MyClassInitialize(TestContext testContext)
        {
            _authentication = new AuthenticationClient(ConfigurationManager.AppSettings["url"], null);
            LoginResponse token = _authentication.login(ConfigurationManager.AppSettings["login"],
                                                        ConfigurationManager.AppSettings["pass"], "unit-csharp");
            Assert.IsNotNull(token);
            Assert.IsNotNull(token.authKey);
            _authKey = token.authKey;
        }
        //
        //Use ClassCleanup to run code after all tests in a class have run
        [ClassCleanup()]
        public static void MyClassCleanup()
        {
            _authentication.logout();
        }
        //
        //Use TestInitialize to run code before running each test
        //[TestInitialize()]
        //public void MyTestInitialize()
        //{
        //}
        //
        //Use TestCleanup to run code after each test has run
        //[TestCleanup()]
        //public void MyTestCleanup()
        //{
        //}
        //
        #endregion

        /// <summary>
        ///A test for Crud
        ///</summary>
        [TestMethod()]
        public void CrudTest()
        {
            var calendar = new CalendarClient(ConfigurationManager.AppSettings["url"], _authKey, ConfigurationManager.AppSettings["calendar"]);

            var serie = new VEventSeries();
            var ev = DefaultVEvent();
            var uid = Guid.NewGuid().ToString();
            ev.transparency = VEventTransparency.Opaque;
            serie.main = ev;
            serie.properties.Add("stringcolor", "black");
            serie.properties.Add("hy", "calendar");
            calendar.create(uid, serie, false);

            ItemValue<VEventSeries> sev = calendar.getComplete(uid);
            Assert.IsNotNull(sev);
            Assert.AreEqual(uid, sev.uid);
            Assert.AreEqual(ev.transparency, sev.value.main.transparency);
            Assert.AreEqual("black", serie.properties["stringcolor"]);
            
            calendar.delete(uid, false);
        }

        /// <summary>
        ///A test for Crud with attendees
        ///</summary>
        [TestMethod()]
        public void CrudAttendeesTest()
        {
            var calendar = new CalendarClient(ConfigurationManager.AppSettings["url"], _authKey, ConfigurationManager.AppSettings["calendar"]);

            var serie = new VEventSeries();
            var ev = DefaultVEvent();
            var uid = Guid.NewGuid().ToString();
            ev.transparency = VEventTransparency.Opaque;
            ev.organizer = new ICalendarElementOrganizer {mailto = "bob@bm.lan"};
            ev.attendees.Add(new ICalendarElementAttendee
            {
                cutype = ICalendarElementCUType.Individual,
                role = ICalendarElementRole.RequiredParticipant,
                mailto = "zz@bm.lan"
            });
            serie.main = ev;

            calendar.create(uid, serie, true);

            ItemValue<VEventSeries> sev = calendar.getComplete(uid);
            Assert.IsNotNull(sev);
            Assert.AreEqual(uid, sev.uid);
            Assert.AreEqual(ev.organizer.mailto, sev.value.main.organizer.mailto);
            Assert.AreEqual(1, sev.value.main.attendees.Count);

            //calendar.delete(uid, false);
        }


        /// <summary>
        ///A test for Crud with attendees
        ///</summary>
        [TestMethod()]
        public void ResetTest()
        {
            var calendar = new CalendarClient(ConfigurationManager.AppSettings["url"], _authKey, ConfigurationManager.AppSettings["calendar"]);

            calendar.reset();

            ListResult<ItemValue<VEventSeries>> sev = calendar.list();
            Assert.IsNotNull(sev);
            Assert.AreEqual(0, sev.total);
        }

        ///A test for FindMe
        ///</summary>
        [TestMethod()]
        public void FindRecipientsNoEmail()
        {
            var containersClient = new ContainersClient(ConfigurationManager.AppSettings["url"], "d787e73c-83dd-4913-bc94-973dc3a70de6");
            List<ContainerDescriptor> cals =
                containersClient.all(new ContainerQuery
                {
                    type = "calendar",
                    verb = new List<Verb> {Verb.Read}
                });
            foreach (ContainerDescriptor cal in cals)
            {
                if (cal.defaultContainer)
                {
                    var calClient = new CalendarClient(ConfigurationManager.AppSettings["url"],
                        "d787e73c-83dd-4913-bc94-973dc3a70de6", cal.uid);
                    foreach (string uid in calClient.all())
                    {
                        ItemValue<VEventSeries> ev = calClient.getComplete(uid);
                        Console.WriteLine(String.Format("[{0}] VEVENT: {1} atts:{2}", ev.value.main.summary, ev.value.main.attendees.Count));
                    }
                }
            }
        }

        /// <summary>
        ///A test for JSON deserialization
        ///</summary>
        [TestMethod()]
        [DeploymentItem("resources")]
        public void DeserializeJsonTest()
        {
            String json = File.ReadAllText(@"resources\domino-vevent.json");

            Console.WriteLine(json);

            var ret = new JavaScriptSerializer().Deserialize<ItemValue<VEvent>>(json);

            Assert.IsNotNull(ret);
        }

        private static VEvent DefaultVEvent()
        {
            DateTime now = DateTime.Now.AddHours(1);
            var ev = new VEvent();
            ev.summary = "test-event-" + DateTime.Now.Ticks;
            var dtstart = DateUtils.SystemDateTimeToBmDateTime(now, BmDateTimePrecision.DateTime, "Europe/Paris");

            var dtend = DateUtils.SystemDateTimeToBmDateTime(now.AddHours(1), BmDateTimePrecision.DateTime,
                "Europe/Paris");

            ev.dtstart = dtstart;
            ev.dtend = dtend;

            ev.rrule = null;
            ev.exdate = null;

            ev.alarm = null;

            return ev;
        }
    }
}
