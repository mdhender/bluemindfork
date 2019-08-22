<RDF:RDF xmlns:RDF="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
         xmlns:em="http://www.mozilla.org/2004/em-rdf#">
  <RDF:Description about="urn:mozilla:extension:bm-connector-tb@blue-mind.net">
    <em:updates>
      <RDF:Seq>
        <RDF:li resource="urn:mozilla:extension:bm-connector-tb@blue-mind.net:${version}"/>
      </RDF:Seq>
    </em:updates>
  </RDF:Description>
  <RDF:Description about="urn:mozilla:extension:bm-connector-tb@blue-mind.net:${version}">
    <em:version>${version}</em:version>
    <em:targetApplication>
      <RDF:Description>
        <em:id>{3550f703-e582-4d05-9a08-453d09bdfdc6}</em:id> 
        <em:minVersion>57.0</em:minVersion>
        <em:maxVersion>100.*</em:maxVersion>
        <em:updateLink>${url}</em:updateLink>
      </RDF:Description>
    </em:targetApplication>
  </RDF:Description>
</RDF:RDF>
