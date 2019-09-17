package no.nav.k9.utgaende.ws

import no.nav.k9.inngaende.RequestContextService
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3

import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.apache.cxf.ws.security.trust.STSClient
import java.net.URI
import javax.xml.namespace.QName

internal class WebServices(
    requestContextService: RequestContextService,
    private val stsClient: STSClient
) {

    private val onBehalfOfOutInterceptor = OnBehalfOfOutInterceptor(requestContextService)

    internal fun PersonV3(serviceUrl: URI) = createOnBehalfOfServicePort(
        ServiceUrl = serviceUrl.toString(),
        ServiceClazz = PersonV3::class.java,
        Wsdl = "wsdl/no/nav/tjeneste/virksomhet/person/v3/Binding.wsdl",
        Namespace = "http://nav.no/tjeneste/virksomhet/person/v3/Binding",
        ServiceName = "Person_v3",
        EndpointName = "Person_v3Port"
    )

    internal fun ArbeidsforholdV3(serviceUrl: URI) = createOnBehalfOfServicePort(
        ServiceUrl = serviceUrl.toString(),
        ServiceClazz = ArbeidsforholdV3::class.java,
        Wsdl = "wsdl/no/nav/tjeneste/virksomhet/arbeidsforhold/v3/Binding.wsdl",
        Namespace = "http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3/Binding",
        ServiceName = "Arbeidsforhold_v3",
        EndpointName = "Arbeidsforhold_v3Port"
    )

    private fun <PORT_TYPE : Any> createOnBehalfOfServicePort(
        ServiceUrl: String,
        ServiceClazz: Class<PORT_TYPE>,
        Wsdl: String,
        Namespace: String,
        ServiceName: String,
        EndpointName: String
    ): PORT_TYPE {
        val factory = JaxWsProxyFactoryBean().apply {
            address = ServiceUrl
            wsdlURL = Wsdl
            serviceName = QName(Namespace, ServiceName)
            endpointName = QName(Namespace, EndpointName)
            serviceClass = ServiceClazz
            features = listOf(WSAddressingFeature(), LoggingFeature()) // TODO: MetricFeature() ?
        }

        val servicePort = factory.create(ServiceClazz)
        val client = ClientProxy.getClient(servicePort)
        client.outInterceptors.add(onBehalfOfOutInterceptor)
        stsClient.configureForOnBehalfOf(
            servicePort = servicePort,
            client = client
        )
        return servicePort
    }
}
