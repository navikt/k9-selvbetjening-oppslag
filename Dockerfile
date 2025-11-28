FROM ghcr.io/navikt/sif-baseimages/java-chainguard-21:2025.11.25.1015z

COPY build/libs/app.jar app.jar

CMD ["java", "-jar", "app.jar"]
