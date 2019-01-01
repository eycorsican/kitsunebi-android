# Kitsunebi

A fully-featured V2Ray client for Android.

## Build

You must first build the `tun2socks.aar` library from [this repo](https://github.com/eycorsican/go-tun2socks-android) and copy it into `app/libs`, get `geoip.dat` and `geosite.dat` from the latest v2ray-core release and copy them into `app/src/main/res/raw`.

## 开发相关问题

这个 V2Ray Android 客户端利用 `go-tun2socks` 把所有的 TCP/UDP 流量转给到 V2Ray 处理，所用的 `v2ray-core` 是没经过任何修改的官方版本，所以在配置和体验方面不会有太大差别。但在 Android 上有一些东西需要特殊处理，这也是在配置和使用上造成一些差别的地方。

- UDP 流量在转给 V2Ray 之前，会先在 `go-tun2socks` 里面经过一个嗅探步骤，如果嗅探出来是 DNS 流量，这些 UDP 流量就不会直接转给 V2Ray，而是把其中需要解析的域名抽取出来再通过 V2Ray 的 DNS 去解析，这时 V2Ray 所用的 DNS 服务器是按照配置文件上的配置来选取，也就是说所用的 DNS 服务器不需要跟原来 UDP 流量的目的地址相同。
- 在 Android 上有几个情况会造成流量/请求的死循环，目前就我所知的有以下几个：
  - 通过 VpnService 的 TUN 接口读取数据，再由代理程序代为发出时，如果不 [protect](https://developer.android.com/reference/android/net/VpnService#protect(int)) 代理程序用来发数据的 socket fd，代理程序发出的数据又会被转到 TUN 接口上，这个问题基本可以用 v2ray-core 提供的 `RegisterDialerController()` 和 `RegisterListenerController()` 两个接口完美解决
  - <del>当代理服务器的地址是域名的时候，如果要转发数据给代理服务器，就必须先把代理服务器的 IP 地址解析出来，但这个 DNS 解析本身又有可能是另一个代理请求（考虑下全局模式），就出现死循环了，这个问题大概可以有两种解决方法：一是在启动 VPN 前把所有代理服务器的 IP 预先解析出来，之后如果碰到要解析这些域名的请求，就直接替换/返回预先解析好的 IP，二是强制所有这些域名不走代理（结合 `go-tun2socks` 的 DNS 请求拦截特性，利用 V2Ray 的 DNS 和路由功能可以比较容易实现）。本项目目前采用第一个方法，但这方法的缺点很明显就是当域名所对应的 DNS 记录中途被修改了的话就要重连 VPN</del>
  - <del>还有一种会引起死循环的情况是 V2Ray 的 DNS 配置里用 `localhost`（不配置 DNS 的话默认就是用 `localhost`，所以必须要配置），一方面因为 `go-tun2socks` 的 DNS 流量拦截特性，另一方面因为 `本地 DNS` 所发出的 UDP 流量不太容易 protect 起来，所以就会形成这样一个死循环：V2Ray 用 `本地 DNS` (`localhost`) 发请求，UDP 流量没被 protect，发到 TUN 接口上，`go-tun2socks` 拦截下来，用 V2Ray 的 DNS 解析，V2Ray 用 `本地 DNS` (`localhost`) 发请求......解决方法当然是要强制禁止用 `localhost`</del>

## 下载

<a href="https://play.google.com/store/apps/details?id=fun.kitsunebi.kitsunebi4android"><img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height="100"></a>

Github Releases: https://github.com/eycorsican/Kitsunebi4Android/releases

## 使用提示

- App 使用较新的 v2ray-core 版本，你或许需要确保服务端也升级到相应的版本，具体版本号请看 Release Notes
- 把配置文件复制粘贴至主界面后，点击连接按钮即可启动
- 如果配置文件不正确或者出错，通常不会有任何错误提示
- 配置文件可使用一个常见的 V2Ray 配置
- 配置文件的 freedom outbound 推荐使用 [`UseIP` 策略](https://www.v2ray.com/chapter_02/protocols/freedom.html#outboundconfigurationobject)
- 配置文件不需要有 Inbound，app 使用了 `tun2socks` 作为 inbound，并已开启 [http,tls 流量嗅探](https://www.v2ray.com/chapter_02/01_overview.html#sniffingobject)
- 设备所有 DNS 请求均会由 V2Ray 的 [DNS 服务器](https://www.v2ray.com/chapter_02/04_dns.html) 来解析，正确设置了 DNS 服务器可以避免 DNS 污染以及 CDN 相关的 DNS 问题
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
            "localhost": "127.0.0.1"
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

- 面下是一个可以拿来日常使用的配置模板，在 `outbounds` 中替换上你的服务器信息即可，其中的路由规则和 DNS 规则从这个 [规则集文件](https://github.com/eycorsican/rule-sets/blob/master/kitsunebi_default.conf) 生成：
```json
{
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
                        "address": "yourserver.com",
                        "port": 10086,
                        "users": [
                            {
                                "alterId": 10,
                                "security": "chacha20-poly1305",
                                "id": "xxxx-xxx-xx-xx-x-xx"
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
                "userLevel": 2018,
                "domainStrategy": "UseIP"
            },
            "streamSettings": {},
            "tag": "direct"
        },
        {
            "tag": "block",
            "protocol": "blackhole",
            "settings": {}
        }
    ],
    "policy": {
        "levels": {
            "0": {
                "uplinkOnly": 0,
                "downlinkOnly": 0,
                "connIdle": 5,
                "handshake": 4
            },
            "2018": {
                "uplinkOnly": 0,
                "downlinkOnly": 0,
                "connIdle": 5,
                "handshake": 4
            }
        }
    },
    "dns": {
        "hosts": {
            "localhost": "127.0.0.1"
        },
        "servers": [
            "223.5.5.5",
            "114.114.114.114",
            {
                "address": "8.8.8.8",
                "port": 53,
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
                ]
            },
            "8.8.4.4"
        ],
        "clientIp": "115.239.211.92"
    },
    "inbounds": [],
    "routing": {
        "rules": [
            {
                "domain": [
                    "domain:doubleclick.net"
                ],
                "outboundTag": "block",
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
                "type": "field",
                "outboundTag": "direct",
                "port": "123"
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
        "domainStrategy": "IPIfNonMatch",
        "strategy": "rules"
    }
}
```
