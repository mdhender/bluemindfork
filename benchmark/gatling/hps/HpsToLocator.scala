package net.bluemind.gatling

import io.gatling.core.Predef._ // 2
import io.gatling.http.Predef._
import scala.concurrent.duration._

class HpsToLocator extends Simulation {

  val scn = scenario("HPS to locator")
            .exec(http("hps -> bm/core")
              .get("http://localhost:8079/location/host/bm/core/admin@vagrant.vmw"))
            .exec(http("hps -> mail/imap")
              .get("http://localhost:8079/location/host/mail/imap/admin@vagrant.vmw"))
            .exec(http("hps -> solr/contact")
              .get("http://localhost:8079/location/host/bm/cal/admin@vagrant.vmw"))
            .exec(http("hps -> bm/core")
              .get("http://localhost:8079/location/host/bm/core/admin@vagrant.vmw"))
            .exec(http("hps -> mail/imap")
              .get("http://localhost:8079/location/host/mail/imap/admin@vagrant.vmw"))
            .exec(http("hps -> solr/contact")
              .get("http://localhost:8079/location/host/bm/cal/admin@vagrant.vmw"))
  
  setUp(scn.inject(rampUsers(2000) over (30 seconds)))

}
