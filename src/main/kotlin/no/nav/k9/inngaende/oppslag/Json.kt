package no.nav.k9.inngaende.oppslag

import org.json.JSONArray
import org.json.JSONObject

internal fun OppslagResultat.somJson(attributter: Set<Attributt>) : JSONObject {
    val json = JSONObject()

    // Meg
    if (attributter.etterspurtMeg()) {
        if (attributter.contains(Attributt.aktørId)) json.put("aktør_id", meg!!.aktørId!!.value)
        if (attributter.contains(Attributt.fornavn)) json.put("fornavn", meg!!.pdlPerson!!.fornavn)
        if (attributter.contains(Attributt.mellomnavn)) json.put("mellomnavn", meg!!.pdlPerson!!.mellomnavn)
        if (attributter.contains(Attributt.etternavn)) json.put("etternavn", meg!!.pdlPerson!!.etternavn)
        if (attributter.contains(Attributt.fødselsdato)) json.put("fødselsdato", meg!!.pdlPerson!!.fødselsdato.toString())
        if (attributter.contains(Attributt.kontonummer)) json.put("kontonummer", meg!!.tpsPerson!!.kontonummer)
    }

    // Barn
    if (attributter.etterspurtBarn()) {
        val barnJsonArray = JSONArray()
        barn?.forEach {
            barnJsonArray.put(JSONObject().apply {
                if (attributter.contains(Attributt.barnAktørId)) put("aktør_id", it.aktørId!!.value)
                if (attributter.contains(Attributt.barnIdentitetsnummer)) put("identitetsnummer", it.pdlBarn!!.ident.value)
                if (attributter.contains(Attributt.barnFornavn)) put("fornavn", it.pdlBarn!!.fornavn)
                if (attributter.contains(Attributt.barnMellomnavn)) put("mellomnavn", it.pdlBarn!!.mellomnavn)
                if (attributter.contains(Attributt.barnEtternavn)) put("etternavn", it.pdlBarn!!.etternavn)
                if (attributter.contains(Attributt.barnFødselsdato)) put("fødselsdato", it.pdlBarn!!.fødselsdato)
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

    // Personlige foretak
    if (attributter.etterspurtPersonligForetak()) {
        val personligeForetakJsonArray = JSONArray()
        personligeForetak?.forEach {
            personligeForetakJsonArray.put(JSONObject().apply {
                if (attributter.contains(Attributt.personligForetakOrganisasjonsnummer)) put("organisasjonsnummer", it.organisasjonsummer)
                if (attributter.contains(Attributt.personligForetakNavn)) put("navn", it.navn)
                if (attributter.contains(Attributt.personligForetakOrganisasjonsform)) put("organisasjonsform", it.organisasjonsform)
                if (attributter.contains(Attributt.personligForetakRegistreringsdato)) put("registreringsdato", it.registreringsdato)
                if (attributter.contains(Attributt.personligForetakOpphørsdato)) put("opphørsdato", it.opphørsdato)
            })
        }
        json.put("personlige_foretak", personligeForetakJsonArray)
    }

    return json
}
