FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.06.29.0807Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
