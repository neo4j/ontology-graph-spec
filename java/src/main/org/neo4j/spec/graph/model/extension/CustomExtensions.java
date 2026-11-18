// Generated from org/neo4j/spec/graph/model/extension/CustomExtensions.kt by :java:generator. Do not edit.
package org.neo4j.spec.graph.model.extension;

import java.util.Map;

/**
 * Extensions for storing arbitrary data in {@code custom}.
 * Plain values (String, Boolean, Long, Double, List and Map) can be put in {@code custom} directly,
 * helpers used to store and read `@Serializable` objects.
 */
public interface CustomExtensions {
    Map<String, Object> getCustom();
}
