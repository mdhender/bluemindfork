package net.bluemind.gatlingtest
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.http.HeaderNames._
import scala.concurrent.duration._

class MailSimulation extends Simulation {

  val ipAddress:String = System.getProperty("ipAddress")
  val nbUsers:String = System.getProperty("nbUsers")
  val duration:String = System.getProperty("duration")
  
  val random = new scala.util.Random();

  val httpConf = http
    .baseURL("https://"+ipAddress)
    .acceptHeader("application/json, text/javascript, */*; q=0.01")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3")
    .connection("keep-alive")

  val headers_2 = Map(
    "Accept" -> """text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8""",
    "Cache-Control" -> """no-cache""",
    "Content-Type" -> """text/plain""",
    "Pragma" -> """no-cache""")

  val headers_3 = Map(
    "Accept" -> """text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8""",
    "Content-Type" -> """application/x-www-form-urlencoded""")

  // Chaine de connection
  val connect = exec(http("access_homepage")
    .get("https://"+ipAddress+"/"))
    .pause(1)
    .exec(http("sso_ok")
      .post("/bluemind_sso_security")
      .headers(headers_3)
      .param("""login""", "bench" + (random.nextInt(5000) + 1) + "@blue-mind.net")
      .param("""password""", """pass""")
      .param("""priv""", """priv""")
      .param("""storedRequestId""", """X""")
      .param("""submit""", """Se connecter"""))
    .pause(569 milliseconds)
    .exec(http("getwebmail")
      .get("/webmail/")
      .check(regex("""<span>FooFirst BarLast</span>""").exists)
      .check(regex("""rcmail\.init""").exists)
      .check(regex("""request_token\":\"([a-z0-9]*)\"""").saveAs("token")))

  // Chaine task_mail
  val task_mail = exec(http("task_mail")
    .get("/webmail/")
    .header("X-Requested-With", """XMLHttpRequest""")
    .header("X-Roundcube-Request", "${token}")
    .queryParam("""_action""", """check-recent""")
    .queryParam("""_remote""", """1""")
    .queryParam("""_mbox""", """INBOX""")
    .queryParam("""_quota""", """1""")
    .queryParam("""_""", """1381324819171""")
    .queryParam("""_unlock""", """0""")
    .queryParam("""_task""", """mail""")
    .queryParam("""_list""", """1"""))
    //.pause(60)
    .pause(1)

  // Chaine bm
  val bm = exec(http("prepare_bm")
    .get("/webmail/"))
    .exec(http("bm_notifications")
      .get("/webmail/bm_notifications.php")
      .header("X-Requested-With", """XMLHttpRequest""")
      .header("X-Roundcube-Request", "${token}")
      .queryParam("""_action""", """getWatingEvents""")
      .queryParam("""_remote""", """1""")
      .queryParam("""_""", """1381324909040""")
      .queryParam("""_task""", """mail"""))
    //.pause(150)
    .pause(2)

  val scn = scenario("Mail idle")
    .exitBlockOnFail{
      exec(connect)
      .repeat(20){ exec(task_mail, bm) }
    }

  setUp(scn.inject(rampUsers(nbUsers.toInt) over (duration.toInt seconds))).protocols(httpConf)
}
