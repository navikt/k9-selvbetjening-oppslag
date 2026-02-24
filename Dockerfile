FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2026.02.23.0830Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
