FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.04.24.0932Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
