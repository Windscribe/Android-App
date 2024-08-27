package main

import (
	"C"
	"github.com/Windscribe/wstunnel/cli"
	"github.com/spf13/cobra"
	"os"
	_ "runtime/cgo"
)

var listenAddress string
var remoteAddress string
var tunnelType int
var mtu int
var extraTlsPadding bool
var logFilePath string
var dev = false

var rootCmd = &cobra.Command{
	Use:   "root",
	Short: "Starts local proxy and connects to server.",
	Long:  "Starts local proxy and sets up connection to the server. At minimum it requires remote server address and log file path.",
	Run: func(cmd *cobra.Command, args []string) {
		Initialise(dev, logFilePath)
		started := StartProxy(listenAddress, remoteAddress, tunnelType, mtu, extraTlsPadding)
		if started == false {
			os.Exit(0)
		}
	},
}

func init() {
	rootCmd.PersistentFlags().StringVarP(&listenAddress, "listenAddress", "l", ":65479", "Local port for proxy > :65479")
	rootCmd.PersistentFlags().StringVarP(&remoteAddress, "remoteAddress", "r", "", "Wstunnel > wss://$ip:$port/tcp/127.0.0.1/$WS_TUNNEL_PORT  Stunnel > https://$ip:$port")
	_ = rootCmd.MarkPersistentFlagRequired("remoteAddress")
	rootCmd.PersistentFlags().IntVarP(&tunnelType, "tunnelType", "t", 1, "WStunnel > 1 , Stunnel > 2")
	rootCmd.PersistentFlags().IntVarP(&mtu, "mtu", "m", 1500, "1500")
	rootCmd.PersistentFlags().BoolVarP(&extraTlsPadding, "extraTlsPadding", "p", false, "Add Extra TLS Padding to ClientHello packet.")
	rootCmd.PersistentFlags().StringVarP(&logFilePath, "logFilePath", "f", "", "Path to log file > file.log")
	_ = rootCmd.MarkPersistentFlagRequired("logFilePath")
	rootCmd.PersistentFlags().BoolVarP(&dev, "dev", "d", false, "Turns on verbose logging.")
}

//export Callback is used by http client to send events to host app
var primaryListenerSocketFd int = -1

//export Initialise
func Initialise(development bool, logFilePath string) {
	cli.InitLogger(development, logFilePath)
}

//export StartProxy
func StartProxy(listenAddress string, remoteAddress string, tunnelType int, mtu int, extraPadding bool) bool {
	cli.Logger.Infof("Starting proxy with listenAddress: %s remoteAddress %s tunnelType: %d mtu %d", listenAddress, remoteAddress, tunnelType, mtu)
	err := cli.NewHTTPClient(listenAddress, remoteAddress, tunnelType, mtu, func(fd int) {
		primaryListenerSocketFd = fd
		cli.Logger.Info("Socket ready to protect.")
	}, cli.Channel, extraPadding).Run()
	if err != nil {
		return false
	}
	return true
}

//export Stop
func Stop() {
	cli.Logger.Info("Disconnect signal from host app.")
	cli.Channel <- "done"
}

//export GetPrimaryListenerSocketFd
func GetPrimaryListenerSocketFd() int {
	return primaryListenerSocketFd
}
