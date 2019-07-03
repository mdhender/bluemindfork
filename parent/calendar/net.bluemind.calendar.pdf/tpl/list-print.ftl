<html>

<head>
	<title>doc</title>
<style type="text/css">
	<#include "style.tpl">
</style>
</head>
<body>



	<div id="page-footer">
		<div class="page-count">
			page <span id="pagenumber"></span> / <span id="pagecount"></span>
		</div>
	</div>
	<div id="page-left-footer">${msg("dateprint.label")}
	${printDate}
		<#if options.showDetail>
			${msg("view.detailsLabel")}
		</#if>
	</div>



<table class="body">
<thead>
<tr>
<td>
	<div id="page-header" >
		<div class="title">${date}</div>
		<div class="calendars">
			<#list cals as cal>
				<span style="color: ${cal.color};">${cal.name}<#if cal_has_next>,
				</#if>
				</span>
			</#list>
		</div>
		</div>
</td>
</tr>
</thead>
<tbody>
<tr>
<td>

		<#list days as day>
	<table class="main">

		<colgroup>
			<col class="col-time" />
			<col class="col-description" />
		</colgroup>
		<thead>
		<tr >
			<td colspan="2" class="day">${day.name}</td>
		</tr>
		</thead>
		<tbody>
		<#list day.events as event>
		<tr class="event">
			<td class="td-time">
				<div class="time">${event.timeSlot}</div></td>
			<td >
				<div class="event-main">
					<div class="title" style="color: ${event.color};">${event.title?html}						
					</div>
					<div class="event-info">
						<#if event.event.location?? && event.event.location?? && event.event.location != '' >
						<div class="location">
							<span class="fieldname"> ${msg("location")} : </span>${event.event.location?html}
						</div>
						</#if>
						<#if options.showDetail &&  event.description??>
						<div class="description">
							<span class="fieldname"> ${msg("description")} : </span>${event.description}
						</div>
						</#if>
						
						<#if event.chair?? >
							<div class="participations">
								<span class="fieldname">${msg("attendee.chair")} :</span>
								<#if event.chair.color??>
							   <span style="text-decoration: underline; color: ${event.chair.color};">
							   <#else>
							   <span>
							   </#if>
							   ${event.chair.name?html}
							  </span>							  
							</div>
						</#if>
						<#if event.attendees?size gt 0>
						<div class="participations">
							<span class="fieldname">${msg("attendee.mandatory")} :</span>
							<#list  event.attendees as attendee>
							   
							   <#if attendee_index < 10 || options.showDetail>
							   <#if attendee.color??>
							   <span style="text-decoration: underline; color: ${attendee.color};">
							   <#else>
							   <span>
							   </#if>
							   ${attendee.name?html}<#if attendee_has_next && ( attendee_index lt 9 || options.showDetail)>,
								</#if>
								</span>
							  
								</#if>
							</#list>
							<#if event.attendees?size gt 10 && !options.showDetail>
							${ msg("attendee.more", event.attendees?size - 10)}
							</#if>
						</div>
						</#if>
						
						
						
						<#if event.fattendees?size gt 0 && options.showDetail>
						<div class="participations">
							<span class="fieldname">${msg("attendee.opt")} :</span>
							<#list  event.fattendees as attendee>
							   
							   <#if attendee_index < 10 || options.showDetail>
							   <#if attendee.color??>
							   <span style="text-decoration: underline; color: ${attendee.color};">
							   <#else>
							   <span>
							   </#if>
							   ${attendee.name?html}<#if attendee_has_next && (attendee_index lt 9 || options.showDetail)>,
								</#if>
								</span>
							  
								</#if>
							</#list>
							<#if event.fattendees?size gt 10 && !options.showDetail>
								${ msg("attendee.more", event.fattendees?size - 10)}
							</#if>
						</div>
						</#if>
					</div>
				</div>
			</td>
		</tr>
		
		</#list>
</tbody>
		
	</table>
		</#list>

</td>
</tr>
</tbody>
</table>

</body>
</html>