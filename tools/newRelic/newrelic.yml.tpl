common: &default_settings
  license_key: 'BM_NR_LIC'
  enable_auto_app_naming: false
  enable_auto_transaction_naming: true
  app_name: BM_PRODUCT (BM_HOST)
  log_level: info
  log_file_count: 1
  
  log_limit_in_kbytes: 1024
  # Default is the logs directory in the newrelic.jar parent directory.
  #log_file_path:
  
  # Default is true.
  ssl: true
  
  # proxy_host: hostname
  # proxy_port: 8080
  # proxy_user: username
  # proxy_password: password

  # Tells transaction tracer and error collector (when enabled)
  # whether or not to capture HTTP params.  When true, frameworks can
  # exclude HTTP parameters from being captured.
  # Default is false.
  capture_params: false
  
  # Tells transaction tracer and error collector to not to collect
  # specific http request parameters. 
  # ignored_params: credit_card, ssn, password

  # Transaction tracer captures deep information about slow
  # transactions and sends this to the New Relic service once a
  # minute. Included in the transaction is the exact call sequence of
  # the transactions including any SQL statements issued.
  transaction_tracer:
  
    # Transaction tracer is enabled by default. Set this to false to
    # turn it off. This feature is only available at the higher product levels.
    # Default is true.
    enabled: true
    
    # Threshold in seconds for when to collect a transaction
    # trace. When the response time of a controller action exceeds
    # this threshold, a transaction trace will be recorded and sent to
    # New Relic. Valid values are any float value, or (default) "apdex_f",
    # which will use the threshold for the "Frustrated" Apdex level
    # (greater than four times the apdex_t value).
    # Default is apdex_f.
    transaction_threshold: apdex_f
 
    # When transaction tracer is on, SQL statements can optionally be
    # recorded. The recorder has three modes, "off" which sends no
    # SQL, "raw" which sends the SQL statement in its original form,
    # and "obfuscated", which strips out numeric and string literals.
    # Default is obfuscated.
    record_sql: obfuscated
    
    # Obfuscate only occurrences of specific SQL fields names.
    # This setting only applies if "record_sql" is set to "raw".
    #obfuscated_sql_fields: credit_card, ssn, password

    # Set this to true to log SQL statements instead of recording them.
    # SQL is logged using the record_sql mode.
    # Default is false.
    log_sql: false

    # Threshold in seconds for when to collect stack trace for a SQL
    # call. In other words, when SQL statements exceed this threshold,
    # then capture and send to New Relic the current stack trace. This is
    # helpful for pinpointing where long SQL calls originate from.
    # Default is 0.5 seconds.
    stack_trace_threshold: 0.5

    # Determines whether the agent will capture query plans for slow
    # SQL queries. Only supported for MySQL and PostgreSQL.
    # Default is true.
    explain_enabled: true

    # Threshold for query execution time below which query plans will not 
    # not be captured.  Relevant only when `explain_enabled` is true.
    # Default is 0.5 seconds.
    explain_threshold: 0.5
    
    # Use this setting to control the variety of transaction traces.
    # The higher the setting, the greater the variety.
    # Set this to 0 to always report the slowest transaction trace.
    # Default is 20.
    top_n: 20
    
  
  # Error collector captures information about uncaught exceptions and
  # sends them to New Relic for viewing
  error_collector:
    
    # Error collector is enabled by default. Set this to false to turn
    # it off. This feature is only available at the higher product levels.
    # Default is true.
    enabled: true
        
    # To stop specific exceptions from reporting to New Relic, set this property
    # to a comma separated list of full class names.
    #
    # ignore_errors:

    # To stop specific http status codes from being reporting to New Relic as errors, 
    # set this property to a comma separated list of status codes to ignore.
    # When this property is commented out it defaults to ignoring 404s.
    #
    # ignore_status_codes: 404

  # Cross Application Tracing adds request and response headers to
  # external calls using the Apache HttpClient libraries to provided better
  # performance data when calling applications monitored by other New Relic Agents.
  #
  cross_application_tracer:
    # Set to true to enable cross application tracing.
    # Default is true.
    enabled: true

  # Thread profiler measures wall clock time, CPU time, and method call counts
  # in your application's threads as they run.
  thread_profiler:

    # Set to false to disable the thread profiler.
    # Default is true.
    enabled: true
  
  #============================== Browser Monitoring ===============================
  # New Relic Real User Monitoring gives you insight into the performance real users are
  # experiencing with your website. This is accomplished by measuring the time it takes for
  # your users' browsers to download and render your web pages by injecting a small amount
  # of JavaScript code into the header and footer of each page. 
  browser_monitoring:
    # By default the agent automatically inserts API calls in compiled JSPs to
    # inject the monitoring JavaScript into web pages.
    # Set this attribute to false to turn off this behavior.
    auto_instrument: true
    # Set this attribute to false to prevent injection of the monitoring JavaScript.
    # Default is true.
    enabled: true
    
# Application Environments
# ------------------------------------------
# Environment specific settings are in this section.
# You can use the environment to override the default settings.
# For example, to change the app_name setting.
# Use -Dnewrelic.environment=<environment> on the Java command line
# to set the environment.
# The default environment is production.

# NOTE if your application has other named environments, you should
# provide configuration settings for these environments here.

development:
  <<: *default_settings
  app_name: My Application (Development)

test:
  <<: *default_settings
  app_name: My Application (Test)

production:
  <<: *default_settings

staging:
  <<: *default_settings
  app_name: My Application (Staging)
