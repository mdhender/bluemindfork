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
using System.Configuration;
using System.IO;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using net.bluemind.authentication.api;
using net.bluemind.filehosting.api;

namespace TestCore2client
{
    /// <summary>
    ///This is a test class for FileHostingTest and is intended
    ///to contain all FileHostingTest Unit Tests
    ///</summary>
    [TestClass()]
    public class FileHostingTest
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
        ///A test for Upload file
        ///</summary>
        [TestMethod()]
        public void UploadRandomFileTest()
        {
            var client = new FileHostingClient(ConfigurationManager.AppSettings["url"], _authKey, "bm.lan");

            string tempFile = GetTestFile();
            FileStream fileStream = new FileStream(tempFile, FileMode.Open, FileAccess.Read);

            string path = "toto/" + Path.GetFileName(tempFile);

            client.store(path, fileStream);

            File.Delete(tempFile);
        }

        /// <summary>
        ///A test for Upload file
        ///</summary>
        [TestMethod()]
        [DeploymentItem("resources")]
        public void UploadFileTest()
        {
            var client = new FileHostingClient(ConfigurationManager.AppSettings["url"], _authKey, "bm.lan");

            const string tempFile = @"resources\banana.png";
            FileStream fileStream = new FileStream(tempFile, FileMode.Open, FileAccess.Read);

            client.store(Path.GetFileName(tempFile), fileStream);
        }

        private String GetTestFile()
        {
            string temp = Path.GetTempFileName();

            var rand = new Random((int) DateTime.UtcNow.Ticks);

            byte[] content = new byte[rand.Next(1000, 10000)];
            rand.NextBytes(content);
            
            File.WriteAllBytes(temp, content);
            return temp;
        }
    }
}
