CACHE MANIFEST
# Current Language : ${lang}
# Version : ${version}
# Explicitly cached entries.

CACHE:

<#assign seq = files>
<#list seq as file>
${file}
</#list>
# favicon.ico

FALLBACK:
/cal/ /cal/index.html

NETWORK:
*
