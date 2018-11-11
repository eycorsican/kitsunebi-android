# Kitsunebi

Not yet ready for opensourcing.

## 下载

<a href="https://play.google.com/store/apps/details?id=fun.kitsunebi.kitsunebi4android"><img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height="100"></a>

Github Releases: https://github.com/eycorsican/Kitsunebi4Android/releases

## 使用提示

- App 使用的 v2ray-core 版本为 v4.3，你或许需要确保服务端也升级到相应的版本
- 把配置文件复制粘贴至主界面后，点击连接按钮即可启动
- 如果配置文件不正确或者出错，通常不会有任何错误提示
- 配置文件可使用一个常见的 V2Ray 配置
- 配置文件的 freedom outbound 需要使用 [`UseIP` 策略](https://www.v2ray.com/chapter_02/protocols/freedom.html#outboundconfigurationobject)
- 配置文件不需要有 Inbound，app 使用了 `tun2socks` 作为 inbound，并已开启 [http,tls 流量嗅探](https://www.v2ray.com/chapter_02/01_overview.html#sniffingobject)
- 如果 freedom outbound 正确配置了 `UseIP`，设备所有 DNS 请求均由 V2Ray 的 [DNS 服务器](https://www.v2ray.com/chapter_02/04_dns.html)来解析，正确设置了 DNS 服务器可以避免 DNS 污染以及 CDN 相关的 DNS 问题
- 下面是一个示例配置：
```json
{
    "log": {
        "loglevel": "info"
    },
    "policy": {
        "0": {
            "bufferSize": 0
        }
    },
    "dns": {
        "clientIP": "115.239.211.92",  # 你的对外地址（或者随便找个同地区的 IP），用来提示 DNS 服务器返回合适的 IP
        "hosts": {
            "localhost": "127.0.0.1",
            "domain:lan": "127.0.0.1",
            "domain:local": "127.0.0.1",
            "domain:arpa": "127.0.0.1"
        },
        "servers": [
            "8.8.8.8",
            "1.1.1.1",
            {
                "address": "223.5.5.5",
                "port": 53,
                "domains": [
                    "geosite:cn"  # 如果 DNS 请求的域名匹配了，就优先使用这个 DNS 服务器去解析
                ]
            }
        ]
    },
    "outbounds": [
        {
            "mux": {
                "concurrency": 8,
                "enabled": false
            },
            "protocol": "vmess",
            "settings": {
                "vnext": [
                    {
                        "users": [
                            {
                                "id": "01f7abbf-1431-48d7-bc22-fd1d614d8dfe",
                                "alterId": 0,
                                "security": "chacha20-poly1305"
                            }
                        ],
                        "address": "myserver.com",
                        "port": 10086
                    }
                ]
            },
            "streamSettings": {
                "network": "tcp"
            },
            "tag": "proxy"
        },
        {
            "protocol": "freedom",
            "settings": {
                "domainStrategy": "UseIP"  # 为了把所有 DNS 请求都给到 V2Ray 处理
            },
            "tag": "direct"
        },
        {
            "protocol": "blackhole",
            "settings": {},
            "tag": "block"
        }
    ],
    "routing": {
        "settings": {
            "rules": [
                {
                    "type": "field",
                    "domain": [
                        "geosite:cn"
                    ],
                    "outboundTag": "direct"
                },
                {
                    "type": "field",
                    "ip": [
                        "geoip:cn",
                        "geoip:private"
                    ],
                    "outboundTag": "direct"
                },
                {
                    "type": "field",
                    "network": "tcp,udp",
                    "outboundTag": "proxy"     # 没匹配任何规则就转发给代理
                }
            ],
            "domainStrategy": "IPIfNonMatch"
        },
        "strategy": "rules"
    }
}
```
