package main

/*
extern int ws_protect_socket(int fd);
*/
import "C"

// protectSocket hands the fd to the host app's VpnService.protect(). The dialer calls this between
// socket() and connect(), so returning before protect() has finished would let the connection be
// routed while the socket is still inside the tunnel.
func protectSocket(fd int) bool {
	return C.ws_protect_socket(C.int(fd)) != 0
}
