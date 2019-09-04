package no.nav.k9.utgaende.cxf

import no.nav.k9.inngaende.RequestContextService
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import java.net.URI
import javax.xml.namespace.QName

internal class WebServices(
    requestContextService: RequestContextService
) {
    private val onBehalfOfOutInterceptor = OnBehalfOfOutInterceptor(requestContextService)

    internal fun PersonV3(serviceUrl: URI) = createServicePort(
        serviceUrl.toString(),
        ServiceClazz = PersonV3::class.java,
        Wsdl = "wsdl/no/nav/tjeneste/virksomhet/person/v3/Binding.wsdl",
        Namespace = "http://nav.no/tjeneste/virksomhet/person/v3/Binding",
        ServiceName = "Person_v3",
        EndpointName = "Person_v3Port"
    )

    private fun <PORT_TYPE> createServicePort(
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
            outInterceptors.add(onBehalfOfOutInterceptor)
        }

        return factory.create(ServiceClazz)
    }
}
