FROM openjdk:11.0.7-jre
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENTRYPOINT java -Denv=prod -Dsocketio_host=$SOCKET -Duser=admin -Dpassword=123456 -jar /root/northstar-trader.jar
