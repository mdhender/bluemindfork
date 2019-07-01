<#include "head.ftl">
<h1 class="update">${msg("resourceBookingUpdated")}</h1>
<#if !available>
  <div class="note">
    <b>${msg("resourceNoteTitle")}:</b><br/>
       ${msg("resourceOutOfWorkingHours")}
  </div>
</#if>
<#include "EventDetail.ftl">
<#include "foot.ftl">

