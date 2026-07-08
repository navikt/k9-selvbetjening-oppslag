FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.07.06.0718Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
