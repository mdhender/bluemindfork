----------------------
BlueMind csharp client
----------------------

---License---
https://www.bluemind.net/licenses-3.5+

---Compatibility---
bm-csharp-client need .net 4.0 at least

---Usage---
Reference core2client.dll and HtmlAgilityPack.dll in your project

Implement a logger
class MyLogger : core2client.BMClient.ILogger
{
    public void LogMessage(string message)
    {
        //use your favorite logger to log all call and response data
    }
}

Sample: get subscribed calendars of user

var auth = new AuthenticationClient("https://bm.test.lan", null);
auth.logger = new MyLogger();
LoginResponse logged = auth.login("john@test.lan", "john-password", "my-client");
if (logged.status == LoginResponseStatus.Ok)
{
    var client = new UserSubscriptionClient("https://bm.test.lan", logged.authKey, logged.authUser.domainUid);
    client.logger = new MyLogger();
    var calendars = client.listSubscriptions(logged.authUser.uid, "calendar");
}

---API usage---
https://forge.bluemind.net/apidoc/
https://forge.bluemind.net/stash/projects/BA/repos/bluemind-samples/browse/csharp-api-examples
 
