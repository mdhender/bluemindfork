<html>
	<head><style><#include "AclChangedNotification.css"></style></head>
	<body>
		<p class="desc">${desc}</p>
		<table>
			<thead>
				<tr>
					<th></th>
					<th>${tableHead}</th>
				</tr>
			</thead>
			<tbody>
			<#list appPermissions as app, permissions>
				<tr>
					<td class="app">${app}</td>
					<td class="permission">
						<ul>
					    <#list permissions as permission>
					        <li><b>${permission.level()}</b> ${permission.target()}</li>
					    </#list>
					    </ul>
					</td>
				</tr>
			</#list>
			</tbody>
		</table>
	</body>
</html>