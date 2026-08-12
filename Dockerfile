FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.08.10.0637Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
