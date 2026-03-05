FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2026.03.04.0913Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
