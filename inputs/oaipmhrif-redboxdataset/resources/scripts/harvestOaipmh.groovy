/***
* This script will list all OAIPMH records, retrieving the nodes specified in the XPath query, saving each node specified in the output format. 

The script will return a list of File instances.
* 
*
* Must have the ff. in the binding:
*
* `config` - the ConfigObject harvester configuration
* 
* Author: <a href='https://github.com/shilob'>Shilo Banihit</a>
*/

import ORG.oclc.oai.harvester2.verb.*
import static java.util.UUID.randomUUID 
  
def baseUrl = config.harvest.oaiPmh?.baseUrl
def set = config.harvest.oaiPmh?.set 
def mdPrefix = config.harvest.oaiPmh?.metadataPrefix
def from = config.harvest.oaiPmh?.from
def until = config.harvest.oaiPmh?.until
def recordQuery = config.harvest.oaiPmh?.recordQuery
  
def numberOfRecs = 0
def getRecords (resToken, baseUrl, from, until, set, mdPrefix, recordQuery) {
  def listRecords = resToken ? new ListRecords(baseUrl, resToken) : new ListRecords(baseUrl, from, until, set, mdPrefix )
  def nodeList = listRecords.getNodeList(recordQuery)
  def data = [:]
  def recordsFileList = []
  data.resToken = listRecords.getResumptionToken()
  data.nodeList = nodeList
  
  for (def i=0; i < nodeList.getLength(); i++){
    def node = nodeList.item(i)
    def recordXml = groovy.xml.XmlUtil.serialize(node)
    def uuid = randomUUID() as String
    def xmlFile = new File(config.harvest.oaiPmh?.outputDir + '/'+uuid.toUpperCase() + '.xml')
    xmlFile.getParentFile().mkdirs()
    xmlFile.withWriter('UTF-8') { writer ->
      writer.write(recordXml)
    }
    recordsFileList << xmlFile
  }
  data.recordsFileList = recordsFileList
  return data
}

try {
  def masterFileList = []
  def records = getRecords(null, baseUrl, from, until, set, mdPrefix, recordQuery)
  masterFileList.addAll(records.recordsFileList)
  while (records.resToken) {
    records = getRecords(records.resToken, baseUrl, from, until, set, mdPrefix, recordQuery)
    masterFileList.addAll(records.recordsFileList)
  }
  println "**********************************"
  println masterFileList.size()
  return masterFileList
} catch (e) {
  return "Error: ${e}"
}
