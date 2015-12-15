/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.model.declaration;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InferedTypeTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Iterator;

public class VariableTreeImpl extends JavaTree implements VariableTree {
  private ModifiersTree modifiers;
  private TypeTree type;
  private IdentifierTree simpleName;
  @Nullable
  private SyntaxToken equalToken;
  @Nullable
  private ExpressionTree initializer;
  @Nullable
  private SyntaxToken endToken;

  // FIXME(Godin): never should be null, i.e. should have default value
  private JavaSymbol.VariableJavaSymbol symbol;

  // Syntax tree holders
  @Nullable
  private ArrayTypeTreeImpl nestedDimensions;
  private boolean vararg = false;

  public VariableTreeImpl(IdentifierTreeImpl simpleName, @Nullable ArrayTypeTreeImpl nestedDimensions) {
    super(Kind.VARIABLE);

    this.modifiers = ModifiersTreeImpl.emptyModifiers();
    this.simpleName = simpleName;
    this.nestedDimensions = nestedDimensions;
    this.initializer = null;
  }

  public VariableTreeImpl(InternalSyntaxToken equalToken, ExpressionTree initializer) {
    super(Kind.VARIABLE);
    this.equalToken = equalToken;
    this.initializer = initializer;
  }

  public VariableTreeImpl(IdentifierTreeImpl simpleName) {
    this(simpleName, null);
    this.type = new InferedTypeTree();
  }

  public VariableTreeImpl(Kind kind, ModifiersTree modifiers, IdentifierTree simpleName, @Nullable ExpressionTree initializer) {
    super(kind);
    this.modifiers = Preconditions.checkNotNull(modifiers);
    this.simpleName = Preconditions.checkNotNull(simpleName);
    this.initializer = initializer;
  }

  public VariableTreeImpl completeType(TypeTree type) {
    TypeTree actualType = type;

    if (nestedDimensions != null) {
      nestedDimensions.setLastChildType(type);
      actualType = nestedDimensions;
    }

    this.type = actualType;

    return this;
  }

  public VariableTreeImpl completeModifiers(ModifiersTreeImpl modifiers) {
    this.modifiers = modifiers;

    return this;
  }

  public VariableTreeImpl completeModifiersAndType(ModifiersTreeImpl modifiers, TypeTree type) {
    return completeModifiers(modifiers).
      completeType(type);
  }

  public VariableTreeImpl completeTypeAndInitializer(TypeTree type, InternalSyntaxToken equalToken, ExpressionTree initializer) {
    this.initializer = initializer;
    this.equalToken = equalToken;

    return completeType(type);
  }

  public VariableTreeImpl completeIdentifierAndDims(IdentifierTreeImpl simpleName, ArrayTypeTreeImpl nestedDimensions) {
    this.simpleName = simpleName;
    if (this.nestedDimensions != null) {
      ArrayTypeTreeImpl newType = nestedDimensions;
      newType.completeType(this.nestedDimensions);
      this.nestedDimensions = newType;
    } else {
      this.nestedDimensions = nestedDimensions;
    }

    return this;
  }

  public void addEllipsisDimension(ArrayTypeTreeImpl dimension) {
    vararg = true;
    if (nestedDimensions != null) {
      nestedDimensions.setLastChildType(dimension);
    } else {
      nestedDimensions = dimension;
    }
  }

  public boolean isVararg() {
    return vararg;
  }

  @Override
  public Kind kind() {
    return Kind.VARIABLE;
  }

  @Override
  public ModifiersTree modifiers() {
    return modifiers;
  }

  @Override
  public TypeTree type() {
    return type;
  }

  @Override
  public IdentifierTree simpleName() {
    return simpleName;
  }

  @Nullable
  @Override
  public ExpressionTree initializer() {
    return initializer;
  }

  @Override
  public org.sonar.plugins.java.api.semantic.Symbol symbol() {
    return symbol;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitVariable(this);
  }

  public JavaSymbol.VariableJavaSymbol getSymbol() {
    return symbol;
  }

  public void setSymbol(JavaSymbol.VariableJavaSymbol symbol) {
    Preconditions.checkState(this.symbol == null);
    this.symbol = symbol;
  }

  @Override
  public int getLine() {
    return ((IdentifierTreeImpl) simpleName()).getLine();
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    Iterator<Tree> initializerIterator = initializer != null ? Iterators.forArray(equalToken, initializer) : Iterators.<Tree>emptyIterator();
    Iterator<Tree> endTokenIterator = endToken != null ? Iterators.<Tree>singletonIterator(endToken) : Iterators.<Tree>emptyIterator();
    return Iterators.concat(
      Iterators.forArray(modifiers, type, simpleName),
      initializerIterator,
      endTokenIterator
      );
  }

  @CheckForNull
  @Override
  public SyntaxToken endToken() {
    return endToken;
  }

  public void setEndToken(InternalSyntaxToken endToken) {
    this.endToken = endToken;
  }
}
