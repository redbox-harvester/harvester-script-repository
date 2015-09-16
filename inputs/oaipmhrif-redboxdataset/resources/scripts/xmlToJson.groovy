/**
*
* Converts the XML contents in `payload` into JSON string
* 
* Must have the ff. in the binding:
* 
* `payload` - the File instance
* `xslPath` - the String path to the XSL used in transformation
*
*/

import javax.xml.transform.*
import javax.xml.transform.stream.*

if (!payload || !xslPath) {
  println "Error, script halted, no 'payload' or 'xslPath' in the binding."
  return null 
}
Source xslt = new StreamSource(new File(xslPath)) 
Transformer transformer = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null).newTransformer(xslt);
//Source text = new StreamSource(payload.text);
Source text = new StreamSource(payload);
StringWriter output = new StringWriter();
transformer.transform(text, new StreamResult(output));
def jsonStr = output.toString()
return jsonStr
