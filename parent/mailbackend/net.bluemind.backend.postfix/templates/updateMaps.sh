#!/bin/bash

POSTFIXMAPS="<#list mapsfilenames as map>${map} </#list>"

for map in ${r"${POSTFIXMAPS}"}; do
	if [ -e ${r"${map}"}"-flat" ]; then
		postmap ${r"${map}"}"-flat"
	fi
done

for map in ${r"${POSTFIXMAPS}"}; do
	if [ -e ${r"${map}"}"-flat.db" ]; then
		mv -f ${r"${map}"}"-flat.db" ${r"${map}"}".db"
	fi
done

service postfix reload
