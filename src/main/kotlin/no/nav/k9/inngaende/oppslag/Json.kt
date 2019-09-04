package no.nav.k9.inngaende.oppslag

import org.json.JSONObject

internal fun OppslagResultat.somJson(attributter: Set<Attributt>) : JSONObject {
    val json = JSONObject()
    if (attributter.contains(Attributt.fornavn)) json.put("fornavn", personV3!!.personnavn.fornavn)
    if (attributter.contains(Attributt.mellomnavn)) json.put("mellomnavn", personV3!!.personnavn.mellomnavn)
    if (attributter.contains(Attributt.etternavn)) json.put("etternavn", personV3!!.personnavn.etternavn)
    return json
}