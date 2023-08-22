<#include "head.ftl">
<h1>
  ${msg("counter.attendees.note", attendee)}
</h1>
<#if note != "">
  <div class="note">
    <b>Note:</b><br/>
    ${note}
  </div>
</#if>
<#include "EventDetail.ftl">
<#include "foot.ftl">