/** reset table **/
table {
    -fs-border-spacing-horizontal: 0;
    -fs-border-spacing-vertical: 0;
    border-spacing: 0;
    border-style: none;
    border-width: 0;
    border: 0;
    padding: 0;
    margin: 0;
}
td {
    border-style: none;
    border-width: 0;
    border: 0;
    margin: 0;
    padding: 0;
}

tr {
    border-style: none;
    border-width: 0;
    border: 0;
    margin: 0;
    padding: 0;
}

thead {
    border-style: none;
    border-width: 0;
    border: 0;
    margin: 0;
    padding: 0;
}

body {
	font-size: 10pt;
	font-family:  serif;
	font-weight: lighter;
}


#page-footer {
	position: running(page-footer);
	text-align: right;
}

#page-left-footer {
	position: running(page-left-footer);
	text-align: left;
}

#page-header {
	border: 1px solid #000;
	padding: 0 1em;
	display: block;
	margin: 0.5cm 0;
	width: 17.6cm;
}

@page { 
		size: a4;
	@bottom-right {
		content: element(page-footer);
	}

@bottom-left {
	content: element(page-left-footer);
}
}

 html, body {
 	margin: 0;
 	padding: 0;
}

#page-header .calendars {
	font-weight: bold;
	font-size:8pt;
}

#page-header>.title {
	font-size:8pt;
	text-align: center;
}


#pagenumber:before {
	content: counter(page);
}

#pagecount:before {
	content: counter(pages);
}

.body {
	-fs-table-paginate: paginate;
}

.main {
	clear: both;
	width: 18.4cm;
	border-collapse: collapse;
	-fs-table-paginate: paginate;
	page-break-inside: avoid;
	border: 1px solid #000;
	margin-bottom: 0.2cm;
}



.day {
	text-align: center;
	margin: 0;
	font-weight: bold;
	font-size: 16px;
	border-bottom: 1px solid #000;
}

.td-time {
	vertical-align: top;
}

.time {
	vertical-align: top;
	padding-left: 1em;
	margin-top: 1em;
	width: 4cm;
}

.event {
}

.event-main {
	width: 13cm;	
	overflow: hidden;
	margin: 1em 0.1cm 0.5cm 0.5cm;
}

.event-main .title {
	font-weight: bolder;
}

.event-info {
	padding-left: 1em;
}

.fieldname {
	font-style: italic;
}

.before-day {
	height: 2em;
}