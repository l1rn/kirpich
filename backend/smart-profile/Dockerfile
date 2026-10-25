from maven:3.9-eclipse-temurin-21 as build
workdir /app

copy pom.xml .
run mvn -B -q dependency:go-offline

copy src ./src
run mvn -B -q package -DskipTests \
    && find target -maxdepth 1 -name '*.jar' ! -name '*.original' -exec cp {} /app/app.jar \;

from eclipse-temurin:21-jre
workdir /app
run useradd --system --no-create-home app \
    && mkdir /data && chown app /data

copy --from build /app/app.jar app.jar

user app 
volume /data
expose 8080
entrypoint ["java", "-jar", "app.jar"]