package schema

import (
	"encoding/json"
	"fmt"
	"maps"
	"slices"
	"strings"
)

var typeNames = []string{"null", "boolean", "object", "array", "number", "integer", "string"}

func Load(data []byte) (*Document, error) {
	var raw json.RawMessage
	if err := json.Unmarshal(data, &raw); err != nil {
		return nil, fmt.Errorf("invalid JSON: %w", err)
	}
	doc := &Document{byName: map[string]*Def{}}
	root, err := doc.parse(raw, "#", true)
	if err != nil {
		return nil, err
	}
	doc.Root = root
	return doc, nil
}

func (d *Document) parse(raw json.RawMessage, path string, root bool) (*Element, error) {
	var members map[string]json.RawMessage
	if json.Unmarshal(raw, &members) != nil {
		return nil, Errorf(path, "a schema must be an object")
	}

	e := &Element{Path: path}
	for _, key := range slices.Sorted(maps.Keys(members)) {
		val, p := members[key], path+"/"+escapePointer(key)
		var err error
		switch key {
		case "$schema", "$id", "$defs":
			if !root {
				return nil, Errorf(p, "%q is only supported on the root schema", key)
			}
			if key == "$defs" {
				err = d.parseDefs(val, p)
			}
			if key == "$id" {
				err = decode(val, &d.ID, p, "a string")
			}
		case "title":
			err = decode(val, &e.Title, p, "a string")
		case "description":
			err = decode(val, &e.Description, p, "a string")
		case "$ref":
			err = decode(val, &e.Ref, p, "a string")
		case "type":
			e.Types, err = parseTypes(val, p)
		case "properties":
			err = d.parseNamed(val, p, func(name string, pe *Element) {
				e.Properties = append(e.Properties, &Property{Name: name, Element: pe})
			})
		case "required":
			err = decode(val, &e.Required, p, "an array of strings")
		case "additionalProperties":
			if string(val) != "false" {
				e.AdditionalProperties, err = d.parse(val, p, false)
			}
		case "items":
			e.Items, err = d.parse(val, p, false)
		case "enum":
			if decode(val, &e.Enum, p, "an array of strings") != nil || len(e.Enum) == 0 {
				return nil, Errorf(p, "\"enum\" must be a non-empty array of strings")
			}
		case "const":
			err = decode(val, &e.Const, p, "a string")
		case "oneOf":
			e.OneOf, err = d.parseOneOf(val, p)
		case "minimum":
			// minimum restricts values, not their type, so it has no effect on the generated code.
		default:
			return nil, Errorf(p, "keyword %q is not supported", key)
		}
		if err != nil {
			return nil, err
		}
	}
	return e, nil
}

// parseNamed parses each member of the object raw as a schema and passes it to add, in order of name.
func (d *Document) parseNamed(raw json.RawMessage, path string, add func(name string, e *Element)) error {
	var members map[string]json.RawMessage
	if err := decode(raw, &members, path, "an object"); err != nil {
		return err
	}
	for _, name := range slices.Sorted(maps.Keys(members)) {
		e, err := d.parse(members[name], path+"/"+escapePointer(name), false)
		if err != nil {
			return err
		}
		add(name, e)
	}
	return nil
}

func (d *Document) parseDefs(raw json.RawMessage, path string) error {
	return d.parseNamed(raw, path, func(name string, e *Element) {
		def := &Def{Name: name, Element: e}
		d.Defs = append(d.Defs, def)
		d.byName[name] = def
	})
}

func (d *Document) parseOneOf(raw json.RawMessage, path string) ([]*Element, error) {
	var branches []json.RawMessage
	if decode(raw, &branches, path, "an array") != nil || len(branches) == 0 {
		return nil, Errorf(path, "\"oneOf\" must be a non-empty array")
	}
	elems := make([]*Element, len(branches))
	for i, branch := range branches {
		var err error
		if elems[i], err = d.parse(branch, fmt.Sprintf("%s/%d", path, i), false); err != nil {
			return nil, err
		}
	}
	return elems, nil
}

func parseTypes(raw json.RawMessage, path string) ([]string, error) {
	var types []string
	if json.Unmarshal(raw, &types) != nil {
		var t string
		if err := decode(raw, &t, path, "a string or an array of strings"); err != nil {
			return nil, err
		}
		types = []string{t}
	}
	for i, t := range types {
		if !slices.Contains(typeNames, t) {
			return nil, Errorf(path, "unknown type %q", t)
		}
		if slices.Contains(types[:i], t) {
			return nil, Errorf(path, "type %q is listed more than once", t)
		}
	}
	return types, nil
}

func decode(raw json.RawMessage, target any, path, want string) error {
	if json.Unmarshal(raw, target) != nil {
		return Errorf(path, "expected %s", want)
	}
	return nil
}

func escapePointer(s string) string {
	return strings.ReplaceAll(strings.ReplaceAll(s, "~", "~0"), "/", "~1")
}
