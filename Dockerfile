# 使用官方CentOS 8基础映像
FROM openjdk:17

# 创建工作目录
RUN mkdir -p /northstar-dist
WORKDIR /northstar-dist

# 复制当前目录下的所有*.jar文件到工作目录
COPY ./*.jar ./

# 添加启动脚本
RUN echo 'java -Denv=prod -Dloader.path=/northstar-dist -jar /northstar-dist/northstar-*.jar' > /northstar-dist/entrypoint.sh

EXPOSE 443
EXPOSE 51688

# 运行启动脚本
CMD ["bash","/northstar-dist/entrypoint.sh"]