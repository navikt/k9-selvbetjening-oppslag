package no.nav.k9.utgaende.ws

import org.apache.cxf.Bus
import org.apache.cxf.BusFactory
import org.apache.cxf.binding.soap.Soap12
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.endpoint.Client
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.ws.policy.PolicyBuilder
import org.apache.cxf.ws.policy.PolicyEngine
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver
import org.apache.cxf.ws.security.SecurityConstants
import org.apache.cxf.ws.security.trust.STSClient
import org.apache.neethi.Policy
import java.net.URI

private const val STS_CLIENT_AUTHENTICATION_POLICY = "classpath:ws/untPolicy.xml"
private const val STS_SAML_POLICY = "classpath:ws/requestSamlPolicy.xml"

internal object WebServiceSTSClient {
    internal fun instance(
        stsUrl: URI,
        username: String,
        password: String): STSClient {
        val bus = BusFactory.getDefaultBus()
        return STSClient(bus).apply {
            isEnableAppliesTo = false
            isAllowRenewing = false

            location = stsUrl.toString()
            features = listOf(LoggingFeature())

            properties = mapOf(
                SecurityConstants.USERNAME to username,
                SecurityConstants.PASSWORD to password
            )
            setPolicy(bus.resolvePolicy(STS_CLIENT_AUTHENTICATION_POLICY))
        }
    }
}


internal fun STSClient.configureForOnBehalfOf(
    servicePort: Any,
    client: Client = ClientProxy.getClient(servicePort)
) {
    client.configureSTS(
        stsClient = this,
        cacheIssuedTokenInEndpoint = false
    )
}

private fun Client.configureSTS(
    stsClient: STSClient,
    cacheIssuedTokenInEndpoint : Boolean
) {
    requestContext[SecurityConstants.STS_CLIENT] = stsClient
    requestContext[SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT] = cacheIssuedTokenInEndpoint
    setClientEndpointPolicy(bus.resolvePolicy(STS_SAML_POLICY))
}

private fun Bus.resolvePolicy(policyUri: String): Policy {
    val registry = getExtension(PolicyEngine::class.java).registry
    val resolved = registry.lookup(policyUri)

    val policyBuilder = getExtension(PolicyBuilder::class.java)
    val referenceResolver = RemoteReferenceResolver("", policyBuilder)

    return resolved ?: referenceResolver.resolveReference(policyUri)
}

private fun Client.setClientEndpointPolicy(policy: Policy) {
    val policyEngine: PolicyEngine = bus.getExtension(PolicyEngine::class.java)
    val message = SoapMessage(Soap12.getInstance())
    val endpointPolicy = policyEngine.getClientEndpointPolicy(endpoint.endpointInfo, null, message)
    policyEngine.setClientEndpointPolicy(endpoint.endpointInfo, endpointPolicy.updatePolicy(policy, message))
}
