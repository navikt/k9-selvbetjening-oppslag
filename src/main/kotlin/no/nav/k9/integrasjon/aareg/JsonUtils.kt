package no.nav.k9.integrasjon.aareg

import no.nav.k9.integrasjon.aareg.TypeArbeidssted.Companion.somTypeArbeidssted
import no.nav.k9.integrasjon.common.getStringOrNull
import no.nav.k9.integrasjon.common.påkrevd
import tools.jackson.databind.JsonNode
import java.time.LocalDate

internal fun JsonNode.hentOrganisasjonerV2(fraOgMed: LocalDate, tilOgMed: LocalDate, inkluderAlleAnsettelsesperioder: Boolean): List<OrganisasjonArbeidsgivere> {
    val alleArbeidsforhold: Sequence<OrganisasjonArbeidsgivere> = hentArbeidsgivereMedAnsettelseperiodeV2()
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

    return when {
        inkluderAlleAnsettelsesperioder -> alleArbeidsforhold.toList()
        else -> alleArbeidsforhold.distinctBy { it.organisasjonsnummer }.toList()
    }
}


internal fun JsonNode.hentFrilansoppdragV2(fraOgMed: LocalDate, tilOgMed: LocalDate): Set<Frilansoppdrag> =
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

internal fun JsonNode.hentPrivateArbeidsgivereV2(fraOgMed: LocalDate, tilOgMed: LocalDate): Set<PrivatArbeidsgiver> =
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

private fun JsonNode.hentArbeidsgivereMedAnsettelseperiodeV2(): Sequence<JsonNode> = this
    .asSequence()
    .filter { it.has("arbeidssted") }
    .filter { it.has("ansettelsesperiode") && it.get("ansettelsesperiode").has("startdato") }

private fun JsonNode.hentStartdatoOgSluttdatoFraAnsettelseperiode(): Pair<String, String?> {
    val ansettelsesperiode = påkrevd("ansettelsesperiode")
    return Pair(ansettelsesperiode.påkrevd("startdato").asString(), ansettelsesperiode.getStringOrNull("sluttdato"))
}

private fun JsonNode.hentIdentAvGittTypeFraArbeidssted(type: IdentType) = påkrevd("arbeidssted")
    .påkrevd("identer")
    .find { it.påkrevd("type").asString() == type.toString() }!!
    .påkrevd("ident")
    .asString()

private fun JsonNode.hentFolkeregistrertIdent() = hentIdentAvGittTypeFraArbeidssted(IdentType.FOLKEREGISTERIDENT)
private fun JsonNode.hentOrganisasjonsnummer() = hentIdentAvGittTypeFraArbeidssted(IdentType.ORGANISASJONSNUMMER)

private enum class IdentType { FOLKEREGISTERIDENT, ORGANISASJONSNUMMER }

private fun JsonNode.erFrilansaktivitet() = påkrevd("type").påkrevd("kode").asString().equals(ArbeidsforholdType.FRILANS.type)
private fun JsonNode.arbeidsstedType() = påkrevd("arbeidssted").påkrevd("type").asString()
private fun JsonNode.arbeidstedErPerson() = arbeidsstedType().equals("Person")
private fun JsonNode.arbeidstedErUnderenhet() = arbeidsstedType().equals("Underenhet")

internal fun erAnsattIPerioden(ansattFom: LocalDate?, ansattTom: LocalDate?, fraOgMed: LocalDate, tilOgMed: LocalDate): Boolean {
    return ansattFom.erLikEllerFør(tilOgMed) && fraOgMed.erLikEllerFør(ansattTom)
}

internal fun LocalDate?.erLikEllerFør(dato: LocalDate?) = if(dato == null || this == null) true else this.isBefore(dato) || this.isEqual(dato)
