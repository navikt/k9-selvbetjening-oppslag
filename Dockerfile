FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.07.09.0728Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
