package no.nav.k9.inngaende.oppslag

import org.json.JSONArray
import org.json.JSONObject

internal fun OppslagResultat.somJson(attributter: Set<Attributt>) : JSONObject {
    val json = JSONObject()

    // Meg
    if (attributter.etterspurtMeg()) {
        if (attributter.contains(Attributt.aktørId)) json.put("aktør_id", meg!!.aktørId!!.value)
        if (attributter.contains(Attributt.fornavn)) json.put("fornavn", meg!!.tpsPerson!!.fornavn)
        if (attributter.contains(Attributt.mellomnavn)) json.put("mellomnavn", meg!!.tpsPerson!!.mellomnavn)
        if (attributter.contains(Attributt.etternavn)) json.put("etternavn", meg!!.tpsPerson!!.etternavn)
        if (attributter.contains(Attributt.fødselsdato)) json.put("fødselsdato", meg!!.tpsPerson!!.fødselsdato.toString())
    }

    // Barn
    if (attributter.etterspurtBarn()) {
        val barnJsonArray = JSONArray()
        barn?.forEach {
            barnJsonArray.put(JSONObject().apply {
                if (attributter.contains(Attributt.barnAktørId)) put("aktør_id", it.aktørId!!.value)
                if (attributter.contains(Attributt.barnFornavn)) put("fornavn", it.tpsBarn!!.fornavn)
                if (attributter.contains(Attributt.barnMellomnavn)) put("mellomnavn", it.tpsBarn!!.mellomnavn)
                if (attributter.contains(Attributt.barnEtternavn)) put("etternavn", it.tpsBarn!!.etternavn)
                if (attributter.contains(Attributt.barnHarSammeAdresse)) put("har_samme_adresse", it.tpsBarn!!.harSammeAdresse)
                if (attributter.contains(Attributt.barnFødselsdato)) put("fødselsdato", it.tpsBarn!!.fødselsdato)
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
                if (attributter.contains(Attributt.arbeidsgivereOrganisasjonerOrganisasjonsnummer)) put("organisasjonsnummer", it.organisasjonsnummer)
                if (attributter.contains(Attributt.arbeidsgivereOrganisasjonerNavn)) put("navn", it.navn)
            })
        }
        arbeidsgivereJson.put("organisasjoner", organisasjonerJson)
        json.put("arbeidsgivere", arbeidsgivereJson)
    }
    return json
}
