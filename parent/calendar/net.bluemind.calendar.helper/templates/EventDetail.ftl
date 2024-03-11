<table>
  <tr>
    <td colspan="2"><h2>${title} <#if .data_model.old_title??><#if old_title != title><i class="updated">(${msg("generic.updated")})</i></#if></#if></h2></td>
  </tr>
  <tr>
    <td class="key">${msg("key.when")}<#if .data_model.old_datebegin??><#if old_datebegin?string(datetime_format) != datebegin?string(datetime_format) || old_dateend?string(datetime_format) != dateend?string(datetime_format) || old_duration != duration><i class="updated">&nbsp;(${msg("generic.updated")})</i></#if></#if></td>
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
      </#if>
      <#if .data_model.old_datebegin??><#if old_datebegin?string(datetime_format) != datebegin?string(datetime_format) || old_dateend?string(datetime_format) != dateend?string(datetime_format) || old_duration != duration>
      <br></br>
      <#if old_allday == "true">
        <#if old_datebegin?string(date_format) == old_dateend?string(date_format)>
          <s>${msg("on")}</s> <span class="old_date"><s>${old_datebegin?string(date_format)}</s></span>
        <#else>
          <s>${msg("from")}</s> <span class="date"><s>${old_datebegin?string(date_format)}</s></span><br />
          <s>${msg("to")}</s> <span class="old_date"><s>${old_dateend?string(date_format)}</s></span>
        </#if>
      <#else>
        <#if old_datebegin?string(date_format) == old_dateend?string(date_format)>
          <s>${msg("on")}</s> <span class="old_date"><s>${old_datebegin?string(date_format)}</s></span> <br />
          <s>${msg("hourFrom")}</s> <span class="old_date"><s>${old_datebegin?string(time_format)}</s></span> <s>${msg("hourTo")}</s> <span class="old_date"><s>${old_dateend?string(time_format)}</s></span>
          <#if old_tz??>
            <span class="tz"><s>(${old_tz})</s></span>
          </#if>
        <#else>
          <s>${msg("on")}</s> <span class="old_date"><s>${old_datebegin?string(date_format)}, ${old_datebegin?string(time_format)}</s></span>
          <#if old_tz??>
            <span class="tz"><s>(${old_tz})</s></span>
          </#if>
          <br />
          ${msg("to")} <span class="old_date"><s>${old_dateend?string(date_format)}, ${old_dateend?string(time_format)}</s></span>
          <#if old_tz??>
            <span class="tz"><s>(${old_tz})</s></span>
          </#if>
        </#if> 
      </#if>
      </#if></#if>
    </td>
  </tr>
  <#if recurrenceKind?? || changes?seq_contains("RRULE")>
  <tr>
    <td class="key"></td>
    <td class="value">
    <#if recurrenceKind??>
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
    </#if>
    <#if .data_model.old_recurrenceKind??>
    	<#if changes?seq_contains("RRULE")>
    		<br></br>
    		<#if .data_model.old_recurrenceFreq == 1>
			    <#switch .data_model.old_recurrenceKind>
			      <#case "DAILY">
			        <s>${msg("daily")}</s>
			      <#break>
			      <#case "WEEKLY">
			        <s>${msg("weekly")}</s>
			        <#list .data_model.old_recurrenceDays as x>
				      <#switch x>
				        <#case "MO">
				         <s>${msg("monday")}</s>
				        <#break>
				        <#case "TU">
				        <s>${msg("tuesday")}</s>
				        <#break>
				        <#case "WE">
				        <s>${msg("wednesday")}</s>
				        <#break>
				        <#case "TH">
				         <s>${msg("thrusday")}</s>
				        <#break>
				        <#case "FR">
				         <s>${msg("friday")}</s>
				        <#break>
				        <#case "SA">
				         <s>${msg("saturday")}</s>
				        <#break>
				        <#case "SU">
				         <s>${msg("sunday")}</s>
				        <#break>
			          </#switch>
					</#list>
			      <#break>
			      <#case "MONTHLYBYDATE">
				  <#case "MONTHLYBYDAY">
			        <s>${msg("monthly")}</s>
			      <#break>
			      <#case "YEARLY">
			        <s>${msg("yearly")}</s>
			      <#break>
			    </#switch>
		    <#else>
				<#switch .data_model.old_recurrenceKind>
			      <#case "DAILY">
			      <s>${msg("dailyFreq",.data_model.old_recurrenceFreq)}</s>
			      <#break>
			      <#case "WEEKLY">
			        <s>${msg("weeklyFreq",.data_model.old_recurrenceFreq)}</s> 
			        <#list .data_model.old_recurrenceDays as x>
				      <#switch x>
				        <#case "MO">
				         <s>${msg("monday")}</s>
				        <#break>
				        <#case "TU">
				        <s>${msg("tuesday")}</s>
				        <#break>
				        <#case "WE">
				        <s>${msg("wednesday")}</s>
				        <#break>
				        <#case "TH">
				         <s>${msg("thrusday")}</s>
				        <#break>
				        <#case "FR">
				         <s>${msg("friday")}</s>
				        <#break>
				        <#case "SA">
				         <s>${msg("saturday")}</s>
				        <#break>
				        <#case "SU">
				         <s>${msg("sunday")}</s>
				        <#break>
			          </#switch>
					</#list>
			      <#break>
			      <#case "MONTHLYBYDATE">
				  <#case "MONTHLYBYDAY">
				    <s>${msg("monthlyFreq",.data_model.old_recurrenceFreq)}</s>
			      <#break>
			      <#case "YEARLY">
				    <s>${msg("yearlyFreq",.data_model.old_recurrenceFreq)}</s>
			      <#break>
			    </#switch>
    		</#if>
    	</#if>
    </#if>
    </td>
  </tr>
  </#if>
  <#if location?? || changes?seq_contains("LOCATION")>
    <tr>
      <td class="key">${msg("key.where")}<#if changes?seq_contains("LOCATION")><i class="updated">&nbsp;(${msg("generic.updated")})</i></#if></td>
      <td class="value"><#if location??>${location}</#if>
      <#if changes?seq_contains("LOCATION")><#if .data_model.old_location??><s><br></br>${old_location}</s></#if></#if></td>
    </tr>
  </#if>
  <#if url?? || changes?seq_contains("URL")>
    <tr>
      <td class="key">${msg("key.url")}<#if changes?seq_contains("URL")><i class="updated">&nbsp;(${msg("generic.updated")})</i></#if></td>
      <td class="value"><#if url??><a href="${url}" target="_blank">${url}</a></#if>
      <#if changes?seq_contains("URL")><#if .data_model.old_url??><s><br></br>${old_url}</s></#if></#if></td>
    </tr>
  </#if>
    <#if conference?? || changes?seq_contains("CONFERENCE")>
    <tr>
      <td class="key">${msg("key.conference")}<#if changes?seq_contains("CONFERENCE")><i class="updated">&nbsp;(${msg("generic.updated")})</i></#if></td>
      <td class="value"><#if conference??>${conference}</#if>
      <#if changes?seq_contains("CONFERENCE")><#if .data_model.old_conference??><s><br></br>${old_conference}</s></#if></#if></td>
    </tr>
  </#if>
  <#if owner??>
  <tr>
    <td class="key">${msg("key.organizer")}</td><td class="value">${owner}</td>
  </tr>
  </#if>
  <#if attendees??>
  <tr>
    <td class="key">${msg("key.attendees")}<#if .data_model.attendeeChanges??><#if .data_model.attendeeChanges == true><i class="updated">&nbsp;(${msg("generic.updated")})</i></#if></#if></td>
    <td class="value">
      <ul class="attendees">
      	<#list added_attendees as attendee>
          <li><b>${attendee}</b><i class="updated">&nbsp;(${msg("generic.added")})</i></li>
        </#list>
        <#list attendees as attendee>
          <li>${attendee}</li>
        </#list>
        <#list deleted_attendees as attendee>
          <li><s>${attendee}</s><i class="updated">&nbsp;(${msg("generic.removed")})</i></li>
        </#list>
      </ul>
    </td>
  </tr>
  </#if>
  <#if description??>
    <tr>
      <td class="key">${msg("key.description")}<#if .data_model.old_description??><#if old_description != description><i class="updated">&nbsp;(${msg("generic.updated")})</i></#if></#if></td>
      <td class="value">${description}</td>
    </tr>
  </#if>
  <tr>
    <td colspan="2"><h2>&nbsp;</h2></td>
  </tr>
</table>
<#if at??>
  <div class="at">${at}</div>
</#if>

