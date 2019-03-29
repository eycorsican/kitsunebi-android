# Kitsunebi

A fully-featured V2Ray client for Android.

## 下载

<a href="https://play.google.com/store/apps/details?id=fun.kitsunebi.kitsunebi4android"><img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height="100"></a>

## 扩展功能
扩展了 v2ray-core 的功能：
- 新增 Latency Balancing Strategy：[详情及配置看这里](https://gist.github.com/eycorsican/356debc8295e752c1df6ad7286f98ad4)

## 关于 DNS 处理
自 v1.0.0 起，默认的 DNS 处理方式为 Fake DNS，启用 Fake DNS 后，DNS 请求的流量几乎不会被传进 V2Ray，所以 V2Ray 的 `内置 DNS` 和 `DNS outbound` 配置不会起太大作用；当 Fake DNS 处于禁用状态，DNS 请求的流量会以正常 UDP 流量的形式进入 V2Ray，这时你可以使用 inbound tag 在路由中配置路由规则来识别出相应 DNS 流量，从而转发给 `DNS outbound`，从而让 V2Ray 的 `内置 DNS` 来处理（看下面配置示例）。如果使用自定义配置的同时开启 Fake DNS，则需要确保 freedom outbound 中的域名策略为 `非 AsIs`。

Fake DNS 跟 V2Ray 的 `流量探测` 在效果上非常相似，目的同样是要拿到请求的域名，但工作原理上有较大差异：
- Fake DNS
  - 适用于任何请求的流量
  - 对于走代理的请求，本地不会实际发出任何 DNS 请求流量（远程 DNS 解析）
  - 会染污本地 DNS 缓存，VPN 关掉后的短暂时间内，可能会导致网络请求异常
- `流量探测`
  - 只适用 http/tls 流量
  - 不能控制本地是否发出 DNS 请求流量（由 V2Ray 的 DNS 功能模块控制）

## 配置示例

```json
{
    "dns": {
        "clientIp": "115.239.211.92",
        "hosts": {
            "localhost": "127.0.0.1"
        },
        "servers": [
            "114.114.114.114",
            {
                "address": "8.8.8.8",
                "domains": [
                    "google",
                    "android",
                    "fbcdn",
                    "facebook",
                    "domain:fb.com",
                    "instagram",
                    "whatsapp",
                    "akamai",
                    "domain:line-scdn.net",
                    "domain:line.me",
                    "domain:naver.jp"
                ],
                "port": 53
            }
        ]
    },
    "log": {
        "loglevel": "warning"
    },
    "outbounds": [
        {
            "protocol": "vmess",
            "settings": {
                "vnext": [
                    {
                        "address": "1.2.3.4",
                        "port": 10086,
                        "users": [
                            {
                                "id": "0e8575fb-a71f-455b-877f-b74e19d3f495"
                            }
                        ]
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
                "domainStrategy": "UseIP"
            },
            "streamSettings": {},
            "tag": "direct"
        },
        {
            "protocol": "blackhole",
            "settings": {},
            "tag": "block"
        },
	{
	    "protocol": "dns",
	    "tag": "dns-out"
	}
    ],
    "policy": {
        "levels": {
            "0": {
                "bufferSize": 4096,
                "connIdle": 30,
                "downlinkOnly": 0,
                "handshake": 4,
                "uplinkOnly": 0
            }
        }
    },
    "routing": {
        "domainStrategy": "IPIfNonMatch",
        "rules": [
            {
                "inboundTag": ["tun2socks"],
                "network": "udp",
                "port": 53,
                "outboundTag": "dns-out",
                "type": "field"
            },
            {
                "domain": [
                    "domain:setup.icloud.com"
                ],
                "outboundTag": "proxy",
                "type": "field"
            },
            {
                "ip": [
                    "8.8.8.8/32",
                    "8.8.4.4/32",
                    "1.1.1.1/32",
                    "1.0.0.1/32",
                    "9.9.9.9/32",
                    "149.112.112.112/32",
                    "208.67.222.222/32",
                    "208.67.220.220/32"
                ],
                "outboundTag": "proxy",
                "type": "field"
            },
            {
                "ip": [
                    "geoip:cn",
                    "geoip:private"
                ],
                "outboundTag": "direct",
                "type": "field"
            },
            {
                "outboundTag": "direct",
                "port": "123",
                "type": "field"
            },
            {
                "domain": [
                    "domain:pstatp.com",
                    "domain:snssdk.com",
                    "domain:toutiao.com",
                    "domain:ixigua.com",
                    "domain:apple.com",
                    "domain:crashlytics.com",
                    "domain:icloud.com",
                    "cctv",
                    "umeng",
                    "domain:weico.cc",
                    "domain:jd.com",
                    "domain:360buy.com",
                    "domain:360buyimg.com",
                    "domain:douyu.tv",
                    "domain:douyu.com",
                    "domain:douyucdn.cn",
                    "geosite:cn"
                ],
                "outboundTag": "direct",
                "type": "field"
            },
            {
                "ip": [
                    "149.154.167.0/24",
                    "149.154.175.0/24",
                    "91.108.56.0/24",
                    "125.209.222.0/24"
                ],
                "outboundTag": "proxy",
                "type": "field"
            },
            {
                "domain": [
                    "twitter",
                    "domain:twimg.com",
                    "domain:t.co",
                    "google",
                    "domain:ggpht.com",
                    "domain:gstatic.com",
                    "domain:youtube.com",
                    "domain:ytimg.com",
                    "pixiv",
                    "domain:pximg.net",
                    "tumblr",
                    "instagram",
                    "domain:line-scdn.net",
                    "domain:line.me",
                    "domain:naver.jp",
                    "domain:facebook.com",
                    "domain:fbcdn.net",
                    "pinterest",
                    "github",
                    "dropbox",
                    "netflix",
                    "domain:medium.com",
                    "domain:fivecdm.com"
                ],
                "outboundTag": "proxy",
                "type": "field"
            }
        ],
        "strategy": "rules"
    }
}
```
