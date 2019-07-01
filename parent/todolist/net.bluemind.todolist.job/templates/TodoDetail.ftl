<table>
  <tr>
    <td colspan="2"><h2>${title} <#if .data_model.old_title??><#if old_title != title><i class="updated">(${msg("generic.updated")})</i></#if></#if></h2></td>
  </tr>
  <tr>
    <td class="key">${msg("key.when")}</td>
    <td class="value">
    	${msg("dueDate")}<span class="date">${datedue?string(date_format)}</span>
    </td>
  </tr>
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
