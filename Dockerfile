FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.04.07.0752Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
