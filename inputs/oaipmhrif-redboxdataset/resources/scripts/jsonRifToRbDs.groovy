/***
*
* Turns a RIF JSON to a RB Dataset
*
* Expects in the binding: 
*
* `config` - ConfigObject
* `payload` - RIF JSON String
*
* Author: <a href='https://github.com/shilob'>Shilo Banihit</a>
*/

import groovy.util.*
import groovy.json.*

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
  
  // Relations
  
  // NLA creators
  
  // Keywords
  
  // FOR / SEO
  
  // Rights
  
  // Related Info
  
  // Citation
  
  
}

return JsonOutput.toJson(templateJson)

/* *******************************************************************/
/* Helper methods 
/* *******************************************************************/

boolean isCollectionOrArray(object) {    
  [Collection, Object[]].any { it.isAssignableFrom(object.getClass()) }
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