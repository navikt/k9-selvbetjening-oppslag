FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2026.02.11.1141Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
