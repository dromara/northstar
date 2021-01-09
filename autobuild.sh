yum install git maven nodejs -y
cd ~
git clone https://gitee.com/KevinHuangwl/northstar.git
git clone https://gitee.com/KevinHuangwl/northstar-monitor.git
curl -sSL https://get.daocloud.io/docker | sh 
systemctl start docker 
echo {\"registry-mirrors\": [\"https://docker.mirrors.ustc.edu.cn\"]} >/etc/docker/daemon.json
systemctl restart docker
docker pull mongo:4.0 
docker pull nginx
cd ~/northstar
mvn clean install && docker build -t northstar-trader . 
cd ~/northstar-monitor
npm config set registry https://registry.npm.taobao.org 
npm i 
echo “安装完成”