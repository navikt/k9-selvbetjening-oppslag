FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2026.01.29.1157Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
