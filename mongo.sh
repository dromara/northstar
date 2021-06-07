#!/bin/bash

echo "安装MongoDB"
sudo yum install libcurl openssl -y
wget https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-ubuntu1604-4.2.14.tgz
tar -xvzf mongodb-linux-x86_64-ubuntu1604-4.2.14.tgz
rm -f mongodb-linux-x86_64-ubuntu1604-4.2.14.tgz
mv mongodb-linux-x86_64-ubuntu1604-4.2.14 ~/mongodb
ln -s  ~/mongodb/bin/* /usr/local/bin/
mkdir -p /var/lib/mongo
mkdir -p /var/log/mongodb
chown -R mongod:mongod /var/lib/mongo /var/log/mongodb
mongod --dbpath /var/lib/mongo --logpath /var/log/mongodb/mongod.log --fork