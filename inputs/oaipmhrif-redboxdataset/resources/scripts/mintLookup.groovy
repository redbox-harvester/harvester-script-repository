/***
*
* Lookups IDs in ReDBox/Mint and enriches the dataset JSON string payload.
*
* Expects in the binding: 
*
* `config` - ConfigObject 
* `payload` - a JSON string representing a RB dataset on its way to the harvester
* `headers` - Message headers, headers['mintLookup'] will be a map array containing: [fld: the field name in tfpackage, type: the lookup type must match to the config type above, idx: the index to use ]
*
* Author: <a href='https://github.com/shilob'>Shilo Banihit</a>
*/
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')

import groovy.util.*
import groovy.json.*
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
  
def jsonSlurper = new JsonSlurper()
def payloadJson = jsonSlurper.parseText(payload)
def tfpackage = payloadJson.data.data.tfpackage
def client = new RESTClient( config.harvest.mintLookup.base)
  
headers['mintLookup'].each { lookup ->
  def typeConfig = config.harvest.mintLookup.types[lookup.type]
  if (typeConfig) {
    def value = tfpackage[lookup.fld]
    def uri = new URIBuilder(client.uri.toString() + typeConfig.uri + value)
    def resp = client.get( uri : uri, contentType: JSON ) 
    if (resp.status == 200) {
      def respJson = resp.data
      if (respJson.OpenSearchResponse.totalResults != "0") {
        typeConfig.mapping.each { k,v ->
          def targetFld = "${typeConfig.baseFld}${lookup.idx}${k}"
          tfpackage[targetFld] = Eval.x(respJson, "x${v}")
          println "****** Set tfpackage['${targetFld}'] = '${tfpackage[targetFld]}', using  expression '${v}' ********"
        }
      } else {
        println "Lookup of type '${lookup.type}' had no result, using value: '${value}'"
      }
    } else {
      println "Lookup of type '${lookup.type}', using value: '${value}' HTTP request failed: ${resp.status}"
    }
  } else {
    println "No config for type: '${lookup.type}' with value: '${value}'"
  }
}

return JsonOutput.toJson(payloadJson)