package generator

import (
	"errors"
	"flag"
	"io/fs"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"

	"github.com/stretchr/testify/require"
)

var update = flag.Bool("update", false, "rewrite the expected files")

func TestGenerate_InvalidPackageName(t *testing.T) {
	_, err := Generate([]byte(`{}`), Options{Package: "func"})
	require.Error(t, err)
}

func TestGenerate_CSharp(t *testing.T) {
	cases, err := filepath.Glob("testdata/expected/*.json")
	require.NoError(t, err)
	for _, schemaPath := range cases {
		name := strings.TrimSuffix(filepath.Base(schemaPath), ".json")
		t.Run(name, func(t *testing.T) {
			data, err := os.ReadFile(schemaPath)
			require.NoError(t, err)
			got, err := Generate(data, Options{Language: "csharp", Namespace: "Generated." + name, SchemaName: filepath.Base(schemaPath)})
			require.NoError(t, err)
			again, err := Generate(data, Options{Language: "csharp", Namespace: "Generated." + name, SchemaName: filepath.Base(schemaPath)})
			require.NoError(t, err)
			require.Equal(t, got, again)
			expected := strings.TrimSuffix(schemaPath, ".json") + ".cs.expected"
			if *update {
				require.NoError(t, os.WriteFile(expected, got, 0o644))
				return
			}
			want, err := os.ReadFile(expected)
			require.NoError(t, err)
			require.Equal(t, string(want), string(got))
		})
	}
}

func TestGenerate_CSharpRejectsInvalidNames(t *testing.T) {
	for _, namespace := range []string{"", "Bad..Name", "3Name", "Some.class", "Some-Name"} {
		_, err := Generate([]byte(`{"title":"Example","type":"object","properties":{}}`), Options{Language: "csharp", Namespace: namespace})
		require.Error(t, err, namespace)
	}
	for _, schema := range []string{
		`{"title":"Type","type":"object","properties":{}}`,
		`{"title":"Enum","type":"string","enum":["one","two"]}`,
		`{"title":"Example","type":"object","properties":{"some-name":{"type":"string"},"some_name":{"type":"string"}}}`,
		`{"title":"Example","type":"object","properties":{"example":{"type":"string"}}}`,
		`{"title":"Example","type":"string","enum":["A_B","a-b"]}`,
		`{"title":"Example","type":"object","properties":{"value":{"type":"string","pattern":"x"}}}`,
	} {
		_, err := Generate([]byte(schema), Options{Language: "csharp", Namespace: "Example.Models"})
		require.Error(t, err, schema)
	}
}

func TestGenerate_Success(t *testing.T) {
	cases, err := filepath.Glob("testdata/expected/*.json")
	require.NoError(t, err)

	for _, schemaPath := range cases {
		name := strings.TrimSuffix(filepath.Base(schemaPath), ".json")
		t.Run(name, func(t *testing.T) {
			data, err := os.ReadFile(schemaPath)
			require.NoError(t, err)

			got, err := Generate(data, Options{Package: "model", SchemaName: filepath.Base(schemaPath)})
			require.NoError(t, err)

			expectedPath := strings.TrimSuffix(schemaPath, ".json") + ".go.expected"
			if *update {
				require.NoError(t, os.WriteFile(expectedPath, got, 0o644))
				return
			}
			want, err := os.ReadFile(expectedPath)
			require.NoError(t, err)
			require.Equal(t, string(want), string(got), "run the tests with -update to accept the change")
		})
	}
}

func TestGenerate_GoVetAndTest(t *testing.T) {
	if testing.Short() {
		t.Skip("runs the go tool")
	}
	goTool, err := exec.LookPath("go")
	if err != nil {
		t.Skip("go tool not found")
	}

	dir := t.TempDir()
	write := func(path string, data []byte) {
		t.Helper()
		require.NoError(t, os.MkdirAll(filepath.Dir(path), 0o755))
		require.NoError(t, os.WriteFile(path, data, 0o644))
	}
	write(filepath.Join(dir, "go.mod"), []byte("module generated\n\ngo 1.25.6\n"))

	expectedFiles, err := filepath.Glob("testdata/expected/*.go.expected")
	require.NoError(t, err)

	for _, expected := range expectedFiles {
		name := strings.TrimSuffix(filepath.Base(expected), ".go.expected")
		src, err := os.ReadFile(expected)
		require.NoError(t, err)
		write(filepath.Join(dir, name, "model.go"), src)

		test, err := os.ReadFile(filepath.Join("testdata", "runtime", name+"_test.go"))
		if errors.Is(err, fs.ErrNotExist) {
			continue
		}
		require.NoError(t, err)
		write(filepath.Join(dir, name, name+"_test.go"), test)
	}

	for _, args := range [][]string{{"vet", "./..."}, {"test", "./..."}} {
		cmd := exec.Command(goTool, args...)
		cmd.Dir = dir
		out, err := cmd.CombinedOutput()
		require.NoError(t, err, "go %s failed:\n%s", strings.Join(args, " "), out)
	}
}
