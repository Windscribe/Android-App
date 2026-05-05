package main

// #cgo LDFLAGS: -llog
// #include <android/log.h>
import "C"

import (
	"github.com/Control-D-Inc/ctrld/cmd/cli"
)

var controller *Controller

// Controller holds global state
type Controller struct {
	stopCh chan struct{}
	Config cli.AppConfig
}

// NewController provides reference to global state to be managed by android vpn service and iOS network extension.
// reference is not safe for concurrent use.
func NewController() *Controller {
	return &Controller{}
}

//export StartCd
func StartCd(CdUID string, HomeDir string, UpstreamProto string, logLevel int, logPath string, hostName string, lanIp string, macAddress string) {
	defer func() {
		if r := recover(); r != nil {
			tag := cstring("ControlD/Panic")
			logger := AndroidLogger{level: C.ANDROID_LOG_ERROR, tag: tag}
			logger.Printf("StartCd panic recovered: %v", r)
			// Clean up controller on panic to prevent invalid state
			if controller != nil && controller.stopCh != nil {
				close(controller.stopCh)
				controller.stopCh = nil
			}
			controller = nil
		}
	}()

	if controller != nil {
		return
	}
	controller = NewController()
	callback := cli.AppCallback{
		HostName: func() string {
			return hostName
		},
		LanIp: func() string {
			return lanIp
		},
		MacAddress: func() string {
			return macAddress
		},
		Exit: func(err string) {

		},
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
			// Ensure cleanup even on panic
			if controller != nil && controller.stopCh != nil {
				close(controller.stopCh)
				controller.stopCh = nil
			}
			controller = nil
		}
	}()

	if controller == nil {
		return 0
	}
	var errorCode = 0
	// Force disconnect without checking pin.
	// In iOS restart is required if vpn detects no connectivity after network change.
	if !restart {
		errorCode = cli.CheckDeactivationPin(pin, controller.stopCh)
	}
	if errorCode == 0 && controller.stopCh != nil {
		close(controller.stopCh)
		controller.stopCh = nil
	}
	controller = nil
	return errorCode
}

//export IsCdRunning
func IsCdRunning() bool {
	return controller != nil && controller.stopCh != nil
}
