FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.10.28.1413Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
