package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.k9.utgaende.rest.NavHeaders

class BrregProxyV1ResponseTransformer : ResponseTransformer() {
    override fun transform(
        request: Request?,
        response: Response?,
        files: FileSource?,
        parameters: Parameters?
    ): Response {
        val personIdent = request!!.getHeader(NavHeaders.PersonIdent)

        return Response.Builder.like(response)
            .body(getResponse(personIdent))
            .build()
    }

    override fun getName(): String {
        return "brreg-proxy-v1"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(personIdent: String) = when (personIdent) {
    "111111111111" -> """
    {
    	"rolle": [{
    		"orgNr": "1",
    		"rollebeskrivelse": "Innehaver",
    		"registreringsDato": "2020-01-01"
    	}, {
    		"orgNr": "2",
    		"rollebeskrivelse": "Deltaker med proratarisk ansvar (delt ansvar)",
    		"registreringsDato": "2020-02-01"
    	}, {
    		"orgNr": "3",
    		"rollebeskrivelse": "Deltaker med solidarisk ansvar (fullt ansvarlig)",
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
    else  -> """
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


