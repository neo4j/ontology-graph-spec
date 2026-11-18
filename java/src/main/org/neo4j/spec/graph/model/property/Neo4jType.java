// Generated from org/neo4j/spec/graph/model/property/Neo4jType.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.property;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

@JsonClassDescription("A Neo4j property type.")
public enum Neo4jType {
    ANY,

    @JsonProperty("LIST<ANY>")
    LIST_ANY,

    BOOLEAN,

    @JsonProperty("LIST<BOOLEAN>")
    LIST_BOOLEAN,

    DATE,

    @JsonProperty("LIST<DATE>")
    LIST_DATE,

    DURATION,

    @JsonProperty("LIST<DURATION>")
    LIST_DURATION,

    FLOAT32,

    @JsonProperty("LIST<FLOAT32>")
    LIST_FLOAT32,

    FLOAT,

    @JsonProperty("LIST<FLOAT>")
    LIST_FLOAT,

    INTEGER8,

    @JsonProperty("LIST<INTEGER8>")
    LIST_INTEGER8,

    INTEGER16,

    @JsonProperty("LIST<INTEGER16>")
    LIST_INTEGER16,

    INTEGER32,

    @JsonProperty("LIST<INTEGER32>")
    LIST_INTEGER32,

    @JsonProperty("INTEGER")
    INTEGER,

    @JsonProperty("LIST<INTEGER>")
    LIST_INTEGER,

    @JsonProperty("LOCAL DATETIME")
    LOCAL_DATETIME,

    @JsonProperty("LIST<LOCAL DATETIME>")
    LIST_LOCAL_DATETIME,

    @JsonProperty("LOCAL TIME")
    LOCAL_TIME,

    @JsonProperty("LIST<LOCAL TIME>")
    LIST_LOCAL_TIME,

    POINT,

    @JsonProperty("LIST<POINT>")
    LIST_POINT,

    STRING,

    @JsonProperty("LIST<STRING>")
    LIST_STRING,

    @JsonProperty("VECTOR<FLOAT>")
    VECTOR_FLOAT,

    @JsonProperty("VECTOR<FLOAT32>")
    VECTOR_FLOAT32,

    @JsonProperty("VECTOR<INTEGER>")
    VECTOR_INTEGER,

    @JsonProperty("VECTOR<INTEGER32>")
    VECTOR_INTEGER32,

    @JsonProperty("VECTOR<INTEGER16>")
    VECTOR_INTEGER16,

    @JsonProperty("VECTOR<INTEGER8>")
    VECTOR_INTEGER8,

    @JsonProperty("ZONED DATETIME")
    ZONED_DATETIME,

    @JsonProperty("LIST<ZONED DATETIME>")
    LIST_ZONED_DATETIME,

    @JsonProperty("ZONED TIME")
    ZONED_TIME,

    @JsonProperty("LIST<ZONED TIME>")
    LIST_ZONED_TIME,

    UUID;

    Neo4jType() {
    }

    public static Neo4jType fromString(String string) {
        if (Objects.equals(string, "ANY")) {
            return Neo4jType.ANY;
        } else if (Objects.equals(string, "LIST<ANY>")) {
            return Neo4jType.LIST_ANY;
        } else if (Objects.equals(string, "BOOLEAN")) {
            return Neo4jType.BOOLEAN;
        } else if (Objects.equals(string, "LIST<BOOLEAN>")) {
            return Neo4jType.LIST_BOOLEAN;
        } else if (Objects.equals(string, "DATE")) {
            return Neo4jType.DATE;
        } else if (Objects.equals(string, "LIST<DATE>")) {
            return Neo4jType.LIST_DATE;
        } else if (Objects.equals(string, "DURATION")) {
            return Neo4jType.DURATION;
        } else if (Objects.equals(string, "LIST<DURATION>")) {
            return Neo4jType.LIST_DURATION;
        } else if (Objects.equals(string, "FLOAT32")) {
            return Neo4jType.FLOAT32;
        } else if (Objects.equals(string, "LIST<FLOAT32>")) {
            return Neo4jType.LIST_FLOAT32;
        } else if (Objects.equals(string, "FLOAT")) {
            return Neo4jType.FLOAT;
        } else if (Objects.equals(string, "LIST<FLOAT>")) {
            return Neo4jType.LIST_FLOAT;
        } else if (Objects.equals(string, "INTEGER8")) {
            return Neo4jType.INTEGER8;
        } else if (Objects.equals(string, "LIST<INTEGER8>")) {
            return Neo4jType.LIST_INTEGER8;
        } else if (Objects.equals(string, "INTEGER16")) {
            return Neo4jType.INTEGER16;
        } else if (Objects.equals(string, "LIST<INTEGER16>")) {
            return Neo4jType.LIST_INTEGER16;
        } else if (Objects.equals(string, "INTEGER32")) {
            return Neo4jType.INTEGER32;
        } else if (Objects.equals(string, "LIST<INTEGER32>")) {
            return Neo4jType.LIST_INTEGER32;
        } else if (Objects.equals(string, "INTEGER")) {
            return Neo4jType.INTEGER;
        } else if (Objects.equals(string, "LIST<INTEGER>")) {
            return Neo4jType.LIST_INTEGER;
        } else if (Objects.equals(string, "LOCAL DATETIME")) {
            return Neo4jType.LOCAL_DATETIME;
        } else if (Objects.equals(string, "LIST<LOCAL DATETIME>")) {
            return Neo4jType.LIST_LOCAL_DATETIME;
        } else if (Objects.equals(string, "LOCAL TIME")) {
            return Neo4jType.LOCAL_TIME;
        } else if (Objects.equals(string, "LIST<LOCAL TIME>")) {
            return Neo4jType.LIST_LOCAL_TIME;
        } else if (Objects.equals(string, "POINT")) {
            return Neo4jType.POINT;
        } else if (Objects.equals(string, "LIST<POINT>")) {
            return Neo4jType.LIST_POINT;
        } else if (Objects.equals(string, "STRING")) {
            return Neo4jType.STRING;
        } else if (Objects.equals(string, "LIST<STRING>")) {
            return Neo4jType.LIST_STRING;
        } else if (Objects.equals(string, "VECTOR<FLOAT>")) {
            return Neo4jType.VECTOR_FLOAT;
        } else if (Objects.equals(string, "VECTOR<FLOAT32>")) {
            return Neo4jType.VECTOR_FLOAT32;
        } else if (Objects.equals(string, "VECTOR<INTEGER>")) {
            return Neo4jType.VECTOR_INTEGER;
        } else if (Objects.equals(string, "VECTOR<INTEGER32>")) {
            return Neo4jType.VECTOR_INTEGER32;
        } else if (Objects.equals(string, "VECTOR<INTEGER16>")) {
            return Neo4jType.VECTOR_INTEGER16;
        } else if (Objects.equals(string, "VECTOR<INTEGER8>")) {
            return Neo4jType.VECTOR_INTEGER8;
        } else if (Objects.equals(string, "ZONED DATETIME")) {
            return Neo4jType.ZONED_DATETIME;
        } else if (Objects.equals(string, "LIST<ZONED DATETIME>")) {
            return Neo4jType.LIST_ZONED_DATETIME;
        } else if (Objects.equals(string, "ZONED TIME")) {
            return Neo4jType.ZONED_TIME;
        } else if (Objects.equals(string, "LIST<ZONED TIME>")) {
            return Neo4jType.LIST_ZONED_TIME;
        } else if (Objects.equals(string, "UUID")) {
            return Neo4jType.UUID;
        } else {
            return null;
        }
    }

    public static String toString(Neo4jType type) {
        if (type == Neo4jType.ANY) {
            return "ANY";
        } else if (type == Neo4jType.LIST_ANY) {
            return "LIST<ANY>";
        } else if (type == Neo4jType.BOOLEAN) {
            return "BOOLEAN";
        } else if (type == Neo4jType.LIST_BOOLEAN) {
            return "LIST<BOOLEAN>";
        } else if (type == Neo4jType.DATE) {
            return "DATE";
        } else if (type == Neo4jType.LIST_DATE) {
            return "LIST<DATE>";
        } else if (type == Neo4jType.DURATION) {
            return "DURATION";
        } else if (type == Neo4jType.LIST_DURATION) {
            return "LIST<DURATION>";
        } else if (type == Neo4jType.FLOAT32) {
            return "FLOAT32";
        } else if (type == Neo4jType.LIST_FLOAT32) {
            return "LIST<FLOAT32>";
        } else if (type == Neo4jType.FLOAT) {
            return "FLOAT";
        } else if (type == Neo4jType.LIST_FLOAT) {
            return "LIST<FLOAT>";
        } else if (type == Neo4jType.INTEGER8) {
            return "INTEGER8";
        } else if (type == Neo4jType.LIST_INTEGER8) {
            return "LIST<INTEGER8>";
        } else if (type == Neo4jType.INTEGER16) {
            return "INTEGER16";
        } else if (type == Neo4jType.LIST_INTEGER16) {
            return "LIST<INTEGER16>";
        } else if (type == Neo4jType.INTEGER32) {
            return "INTEGER32";
        } else if (type == Neo4jType.LIST_INTEGER32) {
            return "LIST<INTEGER32>";
        } else if (type == Neo4jType.INTEGER) {
            return "INTEGER";
        } else if (type == Neo4jType.LIST_INTEGER) {
            return "LIST<INTEGER>";
        } else if (type == Neo4jType.LOCAL_DATETIME) {
            return "LOCAL DATETIME";
        } else if (type == Neo4jType.LIST_LOCAL_DATETIME) {
            return "LIST<LOCAL DATETIME>";
        } else if (type == Neo4jType.LOCAL_TIME) {
            return "LOCAL TIME";
        } else if (type == Neo4jType.LIST_LOCAL_TIME) {
            return "LIST<LOCAL TIME>";
        } else if (type == Neo4jType.POINT) {
            return "POINT";
        } else if (type == Neo4jType.LIST_POINT) {
            return "LIST<POINT>";
        } else if (type == Neo4jType.STRING) {
            return "STRING";
        } else if (type == Neo4jType.LIST_STRING) {
            return "LIST<STRING>";
        } else if (type == Neo4jType.VECTOR_FLOAT) {
            return "VECTOR<FLOAT>";
        } else if (type == Neo4jType.VECTOR_FLOAT32) {
            return "VECTOR<FLOAT32>";
        } else if (type == Neo4jType.VECTOR_INTEGER) {
            return "VECTOR<INTEGER>";
        } else if (type == Neo4jType.VECTOR_INTEGER32) {
            return "VECTOR<INTEGER32>";
        } else if (type == Neo4jType.VECTOR_INTEGER16) {
            return "VECTOR<INTEGER16>";
        } else if (type == Neo4jType.VECTOR_INTEGER8) {
            return "VECTOR<INTEGER8>";
        } else if (type == Neo4jType.ZONED_DATETIME) {
            return "ZONED DATETIME";
        } else if (type == Neo4jType.LIST_ZONED_DATETIME) {
            return "LIST<ZONED DATETIME>";
        } else if (type == Neo4jType.ZONED_TIME) {
            return "ZONED TIME";
        } else if (type == Neo4jType.LIST_ZONED_TIME) {
            return "LIST<ZONED TIME>";
        } else if (type == Neo4jType.UUID) {
            return "UUID";
        } else {
            throw new IllegalStateException("Unexpected when branch");
        }
    }

    public static boolean isVector(Neo4jType type) {
        if ((type == Neo4jType.VECTOR_FLOAT) || (type == Neo4jType.VECTOR_FLOAT32) || (type == Neo4jType.VECTOR_INTEGER) || (type == Neo4jType.VECTOR_INTEGER32) || (type == Neo4jType.VECTOR_INTEGER16) || (type == Neo4jType.VECTOR_INTEGER8)) {
            return true;
        } else {
            return false;
        }
    }
}
