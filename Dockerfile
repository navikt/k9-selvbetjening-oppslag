FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.04.16.1137Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
