using core2client;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using net.bluemind.core.api.date;

namespace TestCore2client
{
    
    
    /// <summary>
    ///This is a test class for DateUtilsTest and is intended
    ///to contain all DateUtilsTest Unit Tests
    ///</summary>
    [TestClass()]
    public class DateUtilsTest
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
        ///A test for SystemDateTimeToBmDateTime
        ///</summary>
        [TestMethod()]
        public void SystemDateTimeToBmDateTimeTest()
        {
            DateTime sysDateTime = new DateTime(2020, 12, 25, 15, 0, 0, DateTimeKind.Utc);
            BmDateTimePrecision precision = BmDateTimePrecision.DateTime;
            string timeZone = "Europe/Istanbul";
           
            BmDateTime actual = DateUtils.SystemDateTimeToBmDateTime(sysDateTime, precision, timeZone);
            Assert.AreEqual("2020-12-25T18:00:00.000+03:00", actual.iso8601);
            Assert.AreEqual("Europe/Istanbul", actual.timezone);
        }
    }
}
