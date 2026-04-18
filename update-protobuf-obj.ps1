# 假设本地环境已经配置好protoc
$SRC_DIR="C:\Users\Admin\eclipse-workspace\northstar\northstar-api\src\main\resources\proto"
$JAVA_DIR="C:\Users\Admin\eclipse-workspace\northstar\northstar-api\src\main\java"
protoc --proto_path=$SRC_DIR --java_out=$JAVA_DIR $SRC_DIR/xyz/redtorch/pb/core_field.proto
protoc --proto_path=$SRC_DIR --java_out=$JAVA_DIR $SRC_DIR/xyz/redtorch/pb/core_enum.proto

# JS的部分在前端项目进行更新