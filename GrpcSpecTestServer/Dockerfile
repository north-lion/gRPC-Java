FROM openjdk:11

# The following settings do not imply port publication.
# To actually expose the port, you need to execute the following command: docker run -p 6565:6565
EXPOSE 6565

COPY ./build/libs/*.jar /app/

ENTRYPOINT ["java","-jar","/app/ideaProjects-1.0-SNAPSHOT.jar"]
