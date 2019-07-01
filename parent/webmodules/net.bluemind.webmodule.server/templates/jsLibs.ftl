<script>
  ${runtime}
</script>

<script>
<#list jsLinks as jsLink> 
	bmLoadBundle('${jsLink.bundle}',"${jsLink.path}",${jsLink.lifecycle?c});
</#list>
</script>