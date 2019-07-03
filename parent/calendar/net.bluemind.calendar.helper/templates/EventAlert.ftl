<#include "head.ftl">
<h1><#include "EventSubjectAlert.ftl"></h1>
<#if reminder_summary??>
  <div class="note">
    ${reminder_summary}
  </div>
</#if>
<#include "EventDetail.ftl">
<#include "foot.ftl">
