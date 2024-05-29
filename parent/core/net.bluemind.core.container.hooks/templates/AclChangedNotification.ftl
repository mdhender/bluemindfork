<html>
	<head><style><#include "AclChangedNotification.css"></style></head>
	<body>
		<p class="desc">${desc}</p>
		<#if tableHeadUpdate??>
		<br/>
		<table>
			<thead>
				<tr>
					<th></th>
					<th>${tableHeadUpdate}</th>
				</tr>
			</thead>
			<tbody>
			<#list appPermissionsUpdate as app, permissionsUpdate>
				<tr>
					<td class="app">${app}</td>
					<td class="permission">
						<ul>
					    <#list permissionsUpdate as permission>
					        <li><b>${permission.level()}</b>(<s>${permission.oldlevel()}</s>) ${permission.target()}</li>
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
			<#list appPermissionsAdd as app, permissionsAdd>
				<tr>
					<td class="app">${app}</td>
					<td class="permission">
						<ul>
					    <#list permissionsAdd as permission>
					        <li><b>${permission.level()}</b> ${permission.target()}</li>
					    </#list>
					    </ul>
					</td>
				</tr>
			</#list>
			</tbody>
		</table>
		</#if>
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
			<#list appPermissionsDelete as app, permissionsDelete>
				<tr>
					<td class="app">${app}</td>
					<td class="permission">
						<ul>
					    <#list permissionsDelete as permission>
					        <li><b>${permission.oldlevel()}</b> ${permission.target()}</li>
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