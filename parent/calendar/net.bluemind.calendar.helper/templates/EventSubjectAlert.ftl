<@compress single_line=true>
<#if reminder_duration == 0>
    ${msg("alertTitleNow", title)}
<#else>
<#if reminder_unit == "seconds">
  <#if reminder_duration == 1>
    ${msg("alertTitleSecond", title, reminder_duration)}
  <#else>
    ${msg("alertTitleSeconds", title, reminder_duration)}
  </#if>
<#elseif reminder_unit == "minutes">
  <#if reminder_duration == 1>
    ${msg("alertTitleMinute", title,reminder_duration)}
  <#else>
    ${msg("alertTitleMinutes", title, reminder_duration)}
  </#if>
<#elseif reminder_unit == "hours">
  <#if reminder_duration == 1>
    ${msg("alertTitleHour", title, reminder_duration)}
  <#else>
    ${msg("alertTitleHours", title, reminder_duration)}
  </#if>
<#else>
  <#if reminder_duration == 1>
    ${msg("alertTitleDay", title, reminder_duration)}
  <#else>
    ${msg("alertTitleDays", title, reminder_duration)}
  </#if>
</#if>
</#if>
</@compress>