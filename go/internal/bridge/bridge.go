package bridge

import (
	"errors"
	"fmt"
	"math"
	"runtime"
	"sync"
	"unsafe"

	"github.com/ebitengine/purego"
)

const (
	// Status bytes written to the first entry in the output buffer. The payload/error message follows this (buf[1:res])
	statusOK    byte = 0
	statusError byte = 1

	// Non-retryable error response codes when the native lib could not write to the output buffer
	invalidInputError = -1
	internalError     = math.MinInt32
)

type Op string

const (
	Migrate  Op = "Migrate"
	Validate Op = "Validate"

	// exported native method names
	migrate  = "migrate"
	validate = "validate"
)

type bridge struct {
	migrate  func(inputJSON unsafe.Pointer, inputType, targetType, targetVersion string, outputBuffer unsafe.Pointer, bufferSize int32) int32
	validate func(inputJSON unsafe.Pointer, validationSets string, outputBuffer unsafe.Pointer, bufferSize int32) int32
}

var loadBridge = sync.OnceValues(bindBridge)

func Call(op Op, model []byte, args ...string) ([]byte, error) {
	b, err := loadBridge()
	if err != nil {
		return nil, err
	}

	if len(model) == 0 {
		return nil, fmt.Errorf("empty input provided")
	}
	for i, arg := range args {
		if len(arg) == 0 {
			return nil, fmt.Errorf("empty input provided [%d]", i+1)
		}
	}
	// The C boundary expects NUL terminated strings, so append a null byte to model bytes
	cModel := append(model, 0)

	// To simplify memory management on the Kotlin side we create an output buffer, still under
	// the scope of the Go GC, for the Kotlin library to write a response to. We must also provide
	// the maximum buffer size to avoid overflows when the Kotlin library writes the output. We use
	// the length of the model as reference.
	buf := make([]byte, 2*len(model))
	res, err := b.call(op, cModel, args, buf)
	if err != nil && res < invalidInputError && res != internalError { // buffer not large enough, retry with required size
		buf = make([]byte, -res)
		res, err = b.call(op, cModel, args, buf)
	}
	if err != nil {
		return nil, err
	}
	if res < 1 {
		return nil, fmt.Errorf("bridge returned a malformed response of %d bytes", res)
	}

	status, payload := buf[0], buf[1:res]
	if status == statusError {
		return nil, fmt.Errorf("received error from library: %s", payload)
	}
	if status != statusOK {
		return nil, fmt.Errorf("bridge returned unknown status byte %d", status)
	}
	return payload, nil
}

func (b *bridge) call(op Op, model []byte, args []string, buf []byte) (int, error) {
	// The input and output buffers are pinned for the duration of the native call so the Go GC cannot move them while
	// the Kotlin library holds the pointers. Pinning also keeps them reachable.
	var pinner runtime.Pinner
	pinner.Pin(&model[0])
	pinner.Pin(&buf[0])
	defer pinner.Unpin()

	in := unsafe.Pointer(&model[0])
	out := unsafe.Pointer(&buf[0])
	outLen := int32(len(buf))

	var res int32
	switch op {
	case Migrate:
		if len(args) != 3 {
			return invalidInputError, fmt.Errorf("migrate requires 3 arguments, got %d", len(args))
		}
		res = b.migrate(in, args[0], args[1], args[2], out, outLen)
	case Validate:
		if len(args) != 1 {
			return invalidInputError, fmt.Errorf("validate requires 1 argument, got %d", len(args))
		}
		res = b.validate(in, args[0], out, outLen)
	default:
		return invalidInputError, fmt.Errorf("unknown bridge call: %s", op)
	}

	if res == internalError {
		return internalError, errors.New("library could not allocate a response, the input likely exceeds the heap allowed by GRAPHSPEC_MAX_HEAP_BYTES")
	}
	if res < 0 {
		return int(res), fmt.Errorf("bridge error (buffer too small or internal failure): code %d", res)
	}
	return int(res), nil
}

func bindBridge() (b *bridge, err error) {
	defer func() {
		// purego.RegisterLibFunc panics if a symbol cannot be bound which gets converted to an error.
		if r := recover(); r != nil {
			b, err = nil, fmt.Errorf("failed to bind graphdatamodel symbols: %v", r)
		}
	}()

	lib, err := library()
	if err != nil {
		return nil, err
	}

	b = &bridge{}
	purego.RegisterLibFunc(&b.migrate, lib, migrate)
	purego.RegisterLibFunc(&b.validate, lib, validate)
	return b, nil
}
