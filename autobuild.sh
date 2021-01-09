yum install git maven -y
rm -rf northstar
git clone https://gitee.com/KevinHuangwl/northstar.git
curl -sSL https://get.daocloud.io/docker | sh 
systemctl start docker 
docker pull mongo:4.0 
cd northstar
mvn clean install && docker build -t northstar-trader . 
echo “安装完成”