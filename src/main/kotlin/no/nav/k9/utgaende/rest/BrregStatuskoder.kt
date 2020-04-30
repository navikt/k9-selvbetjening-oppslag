package no.nav.k9.utgaende.rest

import no.nav.k9.utgaende.rest.BrregStatuskoder.filtrerPåEnhetStatuskombinasjoner
import no.nav.k9.utgaende.rest.BrregStatuskoder.filtrerPåPersonStatuskombinasjoner
import no.nav.k9.utgaende.rest.BrregStatuskoder.statuskombinasjoner
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private object BrregStatuskoder {
    internal val logger: Logger = LoggerFactory.getLogger(BrregStatuskoder::class.java)

    private enum class Statuskombinasjon(
        internal val statuskombinasjon: Pair<Int, Int>,
        internal val melding: String) {
        Ok(Pair(0,0),"Data returnert."),

        EnhetSlettetSomDublett(Pair(0,1), "Enhet x er slettet som dublett - korrekt enhet y er innført."),
        EnhetSlettetSomSammenslått(Pair(0,2),"Enhet x er slettet som sammenslått - korrekt enhet y er innført."),
        EnhetAldriOpprettet(Pair(1,100), "Enhet x aldri opprettet."),
        EnhetSlettet(Pair(1,110), "Enhet x er slettet."),
        EnhetSlettetSomDublettEllerSammenslått(Pair(1,120), "Enhet x er slettet som dublett eller sammenslått - korrekt enhet y er slettet."),

        PersonIkkeFunnet(Pair(1,180), "Personen x er ikke i vår database."),
        PersonDød(Pair(1,181), "Personen x er registrert død."),
        PersonUgyldig(Pair(1,182),"Personen x er registrert ugyldig."),
        PersonHarVerge(Pair(1,183), "Personen x har verge.")
    }

    internal enum class Hovedstatuser {
        Ok,
        ManglerData,
        TekniskFeil;

        internal companion object {
            internal fun fraBrregVerdi(brregVerdi: Int) = when (brregVerdi) {
                0 -> Ok
                1 -> ManglerData
                -1 -> TekniskFeil
                else -> {
                    logger.warn("Ukjent Hovedstatus $brregVerdi fra Brønnøysundregisteret.")
                    TekniskFeil
                }
            }
        }
    }

    internal val okStatuskombinasjoner = setOf(Statuskombinasjon.Ok.statuskombinasjon)

    internal val håndtertePersonStatuskombinasjoner = setOf(
        Statuskombinasjon.PersonIkkeFunnet.statuskombinasjon,
        Statuskombinasjon.PersonDød.statuskombinasjon,
        Statuskombinasjon.PersonUgyldig.statuskombinasjon,
        Statuskombinasjon.PersonHarVerge.statuskombinasjon
    )

    internal val håndterteEnhetStatuskombinasjoner = setOf(
        Statuskombinasjon.EnhetSlettetSomDublett.statuskombinasjon,
        Statuskombinasjon.EnhetSlettetSomSammenslått.statuskombinasjon,
        Statuskombinasjon.EnhetAldriOpprettet.statuskombinasjon,
        Statuskombinasjon.EnhetSlettet.statuskombinasjon,
        Statuskombinasjon.EnhetSlettetSomDublettEllerSammenslått.statuskombinasjon
    )

    internal fun MutableSet<Foretak>.filtrerPåPersonStatuskombinasjoner(statuskombinasjoner: Set<Triple<Int,Int,String>>) = when {
        this.isEmpty() || statuskombinasjoner.isEmpty() -> this
        else -> {
            logger.info("$size foretak filtreres bort på grunn av statuskombinasjoner på person: ${statuskombinasjoner.utenMeldinger().somString()}")
            mutableSetOf()
        }
    }

    internal fun MutableSet<Foretak>.filtrerPåEnhetStatuskombinasjoner(statuskombinasjoner: Set<Triple<Int,Int,String>>) = when {
        this.isEmpty() || statuskombinasjoner.isEmpty() -> this
        else -> {
            val antallFørFiltrering = this.size
            val meldingerUtenKorrektEnhet = statuskombinasjoner.joinToString {
                if (it.utenMelding() == Statuskombinasjon.EnhetSlettetSomDublettEllerSammenslått.statuskombinasjon) it.third
                else it.third.split("-")[0]
            }
            val filtrert = this.filterNot { meldingerUtenKorrektEnhet.contains(it.organisasjonsnummer) }
            if (antallFørFiltrering != filtrert.size) {
                logger.info("${antallFørFiltrering-filtrert.size} foretak filtreres bort på grunn av statuskominasjoner på enhet: ${statuskombinasjoner.utenMeldinger().somString()}")
            }
            filtrert.toMutableSet()
        }
    }

    internal fun JSONObject.statuskombinasjoner() : Set<Triple<Int,Int,String>> {
        val (hovedstatus, statuskoder) = forsikreIkkeTekniskFeil()
        return statuskoder.getJsonArrayOrEmpty("understatus")
            .map { it as JSONObject }
            .map { Triple(hovedstatus, it.getInt("kode"), it.getString("melding")) }
            .toSet()
    }
}

internal fun MutableSet<Foretak>.filtrerPåStatuskoder(brregResponse: JSONObject) : Set<Foretak> {
    val statuskombinasjoner = brregResponse.statuskombinasjoner()
    val okStatuskombinasjoner = statuskombinasjoner.filter { it.utenMelding() in BrregStatuskoder.okStatuskombinasjoner }.toSet()
    val personStatuskombinasjoner = statuskombinasjoner.filter { it.utenMelding() in BrregStatuskoder.håndtertePersonStatuskombinasjoner }.toSet()
    val enhetStatuskombinasjoner = statuskombinasjoner.filter { it.utenMelding() in BrregStatuskoder.håndterteEnhetStatuskombinasjoner }.toSet()
    val filtrert = filtrerPåPersonStatuskombinasjoner(personStatuskombinasjoner).filtrerPåEnhetStatuskombinasjoner(enhetStatuskombinasjoner)
    val uhåndterteStatuskombinasjoner = statuskombinasjoner
        .toMutableSet()
        .fjernAlle(okStatuskombinasjoner)
        .fjernAlle(personStatuskombinasjoner)
        .fjernAlle(enhetStatuskombinasjoner)

    if (uhåndterteStatuskombinasjoner.size > 0) {
        BrregStatuskoder.logger.info("AntallUhåndterteStatuskombinasjoner=${uhåndterteStatuskombinasjoner.size}")
        BrregStatuskoder.logger.info("UhåndterteStatuskombinasjoner=${uhåndterteStatuskombinasjoner.utenMeldinger().somString()}")
    }
    return filtrert
}
internal fun JSONObject.forsikreIkkeTekniskFeil() : Pair<Int, JSONObject> {
    val statuskoder = getJSONObject("statuskoder")
    val hovedstatus = statuskoder.getInt("hovedstatus")
    if (BrregStatuskoder.Hovedstatuser.TekniskFeil == BrregStatuskoder.Hovedstatuser.fraBrregVerdi(hovedstatus)) {
        throw IllegalStateException("Teknisk feil mot Brønnøysundregisteret. Mottok statuskoder ${statuskoder}.")
    }
    return Pair(hovedstatus, statuskoder)
}

private fun <T>MutableSet<T>.fjernAlle(set: Set<T>) : MutableSet<T> {
    removeAll(set)
    return this
}
private fun Pair<Int,Int>.somString() = "${first}:${second}"
private fun Set<Pair<Int,Int>>.somString() = joinToString { it.somString() }
private fun Triple<Int,Int,String>.utenMelding() = Pair(first, second)
private fun Set<Triple<Int,Int,String>>.utenMeldinger() = map {it.utenMelding()}.toSet()

/*

https://www.brreg.no/produkter-og-tjenester/bestille-produkter/maskinlesbare-data-enhetsregisteret/full-tilgang-enhetsregisteret/teknisk-dokumentasjon-for-maskinell-tilgang-til-enhetsregisteret/grunndataws/

Stjerne (*) Foran betyr at vi håndterer statuskombinasjonen i koden ovenfor.
De uten noe lar vi passere i stillhet om vi får dem.
Alfakrøll (@) Under en kombinasjon er avklaringer gjort etter spørsmål til Brønnøysundregisteret.

*   0	0	    Data returnert.
*   0	1       Enhet x er slettet som dublett - korrekt enhet y er innført
*   0	2	    Enhet x er slettet som sammenslått - korrekt enhet y er innført.
    0	3	    Enhet x er registreringsenheten til den enheten som ble bestilt!
    1	10	    Orgnr i input ikke gitt.
    1	20	    Valideringsfeil i inputstreng:
    1	10000	Ikke gyldig organisasjonsnummer. Organisasjonsnummer skal bestå av 9 siffer og kun tall.
*   1	100	    Enhet x aldri opprettet.
*   1	110	    Enhet x er slettet.
*   1	120	    Enhet x er slettet som dublett eller sammenslått - korrekt enhet y er slettet.
    1	130	    Enhet x er en bedrift/virksomhet, juridisk enhet er y.
    1	160     Input er ikke et organisasjonsnummer eller et fødselsnr.
    1	170	    x er ikke et gyldig organisasjonsnummer.
*   1	180	    Personen x er ikke i vår database.
*   1	181	    Personen x er registrert død
*   1	182	    Personen x er registrert ugyldig
*   1	183	    Personen x har verge.
    1	190     Enhet x er ikke en bedrift/virksomhet.
    1	200	    Enhet x det spørres på er en registreringsenhet.
    1	210	    Enhet x er ikke registrert i Frivillighetsregisteret.
    1	2000	Ingen endringer finnes i siste periode (siste døgn/helg).
    0	1010	Enhet x har ikke forretningsadresse.
    0	1020	Enhet x har ikke postadresse.
    0	1030	Enhet x har ikke adresse.
    0	1060	Enhet x har ikke stiftelsesdato.
@   Det vil alltid leveres en registreringsdato.
    0	1070	Enhet x har ikke målform.
    0	1090	Enhet x har ikke virksomhet/art/bransje.
    0	1100	Enhet x har ikke formål.
    0	1111	Enhet x har ikke vedtektsdato.
    0	1115	Enhet x har ikke telefon.
    0	1116	Enhet x har ikke telefax.
    0	1117	Enhet x har ikke mobiltelefon.
    0	1118	Enhet x har ikke e-postadresse.
    0	1119	Enhet x har ikke hjemmeside.
    0	1120	Enhet x har ikke særlige opplysninger.
    0	1125	Enhet x har ikke næringskode.
    0	1130	Enhet x har ikke sektorkode.
    0	1135	Enhet x har ikke ansatte.
    0	1140	Enhet x har ikke kapital.
    0	1145	Enhet x har ikke bedrifter/virksomheter.
    0	1150	Enhet x inngår ikke i konsern.
    0	1155	Enhet x har ikke dato for oppstart.
@   Det vil alltid leveres en registreringsdato.
    0	1160	Enhet x har ikke dato for eierskifte.
    0	1165	Enhet x har ikke hovedforetak.
    0	1180	Enhet x har rolleblokk n (et sett av roller).
@   Understatus 1180 skal ikke forekomme i denne tjenesten. Denne brukes i tjenester som skal returnere roller knyttet til en enhet.
*   -1	-100	Bruker mangler autorisasjon for denne tjenesten.
*   -1	-101	Feil i brukernavn og/eller passord.
*   -1	-1000	Det har oppstått en uventet feil. Ved fortsatt gjentakelse, ta kontakt med Brønnøysundregistrene.

*/
