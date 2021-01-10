yum install git java-11 nodejs wget -y
cd ~
wget https://mirror.bit.edu.cn/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
tar -xvf apache-maven-3.6.3-bin.tar.gz
git clone https://gitee.com/KevinHuangwl/northstar.git
git clone https://gitee.com/KevinHuangwl/northstar-monitor.git
curl -sSL https://get.daocloud.io/docker | sh 
systemctl start docker 
echo {\"registry-mirrors\": [\"https://docker.mirrors.ustc.edu.cn\"]} >/etc/docker/daemon.json
systemctl restart docker
docker pull mongo:4.0 
docker pull nginx
cd ~/northstar
\cp -f settings.xml ~/apache-maven-3.6.3/conf/settings.xml
~/apache-maven-3.6.3/bin/mvn clean install
mv northstar-trader/target/northstar-trader*.jar ~/northstar-trader.jar
docker build -t northstar-trader . 
docker run --name Trader --net=host -v ~:/root -d northstar-trader
cd ~/northstar-monitor
npm config set registry https://registry.npm.taobao.org 
npm i 
docker run --name Mongo --net=host -d mongo:4.0
echo “安装完成”