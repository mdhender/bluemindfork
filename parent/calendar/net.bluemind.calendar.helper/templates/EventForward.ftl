<#include "head.ftl">
<h1 class="info">
  ${msg("invitationForwardedToYou", originator, organizer)}
</h1>
<#if note??>
  <div class="note">
    ${note}
  </div>
</#if>
<#include "EventDetail.ftl">
<#include "EventAttachments.ftl">
<#include "foot.ftl">
