package schema

import (
	"fmt"
	"strings"
)

// Document is a loaded JSON Schema document.
type Document struct {
	ID     string
	Root   *Element
	Defs   []*Def // holds the sorted entries of "$defs"
	byName map[string]*Def
}

type Def struct {
	Name    string
	Element *Element
}

type Element struct {
	Path string

	Title       string
	Description string

	Ref                  string
	Types                []string
	Properties           []*Property
	Required             []string
	AdditionalProperties *Element
	Items                *Element
	Enum                 []string
	Const                *string
	OneOf                []*Element
}

type Property struct {
	Name    string
	Element *Element
}

func (e *Element) Property(name string) *Element {
	for _, p := range e.Properties {
		if p.Name == name {
			return p.Element
		}
	}
	return nil
}

func Errorf(path, format string, args ...any) error {
	return fmt.Errorf("%s: %s", path, fmt.Sprintf(format, args...))
}

// Resolve returns the definition the "$ref" of e points to. Only references to entries of the root's "$defs" are
// supported.
func (d *Document) Resolve(e *Element) (*Def, error) {
	name, ok := strings.CutPrefix(e.Ref, "#/$defs/")
	if !ok {
		return nil, Errorf(e.Path, "reference %q is not supported, only references such as \"#/$defs/Name\" are", e.Ref)
	}
	def := d.byName[name]
	if def == nil {
		return nil, Errorf(e.Path, "reference %q does not resolve to a definition", e.Ref)
	}
	return def, nil
}
