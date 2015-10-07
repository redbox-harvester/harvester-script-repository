client {
    harvesterId = 'oaiPmhReDBoxHarvester'
    description = 'OAIPMH -> File XML -> ReDBox Dataset'
    base = ''
    autoStart = true
    siPath = 'applicationContext-SI-harvester-console.xml'
    classPathEntries = []
}
file {
    runtimePath = 'config/runtime/harvester-config-console.groovy'
    customPath = 'config/custom/harvester-config-console.groovy'
}
harvest {
    pollRate = '1200000' // determines how often to run the inboundScript below
    pollTimeout = '1200000'
    oaiPmh {
      inboundScript = 'resources/scripts/harvestOaipmh.groovy'
      xmlToJsonScript = 'resources/scripts/xmlToJson.groovy'
      jsonRifToRbDsScript = 'resources/scripts/jsonRifToRbDs.groovy'
      mintLookupScript = 'resources/scripts/mintLookup.groovy'
      baseUrl = 'https://rdmp.sydney.edu.au/redbox/verNum1.8/published/feed/oai'
      outputDir = 'output/'
      fileOutput = ''
      metadataPrefix = 'rif'
      set = 'published'
      from = '2015-05-24T06:27:33Z'
      until = '2015-09-24T06:27:33Z'
      recordQuery = '/oai20:OAI-PMH/oai20:ListRecords/oai20:record'
      xslPath = 'resources/xslt/xml-to-json.xsl'
      redbox {
        datasetTemplatePath = 'resources/scripts/template-data/dataset-template.json'
        version = '1.9-SNAPSHOT'
      }
    }
    mintLookup {
      base = 'https://rdmp.sydney.edu.au/redbox/verNum1.8-SNAPSHOT/default/'
      nlaBase = 'http://www.nla.gov.au/apps/srw/search/peopleaustralia?version=1.1&operation=searchRetrieve&recordSchema=urn%3Aisbn%3A1-931666-33-4&maximumRecords=10&startRecord=1&resultSetTTL=300&recordPacking=xml&recordXPath=&sortKeys=&query=dc.identifier+%3D+'
      types {
        anzsrc_for {
          uri = 'proxyGet.script?ns=ANZSRC_FOR&qs=searchTerms%3D'
          baseFld = 'dc:subject.anzsrc:for.'
          mapping = ['.skos:prefLabel':".results[0]['skos:prefLabel']", '.rdf:about':".results[0]['rdf:about']"]
        }
        anzsrc_seo {
          uri = 'proxyGet.script?ns=ANZSRC_SEO&qs=searchTerms%3D'
          baseFld = 'dc:subject.anzsrc:seo.'
          mapping = ['.skos:prefLabel':".results[0]['skos:prefLabel']", '.rdf:about':".results[0]['rdf:about']"]
        }
        person {
          uri = 'proxyGet.script?ns=Parties_People&qs=searchTerms%3D'
          baseFld = 'dc:creator.foaf:Person.'
          mapping = ['.dc:identifier':".results[0]['dc:identifier'", 
                     '.foaf:name':".results[0]['rdfs:label'", 
                     '.foaf:givenName':".results[0]['result-metadata'].all.Given_Name[0]",
                     '.foaf:familyName':".results[0]['result-metadata'].all.Family_Name[0]",
                     '.foaf:title':".results[0]['result-metadata'].all.Honorific[0]"
                    ]
        }
      }
    }
    activemq {
    	url = 'tcp://localhost:9101'
    }
}