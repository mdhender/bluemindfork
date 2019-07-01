<@compress single_line=true>
<#if reminder_unit == "seconds">
  <#if reminder_duration == 1>
    ${msg("alterTitleSecond", title, reminder_duration)}
  <#else>
    ${msg("alterTitleSeconds", title, reminder_duration)}
  </#if>
<#elseif reminder_unit == "minutes">
  <#if reminder_duration == 1>
    ${msg("alterTitleMinute", title,reminder_duration)}
  <#else>
    ${msg("alterTitleMinutes", title, reminder_duration)}
  </#if>
<#elseif reminder_unit == "hours">
  <#if reminder_duration == 1>
    ${msg("alterTitleHour", title, reminder_duration)}
  <#else>
    ${msg("alterTitleHours", title, reminder_duration)}
  </#if>
<#else>
  <#if reminder_duration == 1>
    ${msg("alterTitleDay", title, reminder_duration)}
  <#else>
    ${msg("alterTitleDays", title, reminder_duration)}
  </#if>
</#if>
</@compress>