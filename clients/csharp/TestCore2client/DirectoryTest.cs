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
using System.ComponentModel.Design.Serialization;
using System.Configuration;
using core2client;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using net.bluemind.authentication.api;
using net.bluemind.core.api;
using net.bluemind.core.container.model;
using net.bluemind.directory.api;
using net.bluemind.domain.api;

namespace TestCore2client
{
    
    
    /// <summary>
    ///This is a test class for DirectoryTest and is intended
    ///to contain all DirectoryTest Unit Tests
    ///</summary>
    [TestClass()]
    public class DirectoryTest
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
        //
        //Use ClassCleanup to run code after all tests in a class have run
        //[ClassCleanup()]
        //public static void MyClassCleanup()
        //{
        //}
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
        ///A test for Get users from domain
        ///</summary>
        [TestMethod()]
        public void GetUsersFromDomainTest()
        {
            var login = new AuthenticationClient(ConfigurationManager.AppSettings["url"], null);
            LoginResponse lr = login.login(ConfigurationManager.AppSettings["login"], ConfigurationManager.AppSettings["pass"],
                "GetUsersFromDomainTest");
            Assert.AreEqual(LoginResponseStatus.Ok, lr.status);

            var dc = new DomainsClient(ConfigurationManager.AppSettings["url"], lr.authKey);
            dc.logger = new MyLogger();
            ItemValue<Domain> domain = dc.findByNameOrAliases(ConfigurationManager.AppSettings["login"].Split('@')[1]);

            var dir = new DirectoryClient(ConfigurationManager.AppSettings["url"], lr.authKey, domain.uid);
            dir.logger = new MyLogger();
            ListResult<ItemValue<DirEntry>> entries =
                dir.search(new DirEntryQuery
                {
                    order = new DirEntryQueryOrder { by = DirEntryQueryOrderBy.displayname, dir = DirEntryQueryDir.asc },
                    kindsFilter = new List<BaseDirEntryKind> { BaseDirEntryKind.USER },
                    entries = null,
                    entryUidFilter = null
                });

            Assert.IsNotNull(entries.values);
            Assert.IsTrue(entries.values.Count > 0);
        }

        public class MyLogger : BMClient.ILogger
        {
            public void LogMessage(string message)
            {
                Console.WriteLine(message);
            }
        }
    }
}
