FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2026.01.15.0735Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
