package main

// #cgo LDFLAGS: -llog
// #include <android/log.h>
import "C"

import (
	"sync"

	"github.com/Control-D-Inc/ctrld/cmd/cli"
)

// cli.RunMobile installs package-level state and blocks until stopCh closes, so
// starting it again before a previous run has unwound re-enters that state from
// two goroutines. A panic in one of ctrld's own goroutines is not recoverable
// from here and kills the process, so transitions are serialised.

type Controller struct {
	stopCh   chan struct{}
	stopOnce sync.Once
	Config   cli.AppConfig
}

var (
	controllerMu sync.Mutex
	controller   *Controller
)

// stopCh is created here rather than lazily so it is never nil and never
// reassigned.
func NewController() *Controller {
	return &Controller{stopCh: make(chan struct{})}
}

// signalStop closes stopCh exactly once, from any goroutine.
func (c *Controller) signalStop() {
	c.stopOnce.Do(func() { close(c.stopCh) })
}

func cdLog(format string, args ...interface{}) {
	AndroidLogger{
		level: C.ANDROID_LOG_ERROR,
		tag:   cstring("ControlD/Panic"),
	}.Printf(format, args...)
}

//export StartCd
func StartCd(CdUID string, HomeDir string, UpstreamProto string, logLevel int, logPath string, hostName string, lanIp string, macAddress string) {
	defer func() {
		if r := recover(); r != nil {
			cdLog("StartCd panic recovered: %v", r)
		}
	}()

	controllerMu.Lock()
	if controller != nil {
		controllerMu.Unlock()
		return
	}
	c := NewController()
	c.Config = cli.AppConfig{
		CdUID:         CdUID,
		HomeDir:       HomeDir,
		UpstreamProto: UpstreamProto,
		Verbose:       logLevel,
		LogPath:       logPath,
	}
	controller = c
	controllerMu.Unlock()

	// Cleared only once RunMobile has returned, so IsCdRunning cannot report
	// "stopped" while ctrld is still shutting down.
	defer func() {
		controllerMu.Lock()
		if controller == c {
			controller = nil
		}
		controllerMu.Unlock()
	}()

	callback := cli.AppCallback{
		HostName:   func() string { return hostName },
		LanIp:      func() string { return lanIp },
		MacAddress: func() string { return macAddress },
		Exit:       func(err string) {},
	}

	cli.RunMobile(&c.Config, &callback, c.stopCh)
}

//export StopCd
func StopCd(restart bool, pin int64) int {
	defer func() {
		if r := recover(); r != nil {
			cdLog("StopCd panic recovered: %v", r)
		}
	}()

	controllerMu.Lock()
	c := controller
	controllerMu.Unlock()

	if c == nil {
		return 0
	}

	if !restart {
		if errorCode := cli.CheckDeactivationPin(pin, c.stopCh); errorCode != 0 {
			return errorCode
		}
	}

	c.signalStop()
	return 0
}

//export IsCdRunning
func IsCdRunning() bool {
	controllerMu.Lock()
	defer controllerMu.Unlock()
	return controller != nil
}
