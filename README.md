# k9-selvbetjening-oppslag
Oppslagstjeneste for selvbetjeningsløsninger for Kapittel 9-ytelser. Sykdom i familien.

## API
Tjenesten har kun et funksjonelt endepunkt, `GET` @ `/meg`

API'et gir kun de attributtene man etterspør, tilsvarende GraphQL, men er per nå kun tilgjengelig i REST. (Åpen for PR på å tilgengeliggjøre som GraphQL!)

Man setter ønskede attributter i query parameter `a` og får en JSON response som kun inneholder de etterspurte attriuttene.

### Tilgjengelige attributter

| Attributter                                                           | Nullable  |
|:----------------------------------------------------------------------|:---------:|
| aktoer_id                                                              |           |
| fornavn                                                               |           |
| mellomnavn                                                            | x         |
| etternavn                                                             |           |
| foedselsdato                                                           |           |
| barn[].aktoer_id                                                       |           |
| barn[].fornavn                                                        |           |
| barn[].mellomnavn                                                     | x         |
| barn[].etternavn                                                      |           |
| barn[].foedselsdato                                                    |           |
| arbeidsgivere[].organisasjoner[].organisasjonsnummer                  |           |
| arbeidsgivere[].organisasjoner[].navn                                 | x         |

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

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

Interne henvendelser kan sendes via Slack i kanalen #team-düsseldorf.
