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

// Start Builds and start a http client (Tcp server + Websocket connection)
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

// Channel app >> Golang
var channel = make(chan string)

// Callback for Golang >>> App
var hostAppCallBack AppCallback

// RegisterCallback is called from the app.
func RegisterCallback(callback AppCallback) {
	if callback != nil {
		log.Print("App registered callback")
		hostAppCallBack = callback
	} else {
		channel <- "exit"
	}
}

// AppCallback App should implement this interface and register.
type AppCallback interface {
	// Protect Web socket's underlying file descriptor sent to android for protecting it from VPN Service.
	Protect(fd int)
}
