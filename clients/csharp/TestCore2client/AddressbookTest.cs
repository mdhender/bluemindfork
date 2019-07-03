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
using System.Threading;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using net.bluemind.addressbook.api;
using net.bluemind.authentication.api;
using net.bluemind.core.api;
using net.bluemind.core.container.model;

namespace TestCore2client
{
    /// <summary>
    ///This is a test class for AddressbookTest and is intended
    ///to contain all AddressbookTest Unit Tests
    ///</summary>
    [TestClass()]
    public class AddressbookTest
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
            var book = new AddressBookClient(ConfigurationManager.AppSettings["url"], _authKey, ConfigurationManager.AppSettings["addressbook"]);

            String uid = Guid.NewGuid().ToString();
            var card = DefaultVCard();
            book.create(uid, card);

            ItemValue<VCard> res = book.getComplete(uid);
            Assert.IsNotNull(res);
            Assert.AreEqual(uid, res.uid);

            card.explanatory = new VCardExplanatory();
            card.explanatory.note = "da note";

            book.update(uid, card);
            res = book.getComplete(uid);
            Assert.IsNotNull(res);
            Assert.AreEqual(uid, res.uid);
            Assert.AreEqual(card.explanatory.note, res.value.explanatory.note);

            book.delete(uid);

            res = book.getComplete(uid);
            Assert.IsNull(res);
        }

        private static VCard DefaultVCard()
        {
            var card = new VCard();
            card.kind = VCardKind.individual;
            card.identification.formatedName = new VCardIdentificationFormatedName {value = "Jean Pierre"};
            var email = new VCardCommunicationsEmail
            {
                value = "jean.pierre@marcel.fr",
                parameters = new List<VCardParameter> {new VCardParameter {label = "TYPE", value = "WORK"}}
            };
            card.communications.emails.Add(email);
            return card;
        }

        /// <summary>
        ///A test for AllUids
        ///</summary>
        [TestMethod()]
        public void AllUidsTest()
        {
            var book = new AddressBookClient(ConfigurationManager.AppSettings["url"], _authKey, ConfigurationManager.AppSettings["addressbook"]);
            String uid = Guid.NewGuid().ToString();
            var card = DefaultVCard();
            book.create(uid, card);

            List<String> uids = book.allUids();
            Assert.IsNotNull(uids);
            Assert.IsTrue(uids.Count > 0);
            Assert.IsTrue(uids.Contains(uid));

            book.delete(uid);
        }

        /// <summary>
        ///A test for ChangeSet
        ///</summary>
        [TestMethod()]
        public void ChangeSetTest()
        {
            var book = new AddressBookClient(ConfigurationManager.AppSettings["url"], _authKey, ConfigurationManager.AppSettings["addressbook"]);

            ContainerChangeset<String> changeset = book.changeset(0);
            Assert.IsNotNull(changeset);
            long? currentVersion = changeset.version;

            String uid = Guid.NewGuid().ToString();
            var card = DefaultVCard();
            book.create(uid, card);

            changeset = book.changeset(currentVersion);
            Assert.IsNotNull(changeset);
            Assert.IsTrue(changeset.version > currentVersion);
            Assert.AreEqual(1, changeset.created.Count);
            Assert.AreEqual(uid, changeset.created[0]);

            book.delete(uid);
        }

        /// <summary>
        ///A test for ChangeLog
        ///</summary>
        [TestMethod()]
        public void ChangeLogTest()
        {
            var book = new AddressBookClient(ConfigurationManager.AppSettings["url"], _authKey, ConfigurationManager.AppSettings["addressbook"]);

            ContainerChangeset<String> changeset = book.changeset(0);
            Assert.IsNotNull(changeset);
            long? currentVersion = changeset.version;

            String uid = Guid.NewGuid().ToString();
            var card = DefaultVCard();
            book.create(uid, card);

            ContainerChangelog changelog = book.containerChangelog(currentVersion + 1);
            Assert.IsNotNull(changelog);
            Assert.AreEqual(1, changelog.entries.Count);
            Assert.AreEqual(uid, changelog.entries[0].itemUid);
            Assert.AreEqual(ChangeLogEntryType.Created, changelog.entries[0].type);

            book.delete(uid);
        }

        /// <summary>
        ///A test for GetInfo
        ///</summary>
        [TestMethod()]
        public void GetInfoTest()
        {
            var book = new AddressBookClient(ConfigurationManager.AppSettings["url"], _authKey, ConfigurationManager.AppSettings["addressbook"]);

            String uid = Guid.NewGuid().ToString();
            var card = DefaultVCard();
            book.create(uid, card);

            ItemValue<VCardInfo> info = book.getInfo(uid);
            Assert.IsNotNull(info);
            Assert.AreEqual(VCardKind.individual, info.value.kind);

            book.delete(uid);
        }

        /// <summary>
        ///A test for MultipleGet
        ///</summary>
        [TestMethod()]
        public void MultipleGetTest()
        {
            var book = new AddressBookClient(ConfigurationManager.AppSettings["url"], _authKey, ConfigurationManager.AppSettings["addressbook"]);

            String uid1 = Guid.NewGuid().ToString();
            var card1 = DefaultVCard();
            book.create(uid1, card1);

            String uid2 = Guid.NewGuid().ToString();
            var card2 = DefaultVCard();
            book.create(uid2, card2);

            List<ItemValue<VCard>> items = book.multipleGet(new List<string> { uid1, uid2 });
            Assert.IsNotNull(items);
            Assert.AreEqual(2, items.Count);

            book.delete(uid1);
            book.delete(uid2);
        }

        /// <summary>
        ///A test for MultipleUpdates
        ///</summary>
        [TestMethod()]
        public void MultipleUpdatesTest()
        {
            var book = new AddressBookClient(ConfigurationManager.AppSettings["url"], _authKey, ConfigurationManager.AppSettings["addressbook"]);

            String uid1 = Guid.NewGuid().ToString();
            var card1 = DefaultVCard();

            String uid2 = Guid.NewGuid().ToString();
            var card2 = DefaultVCard();

            var changes = new VCardChanges();
            changes.add.Add(new VCardChangesItemAdd {uid = uid1, value = card1});
            changes.modify.Add(new VCardChangesItemModify {uid = uid2, value = card2});
            book.updates(changes);

            List<ItemValue<VCard>> items = book.multipleGet(new List<string> { uid1, uid2 });
            Assert.IsNotNull(items);
            Assert.AreEqual(2, items.Count);

            changes = new VCardChanges();
            changes.delete.Add(new VCardChangesItemDelete {uid = uid1});
            changes.delete.Add(new VCardChangesItemDelete {uid = uid2});
            book.updates(changes);

            items = book.multipleGet(new List<string> { uid1, uid2 });
            Assert.IsNotNull(items);
            Assert.AreEqual(0, items.Count);
        }

        /// <summary>
        ///A test for Search
        ///</summary>
        [TestMethod()]
        public void SearchTest()
        {
            var book = new AddressBookClient(ConfigurationManager.AppSettings["url"], _authKey, ConfigurationManager.AppSettings["addressbook"]);

            String uid1 = Guid.NewGuid().ToString();
            var card1 = DefaultVCard();
            card1.identification.nickname = new VCardIdentificationNickname {value = "bob" + DateTime.Now.Ticks};
            book.create(uid1, card1);

            // wait for indexing
            Thread.Sleep(1000); 

            var query = new VCardQuery
            {
                size = -1,
                query = "value.identification.nickname.value:" + card1.identification.nickname.value
            };
            ListResult<ItemValue<VCardInfo>> found = book.search(query);

            Assert.IsNotNull(found);
            Assert.AreEqual(1, found.total);
            Assert.AreEqual(uid1, found.values[0].uid);
            Assert.AreEqual(VCardKind.individual, found.values[0].value.kind);

            book.delete(uid1);
        }
    }
}
