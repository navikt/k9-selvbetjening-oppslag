FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.10.05.1242Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
