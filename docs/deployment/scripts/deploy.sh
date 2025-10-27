#!/bin/bash

###############################################################################
# SQL字段级血缘分析平台 - 自动化部署脚本
# 版本: v1.0.0
# 日期: 2025-10-27
###############################################################################

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置变量
APP_NAME="sql-lineage-analyzer"
APP_VERSION="1.0.0"
DEPLOY_DIR="/opt/lineage"
LOG_DIR="/var/log/lineage"
DATA_DIR="/var/lineage"
SERVICE_NAME="lineage"

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查root权限
check_root() {
    if [ "$EUID" -ne 0 ]; then
        log_error "请使用 root 或 sudo 运行此脚本"
        exit 1
    fi
}

# 检查依赖
check_dependencies() {
    log_info "检查系统依赖..."
    
    # 检查 Java
    if ! command -v java &> /dev/null; then
        log_error "未找到 Java，请先安装 JDK 8+"
        exit 1
    fi
    
    local java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$java_version" -lt 8 ]; then
        log_error "Java 版本过低，需要 JDK 8+，当前版本: $java_version"
        exit 1
    fi
    log_info "Java 版本检查通过: $(java -version 2>&1 | head -n 1)"
    
    # 检查 MySQL
    if ! command -v mysql &> /dev/null; then
        log_warn "未找到 MySQL 客户端，跳过数据库检查"
    else
        log_info "MySQL 客户端: $(mysql --version)"
    fi
    
    # 检查 Nginx
    if ! command -v nginx &> /dev/null; then
        log_warn "未找到 Nginx，前端服务可能无法正常工作"
    else
        log_info "Nginx 版本: $(nginx -v 2>&1)"
    fi
}

# 创建目录
create_directories() {
    log_info "创建部署目录..."
    
    mkdir -p "$DEPLOY_DIR"
    mkdir -p "$LOG_DIR"
    mkdir -p "$DATA_DIR/uploads"
    
    log_info "目录创建完成"
}

# 创建用户
create_user() {
    if id "lineage" &>/dev/null; then
        log_info "用户 lineage 已存在"
    else
        log_info "创建系统用户 lineage..."
        useradd -r -s /bin/false lineage
    fi
}

# 复制文件
copy_files() {
    log_info "复制应用文件..."
    
    # 检查 JAR 文件
    if [ ! -f "../../../backend/target/${APP_NAME}-${APP_VERSION}.jar" ]; then
        log_error "未找到 JAR 文件: backend/target/${APP_NAME}-${APP_VERSION}.jar"
        log_info "请先执行: cd backend && mvn clean package"
        exit 1
    fi
    
    # 复制后端
    cp "../../../backend/target/${APP_NAME}-${APP_VERSION}.jar" "$DEPLOY_DIR/"
    
    # 复制前端
    if [ -d "../../../frontend" ]; then
        cp -r "../../../frontend" "$DEPLOY_DIR/"
    else
        log_warn "未找到前端目录，跳过"
    fi
    
    # 复制配置文件
    if [ -f "../config/application-prod.yml" ]; then
        cp "../config/application-prod.yml" "$DEPLOY_DIR/"
    else
        log_warn "未找到生产配置文件"
    fi
    
    log_info "文件复制完成"
}

# 设置权限
set_permissions() {
    log_info "设置文件权限..."
    
    chown -R lineage:lineage "$DEPLOY_DIR"
    chown -R lineage:lineage "$LOG_DIR"
    chown -R lineage:lineage "$DATA_DIR"
    
    chmod 755 "$DEPLOY_DIR"
    chmod 755 "$LOG_DIR"
    chmod 755 "$DATA_DIR"
    
    log_info "权限设置完成"
}

# 初始化数据库
init_database() {
    log_info "初始化数据库..."
    
    # 提示输入数据库信息
    read -p "数据库主机 [localhost]: " DB_HOST
    DB_HOST=${DB_HOST:-localhost}
    
    read -p "数据库端口 [3306]: " DB_PORT
    DB_PORT=${DB_PORT:-3306}
    
    read -p "数据库用户名 [root]: " DB_USER
    DB_USER=${DB_USER:-root}
    
    read -sp "数据库密码: " DB_PASSWORD
    echo
    
    # 测试连接
    if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" -e "SELECT 1" &>/dev/null; then
        log_info "数据库连接成功"
        
        # 创建数据库
        mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" <<EOF
CREATE DATABASE IF NOT EXISTS lineage_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EOF
        log_info "数据库创建完成"
        
        # 导入 schema
        if [ -f "../../../backend/src/main/resources/sql/schema.sql" ]; then
            mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" lineage_db < "../../../backend/src/main/resources/sql/schema.sql"
            log_info "数据库表结构初始化完成"
        else
            log_warn "未找到 schema.sql，跳过表结构初始化"
        fi
    else
        log_error "数据库连接失败"
        exit 1
    fi
}

# 配置 Systemd 服务
configure_systemd() {
    log_info "配置 Systemd 服务..."
    
    cat > "/etc/systemd/system/${SERVICE_NAME}.service" <<EOF
[Unit]
Description=SQL Lineage Analyzer Service
After=network.target mysql.service

[Service]
Type=simple
User=lineage
Group=lineage
WorkingDirectory=${DEPLOY_DIR}

Environment="JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ExecStart=/usr/bin/java \$JAVA_OPTS \\
    -jar ${DEPLOY_DIR}/${APP_NAME}-${APP_VERSION}.jar \\
    --spring.config.location=${DEPLOY_DIR}/application-prod.yml \\
    --spring.profiles.active=prod

Restart=on-failure
RestartSec=10s

StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

    # 重新加载 systemd
    systemctl daemon-reload
    
    log_info "Systemd 服务配置完成"
}

# 配置 Nginx
configure_nginx() {
    log_info "配置 Nginx..."
    
    if ! command -v nginx &> /dev/null; then
        log_warn "Nginx 未安装，跳过配置"
        return
    fi
    
    read -p "服务器域名/IP [localhost]: " SERVER_NAME
    SERVER_NAME=${SERVER_NAME:-localhost}
    
    cat > "/etc/nginx/conf.d/lineage.conf" <<EOF
upstream lineage_backend {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name ${SERVER_NAME};

    location / {
        root ${DEPLOY_DIR}/frontend;
        index index.html;
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://lineage_backend;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        
        proxy_connect_timeout 30s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location /actuator/ {
        proxy_pass http://lineage_backend;
    }

    access_log /var/log/nginx/lineage_access.log;
    error_log /var/log/nginx/lineage_error.log;
}
EOF

    # 测试配置
    if nginx -t &>/dev/null; then
        log_info "Nginx 配置测试通过"
        systemctl reload nginx
        log_info "Nginx 已重新加载"
    else
        log_error "Nginx 配置测试失败"
        exit 1
    fi
}

# 启动服务
start_service() {
    log_info "启动服务..."
    
    # 启动应用
    systemctl enable "${SERVICE_NAME}"
    systemctl start "${SERVICE_NAME}"
    
    # 等待服务启动
    sleep 5
    
    # 检查状态
    if systemctl is-active --quiet "${SERVICE_NAME}"; then
        log_info "服务启动成功"
        systemctl status "${SERVICE_NAME}" --no-pager
    else
        log_error "服务启动失败"
        journalctl -u "${SERVICE_NAME}" -n 50 --no-pager
        exit 1
    fi
}

# 健康检查
health_check() {
    log_info "执行健康检查..."
    
    local max_attempts=10
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
            log_info "健康检查通过"
            return 0
        fi
        
        attempt=$((attempt + 1))
        log_warn "健康检查失败，重试 $attempt/$max_attempts..."
        sleep 3
    done
    
    log_error "健康检查失败"
    return 1
}

# 打印部署信息
print_info() {
    echo ""
    echo "================================================"
    log_info "部署完成！"
    echo "================================================"
    echo ""
    echo "应用目录: ${DEPLOY_DIR}"
    echo "日志目录: ${LOG_DIR}"
    echo "数据目录: ${DATA_DIR}"
    echo ""
    echo "服务管理:"
    echo "  启动: systemctl start ${SERVICE_NAME}"
    echo "  停止: systemctl stop ${SERVICE_NAME}"
    echo "  重启: systemctl restart ${SERVICE_NAME}"
    echo "  状态: systemctl status ${SERVICE_NAME}"
    echo "  日志: journalctl -u ${SERVICE_NAME} -f"
    echo ""
    echo "访问地址:"
    echo "  前端: http://localhost/"
    echo "  后端: http://localhost:8080/"
    echo "  健康检查: http://localhost:8080/actuator/health"
    echo ""
    echo "================================================"
}

# 主流程
main() {
    log_info "开始部署 ${APP_NAME} v${APP_VERSION}..."
    echo ""
    
    check_root
    check_dependencies
    create_directories
    create_user
    copy_files
    set_permissions
    
    # 询问是否初始化数据库
    read -p "是否初始化数据库? (y/n): " INIT_DB
    if [ "$INIT_DB" = "y" ]; then
        init_database
    fi
    
    configure_systemd
    
    # 询问是否配置 Nginx
    read -p "是否配置 Nginx? (y/n): " CONFIG_NGINX
    if [ "$CONFIG_NGINX" = "y" ]; then
        configure_nginx
    fi
    
    start_service
    health_check
    print_info
}

# 执行部署
main "$@"
