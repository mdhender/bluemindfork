<html>
	<head><style><#include "AclChangedNotification.css"></style></head>
	<body>
		<p class="desc">${desc}</p>
		<#if tableHeadDelete??>
		<br/>
		<table>
			<thead>
				<tr>
					<th></th>
					<th>${tableHeadDelete}</th>
				</tr>
			</thead>
			<tbody>
			<#list appPermissionsDeleted as app, permissionsDeleted>
				<tr>
					<td class="app">${app}</td>
					<td class="permission">
						<ul>
					    <#list permissionsDeleted as permission>
					        <li><b>${permission.level()}</b> ${permission.target()}</li>
					    </#list>
					    </ul>
					</td>
				</tr>
			</#list>
			</tbody>
		</table>
		</#if>
		<#if tableHeadAdd??>
		<br/>
		<table>
			<thead>
				<tr>
					<th></th>
					<th>${tableHeadAdd}</th>
				</tr>
			</thead>
			<tbody>
			<#list appPermissionsAdded as app, permissionsAdded>
				<tr>
					<td class="app">${app}</td>
					<td class="permission">
						<ul>
					    <#list permissionsAdded as permission>
					        <li><b>${permission.level()}</b> ${permission.target()}</li>
					    </#list>
					    </ul>
					</td>
				</tr>
			</#list>
			</tbody>
		</table>
		</#if>
	</body>
</html>