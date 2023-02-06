<#include "head.ftl">
<h1 class="update">
  ${msg("todoUpdated", author)}
</h1>
<table>
    <tr>
      <td class="key">${msg("key.summary")}</td>
      <td class="value">${summary}</td>
    </tr>
    <tr>
      <td class="key">${msg("key.status")}</td>
      <td class="value">${status}</td>
    </tr>
</table>
  
<#include "foot.ftl">
