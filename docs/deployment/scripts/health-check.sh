#!/bin/bash

###############################################################################
# SQL字段级血缘分析平台 - 健康检查脚本
###############################################################################

HEALTH_URL="http://localhost:8080/actuator/health"
LOG_FILE="/var/log/lineage/health-check.log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# 时间戳
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# 检查健康状态
check_health() {
    RESPONSE=$(curl -s -w "\n%{http_code}" "$HEALTH_URL" 2>&1)
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        STATUS=$(echo "$BODY" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
        
        if [ "$STATUS" = "UP" ]; then
            echo -e "${GREEN}✅ [$TIMESTAMP] 服务运行正常${NC}"
            echo "[$TIMESTAMP] OK - Service is UP" >> "$LOG_FILE"
            return 0
        else
            echo -e "${RED}❌ [$TIMESTAMP] 服务异常: $STATUS${NC}"
            echo "[$TIMESTAMP] ERROR - Service status: $STATUS" >> "$LOG_FILE"
            return 1
        fi
    else
        echo -e "${RED}❌ [$TIMESTAMP] 服务无响应 (HTTP $HTTP_CODE)${NC}"
        echo "[$TIMESTAMP] ERROR - HTTP $HTTP_CODE - $BODY" >> "$LOG_FILE"
        return 1
    fi
}

# 检查关键指标
check_metrics() {
    # JVM 内存
    MEMORY_USED=$(curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq -r '.measurements[0].value' 2>/dev/null)
    if [ ! -z "$MEMORY_USED" ]; then
        MEMORY_MB=$(echo "scale=2; $MEMORY_USED / 1024 / 1024" | bc)
        echo "内存使用: ${MEMORY_MB}MB"
    fi
    
    # 数据库连接池
    DB_ACTIVE=$(curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq -r '.measurements[0].value' 2>/dev/null)
    if [ ! -z "$DB_ACTIVE" ]; then
        echo "活跃数据库连接: $DB_ACTIVE"
    fi
}

# 执行检查
if check_health; then
    check_metrics
    exit 0
else
    exit 1
fi
