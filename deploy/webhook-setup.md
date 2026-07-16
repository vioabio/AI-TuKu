# ============================================================
# AI-TuKu 服务器端 Webhook 接收器配置
# ============================================================
# 
# 方式一：使用 GitHub Actions 的 Webhook 通知（推荐）
# 在仓库 Settings → Secrets and variables → Actions 中配置:
#   - DEPLOY_WEBHOOK_URL:  http://your-server:9000/webhook/deploy
#   - DEPLOY_WEBHOOK_SECRET: 你的密钥（用于 HMAC 签名验证）
#
# ============================================================
# 方式二：使用 adnanh/webhook 轻量 Webhook 服务
# ============================================================
# 
# 在服务器上安装:
#   sudo apt install webhook    # Debian/Ubuntu
#   或使用 Docker:
#   docker run -d -p 9000:9000 \
#     -v /opt/aituku/deploy/hooks.json:/etc/webhook/hooks.json \
#     --name webhook almir/webhook
#
# hooks.json 示例:
#
# [
#   {
#     "id": "deploy-aituku",
#     "execute-command": "/opt/aituku/deploy/deploy.sh",
#     "command-working-directory": "/opt/aituku",
#     "pass-arguments-to-command": [
#       {"source": "payload", "name": "tag"}
#     ],
#     "trigger-rule": {
#       "match": {
#         "type": "payload-hmac-sha256",
#         "secret": "YOUR_WEBHOOK_SECRET",
#         "parameter": {
#           "source": "header",
#           "name": "X-Hub-Signature-256"
#         }
#       }
#     }
#   }
# ]
#
# ============================================================
# 方式三：Cloudflare Tunnel 暴露内网服务
# ============================================================
# 
# 如果服务器在家宽/内网，用 Cloudflare Tunnel 穿透:
#   cloudflared tunnel create aituku
#   cloudflared tunnel route dns aituku deploy.aituku.com
#   cloudflared tunnel run aituku --url http://localhost:9000
#
# GitHub Webhook URL 改为: https://deploy.aituku.com/webhook/deploy
