function dial(number) {
  rcmail.http_post('plugin.bm_xivo.dial', '_number='+number);

}
