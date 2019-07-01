<table>
  <tr>
    <td colspan="2"><h2>${title} <#if .data_model.old_title??><#if old_title != title><i class="updated">(${msg("generic.updated")})</i></#if></#if></h2></td>
  </tr>
  <tr>
    <td class="key">${msg("key.when")}</td>
    <td class="value">
      <#if allday == "true">
        ${msg("on")} <span class="date">${datebegin?string(date_format)}</span>
      <#else>
        ${msg("on")} <span class="date">${datebegin?string(date_format)}, ${datebegin?string(time_format)}</span>
        <#if tz??>
          <span class="tz">(${tz})</span>
        </#if>
      </#if>
    </td>
  </tr>
  <#if location??>
    <tr>
      <td class="key">${msg("key.where")}</td>
      <td class="value">${location} <#if .data_model.old_location??><#if old_location != location><i class="updated">(${msg("generic.updated")})</i></#if></#if></td>
    </tr>
  </#if>
  <tr>
    <td class="key">${msg("key.organizer")}</td><td class="value">${owner}</td>
  </tr>
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
