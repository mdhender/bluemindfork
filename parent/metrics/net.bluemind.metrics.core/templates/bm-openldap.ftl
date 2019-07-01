# TODO demander lesquelles de ses infos sont à modifier
[[inputs.openldap]]
  host = "localhost"
  port = 389

  # ldaps, starttls, or no encryption. default is an empty string, disabling all encryption.
  # note that port will likely need to be changed to 636 for ldaps
  # valid options: "" | "starttls" | "ldaps"
  ssl = ""

  # skip peer certificate verification. Default is false.
  insecure_skip_verify = false

  # dn/password to bind with. If bind_dn is empty, an anonymous bind is performed.
  bind_dn = "uid=admin, cn=config"
  bind_password = "${password}"
  
  # reverse metric names so they sort more naturally
  # Defaults to false if unset, but is set to true when generating a new config
  reverse_metric_names = true
  
