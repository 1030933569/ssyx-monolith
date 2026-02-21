#!/bin/bash
# 将各模块SQL导入统一的 ssyx 数据库（兼容 MySQL 5.7）
set -e

DB_NAME="${MYSQL_DATABASE:-ssyx}"
SQL_DIR="/sql-scripts"

echo "=== 开始初始化数据库 $DB_NAME ==="

SQL_FILES=(
  "shequ-acl.sql"
  "shequ-sys.sql"
  "shequ-product.sql"
  "shequ-user.sql"
  "shequ-order.sql"
  "shequ-activity.sql"
  "add_leader_apply.sql"
)

for sql_file in "${SQL_FILES[@]}"; do
  filepath="$SQL_DIR/$sql_file"
  if [ -f "$filepath" ]; then
    echo "--- 导入: $sql_file ---"
    sed \
      -e '/^CREATE DATABASE/d' \
      -e '/^USE /d' \
      -e 's/\r$//' \
      -e 's/utf8mb4_0900_ai_ci/utf8mb4_general_ci/g' \
      -e 's/COLLATE=utf8mb4_0900_ai_ci/COLLATE=utf8mb4_general_ci/g' \
      -e "s/ \/\*!80016 DEFAULT ENCRYPTION='N' \*\///g" \
      "$filepath" | mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$DB_NAME"
    echo "--- 完成: $sql_file ---"
  else
    echo "!!! 跳过: $sql_file (不存在)"
  fi
done

echo "=== 数据库初始化完成 ==="
