/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.dmn.client.editors.expressions;

import java.util.Objects;
import java.util.Optional;

import org.jboss.errai.common.client.dom.HTMLElement;
import org.kie.workbench.common.dmn.api.definition.HasExpression;
import org.kie.workbench.common.dmn.api.definition.HasName;
import org.kie.workbench.common.dmn.client.decision.DecisionNavigatorPresenter;
import org.kie.workbench.common.dmn.client.editors.toolbar.ToolbarStateHandler;
import org.kie.workbench.common.dmn.client.session.DMNSession;
import org.kie.workbench.common.stunner.core.client.canvas.event.registration.CanvasElementUpdatedEvent;
import org.kie.workbench.common.stunner.core.graph.Element;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.definition.Definition;
import org.uberfire.mvp.Command;

public class ExpressionEditor implements ExpressionEditorView.Presenter {

    private ExpressionEditorView view;

    private Optional<Command> exitCommand = Optional.empty();

    private Optional<ToolbarStateHandler> toolbarStateHandler;

    private Optional<HasExpression> hasExpression = Optional.empty();

    private DecisionNavigatorPresenter decisionNavigator;

    @SuppressWarnings("unchecked")
    public ExpressionEditor(final ExpressionEditorView view,
                            final DecisionNavigatorPresenter decisionNavigator) {
        this.view = view;
        this.decisionNavigator = decisionNavigator;
        this.view.init(this);
    }

    @Override
    public HTMLElement getElement() {
        return view.getElement();
    }

    @Override
    public void bind(final DMNSession session) {
        view.bind(session);
    }

    @Override
    public ExpressionEditorView getView() {
        return view;
    }

    @Override
    public void setToolbarStateHandler(final ToolbarStateHandler toolbarStateHandler) {
        this.toolbarStateHandler = Optional.ofNullable(toolbarStateHandler);
    }

    @Override
    public void setExpression(final String nodeUUID,
                              final HasExpression hasExpression,
                              final Optional<HasName> hasName) {
        this.hasExpression = Optional.ofNullable(hasExpression);
        view.setExpression(nodeUUID,
                           hasExpression,
                           hasName);

        toolbarStateHandler.ifPresent(ToolbarStateHandler::enterGridView);
    }

    @Override
    public void setExitCommand(final Command exitCommand) {
        this.exitCommand = Optional.ofNullable(exitCommand);
    }

    @Override
    public void exit() {
        exitCommand.ifPresent(command -> {
            decisionNavigator.clearSelections();
            toolbarStateHandler.ifPresent(ToolbarStateHandler::enterGraphView);
            command.execute();
            exitCommand = Optional.empty();
        });
    }

    @Override
    public void handleCanvasElementUpdated(final CanvasElementUpdatedEvent event) {
        final Element<?> element = event.getElement();
        if ((element instanceof Node)) {
            if (element.getContent() instanceof Definition) {
                final Definition definition = (Definition) element.getContent();
                hasExpression.ifPresent(e -> {
                    if (Objects.equals(e, definition.getDefinition())) {
                        view.setReturnToDRGText(Optional.ofNullable((HasName) definition.getDefinition()));
                    }
                });
            }
        }
    }

    Optional<Command> getExitCommand() {
        return exitCommand;
    }
}
