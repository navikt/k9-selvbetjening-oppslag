FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2026.03.09.0820Z

COPY build/libs/app.jar app.jar

CMD ["-jar", "app.jar"]
