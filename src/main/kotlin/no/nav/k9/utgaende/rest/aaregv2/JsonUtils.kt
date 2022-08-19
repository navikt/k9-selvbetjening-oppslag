package no.nav.k9.utgaende.rest.aaregv2

import no.nav.k9.utgaende.rest.*
import no.nav.k9.utgaende.rest.Frilansoppdrag
import no.nav.k9.utgaende.rest.OrganisasjonArbeidsgivere
import no.nav.k9.utgaende.rest.PrivatArbeidsgiver
import no.nav.k9.utgaende.rest.TypeArbeidssted.Companion.somTypeArbeidssted
import no.nav.k9.utgaende.rest.getStringOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

internal fun JSONArray.hentOrganisasjonerV2(fraOgMed: LocalDate, tilOgMed: LocalDate): Set<OrganisasjonArbeidsgivere> =
    hentArbeidsgivereMedAnsettelseperiodeV2()
        .filterNot { it.erFrilansaktivitet() }
        .filter { it.arbeidstedErUnderenhet() }
        .map { ansettelsesforhold ->
            val (ansattFom, ansattTom) = ansettelsesforhold.hentStartdatoOgSluttdatoFraAnsettelseperiode()

            OrganisasjonArbeidsgivere(
                organisasjonsnummer = ansettelsesforhold.hentOrganisasjonsnummer(),
                ansattFom = LocalDate.parse(ansattFom),
                ansattTom = ansattTom?.let { LocalDate.parse(it) }
            )
        }
        .filter { erAnsattIPerioden(it.ansattFom, it.ansattTom, fraOgMed, tilOgMed) }
        .sortedBy { it.ansattFom }
        .distinctBy { it.organisasjonsnummer }
        .toSet()

internal fun JSONArray.hentFrilansoppdragV2(fraOgMed: LocalDate, tilOgMed: LocalDate): Set<Frilansoppdrag> =
    hentArbeidsgivereMedAnsettelseperiodeV2()
        .filter { it.erFrilansaktivitet() }
        .map { ansettelsesforhold ->
            val (ansattFom, ansattTom) = ansettelsesforhold.hentStartdatoOgSluttdatoFraAnsettelseperiode()

            val offentligIdent = if (ansettelsesforhold.arbeidstedErPerson()) ansettelsesforhold.hentFolkeregistrertIdent() else null
            val organisasjonsnummer = if (ansettelsesforhold.arbeidstedErUnderenhet()) ansettelsesforhold.hentOrganisasjonsnummer() else null

            Frilansoppdrag(
                type = ansettelsesforhold.arbeidsstedType().somTypeArbeidssted(),
                organisasjonsnummer = organisasjonsnummer,
                offentligIdent = offentligIdent,
                ansattFom = LocalDate.parse(ansattFom),
                ansattTom = ansattTom?.let { LocalDate.parse(it) }
            )
        }
        .filter { erAnsattIPerioden(it.ansattFom, it.ansattTom, fraOgMed, tilOgMed) }
        .toSet()

internal fun JSONArray.hentPrivateArbeidsgivereV2(fraOgMed: LocalDate, tilOgMed: LocalDate): Set<PrivatArbeidsgiver> =
    hentArbeidsgivereMedAnsettelseperiodeV2()
        .filterNot { it.erFrilansaktivitet() }
        .filter { it.arbeidstedErPerson() }
        .map { ansettelsesforhold ->
            val (ansattFom, ansattTom) = ansettelsesforhold.hentStartdatoOgSluttdatoFraAnsettelseperiode()

            PrivatArbeidsgiver(
                offentligIdent = ansettelsesforhold.hentFolkeregistrertIdent(),
                ansattFom = LocalDate.parse(ansattFom),
                ansattTom = ansattTom?.let { LocalDate.parse(it) }
            )
        }
        .filter { erAnsattIPerioden(it.ansattFom, it.ansattTom, fraOgMed, tilOgMed) }
        .sortedBy { it.ansattFom }
        .distinctBy { it.offentligIdent }
        .toSet()

private fun JSONArray.hentArbeidsgivereMedAnsettelseperiodeV2(): Sequence<JSONObject> = this
    .asSequence()
    .map { it as JSONObject }
    .filter { it.has("arbeidssted") }
    .filter { it.has("ansettelsesperiode") && it.getJSONObject("ansettelsesperiode").has("startdato") }

private fun JSONObject.hentStartdatoOgSluttdatoFraAnsettelseperiode(): Pair<String, String?> {
    val ansettelsesperiode = this.getJSONObject("ansettelsesperiode")
    return Pair(ansettelsesperiode.getString("startdato"), ansettelsesperiode.getStringOrNull("sluttdato"))
}

private fun JSONObject.hentIdentAvGittTypeFraArbeidssted(type: IdentType) = this
    .getJSONObject("arbeidssted")
    .getJSONArray("identer")
    .map { it as JSONObject }
    .find { it.getString("type") == type.toString() }!!
    .getString("ident")

private fun JSONObject.hentFolkeregistrertIdent() = hentIdentAvGittTypeFraArbeidssted(IdentType.FOLKEREGISTERIDENT)
private fun JSONObject.hentOrganisasjonsnummer() = hentIdentAvGittTypeFraArbeidssted(IdentType.ORGANISASJONSNUMMER)

private enum class IdentType { FOLKEREGISTERIDENT, ORGANISASJONSNUMMER }

private fun JSONObject.erFrilansaktivitet() = getJSONObject("type").getString("kode").equals(ArbeidsforholdType.FRILANS.type)
private fun JSONObject.arbeidsstedType() = getJSONObject("arbeidssted").getString("type")
private fun JSONObject.arbeidstedErPerson() = arbeidsstedType().equals("Person")
private fun JSONObject.arbeidstedErUnderenhet() = arbeidsstedType().equals("Underenhet")

internal fun erAnsattIPerioden(ansattFom: LocalDate?, ansattTom: LocalDate?, fraOgMed: LocalDate, tilOgMed: LocalDate): Boolean {
    return ansattFom.erLikEllerFør(tilOgMed) && fraOgMed.erLikEllerFør(ansattTom)
}

internal fun LocalDate?.erLikEllerFør(dato: LocalDate?) = if(dato == null || this == null) true else this.isBefore(dato) || this.isEqual(dato)