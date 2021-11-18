require ["body", "copy", "fileinto", "imapflags", "vacation", "include" ];

include :global "${domainName}.sieve";

if allof (header :contains "X-BM-Discard" "${mailboxUid}") {
	discard;
	stop;
}

# vacation
<#if vacation.enabled>
if allof (not address :contains "from" "noreply@",
		  not address :contains "from" "no-reply@",
		  not header :contains "Precedence" ["bulk", "list"],
		  not header :contains "X-Spam-Flag" "YES",
		  not header :contains "X-DSPAM-Result" "Spam") {
	vacation :days 3 :from "${from}" :addresses [<#list mails as mail>"${mail}"<#if mail_has_next>,</#if></#list>] :subject "${vacationSubject}" ${vacationText};
}
</#if>

# forward
<#if forward.enabled>
<#list forward.emails as fe>
redirect <#if forward.localCopy>:copy</#if> "${fe}";
</#list>
</#if>

# filters
<#list filters as f>
${f.rule}
<#if f.star>
	setflag "\\Flagged";
</#if>
<#if f.read>
	setflag "\\Seen";
</#if>
<#if f.deliver?? && f.deliver != "">
	fileinto "${f.deliver}";
</#if>
<#if f.discard>
	discard;
</#if>
<#list f.forward.emails as fe>
	redirect <#if f.forward.localCopy>:copy</#if> "${fe}";
</#list>
<#if f.stop>
	stop;
</#if>
}
</#list>

# END
