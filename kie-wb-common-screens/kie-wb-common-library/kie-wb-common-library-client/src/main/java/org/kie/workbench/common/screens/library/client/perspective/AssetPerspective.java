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

package org.kie.workbench.common.screens.library.client.perspective;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.guvnor.common.services.project.model.Project;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.guvnor.structure.repositories.Repository;
import org.kie.workbench.common.screens.library.api.ProjectInfo;
import org.kie.workbench.common.screens.library.client.events.AssetDetailEvent;
import org.kie.workbench.common.screens.library.client.events.ProjectDetailEvent;
import org.kie.workbench.common.screens.library.client.util.LibraryBreadcrumbs;
import org.kie.workbench.common.screens.library.client.util.LibraryPlaces;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.annotations.Perspective;
import org.uberfire.client.annotations.WorkbenchPerspective;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.workbench.panels.impl.SimpleWorkbenchPanelPresenter;
import org.uberfire.ext.editor.commons.client.event.ConcurrentDeleteAcceptedEvent;
import org.uberfire.ext.editor.commons.client.event.ConcurrentRenameAcceptedEvent;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.mvp.impl.PathPlaceRequest;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;

@ApplicationScoped
@WorkbenchPerspective( identifier = LibraryPlaces.ASSET_PERSPECTIVE )
public class AssetPerspective {

    private PlaceManager placeManager;

    private LibraryBreadcrumbs libraryBreadcrumbs;

    private Event<ProjectDetailEvent> projectDetailEvent;

    private Event<AssetDetailEvent> assetDetailEvent;

    ObservablePath path;

    ProjectInfo projectInfo;

    @Inject
    public AssetPerspective( final PlaceManager placeManager,
                             final LibraryBreadcrumbs libraryBreadcrumbs,
                             final Event<ProjectDetailEvent> projectDetailEvent,
                             final Event<AssetDetailEvent> assetDetailEvent ) {
        this.placeManager = placeManager;
        this.libraryBreadcrumbs = libraryBreadcrumbs;
        this.projectDetailEvent = projectDetailEvent;
        this.assetDetailEvent = assetDetailEvent;
    }

    public void assetSelected( @Observes final AssetDetailEvent assetDetails ) {
        projectInfo = assetDetails.getProjectInfo();
        final PlaceRequest placeRequest = generatePlaceRequest( assetDetails.getPath() );

        if ( assetDetails.getPath() == null ) {
            path = null;
        } else {
            path = ( (PathPlaceRequest) placeRequest ).getPath();
            path.onRename( () -> updateBreadcrumbs( projectInfo, path ) );
            path.onDelete( this::goBackToProject );
        }

        placeManager.goTo( placeRequest );
        updateBreadcrumbs( projectInfo, path );
    }

    public void assetDeletedAccepted( @Observes final ConcurrentDeleteAcceptedEvent concurrentDeleteAcceptedEvent ) {
        if ( path != null && path.equals( concurrentDeleteAcceptedEvent.getPath() ) ) {
            goBackToProject();
        }
    }

    public void assetRenamedAccepted( @Observes final ConcurrentRenameAcceptedEvent concurrentRenameAcceptedEvent ) {
        if ( path != null && path.equals( concurrentRenameAcceptedEvent.getPath() ) ) {
            updateBreadcrumbs( projectInfo, path );
        }
    }

    private void updateBreadcrumbs( final ProjectInfo projectInfo,
                                    final Path path ) {
        libraryBreadcrumbs.setupLibraryBreadCrumbsForAsset( projectInfo,
                                                            path );
    }

    private void goBackToProject() {
        placeManager.goTo( LibraryPlaces.PROJECT_SCREEN );
        projectDetailEvent.fire( new ProjectDetailEvent( projectInfo ) );
    }

    PlaceRequest generatePlaceRequest( final Path path ) {
        if ( path == null ) {
            return new DefaultPlaceRequest( "projectScreen" );
        }

        return new PathPlaceRequest( path );
    }

    @Perspective
    public PerspectiveDefinition buildPerspective() {
        final PerspectiveDefinition p = new PerspectiveDefinitionImpl( SimpleWorkbenchPanelPresenter.class.getName() );
        p.setName( "Asset Perspective" );

        return p;
    }
}
