package net.bluemind.gatling

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import scala.concurrent.duration._
import bootstrap._
import assertions._

class LocateCore extends Simulation {

  val scn = scenario("Locator")
            .exec(http("bm/core 1")
              .get("http://localhost:8084/location/host/bm/core/admin@willow.vmw"))
            .exec(http("mail/imap 1")
              .get("http://localhost:8084/location/host/mail/imap/admin@buffy.vmw"))
            .exec(http("solr/contact 1")
              .get("http://localhost:8084/location/host/solr/contact/admin@willow.vmw"))
            .exec(http("bm/core 2")
              .get("http://localhost:8084/location/host/bm/core/admin@willow.vmw"))
            .exec(http("mail/imap 2")
              .get("http://localhost:8084/location/host/mail/imap/admin@buffy.vmw"))
            .exec(http("solr/contact 2")
              .get("http://localhost:8084/location/host/solr/contact/admin@willow.vmw"))
  
  setUp(scn.inject(ramp(5000 users over 10 seconds)))

}
