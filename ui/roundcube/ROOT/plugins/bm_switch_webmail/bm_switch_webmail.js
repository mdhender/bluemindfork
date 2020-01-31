
rcmail.addEventListener('init', function(evt) {
    
    var switchWebmailHTML = `<a id="switch-webmail" href="#"><div id="switch-webmail-text">` 
        + rcmail.labels['bm_switch_webmail.switchwebmail'] + 
        `</div><div id="switch-webmail-input"></div></a>`;

    $("#quicksearchbar").after(switchWebmailHTML);

    var element = $('#switch-webmail');
    element.on('click', function(evt) {
        rcmail.http_request('plugin.bm_switch_webmail.click');
    });
});