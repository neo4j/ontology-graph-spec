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
package org.neo4j.spec.graph.model.relationship;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Objects;

@JsonClassDescription("One end of a relationship.")
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public final class RelationshipTarget {
    private String node;

    private String label;

    private Integer count;

    private Integer minCount;

    private Integer maxCount;

    private RelationshipTarget(String node, String label, Integer count, Integer minCount, Integer maxCount) {
        this.node = node;
        this.label = label;
        this.count = count;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getNode() {
        return this.node;
    }

    public void setNode(String value) {
        this.node = value;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String value) {
        this.label = value;
    }

    public Integer getCount() {
        return this.count;
    }

    public void setCount(Integer value) {
        this.count = value;
    }

    public Integer getMinCount() {
        return this.minCount;
    }

    public void setMinCount(Integer value) {
        this.minCount = value;
    }

    public Integer getMaxCount() {
        return this.maxCount;
    }

    public void setMaxCount(Integer value) {
        this.maxCount = value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RelationshipTarget)) return false;
        RelationshipTarget that = (RelationshipTarget) other;
        return Objects.equals(node, that.node)
                && Objects.equals(label, that.label)
                && Objects.equals(count, that.count)
                && Objects.equals(minCount, that.minCount)
                && Objects.equals(maxCount, that.maxCount);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(node);
        result = 31 * result + Objects.hashCode(label);
        result = 31 * result + Objects.hashCode(count);
        result = 31 * result + Objects.hashCode(minCount);
        result = 31 * result + Objects.hashCode(maxCount);
        return result;
    }

    @Override
    public String toString() {
        return "RelationshipTarget(node=" + node + ", label=" + label + ", count=" + count + ", minCount=" + minCount
                + ", maxCount=" + maxCount + ")";
    }

    @JsonProperty("node")
    @JsonPropertyDescription("Id of the node at this end.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonNode() {
        return Objects.equals(this.node, "") ? null : this.node;
    }

    @JsonProperty("label")
    @JsonPropertyDescription("Label of the node at this end.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String jsonLabel() {
        return Objects.equals(this.label, "") ? null : this.label;
    }

    @JsonProperty("count")
    @JsonPropertyDescription("Exact number of relationships per node at this end. Null when unconstrained.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer jsonCount() {
        return Objects.equals(this.count, null) ? null : this.count;
    }

    @JsonProperty("min_count")
    @JsonPropertyDescription("Minimum number of relationships per node at this end.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer jsonMinCount() {
        return Objects.equals(this.minCount, null) ? null : this.minCount;
    }

    @JsonProperty("max_count")
    @JsonPropertyDescription("Maximum number of relationships per node at this end.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer jsonMaxCount() {
        return Objects.equals(this.maxCount, null) ? null : this.maxCount;
    }

    @JsonCreator
    private static RelationshipTarget fromJson(
            @JsonProperty("node") String node,
            @JsonProperty("label") String label,
            @JsonProperty("count") Integer count,
            @JsonProperty("min_count") Integer minCount,
            @JsonProperty("max_count") Integer maxCount) {
        return new RelationshipTarget(
                node != null ? node : "",
                label != null ? label : "",
                count,
                minCount,
                maxCount);
    }

    public static final class Builder {
        private String node = "";
        private String label = "";
        private Integer count = null;
        private Integer minCount = null;
        private Integer maxCount = null;

        private Builder() {}

        public Builder node(String node) {
            this.node = node;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder count(Integer count) {
            this.count = count;
            return this;
        }

        public Builder minCount(Integer minCount) {
            this.minCount = minCount;
            return this;
        }

        public Builder maxCount(Integer maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public RelationshipTarget build() {
            return new RelationshipTarget(node, label, count, minCount, maxCount);
        }
    }
}
