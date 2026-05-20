FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.05.19.1046Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
