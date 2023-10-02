package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import no.nav.k9.utgaende.rest.NavHeaders

class BrregProxyV1ResponseTransformer : ResponseTransformerV2 {
    override fun getName(): String {
        return "brreg-proxy-v1"
    }

    override fun transform(response: Response, serveEvent: ServeEvent): Response {
        val personIdent = serveEvent.request.getHeader(NavHeaders.PersonIdent)

        return Response.Builder.like(response)
            .body(getResponse(personIdent))
            .build()
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(personIdent: String) = when (personIdent) {
    "111111111111" -> """
    {
    	"rolle": [{
    		"orgnr": "1",
    		"rollebeskrivelse": "Innehaver",
    		"registreringsDato": "2020-01-01"
    	}, {
    		"orgnr": "2",
    		"rollebeskrivelse": "Deltaker med delt ansvar",
    		"registreringsDato": "2020-02-01"
    	}, {
    		"orgnr": "3",
    		"rollebeskrivelse": "Deltaker med fullt ansvar",
    		"registreringsDato": "2020-03-01"
    	}],
    	"statuskoder": {
    		"hovedstatus": 0,
    		"understatus": [{
    			"kode": 0,
    			"melding": "Data returnert."
    		}]
    	}
    }
    """.trimIndent()

    "22222222222" -> """
    {
    	"rolle": [{
    		"orgnr": "1",
    		"rollebeskrivelse": "Daglig leder/ adm.direktør",
    		"registreringsDato": "2020-05-05"
    	},{
    		"orgnr": "1",
    		"rollebeskrivelse": "Innehaver",
    		"registreringsDato": "2020-01-02"
    	}, {
    		"orgnr": "1",
    		"rollebeskrivelse": "Kontaktperson",
    		"registreringsDato": "2020-04-04"
    	}],
    	"statuskoder": {
    		"hovedstatus": 0,
    		"understatus": [{
    			"kode": 0,
    			"melding": "Data returnert."
    		}]
    	}
    }
    """.trimIndent()

    else -> """
    {
    	"rolle": [],
    	"statuskoder": {
    		"hovedstatus": 1,
    		"understatus": [{
    			"kode": 180,
    			"melding": "Personen $personIdent finnes ikke i vår database"
    		}]
    	}
    }
    """.trimIndent()
}


