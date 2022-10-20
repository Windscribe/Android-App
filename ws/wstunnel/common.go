package wstunnel

const (
	// BufferSize is the size of the intermediate buffer for network packets
	BufferSize = 1024
)

// Runner defines a basic interface with only a Run() function
type Runner interface {
	Run() error
}
