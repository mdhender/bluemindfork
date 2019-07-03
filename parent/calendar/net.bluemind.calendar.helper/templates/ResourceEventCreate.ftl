<#include "head.ftl">
<h1>${msg("resourceBooking")}</h1>
<#if !available>
  <div class="note">
    <b>${msg("resourceNoteTitle")}:</b><br/>
       ${msg("resourceOutOfWorkingHours")}
  </div>
</#if>
<#include "EventDetail.ftl">
<#include "foot.ftl">