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
using Microsoft.VisualStudio.TestTools.UnitTesting;
using net.bluemind.authentication.api;
using net.bluemind.core.container.api;
using net.bluemind.core.container.model;
using net.bluemind.core.container.model.acl;
using net.bluemind.user.api;

namespace TestCore2client
{
    
    
    /// <summary>
    ///This is a test class for ContainerTest and is intended
    ///to contain all ContainerTest Unit Tests
    ///</summary>
    [TestClass()]
    public class ContainerTest
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
        ///A test for All
        ///</summary>
        [TestMethod()]
        public void AllTest()
        {
            var containers = new ContainersClient(ConfigurationManager.AppSettings["url"], _authKey);
            var query = new ContainerQuery();
            query.type = "addressbook";
            query.verb = null;
            List<ContainerDescriptor> all = containers.all(query);
            Assert.IsNotNull(all);
            bool personnalFound = false;
            foreach (ContainerDescriptor descriptor in all)
            {
                if (descriptor.defaultContainer && descriptor.writable == true && descriptor.name == "contacts"
                    && descriptor.owner == ConfigurationManager.AppSettings["login"].Split('@')[0])
                    personnalFound = true;
            }
            Assert.IsTrue(personnalFound, "personnal addressbook not found");
        }

        /// <summary>
        ///A test for ListSubscription
        ///</summary>
        [TestMethod()]
        public void ListSubscriptionTest()
        {
            var containers = new UserSubscriptionClient(ConfigurationManager.AppSettings["url"], _authKey,
                ConfigurationManager.AppSettings["login"].Split('@')[0]);
            List<ContainerSubscriptionDescriptor> subs =
                containers.listSubscriptions(ConfigurationManager.AppSettings["login"].Split('@')[1], "addressbook");
            Assert.IsNotNull(subs);
            bool personnalFound = false;
            foreach (ContainerSubscriptionDescriptor descriptor in subs)
            {
                if (descriptor.defaultContainer && descriptor.name == "contacts"
                    && descriptor.owner == ConfigurationManager.AppSettings["login"].Split('@')[0])
                    personnalFound = true;
            }
            Assert.IsTrue(personnalFound, "personnal addressbook not found");
        }

        /// <summary>
        ///A test for TodoList
        ///</summary>
        [TestMethod()]
        public void TodoListTest()
        {
            var containers = new ContainersClient(ConfigurationManager.AppSettings["url"], _authKey);
            var query = new ContainerQuery();
            query.type = "todolist";
            query.verb = null;
            List<ContainerDescriptor> all = containers.all(query);
            Assert.IsNotNull(all);
        }

        /// <summary>
        ///A test for Verb
        ///</summary>
        [TestMethod()]
        public void VerbTest()
        {
            Verb v = Verb.Write;
            Console.WriteLine("VERB: " + v);
        }
    }
}
