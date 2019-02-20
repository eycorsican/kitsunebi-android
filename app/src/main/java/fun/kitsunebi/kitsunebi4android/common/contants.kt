package `fun`.kitsunebi.kitsunebi4android.common

class Constants {
    companion object {
        val PREFERENCE_CONFIG_KEY = "preference_config_key"
        val SUBSCRIBE_CONFIG_URL_KEY = "subscribe_config_url_key"
        val DEFAULT_CONFIG = """
            {
    "log": {
        "loglevel": "warning"
    },
    "outbounds": [
        {
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
                "handshake": 4,
            	"bufferSize": 4096
            },
            "2018": {
                "uplinkOnly": 0,
                "downlinkOnly": 0,
                "connIdle": 5,
                "handshake": 4,
            	"bufferSize": 4096
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
        """.trimIndent()
    }
}