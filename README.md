# k9-selvbetjening-oppslag

![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=navikt_k9-selvbetjening-oppslag&metric=ncloc)
![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=navikt_k9-selvbetjening-oppslag&metric=alert_status)
![Coverage](https://sonarcloud.io/api/project_badges/measure?project=navikt_k9-selvbetjening-oppslag&metric=coverage)
![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=navikt_k9-selvbetjening-oppslag&metric=code_smells)
![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=navikt_k9-selvbetjening-oppslag&metric=sqale_index)
![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=navikt_k9-selvbetjening-oppslag&metric=duplicated_lines_density)
![Bugs](https://sonarcloud.io/api/project_badges/measure?project=navikt_k9-selvbetjening-oppslag&metric=bugs)

Oppslagstjeneste for selvbetjeningsløsninger for Kapittel 9-ytelser. Sykdom i familien.

## API
Tjenesten har kun et funksjonelt endepunkt, `GET` @ `/meg`

API'et gir kun de attributtene man etterspør, tilsvarende GraphQL, men er per nå kun tilgjengelig i REST. (Åpen for PR på å tilgengeliggjøre som GraphQL!)

Man setter ønskede attributter i query parameter `a` og får en JSON response som kun inneholder de etterspurte attriuttene.

### Tilgjengelige attributter

| Attributter                                                           | Nullable  |
|:----------------------------------------------------------------------|:---------:|
| aktør_id                                                              |           |
| fornavn                                                               |           |
| mellomnavn                                                            | x         |
| etternavn                                                             |           |
| fødselsdato                                                           |           |
| barn[].aktør_id                                                       |           |
| barn[].identitetsnummer                                               |           |
| barn[].fornavn                                                        |           |
| barn[].mellomnavn                                                     | x         |
| barn[].etternavn                                                      |           |
| barn[].fødselsdato                                                    |           |
| barn[].har_samme_adresse                                              | x         |
| arbeidsgivere[].organisasjoner[].organisasjonsnummer                  |           |
| arbeidsgivere[].organisasjoner[].navn                                 | x         |
| personlige_foretak[].organisasjonsnummer                              |           |
| personlige_foretak[].navn                                             | x         |
| personlige_foretak[].organisasjonsform                                |           |
| personlige_foretak[].registreringsdato                                |           |
| personlige_foretak[].opphørsdato                                      | x         |
| private_arbeidsgivere[].ansettelsesperiode                            |           |
| private_arbeidsgivere[].offentlig_ident                               |           |
| frilansoppdrag[]                                                      |           |

### Personlige foretak
`personlige_foretak[]` returnerer personlig foretak som omfatter `organisasjonsform` `ENK`(Enkeltpersonforetak),  `ANS` (Ansvarlig selskap)  og `DA` (Selskap med delt ansvar).
Det tre nevnte verdiene er de man kan få som `organisasjonsform`

### Frilansoppdrag
`frilansoppdrag[]` returnerer frilansoppdrag som omfatter arbeidsforhold av typen "frilanserOppdragstakerHonorarPersonerMm"

### Eksempel

- Hente navn

`curl "http://localhost:8080?a=fornavn&a=mellomnavn&a=etternavn"`

- Hente aktør ID og organisasjonsnummer på arbeidsgivere

`curl "http://localhost:8080?a=aktør_id&a=arbeidsgivere%5B%5D.organisasjoner%5B%5D.organisasjonsnummer%20%20"`


### Authorization
`Authorization` header må settes til `Bearer $INNLOGGET_BRUKES_ID_TOKEN` 

### Correlation ID & Request ID
Correlation ID blir propagert videre, og har ikke nødvendigvis sitt opphav hos konsumenten. Må settes Som `X-Correlation-ID`-header.

Request ID blir ikke propagert videre, og skal ha sitt opphav hos konsumenten. Kan settes om `X-Request-ID`-header

## Bygge lokalt
Ved bygg kreves det at naisdevice er tilkoblet slik at introspect mot graphql kjører.
Dette generer de nødvendige query klassene basert på `src/main/resources/pdl`.

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

Interne henvendelser kan sendes via Slack i kanalen #sykdom-i-familien
