FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.09.02.1329Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
