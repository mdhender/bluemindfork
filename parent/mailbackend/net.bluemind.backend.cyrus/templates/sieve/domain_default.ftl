require ["body", "copy", "fileinto", "imapflags" ];

if allof (header :is "X-BM-Discard" "discard") {
        discard;
        stop;
}

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
