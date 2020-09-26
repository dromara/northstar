FROM openjdk:11.0.7-jre
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY /target /root
ENTRYPOINT java -Denv=$ENV -Dsocketio_host=$SOCKET -Demail=$EMAIL -Dsub_email=$SUBEMAIL -Duser=$USER -Dpassword=$PASSWORD -jar /root/northstar-core-0.0.1-SNAPSHOT.jar
