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
using System.ComponentModel.Design.Serialization;
using core2client;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using net.bluemind.authentication.api;

namespace TestCore2client
{
    
    
    /// <summary>
    ///This is a test class for AuthenticationTest and is intended
    ///to contain all AuthenticationTest Unit Tests
    ///</summary>
    [TestClass()]
    public class AuthenticationTest
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
        ///A test for Login Logout
        ///</summary>
        [TestMethod()]
        public void LoginLogoutTest()
        {
            var auth = new AuthenticationClient("https://trusty.bm.lan", null);
            LoginResponse token = auth.login("admin@bm.lan", "admin", "unit-csharp");

            Assert.IsNotNull(token);
            Assert.AreEqual(LoginResponseStatus.Ok, token.status);
            Assert.IsNotNull(token.authKey);

            auth.logout();
        }

        public class MyLogger : BMClient.ILogger
        {
            public void LogMessage(string message)
            {
                Console.WriteLine("zz");
            }
        }
    }
}
