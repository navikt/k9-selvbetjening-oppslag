package no.nav.k9.integrasjon.aareg

internal enum class ArbeidsforholdStatus(){
    AKTIV,
    AVSLUTTET,
    FREMTIDIG;

    companion object{
        internal fun somQueryParameters() = ArbeidsforholdStatus.values().joinToString(",")
    }
}