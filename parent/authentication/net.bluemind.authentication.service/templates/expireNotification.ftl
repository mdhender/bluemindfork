<html>
	<head>
	  <#include "style.ftl">
	</head>
	
	<body>
		<@compress single_line=true>
		<h1>
		${msg("expireNotificationSubject", notificationInterval)} <#if notificationInterval gt 1>${msg("days")}<#else>${msg("day")}</#if>
		</h1>
		</@compress>
		
		<div class="explain">
		<#if externalUrl??>
			${msg("explain", "<a href=\"https://" + externalUrl + "/settings/index.html#myAccount\">", "</a>")}
		<#else>
			${msg("explain", "", "")}
		</#if>
        </div>
	</body>
</html>
