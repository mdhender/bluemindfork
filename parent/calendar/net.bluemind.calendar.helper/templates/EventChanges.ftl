<table style="background-color:#FFE6D9; !important">
	<tr height="45px">
      	<td class="value"><span style="color:orange;font-weight: bold;">&nbsp;&nbsp;&nbsp;&nbsp;${msg("infos.changed")}</span>
      	<#list changes as change>
      	${msg("${change}")}<#sep>, </#sep>
      	</#list>
      	</td>
    </tr>
</table>

