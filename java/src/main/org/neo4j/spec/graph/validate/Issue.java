/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.spec.graph.validate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class Issue {
    private final String code;

    private final String message;

    private final String path;

    private final Map<String, String> details;

    public Issue(String code, String message, String path, Map<String, String> details) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.details = details;
    }

    public Issue(String code, String message, String path) {
        this(code, message, path, Collections.emptyMap());
    }

    public Issue(String code, String message) {
        this(code, message, null, Collections.emptyMap());
    }

    public String getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public String getPath() {
        return this.path;
    }

    public Map<String, String> getDetails() {
        return this.details;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Issue)) return false;
        Issue that = (Issue) other;
        return Objects.equals(code, that.code)
                && Objects.equals(message, that.message)
                && Objects.equals(path, that.path)
                && Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message, path, details);
    }

    @Override
    public String toString() {
        return "Issue(code=" + code + ", message=" + message + ", path=" + path + ", details=" + details + ")";
    }

    @JsonProperty("code")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonCode() {
        return this.code;
    }

    @JsonProperty("message")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String jsonMessage() {
        return this.message;
    }

    @JsonProperty("path")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonPath() {
        return Objects.equals(this.path, null) ? null : this.path;
    }

    @JsonProperty("details")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> jsonDetails() {
        return Objects.equals(this.details, Collections.emptyMap()) ? null : this.details;
    }

    @JsonCreator
    private static Issue fromJson(
            @JsonProperty("code") String code,
            @JsonProperty("message") String message,
            @JsonProperty("path") String path,
            @JsonProperty("details") Map<String, String> details) {
        return new Issue(
                Objects.requireNonNull(code, "Field 'code' is required"),
                Objects.requireNonNull(message, "Field 'message' is required"),
                path,
                details != null ? details : Collections.emptyMap());
    }
}
