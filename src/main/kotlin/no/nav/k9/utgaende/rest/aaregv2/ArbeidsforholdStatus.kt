package no.nav.k9.utgaende.rest.aaregv2

internal enum class ArbeidsforholdStatus(){
    AKTIV,
    AVSLUTTET,
    FREMTIDIG;

    companion object{
        internal fun somQueryParameters() = ArbeidsforholdStatus.values().joinToString(",")
    }
}