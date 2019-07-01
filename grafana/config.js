///// @scratch /configuration/config.js/1
 // == Configuration
 // config.js is where you will find the core Grafana configuration. This file contains parameter that
 // must be set before Grafana is run for the first time.
 ///
define(['settings'],
function (Settings) {
  

  return new Settings({

    // datasources, you can add multiple
    datasources: {
      influx: {
        default: true,
        type: 'influxdb',
        url: "https://" + window.location.hostname + "/db/bm",
        username: 'root',
        password: 'root'
      }
    },

    // elasticsearch url
    // used for storing and loading dashboards, optional
    // For Basic authentication use: http://username:password@domain.com:9200
    // elasticsearch: "http://"+window.location.hostname+":9200",

    // default start dashboard
    default_route: '/dashboard/file/bluemind.json',

    timezoneOffset: null,

    // set to false to disable unsaved changes warning
    unsaved_changes_warning: false,

    // set the default timespan for the playlist feature
    // Example: "1m", "1h"
    playlist_timespan: "1m",


    // Add your own custom pannels
    plugins: {
      panels: []
    }

  });
});
