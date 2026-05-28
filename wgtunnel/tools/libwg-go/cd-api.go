package main

// #cgo LDFLAGS: -llog
// #include <android/log.h>
import "C"

import (
	"sync"
	"github.com/Control-D-Inc/ctrld/cmd/cli"
)

var controller *Controller

// Controller holds global state
type Controller struct {
	stopCh   chan struct{}
	stopOnce sync.Once
	Config   cli.AppConfig
}

// NewController provides reference to global state to be managed by android vpn service and iOS network extension.
// reference is not safe for concurrent use.
func NewController() *Controller {
	return &Controller{}
}

func safeStop() {
	if controller != nil {
		controller.stopOnce.Do(func() {
			if controller.stopCh != nil {
				close(controller.stopCh)
				controller.stopCh = nil
			}
		})
		controller = nil
	}
}

//export StartCd
func StartCd(CdUID string, HomeDir string, UpstreamProto string, logLevel int, logPath string, hostName string, lanIp string, macAddress string) {
	defer func() {
		if r := recover(); r != nil {
			tag := cstring("ControlD/Panic")
			logger := AndroidLogger{level: C.ANDROID_LOG_ERROR, tag: tag}
			logger.Printf("StartCd panic recovered: %v", r)
			safeStop()
		}
	}()

	if controller != nil {
		return
	}

	controller = NewController()

	callback := cli.AppCallback{
		HostName: func() string { return hostName },
		LanIp: func() string { return lanIp },
		MacAddress: func() string { return macAddress },
		Exit: func(err string) {},
	}

	if controller.stopCh == nil {
		controller.stopCh = make(chan struct{})
		controller.Config = cli.AppConfig{
			CdUID:         CdUID,
			HomeDir:       HomeDir,
			UpstreamProto: UpstreamProto,
			Verbose:       logLevel,
			LogPath:       logPath,
		}

		cli.RunMobile(&controller.Config, &callback, controller.stopCh)
	}
}

//export StopCd
func StopCd(restart bool, pin int64) int {
	defer func() {
		if r := recover(); r != nil {
			tag := cstring("ControlD/Panic")
			logger := AndroidLogger{level: C.ANDROID_LOG_ERROR, tag: tag}
			logger.Printf("StopCd panic recovered: %v", r)
			safeStop()
		}
	}()

	if controller == nil {
		return 0
	}

	errorCode := 0

	if !restart {
		errorCode = cli.CheckDeactivationPin(pin, controller.stopCh)
	}

	if errorCode == 0 {
		safeStop()
	}

	return errorCode
}

//export IsCdRunning
func IsCdRunning() bool {
	return controller != nil && controller.stopCh != nil
}