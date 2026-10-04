#!/bin/bash
#
# env.sh — Northstar 运行环境准备脚本（健壮性增强版）
#
# 功能：
#   1. 创建 ~/northstar-env 与 ~/northstar-dist
#   2. 检查并安装 JDK（默认 25），自动适配 x86_64 / aarch64
#   3. 将 JDK 命令软链到系统 PATH，并幂等写入 JAVA_HOME 环境变量
#   4. 准备 RL 强化学习训练的 Python 环境：在 ~/northstar-env/rl-venv 创建虚拟环境，
#      安装 northstar-rl/training/requirements.txt 依赖（numpy/torch/gymnasium 等），
#      并幂等写入 NS_RL_PYTHON 指向该虚拟环境解释器，
#      应用启动时自动使用（未设置时默认的 python 为系统解释器，缺少依赖会导致训练失败）
#      注意：venv 属于运行环境而非源码仓库，生产环境（northstar-dist）下没有 northstar-rl 目录
#
# 支持通过环境变量覆盖默认行为：
#   JDK_MAJOR=25            目标 JDK 主版本号
#   JDK_URL=<url>           自定义下载地址（指定后不再使用内置地址，可指向国内镜像）
#   ENV_DIR=<path>          JDK/RL venv 安装目录（默认 ~/northstar-env，rl-venv 位于其下）
#   DIST_DIR=<path>         部署目录（默认 ~/northstar-dist）
#   LINK_DIR=<path>         命令软链目录（默认 /usr/local/bin，无权限时自动回退 ~/.local/bin）
#   CURL_OPTS="<opts>"      透传给 curl 的额外参数（如 "-k" 忽略证书校验）
#   PYTHON=<path>           用于创建 venv 的系统 Python（默认自动探测 python3 / python）
#   REQUIREMENTS=<path>     RL 依赖清单 requirements.txt 路径（默认按候选路径自动查找，找不到则用内置默认依赖）
#   PIP_INDEX_URL=<url>     pip 镜像地址（官方源不可达时，如 https://pypi.tuna.tsinghua.edu.cn/simple）
#
# 网络受限环境可在执行前自行 export https_proxy=http://<proxy>:<port>
#
set -eEuo pipefail

# ---------- 日志与错误处理 ----------
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }
err() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: $*" >&2; }
trap 'err "脚本在第 $LINENO 行执行失败，已退出。"' ERR

# ---------- 可配置参数 ----------
JDK_MAJOR="${JDK_MAJOR:-25}"
ENV_DIR="${ENV_DIR:-$HOME/northstar-env}"
DIST_DIR="${DIST_DIR:-$HOME/northstar-dist}"
JDK_HOME="$ENV_DIR/jdk-$JDK_MAJOR"
CURL_OPTS="${CURL_OPTS:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RL_VENV_PYTHON=""

# ---------- 工具函数 ----------

# 从 java -version 输出解析主版本号（兼容 "1.8.0_XXX" 旧式版本号）
parse_major() {
    local ver
    ver=$("$1" -version 2>&1 | awk -F '"' '/version/ {print $2; exit}' || true)
    [[ -z "$ver" ]] && return 0
    if [[ "$ver" == 1.* ]]; then
        echo "$ver" | cut -d. -f2
    else
        echo "$ver" | cut -d. -f1
    fi
}

# 判断指定 java 可执行文件是否为目标版本且能正常运行
is_target_jdk() {
    local java_bin="$1"
    [[ -x "$java_bin" ]] || return 1
    [[ "$(parse_major "$java_bin")" == "$JDK_MAJOR" ]]
}

# 带校验的下载：依次尝试多个下载源，tar 完整性校验失败自动切换
download_jdk() {
    local out="$1"
    local -a urls
    if [[ -n "${JDK_URL:-}" ]]; then
        urls=("$JDK_URL")
    else
        urls=(
            "https://download.oracle.com/java/${JDK_MAJOR}/latest/jdk-${JDK_MAJOR}_linux-${JDK_ARCH}_bin.tar.gz"
            "https://api.adoptium.net/v3/binary/latest/${JDK_MAJOR}/ga/linux/${JDK_ARCH}/jdk/hotspot/normal/eclipse"
        )
    fi

    local url
    for url in "${urls[@]}"; do
        log "尝试下载：$url"
        if curl -fsSL --connect-timeout 15 --retry 3 --retry-delay 3 $CURL_OPTS -o "$out" "$url"; then
            if tar -tzf "$out" >/dev/null 2>&1; then
                log "下载完成，完整性校验通过。"
                return 0
            fi
            err "下载文件不完整或已损坏，尝试下一个下载源。"
        else
            err "下载失败，尝试下一个下载源。"
        fi
        rm -f "$out"
    done
    return 1
}

# 全新安装 JDK
install_jdk() {
    log "安装 JDK $JDK_MAJOR（$JDK_ARCH）..."

    # 磁盘空间预检（解压 + 安装包约需 500MB）
    local avail
    avail=$(df -m --output=avail "$ENV_DIR" | tail -n1 | tr -d ' ')
    if [[ "$avail" -lt 1024 ]]; then
        log "警告：$ENV_DIR 所在分区剩余空间 ${avail}MB，可能不足（建议 >= 1GB）。"
    fi

    local tmp_dir pkg extracted
    tmp_dir=$(mktemp -d "$ENV_DIR/.jdk-install.XXXXXX")
    pkg="$tmp_dir/jdk.tar.gz"

    if ! download_jdk "$pkg"; then
        rm -rf "$tmp_dir"
        err "所有下载源均失败。可通过 JDK_URL=<镜像地址> 指定可用下载源后重试。"
        exit 1
    fi

    log "解压安装包..."
    tar -xzf "$pkg" -C "$tmp_dir"

    # 统一重命名为固定目录 jdk-$JDK_MAJOR，避免原脚本 find jdk* 匹配多目录导致的歧义
    extracted=$(find "$tmp_dir" -mindepth 1 -maxdepth 1 -type d -print -quit)
    if [[ -z "$extracted" || ! -x "$extracted/bin/java" ]]; then
        rm -rf "$tmp_dir"
        err "安装包结构异常，未找到可执行的 java。"
        exit 1
    fi

    rm -rf "$JDK_HOME"
    mv "$extracted" "$JDK_HOME"
    rm -rf "$tmp_dir"

    # 安装后自检：版本正确且能真正运行
    if ! is_target_jdk "$JDK_HOME/bin/java"; then
        err "JDK 安装后自检失败（版本不符或二进制无法在当前架构运行）。"
        exit 1
    fi
    log "JDK $JDK_MAJOR 安装完成：$JDK_HOME"
}

# 解析依赖清单路径：优先 REQUIREMENTS 环境变量；其次按候选路径查找（仓库布局 / 部署目录布局）；
# 均不存在时返回空（调用方退回内置默认依赖）。
resolve_requirements() {
    if [[ -n "${REQUIREMENTS:-}" && -f "${REQUIREMENTS}" ]]; then
        echo "${REQUIREMENTS}"
        return 0
    fi
    local c
    for c in \
        "$SCRIPT_DIR/northstar-rl/training/requirements.txt" \
        "$SCRIPT_DIR/training/requirements.txt" \
        "northstar-rl/training/requirements.txt" \
        "training/requirements.txt" \
        "../northstar-rl/training/requirements.txt"; do
        if [[ -f "$c" ]]; then
            echo "$c"
            return 0
        fi
    done
    return 1
}

# 准备 RL 训练 Python 环境：venv + requirements + NS_RL_PYTHON（失败仅告警，不影响 JDK 安装）
# venv 与 JDK 同属运行环境，安装在 ENV_DIR/rl-venv（生产环境 northstar-dist 下没有 northstar-rl 源码目录）
setup_python_rl_env() {
    local venv="$ENV_DIR/rl-venv"

    # 已存在 venv 直接复用；否则用系统 Python 创建（PYTHON 环境变量可指定）
    local sys_py="${PYTHON:-}"
    if [[ -z "$sys_py" ]]; then
        if command -v python3 >/dev/null 2>&1; then
            sys_py="$(command -v python3)"
        elif command -v python >/dev/null 2>&1; then
            sys_py="$(command -v python)"
        else
            log "未检测到 Python，跳过 RL 训练环境准备（强化学习训练需要 Python 3.10+）。"
            return 0
        fi
    fi

    if [[ ! -x "$venv/bin/python" ]]; then
        log "创建 RL 训练虚拟环境：$venv"
        if ! "$sys_py" -m venv "$venv"; then
            err "虚拟环境创建失败（Debian/Ubuntu 可能需要先安装 python3-venv 包），跳过 RL 依赖安装。"
            return 0
        fi
    fi

    local vpy="$venv/bin/python"
    "$vpy" -m pip install --upgrade pip --disable-pip-version-check -q
    # 可通过 PIP_INDEX_URL 环境变量指定 pip 镜像（如 https://pypi.tuna.tsinghua.edu.cn/simple）
    local -a pip_opts=(install --disable-pip-version-check)
    if [[ -n "${PIP_INDEX_URL:-}" ]]; then
        pip_opts+=(-i "$PIP_INDEX_URL")
    fi
    local requirements
    if requirements=$(resolve_requirements); then
        log "安装/校验 RL 训练依赖：$requirements"
        pip_opts+=(-r "$requirements")
    else
        # 内置默认依赖与 northstar-rl/training/requirements.txt 保持一致，新增依赖需同步两处
        log "未找到 requirements.txt，安装内置默认依赖：torch gymnasium numpy pytest"
        pip_opts+=(torch gymnasium numpy pytest)
    fi
    if ! "$vpy" -m pip "${pip_opts[@]}"; then
        err "RL 依赖安装失败，强化学习训练将不可用。请检查网络后重跑；如官方源无法访问，可先 export PIP_INDEX_URL=<镜像地址> 再重试。"
        return 0
    fi
    # 自检：train_round.py 依赖 numpy/torch 必须可导入
    if ! "$vpy" -c "import numpy, torch" >/dev/null 2>&1; then
        err "依赖自检失败（numpy/torch 不可导入），强化学习训练将不可用。"
        return 0
    fi

    RL_VENV_PYTHON="$vpy"
    export NS_RL_PYTHON="$vpy"
    log "RL 训练 Python 环境就绪：$vpy"
}

# 将 JDK bin 下所有命令软链到 LINK_DIR
link_commands() {
    log "链接 JDK 命令到 $LINK_DIR ..."
    mkdir -p "$LINK_DIR"
    local f name
    for f in "$JDK_HOME"/bin/*; do
        name=$(basename "$f")
        if [[ -e "$LINK_DIR/$name" && ! -L "$LINK_DIR/$name" ]]; then
            log "警告：$LINK_DIR/$name 已存在且不是软链接，将被覆盖。"
        fi
        $SUDO ln -sf "$f" "$LINK_DIR/$name"
    done
}

# 幂等写入 JAVA_HOME / PATH（含 RL 训练 Python 解释器 NS_RL_PYTHON）
configure_env() {
    local content="# >>> northstar-jdk >>>
export JAVA_HOME=$JDK_HOME
export PATH=\$JAVA_HOME/bin:\$PATH"
    if [[ -n "$RL_VENV_PYTHON" ]]; then
        content="$content
export NS_RL_PYTHON=$RL_VENV_PYTHON"
    fi
    content="$content
# <<< northstar-jdk <<<"

    if [[ $EUID -eq 0 || -n "$SUDO" ]]; then
        log "写入全局环境变量：/etc/profile.d/northstar-jdk.sh"
        echo "$content" | $SUDO tee /etc/profile.d/northstar-jdk.sh >/dev/null
        $SUDO chmod 644 /etc/profile.d/northstar-jdk.sh
    else
        local rc="$HOME/.bashrc"
        log "写入用户环境变量：$rc"
        # 先移除旧块再追加，保证重复执行不产生重复配置
        sed -i '/# >>> northstar-jdk >>>/,/# <<< northstar-jdk <<</d' "$rc" 2>/dev/null || true
        echo "$content" >> "$rc"
    fi
}

main() {
    # CPU 架构自适应（Oracle 与 Adoptium 的命名一致：x64 / aarch64）
    case "$(uname -m)" in
        x86_64|amd64)  JDK_ARCH="x64" ;;
        aarch64|arm64) JDK_ARCH="aarch64" ;;
        *) err "不支持的 CPU 架构：$(uname -m)，请手动安装 JDK $JDK_MAJOR。"; exit 1 ;;
    esac

    # 权限探测：root 直接写 /usr/local/bin；普通用户有 sudo 用 sudo；否则回退 ~/.local/bin
    SUDO=""
    if [[ $EUID -ne 0 ]] && command -v sudo >/dev/null 2>&1 && sudo -v 2>/dev/null; then
        SUDO="sudo"
    fi
    if [[ $EUID -eq 0 || -n "$SUDO" ]]; then
        LINK_DIR="${LINK_DIR:-/usr/local/bin}"
    else
        LINK_DIR="${LINK_DIR:-$HOME/.local/bin}"
    fi

    log "准备环境依赖（目标 JDK：$JDK_MAJOR，架构：$JDK_ARCH，安装目录：$ENV_DIR）..."
    mkdir -p "$ENV_DIR" "$DIST_DIR"

    # 1) 系统 PATH 中已有目标版本：反推其真实 JAVA_HOME，供后续软链与环境变量配置使用
    if command -v java >/dev/null 2>&1 && is_target_jdk "$(command -v java)"; then
        local sys_java
        sys_java=$(readlink -f "$(command -v java)")
        JDK_HOME=$(dirname "$(dirname "$sys_java")")
        log "系统已存在 JDK $JDK_MAJOR：$sys_java，跳过安装。"
    # 2) 本地安装目录已有可用 JDK（重装/续跑场景直接复用，不重复下载）
    elif is_target_jdk "$JDK_HOME/bin/java"; then
        log "检测到已安装的 JDK：$JDK_HOME，跳过下载。"
    # 3) 全新安装
    else
        install_jdk
    fi

    link_commands
    setup_python_rl_env
    configure_env
    hash -r 2>/dev/null || true

    log "环境安装完成。当前 java 版本："
    "$JDK_HOME/bin/java" -version

    if [[ ":$PATH:" != *":$LINK_DIR:"* ]]; then
        log "提示：$LINK_DIR 不在当前 PATH 中，请重新登录或执行 source ~/.bashrc（或 /etc/profile）后生效。"
    fi
}

# 允许被 source 以复用函数（如测试），直接执行时才进入主流程
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
