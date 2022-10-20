package wstunnel

import (
	"crypto/tls"
	"crypto/x509"
	"github.com/gorilla/websocket"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"net/url"
	"syscall"
	"time"
)

////////////////////////////////////////////////////////////////////////////////
// httpClient
////////////////////////////////////////////////////////////////////////////////

// httpClient implements the Runner interface
type httpClient struct {
	connectWS string
	listenTCP string
	callback  func(fd int)
	channel   chan string
}

func NewHTTPClient(listenTCP, connectWS string, callback func(fd int), channel chan string) Runner {
	return &httpClient{
		connectWS: connectWS,
		listenTCP: listenTCP,
		callback:  callback,
		channel:   channel,
	}
}

func (h *httpClient) Run() error {
	tcpAdr, _ := net.ResolveTCPAddr("tcp", h.listenTCP)
	tcpConnection, err := net.ListenTCP("tcp", tcpAdr)
	if err != nil {
		return err
	}
	defer tcpConnection.Close()
	var active = true
	log.Printf("Listening to %s", h.listenTCP)
	for active {
		tcpConn, err := tcpConnection.Accept()
		if err != nil {
			log.Printf("Error: could not accept the connection: %s", err)
			continue
		}
		log.Printf("New connection from: %s", tcpConn.RemoteAddr().String())

		wsConn, err := h.createWsConnection(tcpConn.RemoteAddr().String())
		if err != nil || wsConn == nil {
			log.Printf("%s - Ws connection > Error while dialing %s: %s", tcpConn.RemoteAddr(), h.connectWS, err)
			tcpConn.Close()
			continue
		}

		b := NewBidirConnection(tcpConn, wsConn, time.Second*10)
		go b.Run()

		select {
		case msg := <-h.channel:
			if msg == "exit" {
				wsConn.Close()
				tcpConn.Close()
				active = false
			}
		}
	}
	return err
}

func (h *httpClient) toWsURL(asString string) (string, error) {
	asURL, err := url.Parse(asString)
	if err != nil {
		return asString, err
	}

	switch asURL.Scheme {
	case "http":
		asURL.Scheme = "ws"
	case "https":
		asURL.Scheme = "wss"
	}
	return asURL.String(), nil
}

// Creates a connection to websocket server.
func (h *httpClient) createWsConnection(remoteAddr string) (wsConn *websocket.Conn, err error) {
	wsConnectUrl := h.connectWS
	for {
		var wsURL string
		wsURL, err = h.toWsURL(wsConnectUrl)
		if err != nil {
			return
		}
		log.Printf("%s - Connecting to %s", remoteAddr, wsURL)
		var httpResponse *http.Response
		dialer := *websocket.DefaultDialer
		serverCert, _ := ioutil.ReadFile("server.crt")
		serverKey, _ := ioutil.ReadFile("server.key")
		privateKey, _ := x509.ParsePKCS1PrivateKey(serverKey)
		// Access underlying socket fd before connecting to it.
		customNetDialer := &net.Dialer{}
		customNetDialer.Control = func(network, address string, c syscall.RawConn) error {
			return c.Control(func(fd uintptr) {
				i := int(fd)
				if err != nil {
					return
				}
				h.callback(i)
			})
		}
		dialer.NetDial = func(network, addr string) (net.Conn, error) {
			return customNetDialer.Dial(network, addr)
		}
		cert := tls.Certificate{
			Certificate: [][]byte{serverCert},
			PrivateKey:  privateKey,
		}
		dialer.TLSClientConfig = &tls.Config{Certificates: []tls.Certificate{cert}, InsecureSkipVerify: true}
		header := http.Header{}
		wsConn, httpResponse, err = dialer.Dial(wsURL, header)

		if httpResponse != nil {
			switch httpResponse.StatusCode {
			case http.StatusMovedPermanently, http.StatusFound, http.StatusSeeOther, http.StatusTemporaryRedirect, http.StatusPermanentRedirect:
				wsConnectUrl = httpResponse.Header.Get("Location")
				log.Printf("%s - Redirect to %s", remoteAddr, wsConnectUrl)
				continue
			}
		}
		return
	}
}
