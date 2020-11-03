<#include "head.ftl">
<h1 class="info">
  ${msg("declineCounter", owner)}
</h1>
<table>
  <tr>
    <td colspan="2"><h2>${title}</h2></td>
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
</table>
<#include "foot.ftl">
