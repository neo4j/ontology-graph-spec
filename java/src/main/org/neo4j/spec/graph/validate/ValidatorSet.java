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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.neo4j.spec.graph.model.GraphModel;

/**
 * An immutable, ordered collection of {@link Validator}s with no repeats, such as {@link Validators#core()}. Sets
 * combine with {@link #and}, so built-in and custom validators can be mixed freely. A set is itself a
 * {@link Validator}; nesting one set in another flattens it.
 */
public final class ValidatorSet implements Validator, Iterable<Validator> {

    private static final ValidatorSet EMPTY = new ValidatorSet(List.of());

    private final List<Validator> validators;

    private ValidatorSet(Iterable<? extends Validator> validators) {
        Set<Validator> unique = new LinkedHashSet<>();
        for (Validator validator : validators) {
            Objects.requireNonNull(validator, "validator");
            if (validator instanceof ValidatorSet) {
                unique.addAll(((ValidatorSet) validator).validators);
            } else {
                unique.add(validator);
            }
        }
        this.validators = Collections.unmodifiableList(new ArrayList<>(unique));
    }

    /** A set with no validators. */
    public static ValidatorSet empty() {
        return EMPTY;
    }

    /** A set of these validators, in order, without repeats. */
    public static ValidatorSet of(Validator... validators) {
        return new ValidatorSet(Arrays.asList(validators));
    }

    /** A set of these validators, in iteration order, without repeats. Nested sets are flattened. */
    public static ValidatorSet of(Iterable<? extends Validator> validators) {
        if (validators instanceof ValidatorSet) {
            return (ValidatorSet) validators;
        }
        return new ValidatorSet(validators);
    }

    /** A set of this set's validators followed by those of another set. */
    public ValidatorSet and(ValidatorSet other) {
        List<Validator> combined = new ArrayList<>(validators);
        combined.addAll(other.validators);
        return new ValidatorSet(combined);
    }

    /** A set of this set's validators followed by these. */
    public ValidatorSet and(Validator... others) {
        List<Validator> combined = new ArrayList<>(validators);
        combined.addAll(Arrays.asList(others));
        return new ValidatorSet(combined);
    }

    /** The validators, in order. */
    public List<Validator> validators() {
        return validators;
    }

    /** The number of validators. */
    public int size() {
        return validators.size();
    }

    @Override
    public Iterator<Validator> iterator() {
        return validators.iterator();
    }

    /**
     * Runs the validators and their dependencies against a model. Each runs once, after its dependencies, and is
     * skipped if any of them found issues.
     *
     * @return every issue found, in the order the validators ran, as an unmodifiable list; empty when the model has none
     * @throws IllegalStateException if validators depend on each other in a cycle
     */
    @Override
    public List<Issue> validate(GraphModel model) {
        Objects.requireNonNull(model, "model");
        Set<Validator> ordered = new LinkedHashSet<>();
        Set<Validator> visiting = new LinkedHashSet<>();
        for (Validator validator : validators) {
            order(validator, ordered, visiting);
        }
        Set<Validator> unhealthy = new HashSet<>();
        List<Issue> issues = new ArrayList<>();
        for (Validator validator : ordered) {
            if (validator instanceof ValidatorSet) {
                continue;
            }
            if (dependencies(validator).stream().anyMatch(unhealthy::contains)) {
                unhealthy.add(validator);
                continue;
            }
            List<Issue> found = Objects.requireNonNull(validator.validate(model), "issues of " + validator);
            if (!found.isEmpty()) {
                unhealthy.add(validator);
                issues.addAll(found);
            }
        }
        return Collections.unmodifiableList(issues);
    }

    /** Adds the validator after its dependencies, depth first, so earlier entries never depend on later ones. */
    private static void order(Validator validator, Set<Validator> ordered, Set<Validator> visiting) {
        if (validator instanceof ValidatorSet) {
            for (Validator member : ((ValidatorSet) validator).validators) {
                order(member, ordered, visiting);
            }
            return;
        }
        if (ordered.contains(validator)) {
            return;
        }
        if (!visiting.add(validator)) {
            throw new IllegalStateException("Validators have a dependency cycle: " + cycle(visiting, validator));
        }
        for (Validator dependency : dependencies(validator)) {
            order(dependency, ordered, visiting);
        }
        visiting.remove(validator);
        ordered.add(validator);
    }

    private static String cycle(Set<Validator> visiting, Validator repeated) {
        List<String> names = new ArrayList<>();
        boolean inCycle = false;
        for (Validator validator : visiting) {
            inCycle = inCycle || validator.equals(repeated);
            if (inCycle) {
                names.add(String.valueOf(validator));
            }
        }
        names.add(String.valueOf(repeated));
        return String.join(" -> ", names);
    }

    /** The direct dependencies of a validator, with any nested sets flattened. */
    private static List<Validator> dependencies(Validator validator) {
        List<Validator> result = new ArrayList<>();
        for (Validator dependency : validator.dependsOn()) {
            Objects.requireNonNull(dependency, "dependency of " + validator);
            if (dependency instanceof ValidatorSet) {
                result.addAll(((ValidatorSet) dependency).validators);
            } else {
                result.add(dependency);
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof ValidatorSet && validators.equals(((ValidatorSet) o).validators));
    }

    @Override
    public int hashCode() {
        return validators.hashCode();
    }

    @Override
    public String toString() {
        return "ValidatorSet" + validators;
    }
}
