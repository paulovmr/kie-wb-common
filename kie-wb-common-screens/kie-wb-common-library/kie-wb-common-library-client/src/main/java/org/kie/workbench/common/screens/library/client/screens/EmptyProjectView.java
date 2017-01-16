/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.screens.library.client.screens;

import javax.inject.Inject;

import com.google.gwt.event.dom.client.ClickEvent;
import org.jboss.errai.common.client.dom.Anchor;
import org.jboss.errai.common.client.dom.Div;
import org.jboss.errai.common.client.dom.Form;
import org.jboss.errai.common.client.dom.Paragraph;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.jboss.errai.ui.client.local.api.IsElement;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.kie.workbench.common.screens.library.client.widgets.ProjectActionsWidget;
import org.kie.workbench.common.screens.library.client.widgets.ResourceHandlerWidget;
import org.kie.workbench.common.widgets.client.handlers.NewResourceHandler;

@Templated
public class EmptyProjectView implements EmptyProjectScreen.View,
                                         IsElement {

    private EmptyProjectScreen presenter;

    @Inject
    private ManagedInstance<ResourceHandlerWidget> resourceHandlerWidgets;

    @Inject
    private ProjectsDetailScreen projectsDetailScreen;

    @Inject
    private ProjectActionsWidget projectActionsWidget;

    @Inject
    @DataField("toolbar")
    Form toolbar;

    @Inject
    @DataField("details-container")
    Div detailsContainer;

    @Inject
    @DataField("resource-handler-container")
    Div resourceHandlerContainer;

    @Inject
    @DataField("browse-more-types")
    Anchor browseMoreTypes;

    @Inject
    @DataField("uploader")
    Anchor uploader;

    @Inject
    @DataField("uploader-container")
    Paragraph uploaderContainer;

    @Inject
    @DataField("project-name")
    Div projectNameContainer;

    @Inject
    @DataField("main-container")
    Div mainContainer;

    @Override
    public void init( final EmptyProjectScreen presenter ) {
        this.presenter = presenter;
        resourceHandlerContainer.setTextContent( "" );
        detailsContainer.appendChild( projectsDetailScreen.getView().getElement() );
        projectActionsWidget.init( () -> presenter.goToSettings() );
        toolbar.appendChild( projectActionsWidget.getView().getElement() );
    }

    @Override
    public void setProjectName( final String projectName ) {
        projectNameContainer.setTextContent( projectName );
    }

    @Override
    public void addResourceHandler( final NewResourceHandler newResourceHandler ) {
        final ResourceHandlerWidget resourceHandlerWidget = resourceHandlerWidgets.get();
        resourceHandlerWidget.init( newResourceHandler.getDescription(),
                                    newResourceHandler.getIcon(),
                                    newResourceHandler.getCommand( presenter.getNewResourcePresenter() ) );
        resourceHandlerContainer.appendChild( resourceHandlerWidget.getElement() );
    }

    @EventHandler("browse-more-types")
    public void browseMoreTypes( final ClickEvent clickEvent ) {
        resourceHandlerContainer.getClassList().remove( "retracted" );
        browseMoreTypes.setHidden( true );
    }

    @EventHandler("uploader")
    public void upload( final ClickEvent clickEvent ) {
        presenter.getUploadHandler().getCommand( presenter.getNewResourcePresenter() ).execute();
    }
}
