FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.06.17.1117Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
