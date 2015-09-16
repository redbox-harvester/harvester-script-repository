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
    pollRate = '60000' // determines how often to run the inboundScript below
    pollTimeout = '60000'
    oaiPmh {
      inboundScript = 'resources/scripts/harvestOaipmh.groovy'
      xmlToJsonScript = 'resources/scripts/xmlToJson.groovy'
      jsonRifToRbDsScript = 'resources/scripts/jsonRifToRbDs.groovy'
      baseUrl = 'https://rdmp.sydney.edu.au/redbox/verNum1.8/published/feed/oai'
      outputDir = 'output/'
      fileOutput = ''
      metadataPrefix = 'rif'
      set = 'published'
      from = '2014-06-30T02:27:20Z'
      until = '2015-06-30T02:27:20Z'
      recordQuery = '/oai20:OAI-PMH/oai20:ListRecords/oai20:record'
      xslPath = 'resources/xslt/xml-to-json.xsl'
      redbox {
        datasetTemplatePath = 'resources/scripts/template-data/dataset-template.json'
        version = '1.9-SNAPSHOT'
      }
    }
    activemq {
    	url = 'tcp://localhost:9101'
    }
}