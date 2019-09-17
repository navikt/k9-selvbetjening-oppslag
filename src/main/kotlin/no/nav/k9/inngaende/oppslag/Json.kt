package no.nav.k9.inngaende.oppslag

import org.json.JSONArray
import org.json.JSONObject
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.XMLGregorianCalendar

internal fun OppslagResultat.somJson(attributter: Set<Attributt>) : JSONObject {
    val json = JSONObject()

    // Meg
    if (attributter.etterspurtMeg()) {
        if (attributter.contains(Attributt.aktørId)) json.put("aktør_id", meg!!.aktørId!!.value)
        if (attributter.contains(Attributt.fornavn)) json.put("fornavn", meg!!.person!!.personnavn.fornavn)
        if (attributter.contains(Attributt.mellomnavn)) json.put("mellomnavn", meg!!.person!!.personnavn.mellomnavn)
        if (attributter.contains(Attributt.etternavn)) json.put("etternavn", meg!!.person!!.personnavn.etternavn)
        if (attributter.contains(Attributt.fødselsdato)) json.put("fødselsdato", meg!!.person!!.foedselsdato.foedselsdato.iso8601date())
    }

    // Barn
    if (attributter.etterspurtBarn()) {
        val barnJsonArray = JSONArray()
        barn?.forEach {
            barnJsonArray.put(JSONObject().apply {
                if (attributter.contains(Attributt.barnAktørId)) put("aktør_id", it.aktørId!!.value)
                if (attributter.contains(Attributt.barnFornavn)) put("fornavn", it.person!!.personnavn.fornavn)
                if (attributter.contains(Attributt.barnMellomnavn)) put("mellomnavn", it.person!!.personnavn.mellomnavn)
                if (attributter.contains(Attributt.barnEtternavn)) put("etternavn", it.person!!.personnavn.etternavn)
                if (attributter.contains(Attributt.barnFødselsdato)) put("fødselsdato", it.person!!.foedselsdato.foedselsdato.iso8601date())
            })

        }
        json.put("barn", barnJsonArray)
    }

    // Arbeidsgivere
    if (attributter.etterspurtArbeidsgibereOrganaisasjoner()) {
        val arbeidsgivereJson = JSONObject()
        val organisasjonerJson = JSONArray()
        arbeidsgivereOrganisasjoner?.forEach {
            organisasjonerJson.put(JSONObject().apply {
                if (attributter.contains(Attributt.arbeidsgivereOrganisasjonerOrganisasjonsnummer)) put("organisasjonsnummer", it.orgnummer)
                if (attributter.contains(Attributt.arbeidsgivereOrganisasjonerNavn)) put("navn", it.navn)
            })
        }
        arbeidsgivereJson.put("organisasjoner", organisasjonerJson)
        json.put("arbeidsgivere", arbeidsgivereJson)
    }
    return json
}


internal fun XMLGregorianCalendar.iso8601date() = "$year-${month.toString().padStart(2,'0')}-${(if (day == DatatypeConstants.FIELD_UNDEFINED) 1 else day).toString().padStart(2,'0')}"