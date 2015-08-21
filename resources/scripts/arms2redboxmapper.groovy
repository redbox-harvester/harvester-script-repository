import org.springframework.integration.Message
import org.springframework.integration.annotation.Header
import org.springframework.integration.annotation.Payload
import org.springframework.integration.annotation.Transformer
import org.springframework.integration.file.FileHeaders
import org.springframework.integration.support.MessageBuilder
import groovy.util.ConfigObject
import groovy.json.*


class ARMS2RedboxMapper {

   ConfigObject config

   @Transformer
   public	Message<String> handleJson(final Message<String> inputMessage) {
     //comes through as a byte array for some reason...
     String payload = new String(inputMessage.getPayload())
     String type = inputMessage.getHeaders().get("type")
     def payloadJson = new JsonSlurper().parseText(payload)
     def templateJson = new JsonSlurper().parse(new FileReader(new File(config.redboxTemplatePath)))

     templateJson['data']['data']['datasetId'] = payloadJson['oid']
     templateJson['data']['data']['workflow.metadata']['formData']['description'] = payloadJson['collection:description']
     templateJson['data']['data']['workflow.metadata']['formData']['title'] = payloadJson['dc:title']

     templateJson['data']['data']['tfpackage']['title'] = payloadJson['dc:title']
     templateJson['data']['data']['tfpackage']['dc:title'] = payloadJson['dc:title']
     templateJson['data']['data']['tfpackage']['dc:description'] = payloadJson['collection:description']
     templateJson['data']['data']['tfpackage']['description'] = payloadJson['collection:description']
     templateJson['data']['data']['tfpackage']['locrel:dtm.foaf:Agent.foaf:name'] = payloadJson['dataprovider:givenName'] + " " + payloadJson['dataprovider:familyName']

     //Set the primary contact
    //  templateJson['data']['data']['tfpackage']['locrel:prc.foaf:Person.foaf:title'] = payloadJson['dataprovider:title']
     templateJson['data']['data']['tfpackage']['locrel:prc.foaf:Person.foaf:givenName'] = payloadJson['dataprovider:givenName']
     templateJson['data']['data']['tfpackage']['locrel:prc.foaf:Person.foaf:familyName'] = payloadJson['dataprovider:familyName']
     templateJson['data']['data']['tfpackage']['locrel:prc.foaf:Person.foaf:email'] = payloadJson['dataprovider:email']

     templateJson['data']['data']['tfpackage']['bibo:Website.1.dc:identifier'] = payloadJson['discovery-metadata']

     templateJson['data']['data']['tfpackage']['armsRequestOid'] = payloadJson['oid']

     def payloadString = JsonOutput.toJson(templateJson)
  	 final Message<String> message = MessageBuilder.withPayload(JsonOutput.prettyPrint(payloadString)).setHeader("type",type)
  					.build()

  		return message
  	}

}
