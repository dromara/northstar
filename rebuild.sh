cd ~/northstar
~/apache-maven-3.6.3/bin/mvn clean install
\mv -f northstar-trader/target/northstar-trader*.jar ~/northstar-trader.jar
docker restart Trader