<#include "head.ftl">
<#assign participation = {"ACCEPTED": msg("participation.ACCEPTED"), "DECLINED": msg("participation.DECLINED"), "NEEDS-ACTION":msg("participation.NEEDS-ACTION"), "TENTATIVE":msg("participation.TENTATIVE")}>
<#assign css = {"ACCEPTED":"accepted", "DECLINED":"declined", "NEEDS-ACTION":"needsaction", "TENTATIVE":"tentative"}>
<h1 class="${css[state]}">
  ${msg("resourceBookingParticipationUpdate",participation[state],attendee)}<#if exdate??> of ${exdate?string(date_format)}</#if>
</h1>
<#if note != "">
  <div class="note">
    <b>${msg("resourceNoteTitle")}</b><br/>
    ${note}
  </div>
</#if>
<#include "EventDetail.ftl">
<#include "foot.ftl">