yum install git java-11-openjdk-devel.x86_64 nodejs wget -y
cd ~
wget https://mirror.bit.edu.cn/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
tar -xvf apache-maven-3.6.3-bin.tar.gz
ln -s ~/apache-maven-3.6.3/bin/mvn /usr/local/bin/
curl -sSL https://get.daocloud.io/docker | sh 
systemctl enable docker
systemctl start docker 
echo {\"registry-mirrors\": [\"https://docker.mirrors.ustc.edu.cn\"]} >/etc/docker/daemon.json
systemctl restart docker
docker pull mongo:4.0 
docker run --name Mongo --net=host -d mongo:4.0
wget https://gitee.com/KevinHuangwl/northstar/raw/master/settings.xml
\cp -f settings.xml ~/apache-maven-3.6.3/conf/settings.xml
echo "docker start Mongo" >>/etc/rc.local
echo “环境安装完成”