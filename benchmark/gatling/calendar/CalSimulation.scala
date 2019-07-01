package net.bluemind.gatlingtest
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.http.HeaderNames._
import scala.concurrent.duration._

class CalSimulation extends Simulation {

  val ipAddress:String = System.getProperty("ipAddress")
  val nbUsers:String = System.getProperty("nbUsers")
  val duration:String = System.getProperty("duration")

  val httpConf = http
    .baseURL("https://"+ipAddress)
    .acceptHeader("image/png,image/*;q=0.8,*/*;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3")
    .connection("keep-alive")

  val headers_3 = Map(
    "Accept" -> """text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8""",
    "Content-Type" -> """application/x-www-form-urlencoded""")

  // Chaine de connection
  val connect = exec(http("access_homepage")
    .get("https://"+ipAddress+"/"))
    .pause(14)
    .exec(http("sso_ok")
      .post("/bluemind_sso_security")
      .headers(headers_3)
      .param("""login""", """user.test@numergy.lan""")
      .param("""password""", """bluemind""")
      .param("""priv""", """priv""")
      .param("""storedRequestId""", """X""")
      .param("""submit""", """Se connecter"""))
    .pause(569 milliseconds)
    .exec(http("getwebmail")
      .get("/webmail/")
      .check(regex("""<span>user toto</span>""").exists)
      .check(regex("""request_token\":\"([a-z][0-9]*)\"\}""").saveAs("userToken")))

  val timestamp: Long = System.currentTimeMillis()

  val ping = exec(http("ping")
    .post("https://"+ipAddress+"/cal/calendar/ping"))
    .pause(5)

  val bmc_sync = exec(http("bmc_sync")
    .post("https://"+ipAddress+"/cal/calendar/bmc")
    .param("service", "calendar")
    .param("method", "doSync")
    .param("updated", "[]")
    .param("removed", "[]")
    .param("calendars", """[{"calendar":3,"lastSync":""" + timestamp + ""","mode":"CHANGED_AFTER_DATE"}]""")
    .param("notification", "[]")
    .param("alerts", "[]")
    .check(status.is(200)))
    .pause(30)

  val get_calendar = exec(http("get_calendar")
    .post("https://"+ipAddress+"/cal/calendar/bmc")
    .param("service", "user")
    .param("method", "getCalendars")
    .check(status.is(200)))
    .pause(60)

  val get_mail = exec(http("get_mail")
    .post("https://"+ipAddress+"/cal/calendar/bmc")
    .param("service", "user")
    .param("method", "getUnreadMailCount")
    .check(status.is(200)))
    .pause(60)

  val scn = scenario("Calendar idle")
    .exec(connect)
    .exec(repeat(24) { ping }, repeat(4) { bmc_sync }, repeat(2) { get_mail })

  setUp(scn.inject(rampUsers(nbUsers.toInt) over (duration.toInt seconds))).protocols(httpConf)

}
