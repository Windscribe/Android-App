package api

import (
	"Ws/wstunnel"
	_ "golang.org/x/mobile/bind"
	"log"
	"os"
)

/*
This project uses gomobile to build android and ios libraries used in Windscribe apps for Wstunnel support.
*/

// Start Builds and start a http client (Tcp server + Bi directional connection between clients and Websocket server)
// This Function blocks until exit signal is sent by host app.
func Start(listenAddress string, wsAddress string, logFilePath string) bool {
	initLogger(logFilePath)
	err := wstunnel.NewHTTPClient(listenAddress, wsAddress, func(fd int) {
		if hostAppCallBack != nil {
			hostAppCallBack.Protect(fd)
		} else {
			log.Print("App has not registered callback.")
		}
	}, channel).Run()
	if err != nil {
		return false
	}
	return true
}

// Log output is saved to the app provided log file.
func initLogger(logFilePath string) {
	file, err := os.OpenFile(logFilePath, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0666)
	if err != nil {
		log.Fatal(err)
	}
	log.SetOutput(file)
}

// Channel Host app > Library
var channel = make(chan string)

// Callback for Library > Host app
var hostAppCallBack AppCallback

// RegisterCallback is called from the host app.
func RegisterCallback(callback AppCallback) {
	if callback != nil {
		log.Print("Connecting to host app.")
		hostAppCallBack = callback
	} else {
		log.Print("Disconnecting from host app.")
		channel <- "exit"
	}
}

// AppCallback Host app should implement this interface and register.
type AppCallback interface {
	// Protect Web socket's underlying file descriptor sent to host app for protecting it from VPN Service.
	Protect(fd int)
}
