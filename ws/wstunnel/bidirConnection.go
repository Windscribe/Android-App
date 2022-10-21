package wstunnel

import (
	"io"
	"log"
	"net"
	"os"
	"time"

	"github.com/gorilla/websocket"
)

////////////////////////////////////////////////////////////////////////////////
// bidirConnection
////////////////////////////////////////////////////////////////////////////////

// bidirConnection implements the Runner interface
type bidirConnection struct {
	tcpConn        net.Conn
	wsConn         *websocket.Conn
	tcpReadTimeout time.Duration
}

// NewBidirConnection to create an object to transfer data between the TCP socket and web connection in bidirectional way
func NewBidirConnection(tcpConn net.Conn, wsConn *websocket.Conn, tcpReadTimeout time.Duration) Runner {
	return &bidirConnection{
		tcpConn:        tcpConn,
		wsConn:         wsConn,
		tcpReadTimeout: tcpReadTimeout,
	}
}

func (b *bidirConnection) sendTCPToWS() {
	defer b.close()
	data := make([]byte, BufferSize)
	for {
		if b.tcpReadTimeout > 0 {
			_ = b.tcpConn.SetReadDeadline(time.Now().Add(b.tcpReadTimeout))
		}
		readSize, err := b.tcpConn.Read(data)
		if err != nil && !os.IsTimeout(err) {
			if err != io.EOF {
				log.Printf("TCPToWS - Error while reading from TCP: %s", err)
			}
			return
		}

		if err := b.wsConn.WriteMessage(websocket.BinaryMessage, data[:readSize]); err != nil {
			log.Printf("TCPToWS - Error while writing to WS: %s", err)
			return
		}
	}
}

func (b *bidirConnection) sendWSToTCP() {
	defer b.close()
	data := make([]byte, BufferSize)
	for {
		messageType, wsReader, err := b.wsConn.NextReader()
		if err != nil {
			log.Printf("WSToTCP - Error while reading from WS: %s", err)
			return
		}
		if messageType != websocket.BinaryMessage {
			log.Printf("WSToTCP - Got wrong message type from WS: %d", messageType)
			return
		}

		for {
			readSize, err := wsReader.Read(data)
			if err != nil {
				if err != io.EOF {
					log.Printf("WSToTCP - Error while reading from WS: %s", err)
				}
				break
			}

			if _, err := b.tcpConn.Write(data[:readSize]); err != nil {
				log.Printf("WSToTCP - Error while writing to TCP: %s", err)
				return
			}
		}
	}
}

func (b *bidirConnection) Run() error {
	go b.sendTCPToWS()
	b.sendWSToTCP()
	return nil
}

func (b *bidirConnection) close() {
	_ = b.wsConn.WriteControl(websocket.CloseMessage, []byte{}, time.Now().Add(time.Second))
	_ = b.wsConn.Close()
	_ = b.tcpConn.Close()
}
