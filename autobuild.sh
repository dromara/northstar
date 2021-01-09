yum install git maven && \
git clone https://gitee.com/KevinHuangwl/northstar.git && \
curl -sSL https://get.daocloud.io/docker | sh && \
echo {"registry-mirrors": ["https://docker.mirrors.ustc.edu.cn"]} >/etc/docker/daemon.json && \
systemctl start docker && \
docker pull mongo:4.0 && \
mvn clean install && docker build -t northstar-trader . && \
echo “安装完成”