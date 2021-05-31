cd ~/northstar
~/apache-maven-3.6.3/bin/mvn clean install
\mv -f northstar-main/target/northstar-main*.jar ~/northstar-main.jar
docker restart Trader