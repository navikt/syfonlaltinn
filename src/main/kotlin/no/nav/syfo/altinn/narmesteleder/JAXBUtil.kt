package no.nav.syfo.altinn.narmesteleder

import com.sun.xml.bind.api.JAXBRIContext.newInstance
import generated.XMLOppgiPersonallederM
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXBElement
import javax.xml.bind.Marshaller
import javax.xml.transform.stream.StreamResult

class JAXBUtil {
    companion object {
        val JAXBContext = newInstance(
            XMLOppgiPersonallederM::class.java
        )

        fun marshall(element: XMLOppgiPersonallederM): String {
            val stringWriter = StringWriter()
            val marshaller = JAXBContext.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8")
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true)
            marshaller.marshal(element, StreamResult(stringWriter))
            return stringWriter.toString()
        }

        fun unmarshallNarmesteLederSkjema(value: String): XMLOppgiPersonallederM {
            return (JAXBContext.createUnmarshaller().unmarshal(StringReader(value)) as JAXBElement<XMLOppgiPersonallederM>).value
        }
    }
}
