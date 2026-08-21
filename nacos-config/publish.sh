#!/bin/bash
# 一键发布 Nacos 配置
#
# 用法：在 nacos-config 目录下执行
#   bash publish.sh
# 或（Windows Git Bash / WSL）：
#   ./publish.sh
#
# 说明：把当前目录下的 auth.yaml / book.yaml / order.yaml / gateway.yaml
#       发布到 Nacos（dataId 分别为 <服务名>.yaml，group 为 DEFAULT_GROUP）。
#       依赖：curl + Nacos 已在运行。

# Nacos 地址，可用环境变量 NACOS_ADDR 覆盖
NACOS_ADDR="${NACOS_ADDR:-localhost:8848}"

# 切换到脚本所在目录，保证能找到 yaml 文件
cd "$(dirname "$0")" || exit 1

echo "Nacos 地址：http://${NACOS_ADDR}"
echo "----------------------------------------"

for svc in auth book order gateway; do
  file="${svc}.yaml"
  if [ ! -f "$file" ]; then
    echo "跳过：$file 不存在"
    continue
  fi

  result=$(curl -s -X POST "http://${NACOS_ADDR}/nacos/v1/cs/configs" \
    --data-urlencode "dataId=${svc}.yaml" \
    --data-urlencode "group=DEFAULT_GROUP" \
    --data-urlencode "content@${file}")

  echo "[${file}] 发布结果：${result}"
done

echo "----------------------------------------"
echo "完成。可在 Nacos 控制台查看，或重启后端服务生效。"
