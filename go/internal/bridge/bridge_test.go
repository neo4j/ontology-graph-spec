package bridge

import (
	"bytes"
	"os"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestExtractEmbeddedLib(t *testing.T) {
	if len(embeddedLib) == 0 {
		t.Skip("no embedded library for this build/platform")
	}

	path, err := extractEmbeddedLib()
	require.NoError(t, err)
	t.Cleanup(func() {
		_ = os.RemoveAll(filepath.Dir(path))
	})

	got, err := os.ReadFile(path)
	require.NoError(t, err)
	require.True(t, bytes.Equal(embeddedLib, got), "extracted bytes should match the embedded library")

	info, err := os.Stat(filepath.Dir(path))
	require.NoError(t, err)
	require.Zero(t, info.Mode().Perm()&0o077, "extraction dir must not be group/other accessible")
}

func TestResolveLibraryPathEnvOverride(t *testing.T) {
	t.Setenv(LibPathEnv, "/explicit/path/libgraphdatamodel.so")

	path, err := resolveLibraryPath()
	require.NoError(t, err)
	require.Equal(t, "/explicit/path/libgraphdatamodel.so", path)
}

func TestOpenLibraryMissingLibIsActionable(t *testing.T) {
	t.Setenv(LibPathEnv, "/some/path/to/non-existent.so")

	_, err := openLibrary()
	require.Error(t, err)
	require.ErrorContains(t, err, "glibc and libstdc++")
	require.ErrorContains(t, err, LibPathEnv)
}

func TestCallInputValidation(t *testing.T) {
	if _, err := loadBridge(); err != nil {
		t.Skipf("native library unavailable: %v", err)
	}

	testCases := []struct {
		name    string
		op      Op
		model   []byte
		args    []string
		wantErr string
	}{
		{
			name:    "empty model",
			op:      Migrate,
			model:   []byte{},
			args:    []string{"0", "1", "2"},
			wantErr: "empty input provided",
		},
		{
			name:    "args containing empty value",
			op:      Migrate,
			model:   []byte("0"),
			args:    []string{"1", "2", ""},
			wantErr: "empty input provided [3]",
		},
		{
			name:    "not enough args",
			op:      Migrate,
			model:   []byte("0"),
			args:    []string{"1"},
			wantErr: "migrate requires 3 arguments",
		},
		{
			name:    "too many args for migrate",
			op:      Migrate,
			model:   []byte("0"),
			args:    []string{"1", "2", "3", "4"},
			wantErr: "migrate requires 3 arguments",
		},
		{
			name:    "not enough args for validate",
			op:      Validate,
			model:   []byte("0"),
			wantErr: "validate requires 1 argument",
		},
		{
			name:    "too many args for validate",
			op:      Validate,
			model:   []byte("0"),
			args:    []string{"1", "2"},
			wantErr: "validate requires 1 argument",
		},
		{
			name:    "unknown operation",
			op:      Op("unknown-op"),
			model:   []byte("0"),
			wantErr: "unknown bridge call",
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			_, err := Call(tc.op, tc.model, tc.args...)
			require.ErrorContains(t, err, tc.wantErr)
		})
	}
}

func TestCallRetriedIfOutputBufferNotLargeEnough(t *testing.T) {
	// An empty graph-spec model will get transformed to a fully initialised data model which
	// will be far larger than the input model. This test checks that the client will successfully
	// retry with the required buffer size in cases like these.
	res, err := Call(Migrate, []byte(`{"schema":"https://neo4j.com/docs/ontology-graph-spec/1.0.0/schema.json","version":"1","id":"test"}`), "graph_spec", "data_model", "3.0.0")
	require.NotEmpty(t, res)
	require.NoError(t, err)
}
