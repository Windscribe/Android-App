# tcp-over-websocket
Simple TCP over Websocket tunneling

## Why TCP over Websocket
Some crappy networks only allow HTTP(S). This small program allows you to tunnel
anything in HTTP(S).

Note that the URL path is always `/`, the server does not support SSL/TLS out of
the box and there is one remote TCP address per instance. This is done on
purpose as this program is intended to be run behind a reverse proxy.

## Install
Having a working [Golang](https://golang.org/) environment:
```
go install github.com/trazfr/tcp-over-websocket@latest
```

## Use

### Server side
The server listens for Websocket inbound connections. Each time a client is
connected, the server opens a new TCP connection to the remote service.

**Example**: ssh to github
```
# on server example.org
tcp-over-websocket server -listen_ws :8080 -remote_tcp github.com:22
```

### Client side
The client opens a TCP connection and creates a Websocket connection toward a
tcp-over-websocket server each time a TCP connection is accepted.

**Example**: open a tunnel to the server
```
# on the client, target the previous server on example.org
tcp-over-websocket client -listen_tcp :1234 -remote_ws ws://example.org:8080/
```

To use the tunnel, one may only run `ssh -p 1234 127.0.0.1` on the client.

### Note about reverse proxy usage
tcp-over-websocket does not support SSL/TLS on server side. To be able to host
a website and several tunnel servers at the same place, one may configure an
HTTP reverse proxy.

Let's assume we want to serve the previous server on SSL/TLS on the URL
`wss://example.org/github-ssh/`.

Using a similar configuration, one NGINX instance may be the reverse proxy for
several tunnels for different services by changing the URL.

#### NGINX as reverse proxy
NGINX configuration extract:
```
http {
    map $http_upgrade $connection_upgrade {
        default upgrade;
        '' close;
    }

    server {
        listen 443 ssl default_server;
        listen [::]:443 default_server ipv6only=on ssl;
        server_name example.org;
        // put here the SSL/TLS configuration

        location = /github-ssh/ {
            limit_except GET {
                deny all;
            }
            // for "tcp-over-websocket server-listen_ws 127.0.0.1:8080 -remote_tcp github.com:22"
            proxy_pass http://127.0.0.1:8080/;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection $connection_upgrade;
        }
        // TODO configure different locations to different instances of tcp-over-websocket
    }
}
```

To connect from the client, the command is like this:
```
tcp-over-websocket client -listen_tcp :1234 -remote_ws wss://example.org/github-ssh/
```

## Misc

### Prometheus service
The tcp-over-websocket server hosts a Prometheus service. It only exposes some
basic metrics.
```
curl http://example.org:8080/metrics
```
