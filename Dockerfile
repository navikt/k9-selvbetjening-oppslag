FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.23.1517Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
