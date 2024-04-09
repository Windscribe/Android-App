module golang.zx2c4.com/wireguard/android

go 1.19

require (
	golang.org/x/sys v0.13.0
	golang.zx2c4.com/wireguard v0.0.0-20230223-e24fc776e0ffb6e3c293895163d95bdd8a3c386f
)

require (
	golang.org/x/crypto v0.14.0 // indirect
	golang.org/x/net v0.17.0 // indirect
	golang.zx2c4.com/wintun v0.0.0-20230126152724-0fa3db229ce2 // indirect
)

replace golang.zx2c4.com/wireguard => github.com/Windscribe/wireguard v1.0.2
