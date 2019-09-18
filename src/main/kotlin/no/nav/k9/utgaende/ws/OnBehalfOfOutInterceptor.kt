package no.nav.k9.utgaende.ws

import no.nav.k9.inngaende.RequestContextService
import org.apache.cxf.binding.soap.SoapHeader
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.jaxb.JAXBDataBinding
import org.apache.cxf.message.Message
import org.apache.cxf.phase.AbstractPhaseInterceptor
import org.apache.cxf.phase.Phase
import org.apache.cxf.ws.security.SecurityConstants
import org.slf4j.LoggerFactory
import java.util.*
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.bind.JAXBException
import javax.xml.namespace.QName

internal class OnBehalfOfOutInterceptor(
    private val requestContextService: RequestContextService
) : AbstractPhaseInterceptor<Message>(Phase.SETUP) {

    private companion object {
        private val logger = LoggerFactory.getLogger(OnBehalfOfOutInterceptor::class.java)
        private const val OIDC_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt"
        private val callIdQname = QName("uri:no.nav.applikasjonsrammeverk", "callId")
    }

    override fun handleMessage(message: Message) {
        val token = requestContextService.getIdToken().value
        val correlationId = requestContextService.getCorrelationId().value

        val wrappedToken = wrapTokenForTransport(token.toByteArray())
        message[SecurityConstants.STS_TOKEN_ON_BEHALF_OF] = createOnBehalfOfElement(wrappedToken)

        when (message) {
            is SoapMessage ->
                try {
                    val endpointInfo = message.exchange?.endpoint?.endpointInfo
                    val service = endpointInfo?.service?.name?.localPart
                    val operation = message.exchange?.bindingOperationInfo?.name?.localPart

                    logger.info("Utgående kall til service=$service operation=$operation")

                    val header = SoapHeader(callIdQname, correlationId, JAXBDataBinding(String::class.java))
                    message.headers.add(header)
                } catch (cause: JAXBException) {
                    logger.warn("Feil ved setting av Correlation ID på request", cause)
                }
        }
    }

    private fun createOnBehalfOfElement(wrappedToken: String): Element {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(InputSource(StringReader(wrappedToken)))
        return document.documentElement
    }

    private fun wrapTokenForTransport(token: ByteArray): String {
        val base64encodedToken = Base64.getEncoder().encodeToString(token)
        return ("<wsse:BinarySecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\""
                + " EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\""
                + " ValueType=\"" + OIDC_TOKEN_TYPE + "\" >" + base64encodedToken + "</wsse:BinarySecurityToken>")
    }
}