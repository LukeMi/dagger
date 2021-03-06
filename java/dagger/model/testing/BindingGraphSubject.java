/*
 * Copyright (C) 2018 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dagger.model.testing;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.truth.Truth.assertAbout;
import static dagger.internal.codegen.DaggerStreams.toImmutableSet;

import com.google.common.collect.ImmutableSet;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import dagger.model.BindingGraph;
import dagger.model.BindingGraph.BindingNode;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

/** A Truth subject for making assertions on a {@link BindingGraph}. */
public final class BindingGraphSubject extends Subject<BindingGraphSubject, BindingGraph> {

  /** Starts a fluent assertion about a {@link BindingGraph}. */
  public static BindingGraphSubject assertThat(BindingGraph bindingGraph) {
    return assertAbout(BindingGraphSubject::new).that(bindingGraph);
  }

  private BindingGraphSubject(FailureMetadata metadata, @NullableDecl BindingGraph actual) {
    super(metadata, actual);
  }

  /**
   * Asserts that the graph has at least one binding with an unqualified key.
   *
   * @param type the canonical name of the type, as returned by {@link TypeMirror#toString()}
   */
  public void hasBindingWithKey(String type) {
    bindingWithKey(type);
  }

  /**
   * Asserts that the graph has at least one binding with a qualified key.
   *
   * @param qualifier the canonical string form of the qualifier, as returned by {@link
   *     javax.lang.model.element.AnnotationMirror AnnotationMirror.toString()}
   * @param type the canonical name of the type, as returned by {@link TypeMirror#toString()}
   */
  public void hasBindingWithKey(String qualifier, String type) {
    bindingWithKey(qualifier, type);
  }

  /**
   * Returns a subject for testing the binding for an unqualified key.
   *
   * @param type the canonical name of the type, as returned by {@link TypeMirror#toString()}
   */
  public BindingNodeSubject bindingWithKey(String type) {
    return bindingWithKeyString(keyString(type));
  }

  /**
   * Returns a subject for testing the binding for a qualified key.
   *
   * @param qualifier the canonical string form of the qualifier, as returned by {@link
   *     javax.lang.model.element.AnnotationMirror AnnotationMirror.toString()}
   * @param type the canonical name of the type, as returned by {@link TypeMirror#toString()}
   */
  public BindingNodeSubject bindingWithKey(String qualifier, String type) {
    return bindingWithKeyString(keyString(qualifier, type));
  }

  private BindingNodeSubject bindingWithKeyString(String keyString) {
    ImmutableSet<BindingNode> bindingNodes = getBindingNodes(keyString);
    if (bindingNodes.isEmpty()) {
      fail("has binding with key", keyString);
    }
    // TODO(dpb): Handle multiple bindings for the same key.
    if (bindingNodes.size() > 1) {
      failWithBadResults(
          "has only one binding with key", keyString, "has the following bindings:", bindingNodes);
    }
    return check("bindingWithKey(%s)", keyString)
        .about(BindingNodeSubject::new)
        .that(getOnlyElement(bindingNodes));
  }

  private ImmutableSet<BindingNode> getBindingNodes(String keyString) {
    return actual()
        .bindingNodes()
        .stream()
        .filter(node -> node.binding().key().toString().equals(keyString))
        .collect(toImmutableSet());
  }

  private static String keyString(String type) {
    return type;
  }

  private static String keyString(String qualifier, String type) {
    return String.format("%s %s", qualifier, type);
  }

  /** A Truth subject for a {@link BindingNode}. */
  public final class BindingNodeSubject extends Subject<BindingNodeSubject, BindingNode> {

    BindingNodeSubject(FailureMetadata metadata, @NullableDecl BindingNode actual) {
      super(metadata, actual);
    }

    /**
     * Asserts that the binding node depends on a binding with an unqualified key.
     *
     * @param type the canonical name of the type, as returned by {@link TypeMirror#toString()}
     */
    public void dependsOnBindingWithKey(String type) {
      dependsOnBindingWithKeyString(keyString(type));
    }

    /**
     * Asserts that the binding node depends on a binding with a qualified key.
     *
     * @param qualifier the canonical string form of the qualifier, as returned by {@link
     *     javax.lang.model.element.AnnotationMirror AnnotationMirror.toString()}
     * @param type the canonical name of the type, as returned by {@link TypeMirror#toString()}
     */
    public void dependsOnBindingWithKey(String qualifier, String type) {
      dependsOnBindingWithKeyString(keyString(qualifier, type));
    }

    private void dependsOnBindingWithKeyString(String keyString) {
      if (actualBindingGraph()
          .successors(actual())
          .stream()
          .filter(node -> node instanceof BindingNode)
          .map(node -> (BindingNode) node)
          .noneMatch(node -> node.binding().key().toString().equals(keyString))) {
        fail("has successor with key", keyString);
      }
    }

    private BindingGraph actualBindingGraph() {
      return BindingGraphSubject.this.actual();
    }
  }
}
