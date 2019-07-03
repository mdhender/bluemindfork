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
using System.IO;
using core2client;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using net.bluemind.addressbook.api;
using net.bluemind.authentication.api;
using net.bluemind.calendar.api;
using net.bluemind.core.api;
using net.bluemind.core.api.date;
using net.bluemind.icalendar.api;
using net.bluemind.mailbox.api;
using net.bluemind.user.api;

namespace TestCore2client
{
    /// <summary>
    ///This is a test class for HashTest and is intended
    ///to contain all HashTest Unit Tests
    ///</summary>
    [TestClass()]
    public class HashTest
    {
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
        //[ClassInitialize()]
        //public static void MyClassInitialize(TestContext testContext)
        //{
        //}
        ////
        ////Use ClassCleanup to run code after all tests in a class have run
        //[ClassCleanup()]
        //public static void MyClassCleanup()
        //{
        //}
        ////
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
        /// A test for Crud
        ///</summary>
        [TestMethod()]
        public void GetHashTest()
        {
            var ev = DefaultVEvent();

            ev.attendees.Add(new ICalendarElementAttendee
            {
                mailto = "attendee1@mail.fr",
                partStatus = ICalendarElementParticipationStatus.NeedsAction,
                role = ICalendarElementRole.RequiredParticipant
            });
            ev.attendees.Add(new ICalendarElementAttendee
            {
                mailto = "attendee2@mail.fr",
                partStatus = ICalendarElementParticipationStatus.Accepted,
                role = ICalendarElementRole.RequiredParticipant
            });

            int hash1 = ev.GetHashCode();
            Console.WriteLine("HashCode: " + hash1);

            ev.attendees.Reverse();
            int hash2 = ev.GetHashCode();
            Console.WriteLine("HashCode: " + hash2);
            Assert.AreEqual(hash1, hash2);

            ev.summary += "-updated";
            int hash3 = ev.GetHashCode();
            Console.WriteLine("HashCode: " + hash3);
            Assert.AreNotEqual(hash1, hash3);

            ev.attendees.Add(new ICalendarElementAttendee
            {
                mailto = "attendee3@mail.fr",
                partStatus = ICalendarElementParticipationStatus.Accepted,
                role = ICalendarElementRole.OptionalParticipant
            });
            int hash4 = ev.GetHashCode();
            Console.WriteLine("HashCode: " + hash4);
            Assert.AreNotEqual(hash1, hash4);
        }

        /// <summary>
        /// A test for ServerVsOutlookTest
        /// </summary>
        [TestMethod()]
        [DeploymentItem("resources")]
        public void ServerVsOutlookTest()
        {
            VEvent server = BmUtils.JsonToObject<VEvent>(File.ReadAllText(@"resources\server-vevent.json"));
            VEvent client = BmUtils.JsonToObject<VEvent>(File.ReadAllText(@"resources\client-vevent.json"));

            int hashServer = server.GetHashCode();
            Console.WriteLine("Hash server: " + hashServer);
            int hashClient = client.GetHashCode();
            Console.WriteLine("Hash client: " + hashClient);

            Assert.AreEqual(hashServer, hashClient);
        }

        /// <summary>
        /// A test for ServerVsOutlookAfterAcceptTest
        /// </summary>
        [TestMethod()]
        [DeploymentItem("resources")]
        public void ServerVsOutlookAfterAcceptTest()
        {
            VEvent server = BmUtils.JsonToObject<VEvent>(File.ReadAllText(@"resources\server-vevent-accept.json"));
            VEvent client = BmUtils.JsonToObject<VEvent>(File.ReadAllText(@"resources\client-vevent-accept.json"));

            foreach (ICalendarElementAttendee attendee in server.attendees)
            {
                attendee.IgnoreStatusInHashCode = true;
            }
            foreach (ICalendarElementAttendee attendee in client.attendees)
            {
                attendee.IgnoreStatusInHashCode = true;
            }

            int hashServer = server.GetHashCode();
            Console.WriteLine("Hash server: " + hashServer);
            int hashClient = client.GetHashCode();
            Console.WriteLine("Hash client: " + hashClient);

            Assert.AreEqual(hashServer, hashClient);
        }

        /// <summary>
        /// A test for ServerVsOutlookAfterDeclineTest
        /// </summary>
        [TestMethod()]
        [DeploymentItem("resources")]
        public void ServerVsOutlookAfterDeclineTest()
        {
            VEvent server = BmUtils.JsonToObject<VEvent>(File.ReadAllText(@"resources\server-vevent-decline.json"));
            VEvent client = BmUtils.JsonToObject<VEvent>(File.ReadAllText(@"resources\client-vevent-decline.json"));

            foreach (ICalendarElementAttendee attendee in server.attendees)
            {
                attendee.IgnoreStatusInHashCode = true;
            }
            foreach (ICalendarElementAttendee attendee in client.attendees)
            {
                attendee.IgnoreStatusInHashCode = true;
            }

            int hashServer = server.GetHashCode();
            Console.WriteLine("Hash server: " + hashServer);
            int hashClient = client.GetHashCode();
            Console.WriteLine("Hash client: " + hashClient);

            Assert.AreEqual(hashServer, hashClient);
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

        /// <summary>
        /// A test for ServerVsOutlookAfterDeclineTest
        /// </summary>
        [TestMethod()]
        public void Populate()
        {
            AuthenticationClient auth = new AuthenticationClient("https://mapi.bm.lan", null);
            LoginResponse logged;
            try
            {
                logged = auth.login("admin0@global.virt", "admin", "csharp-populate");
            }
            catch (Exception)
            {
                logged = auth.login("admin0@global.virt", "admin", "csharp-populate");
            }

            UserClient client = new UserClient("https://mapi.bm.lan", logged.authKey, "bm.lan");

            for (int i = 0; i < 20; i++)
            {
                User user = getUser(i);
                String uid = user.login;
                Console.WriteLine("--");
                client.create(uid, user);
            }
        }

        private User getUser(int i)
        {
            User u = new User();
            u.login = "bob" + i;
            u.password = "bob";
            u.routing = MailboxRouting.@internal;

            u.emails = new List<Email>{new Email {address = u.login + "@bm.lan", allAliases = false, isDefault = true}};
            VCard card = new VCard();
            card.identification.name = new VCardIdentificationName {familyNames = "bm.lan", givenNames = u.login};
            card.identification.formatedName = new VCardIdentificationFormatedName {value = u.login};

            u.contactInfos = card;
            return u;
        }
    }
}
