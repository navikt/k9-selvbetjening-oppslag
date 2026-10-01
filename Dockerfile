FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.09.16.0815Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
