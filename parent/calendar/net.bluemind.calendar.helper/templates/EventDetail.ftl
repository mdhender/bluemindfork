<table>
  <tr>
    <td colspan="2"><h2>${title} <#if .data_model.old_title??><#if old_title != title><i class="updated">(${msg("generic.updated")})</i></#if></#if></h2></td>
  </tr>
  <tr>
    <td class="key">${msg("key.when")}</td>
    <td class="value">
      <#if allday == "true">
        <#if datebegin?string(date_format) == dateend?string(date_format)>
          ${msg("on")} <span class="date">${datebegin?string(date_format)}</span>
        <#else>
          ${msg("from")} <span class="date">${datebegin?string(date_format)}</span><br />
          ${msg("to")} <span class="date">${dateend?string(date_format)}</span>
        </#if>
      <#else>
        <#if datebegin?string(date_format) == dateend?string(date_format)>
          ${msg("on")} <span class="date">${datebegin?string(date_format)}</span> <br />
          ${msg("hourFrom")} <span class="date">${datebegin?string(time_format)}</span> ${msg("hourTo")} <span class="date">${dateend?string(time_format)}</span>
          <#if tz??>
            <span class="tz">(${tz})</span>
          </#if>
        <#else>
          ${msg("on")} <span class="date">${datebegin?string(date_format)}, ${datebegin?string(time_format)}</span>
          <#if tz??>
            <span class="tz">(${tz})</span>
          </#if>
          <br />
          ${msg("to")} <span class="date">${dateend?string(date_format)}, ${dateend?string(time_format)}</span>
          <#if tz??>
            <span class="tz">(${tz})</span>
          </#if>
        </#if> 
        <#if .data_model.old_datebegin??><#if old_datebegin?string(datetime_format) != datebegin?string(datetime_format) || old_dateend?string(datetime_format) != dateend?string(datetime_format) || old_duration != duration><i class="updated">(${msg("generic.updated")})</i></#if></#if>
      </#if>
    </td>
  </tr>
  <#if recurrenceKind??>
  <tr>
    <td class="key"></td>
    <td class="value">
    <#if recurrenceFreq == 1>
	    <#switch recurrenceKind>
	      <#case "DAILY">
	        ${msg("daily")}
	      <#break>
	      <#case "WEEKLY">
	        ${msg("weekly")}
	        <#list recurrenceDays as x>
		      <#switch x>
		        <#case "MO">
		         ${msg("monday")}
		        <#break>
		        <#case "TU">
		        ${msg("tuesday")}
		        <#break>
		        <#case "WE">
		        ${msg("wednesday")}
		        <#break>
		        <#case "TH">
		         ${msg("thrusday")}
		        <#break>
		        <#case "FR">
		         ${msg("friday")}
		        <#break>
		        <#case "SA">
		         ${msg("saturday")}
		        <#break>
		        <#case "SU">
		         ${msg("sunday")}
		        <#break>
	          </#switch>
			</#list>
	      <#break>
	      <#case "MONTHLYBYDATE">
		  <#case "MONTHLYBYDAY">
	        ${msg("monthly")}
	      <#break>
	      <#case "YEARLY">
	        ${msg("yearly")}
	      <#break>
	    </#switch>
    <#else>
		<#switch recurrenceKind>
	      <#case "DAILY">
	      ${msg("dailyFreq",recurrenceFreq)}
	      <#break>
	      <#case "WEEKLY">
	        ${msg("weeklyFreq",recurrenceFreq)} 
	        <#list recurrenceDays as x>
		      <#switch x>
		        <#case "MO">
		         ${msg("monday")}
		        <#break>
		        <#case "TU">
		         ${msg("tuesday")}
		        <#break>
		        <#case "WE">
		         ${msg("wednesday")}
		        <#break>
		        <#case "TH">
		         ${msg("thrusday")}
		        <#break>
		        <#case "FR">
		         ${msg("friday")}
		        <#break>
		        <#case "SA">
		         ${msg("saturday")}
		        <#break>
		        <#case "SU">
		         ${msg("sunday")}
		        <#break>
	          </#switch>
			</#list>
	      <#break>
	      <#case "MONTHLYBYDATE">
		  <#case "MONTHLYBYDAY">
		    ${msg("monthlyFreq",recurrenceFreq)}
	      <#break>
	      <#case "YEARLY">
		    ${msg("yearlyFreq",recurrenceFreq)}
	      <#break>
	    </#switch>
    </#if>
    </td>
  </tr>
  </#if>
  <#if location??>
    <tr>
      <td class="key">${msg("key.where")}</td>
      <td class="value">${location} <#if .data_model.old_location??><#if old_location != location><i class="updated">(${msg("generic.updated")})</i></#if></#if></td>
    </tr>
  </#if>
  <#if url??>
    <tr>
      <td class="key">${msg("key.url")}</td>
      <td class="value"><a href="${url}" target="_blank">${url}</a> <#if .data_model.old_url??><#if old_url != url><i class="updated">(${msg("generic.updated")})</i></#if></#if></td>
    </tr>
  </#if>
    <#if conference??>
    <tr>
      <td class="key">${msg("key.conference")}</td>
      <td class="value"><a href="${conference}" target="_blank">${conference}</a> <#if .data_model.old_conference??><#if old_conference != conference><i class="updated">(${msg("generic.updated")})</i></#if></#if></td>
    </tr>
  </#if>
  <#if owner??>
  <tr>
    <td class="key">${msg("key.organizer")}</td><td class="value">${owner}</td>
  </tr>
  </#if>
  <#if attendees??>
  <tr>
    <td class="key">${msg("key.attendees")}</td>
    <td class="value">
      <ul class="attendees">
        <#list attendees as attendee>
          <li>${attendee}</li>
        </#list>
      </ul>
    </td>
  </tr>
  </#if>
  <#if description??>
    <tr>
      <td class="key">${msg("key.description")}</td>
      <td class="value">${description} <#if .data_model.old_description??><#if old_description != description><i class="updated">(${msg("generic.updated")})</i></#if></#if></td>
    </tr>
  </#if>
  <tr>
    <td colspan="2"><h2>&nbsp;</h2></td>
  </tr>
</table>
<#if at??>
  <div class="at">${at}</div>
</#if>

