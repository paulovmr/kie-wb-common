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

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.kie.workbench.common.screens.library.api.ProjectInfo;
import org.kie.workbench.common.screens.library.client.events.AssetDetailEvent;
import org.kie.workbench.common.screens.library.client.events.ProjectDetailEvent;
import org.kie.workbench.common.screens.library.client.resources.i18n.LibraryConstants;
import org.kie.workbench.common.screens.library.client.util.LibraryPlaces;
import org.kie.workbench.common.widgets.client.handlers.NewResourceHandler;
import org.kie.workbench.common.widgets.client.handlers.NewResourcePresenter;
import org.kie.workbench.common.widgets.client.handlers.NewResourceSuccessEvent;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.mvp.UberElement;
import org.uberfire.mvp.impl.DefaultPlaceRequest;

@WorkbenchScreen( identifier = LibraryPlaces.EMPTY_PROJECT_SCREEN )
public class EmptyProjectScreen {

    public interface View extends UberElement<EmptyProjectScreen> {

        void setProjectName( String projectName );

        void addResourceHandler( NewResourceHandler newResourceHandler );
    }

    private View view;

    private TranslationService ts;

    private ManagedInstance<NewResourceHandler> newResourceHandlers;

    private NewResourcePresenter newResourcePresenter;

    private Event<ProjectContextChangeEvent> projectContextChangeEvent;

    private Event<AssetDetailEvent> assetDetailEvent;

    private PlaceManager placeManager;

    private ProjectInfo projectInfo;

    private NewResourceHandler uploadHandler;

    @Inject
    public EmptyProjectScreen( final View view,
                               final TranslationService ts,
                               final ManagedInstance<NewResourceHandler> newResourceHandlers,
                               final NewResourcePresenter newResourcePresenter,
                               final Event<ProjectContextChangeEvent> projectContextChangeEvent,
                               final Event<AssetDetailEvent> assetDetailEvent,
                               final PlaceManager placeManager ) {
        this.view = view;
        this.ts = ts;
        this.newResourceHandlers = newResourceHandlers;
        this.newResourcePresenter = newResourcePresenter;
        this.projectContextChangeEvent = projectContextChangeEvent;
        this.assetDetailEvent = assetDetailEvent;
        this.placeManager = placeManager;
    }

    public void onStartup( @Observes final ProjectDetailEvent projectDetailEvent ) {
        this.projectInfo = projectDetailEvent.getProjectInfo();

        projectContextChangeEvent.fire( new ProjectContextChangeEvent( projectDetailEvent.getProjectInfo().getOrganizationalUnit(),
                                                                       projectDetailEvent.getProjectInfo().getRepository(),
                                                                       projectDetailEvent.getProjectInfo().getBranch(),
                                                                       projectDetailEvent.getProjectInfo().getProject() ) );

        for ( NewResourceHandler newResourceHandler : newResourceHandlers ) {
            if ( newResourceHandler.canCreate() ) {
                if ( isUploadHandler( newResourceHandler ) ) {
                    uploadHandler = newResourceHandler;
                } else if ( !isPackageHandler( newResourceHandler )
                        && !isProjectHandler( newResourceHandler ) ) {
                    view.addResourceHandler( newResourceHandler );
                }
            }
        }

        view.setProjectName( projectInfo.getProject().getProjectName() );
    }

    public void newResourceCreated( @Observes final NewResourceSuccessEvent newResourceSuccessEvent ) {
        placeManager.goTo( LibraryPlaces.ASSET_PERSPECTIVE );
        assetDetailEvent.fire( new AssetDetailEvent( projectInfo, newResourceSuccessEvent.getPath() ) );
    }

    public NewResourcePresenter getNewResourcePresenter() {
        return newResourcePresenter;
    }

    private boolean isProjectHandler( final NewResourceHandler handler ) {
        return handler.getClass().getName().contains( "NewProjectHandler" );
    }

    private boolean isPackageHandler( final NewResourceHandler handler ) {
        return handler.getClass().getName().contains( "NewPackageHandler" );
    }

    private boolean isUploadHandler( final NewResourceHandler handler ) {
        return handler.getClass().getName().contains( "NewFileUploader" );
    }

    public NewResourceHandler getUploadHandler() {
        return uploadHandler;
    }

    public void goToSettings() {
        placeManager.goTo( LibraryPlaces.ASSET_PERSPECTIVE );
        assetDetailEvent.fire( new AssetDetailEvent( projectInfo, null ) );
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return ts.getTranslation( LibraryConstants.EmptyProjectScreen_Title );
    }

    @WorkbenchPartView
    public UberElement<EmptyProjectScreen> getView() {
        return view;
    }
}
