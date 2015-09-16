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

def srcDf = "yyyy-MM-dd'T'mm:hh:ss"
def targetDf = "yyyy-MM-dd"
  
templateJson.data.data.with {
  def regObj = metadata.registryObjects.registryObject
  def identifier = record.header.identifier
  // Basics
  datasetId = identifier.substring(identifier.lastIndexOf('/') + 1)
  workflowMeta.formData.description =  tfpackage.description = tfpackage['dc:description'] =  regObj.collection.description.$
  workflowMeta.formData.title = tfpackage.title = tfpackage['dc:title'] = regObj.collection.name.namePart
  // Dates
  tfpackage['dc:created'] = Date.parse(srcDf, regObj.collection.dateAccessioned).format(targetDf)
  if (regObj.collection.dateModified) {
    tfpackage['dc:modified'] = Date.parse(srcDf, regObj.collection.dateModified).format(targetDf)
  }
  // Location
  if (regObj.collection.location.address) {
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
  
  // Geospatial
  
  // Relations
  
  // NLA creators
  
  // Keywords
  
  // FOR / SEO
  
  // Rights
  
  // Related Info
  
  // Citation
  
  
}

return JsonOutput.prettyPrint(JsonOutput.toJson(templateJson))


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