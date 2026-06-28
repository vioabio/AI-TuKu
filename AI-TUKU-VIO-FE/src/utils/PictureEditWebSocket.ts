type MessageHandler = (msg: any) => void

export class PictureEditWebSocket {
  private ws: WebSocket | null = null
  private readonly pictureId: number | string
  private handlers: Record<string, MessageHandler[]> = {}

  constructor(pictureId: number | string) {
    this.pictureId = pictureId
  }

  connect() {
    const url = `ws://localhost:8123/api/ws/picture/edit?pictureId=${this.pictureId}`
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      console.log('WebSocket 连接已建立，pictureId:', this.pictureId)
    }

    this.ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        const type = msg.type
        if (type && this.handlers[type]) {
          this.handlers[type].forEach(handler => handler(msg))
        }
      } catch (e) {
        console.error('解析 WebSocket 消息失败:', e)
      }
    }

    this.ws.onclose = (event) => {
      console.log('WebSocket 连接已关闭，code:', event.code)
    }

    this.ws.onerror = (error) => {
      console.error('WebSocket 连接错误:', error)
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.handlers = {}
  }

  sendMessage(message: Record<string, any>) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    } else {
      console.warn('WebSocket 未连接，无法发送消息')
    }
  }

  on(type: string, handler: MessageHandler) {
    if (!this.handlers[type]) {
      this.handlers[type] = []
    }
    this.handlers[type].push(handler)
  }
}
