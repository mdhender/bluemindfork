require ["body", "copy", "fileinto", "imapflags", "vacation", "include" ];

# a failing fileinto in the global script, eg. fileinto Junk, would block
# the whole execution, so we remove the include for now
# include :global "${domainName}.sieve";

# vacation
<#if vacation.enabled>
if allof (not header :contains "Precedence" ["bulk", "list"],
		  not header :contains "X-Spam-Flag" "YES",
		  not header :contains "X-DSPAM-Result" "Spam") {
	vacation :days 3 :from "${from}" :addresses [<#list mails as mail>"${mail}"<#if mail_has_next>,</#if></#list>] :subject "${vacation.subject?replace("\"", "\\\"")?replace("'", "\\'")}" "${vacation.text?replace("\"", "\\\"")?replace("'", "\\'")}";
}
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
	stop;
}
</#list>

# END
