package no.nav.k9.inngaende.oppslag

import org.json.JSONArray
import org.json.JSONObject


private val barnAttributter = setOf(Attributt.barnFornavn, Attributt.barnMellomnavn, Attributt.barnEtternavn)

internal fun OppslagResultat.somJson(attributter: Set<Attributt>) : JSONObject {
    val json = JSONObject()
    if (attributter.contains(Attributt.fornavn)) json.put("fornavn", personV3!!.personnavn.fornavn)
    if (attributter.contains(Attributt.mellomnavn)) json.put("mellomnavn", personV3!!.personnavn.mellomnavn)
    if (attributter.contains(Attributt.etternavn)) json.put("etternavn", personV3!!.personnavn.etternavn)

    if (attributter.harEtterspurtBarn()) {
        val barn = JSONArray()
        personV3?.harFraRolleI?.filter { it.tilRolle.value == "BARN" }?.forEach {
            barn.put(JSONObject().apply {
                 put("fornavn", it.tilPerson.personnavn.fornavn)
                .put("mellomnavn", it.tilPerson.personnavn.mellomnavn)
                .put("etternavn", it.tilPerson.personnavn.etternavn)
            })
        }
        json.put("barn", barn)
    }

    return json
}

private fun Set<Attributt>.harEtterspurtBarn() = any { it in barnAttributter }
