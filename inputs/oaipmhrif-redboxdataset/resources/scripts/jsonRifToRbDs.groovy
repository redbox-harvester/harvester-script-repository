/***
*
* Turns a RIF JSON to a RB Dataset
*
* Expects in the binding: 
*
* `config` - ConfigObject: 
*             config.harvest.oaiPmh.redbox.datasetTemplatePath - dataset template path
*             config.harvest.oaiPmh.redbox.version - ReDBox version
* `payload` - RIF JSON String
* `headers` - Message headers, when there's a Mint lookup required, headers['mintLookup'] will be a map array containing: [fld: the field name in tfpackage, type: the lookup type]
*
* Author: <a href='https://github.com/shilob'>Shilo Banihit</a>
*/

import org.springframework.integration.*
import org.springframework.integration.support.*
import groovy.util.*
import groovy.json.*

def msgHeaders = [:]
def json = new JsonSlurper().parseText(payload)
def record = json.record
def header = record.header
def metadata = record.metadata
def templateJson = new JsonSlurper().parse(new File(config.harvest.oaiPmh.redbox.datasetTemplatePath))
def workflowMeta = templateJson.data.data['workflow.metadata']

def srcDf = "yyyy-MM-dd'T'HH:mm:ss"
def targetDf = "yyyy-MM-dd"
def rbVersion = config.harvest.oaiPmh.redbox.version
  
templateJson.data.data.with {
  def regObj = metadata.registryObjects.registryObject
  def identifier = record.header.identifier
  // Basics
  datasetId = identifier.substring(identifier.lastIndexOf('/') + 1)
   workflowMeta.formData.description =  tfpackage.description = tfpackage['dc:description'] =  regObj.collection.description.$
  workflowMeta.formData.title = tfpackage.title = tfpackage['dc:title'] = regObj.collection.name.namePart
  tfpackage['redbox:formVersion'] = rbVersion
  // Dates
  tfpackage['dc:created'] = convertDate(srcDf, targetDf, regObj.collection.dateAccessioned)
  if (regObj.collection.dateModified) {
    tfpackage['dc:modified'] = convertDate(srcDf, targetDf, regObj.collection.dateModified)
  }
  // Location
  if (regObj.collection.location?.address) {
    int addressCtr = 1
    if (isCollectionOrArray(regObj.collection.location.address)) {
      regObj.collection.location.address.each {
        ctr = addAddress(addressCtr, it, tfpackage)
      }
    } else {
      addAddress(addressCtr, regObj.collection.location.address, tfpackage)
    }
  }
  // Coverage 
  int spatialCtr = 1
  if (isCollectionOrArray(regObj.collection.coverage)) {
    regObj.collection.coverage.each {coverageObj->
      spatialCtr = addCoverage(spatialCtr, coverageObj, tfpackage, srcDf, targetDf)
    }
  } else {
    spatialCtr = addCoverage(spatialCtr, regObj.collection.coverage, tfpackage, srcDf, targetDf)
  }
  
  // Relations: NLA, Service, etc.
  def relCtrs = [person:1, service:1]
  if (isCollectionOrArray(regObj.collection.relatedObject)) {
    regObj.collection.relatedObject.each {relatedObj->
      addRelatedObj(relCtrs, relatedObj, tfpackage,msgHeaders)
    }
  } else {
    addRelatedObj(relatedCtrs, regObj.collection.relatedObject, tfpackage,msgHeaders)
  }
  
  // Subject: Keywords, FOR / SEO, ANZSRC TOA
  def subjCtrs = [keyword:1, for:1, seo:1]
  if (isCollectionOrArray(regObj.collection.subject)) {
    regObj.collection.subject.each { subjectObj->
      addSubject(subjCtrs, subjectObj, tfpackage, msgHeaders)
    }
  } else {
    addSubject(subjCtrs, regObj.collection.subject, tfpackage, msgHeaders)
  }
  
  // Rights
  if (regObj.collection.rights) {
    if (regObj.collection.rights.rightsStatement) {
      tfpackage["dc:accessRights.dc:RightsStatement.skos:prefLabel"] = regObj.collection.rights.rightsStatement instanceof String ? regObj.collection.rights.rightsStatement : regObj.collection.rights.rightsStatement.$
      tfpackage["dc:accessRights.dc:identifier"] = regObj.collection.rights.rightsStatement instanceof String ? '' : regObj.collection.rights.rightsStatement?.rightsUri
    } 
    if (regObj.collection.rights.accessRights) {
      tfpackage["dc:accessRights.skos:prefLabel"] = regObj.collection.rights.accessRights instanceof String ? regObj.collection.rights.accessRights : regObj.collection.rights.accessRights.$
      tfpackage["dc:accessRights.dc:RightsStatement.dc:identifier"] = regObj.collection.rights.accessRights instanceof String ? '' : regObj.collection.rights.accessRights?.rightsUri
    } 
    if (regObj.collection.rights.license) {
      tfpackage["dc:license.skos:prefLabel"] = regObj.collection.rights.license instanceof String ? regObj.collection.rights.license : regObj.collection.rights.license.$
      tfpackage["dc:license.dc:identifier"] = regObj.collection.rights.license instanceof String ? '' : regObj.collection.rights.license?.rightsUri
    } 
  }
  // Related Info
  def relInfoCtrs = [publication:1, website:1, service:1]
  if (isCollectionOrArray(regObj.collection.relatedInfo)) {
    regObj.collection.relatedInfo.each {
      addRelatedInfo(it, relInfoCtrs, tfpackage)
    }
  } else {
    addRelatedInfo(regObj.collection.relatedInfo, relInfoCtrs, tfpackage)
  }
  // Citation
  if (regObj.collection.citationInfo) {
    tfpackage['dc:biblioGraphicCitation.redbox:sendCitation'] = 'on'
    if (regObj.collection.citationInfo.fullCitation) {
      tfpackage['dc:biblioGraphicCitation.skos:prefLabel'] = regObj.collection.citationInfo.fullCitation.$
    }
  }
  // end of Mapping
}


def payloadString = JsonOutput.toJson(templateJson)
 
final Message<String> message = MessageBuilder.withPayload(payloadString).copyHeaders(msgHeaders).build()

return message

/* *******************************************************************/
/* Methods
/* *******************************************************************/

boolean isCollectionOrArray(object) {    
  [Collection, Object[]].any { it.isAssignableFrom(object.getClass()) }
}

def getHeaders(msgHeaders) {
  if (!msgHeaders['mintLookup']) msgHeaders['mintLookup'] = []
  return msgHeaders['mintLookup']
}

def addAddress(ctr, addressObj, tfpackage) {
  if (addressObj.physical) {
    tfpackage['vivo:Location.vivo:GeographicLocation.gn:name'] = addressObj.physical.addressPart.$
  } else if (addressObj.electronic) {
    tfpackage['bibo:Website.'+ctr+'.dc:identifier'] = addressObj.electronic.value
    ctr++
  }
  return ctr
}

def convertDate(fromDf, toDf, dateStr) {
  return Date.parse(fromDf, dateStr).format(toDf) 
}

def addRelatedInfo(relInfo, relInfoCtrs, tfpackage) {
  int ctr
  String fld,id,title
  id = relInfo.identifier.$
  title = relInfo.identifier.title ? relInfo.identifier.title : ''
  switch (relInfo.type) {
    case 'publication':
      fld = "dc:relation.swrc:Publication."
      ctr = relInfoCtrs.publication
      relInfoCtrs.publication++
      break;
    case 'website':
      fld = "dc:relation.bibo:Website."
      ctr = relInfoCtrs.website
      relInfoCtrs.website++
      break;
    case 'service':
      fld = "dc:relation.vivo:Service."
      ctr = relInfoCtrs.service
      relInfoCtrs.service++
      break;
  }
  if (fld) {
    tfpackage[fld + ctr + ".dc:identifier"] = id
    tfpackage[fld + ctr + ".dc:title"] = title
  }
}

def addSubject(subjCtrs, subjectObj, tfpackage, msgHeaders) {
  def subjType = subjectObj.type
  def hdrType = ''
  // Keyword
  if (subjType == 'local') {
    tfpackage['dc:subject.vivo:keyword.' + subjCtrs.keyword + '.rdf:PlainLiteral'] = subjectObj.$
    subjCtrs.keyword++
    return
  }
  if (subjType == 'anzsrc-for' || subjType == 'anzsrc-seo') {
    // this value will need to be looked up later
    def key = ''
    def idx
    if (subjType == 'anzsrc-for') {
      idx = subjCtrs.for
      key =  'dc:subject.anzsrc:for.' + idx + '.rdf:resource' 
      hdrType = 'anzsrc_for'
      subjCtrs.for++
    } else {
      idx = subjCtrs.seo
      key = 'dc:subject.anzsrc:seo.' + idx + '.rdf:resource' 
      hdrType = 'anzsrc_seo'
      subjCtrs.seo++
    }
    tfpackage[key] = subjectObj.$
    getHeaders(msgHeaders) << [fld:key, type:hdrType, idx: idx]
    return
  }
  if (subjType == 'anzsrc-toa') {
    tfpackage['dc:subject.anzsrc:toa.skos:prefLabel'] = subjectObj.$
    return
  }
}

def addRelatedObj(relCtrs, relatedObj, tfpackage, msgHeaders) {
  def relType = relatedObj.relation?.type
  // person
  if (relType == 'hasCollector') {
    def key = 'dc:creator.foaf:Person.' + relCtrs.person + '.dc:identifier'
    tfpackage[key] = relatedObj.key
    getHeaders(msgHeaders) << [fld:key, type:'person', idx:relCtrs.person]
    relCtrs.person++
    return
  }
}

def addCoverage(spatialCtr, coverageObj, tfpackage, srcDf, targetDf) {
  def literal = coverageObj?.temporal?.text ? coverageObj?.temporal?.text : coverageObj?.spatial?.$
  if (literal) {
    // Temporal
    if (coverageObj?.temporal?.text) 
      tfpackage['dc:coverage.redbox:timePeriod'] = "${literal}"
    if (coverageObj.temporal?.date) { 
      // having 2 dates is not guaranteed
      if (isCollectionOrArray(coverageObj.temporal.date)) {
        coverageObj.temporal.date.each { dtObj ->
          addTemporalDate(dtObj, srcDf, targetDf, tfpackage)
        }
      } else {
        // single dates
        addTemporalDate(coverageObj.temporal.date, srcDf, targetDf, tfpackage)
      }
    }
    // Spatial
    if (coverageObj.spatial) {
      if (isCollectionOrArray(coverageObj.spatial)) {
        coverageObj.spatial.each { spatialObj ->
          spatialCtr = addSpatial(spatialCtr, spatialObj, tfpackage)
        }
      } else {
        spatialCtr = addSpatial(spatialCtr, coverageObj.spatial, tfpackage)
      }
    }
  }
  return spatialCtr
}

def addTemporalDate(dtObj, srcDf, targetDf, tfpackage) {
  if (dtObj.type == 'dateFrom')
    tfpackage['dc:coverage.vivo:DateTimeInterval.vivo:start'] = convertDate(srcDf, targetDf, dtObj.$)
  else if (dtObj.type == 'dateTo')
    tfpackage['dc:coverage.vivo:DateTimeInterval.vivo:end'] = convertDate(srcDf, targetDf, dtObj.$)
}

def addSpatial(ctr, spatialObj, tfpackage) {
  tfpackage['dc:coverage.vivo:GeographicLocation.'+ctr+'.dc:type'] = spatialObj.type
  if (spatialObj.type == 'dcmiPoint') {
    def arr = spatialObj.$.split(';')
    tfpackage['dc:coverage.vivo:GeographicLocation.'+ctr+'.rdf:PlainLiteral'] = arr[0]
    tfpackage['dc:coverage.vivo:GeographicLocation.'+ctr+'.geo:long'] = arr[1].split('=')[1]
    tfpackage['dc:coverage.vivo:GeographicLocation.'+ctr+'.geo:lat'] = arr[2].split('=')[1]
  } else {
    tfpackage['dc:coverage.vivo:GeographicLocation.'+ctr+'.rdf:PlainLiteral'] = spatialObj.$
  }
  return ++ctr
}