/**
 * @fileoverview About this file
 */


/**
 * Login.
 * @param {function(this: T, Object)} cb
 * @param {T=} opt_scope
 * @template T
 */
var request = function(q, cb, opt_scope) {
  var xhr = new XMLHttpRequest();
  var url = 'https://kyawtun.insightfulcrm.com/service/v4/rest.php';
  // var url = 'https://tuwrwh9927.trial.sugarcrm.com/rest/v10';
  xhr.open('GET', url, true);
  xhr.withCredentials = false;
  xhr.onload = function(e) {
    var json = null;
    if (xhr.status < 400) {
      json = JSON.parse(xhr.responseText);
    }

    cb.call(opt_scope, json);

  };
  xhr.send();
};


request('server_info', function(x) {
  console.log(x);
});

var rest_data = {"user_auth": {"user_name": "admin", "password": "d2906f5293adfba173ee0a5a26a50968"}, "application_name": "Yathit CRM", "name_value_list": {"language": "", "notifyonsave": "false"}};
var ele = document.getElementById('rest');
ele.value = JSON.stringify(rest_data);

setTimeout(function() {
  // location.href = 'chrome-extension://fmhdpfhfppingdiiefgnanjceieflpkj/crm-ex/option-page.html';
  window.open('chrome-extension://fmhdpfhfppingdiiefgnanjceieflpkj/crm-ex/option-page.html');
}, 1000);



