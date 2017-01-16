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

import javax.enterprise.event.Event;

import org.guvnor.common.services.project.model.Project;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.guvnor.structure.repositories.Repository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.screens.library.api.ProjectInfo;
import org.kie.workbench.common.screens.library.client.events.AssetDetailEvent;
import org.kie.workbench.common.screens.library.client.events.ProjectDetailEvent;
import org.kie.workbench.common.screens.library.client.perspective.AssetPerspective;
import org.kie.workbench.common.screens.library.client.util.LibraryBreadcrumbs;
import org.kie.workbench.common.screens.library.client.util.LibraryPlaces;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.ext.editor.commons.client.event.ConcurrentDeleteAcceptedEvent;
import org.uberfire.ext.editor.commons.client.event.ConcurrentRenameAcceptedEvent;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.impl.PathPlaceRequest;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AssetPerspectiveTest {

    @Mock
    private PlaceManager placeManager;

    @Mock
    private LibraryBreadcrumbs libraryBreadcrumbs;

    @Mock
    private Event<ProjectDetailEvent> projectDetailEvent;

    @Mock
    private Event<AssetDetailEvent> assetDetailEvent;

    private AssetPerspective assetPerspective;

    @Before
    public void setup() {
        assetPerspective = spy( new AssetPerspective( placeManager,
                                                      libraryBreadcrumbs,
                                                      projectDetailEvent,
                                                      assetDetailEvent ) );
    }

    @Test
    public void assetSelectedTest() {
        final ProjectInfo projectInfo = createProjectInfo();
        final ObservablePath assetPath = createPath();
        final PathPlaceRequest pathPlaceRequest = createPathPlaceRequest( assetPath );
        doReturn( pathPlaceRequest ).when( assetPerspective ).generatePlaceRequest( assetPath );

        assetPerspective.assetSelected( new AssetDetailEvent( projectInfo, assetPath ) );

        verify( assetPath ).onDelete( any( Command.class ) );
        verify( assetPath ).onRename( any( Command.class ) );
        verify( placeManager ).goTo( pathPlaceRequest );
        verify( libraryBreadcrumbs ).setupLibraryBreadCrumbsForAsset( projectInfo,
                                                                      assetPath );
    }

    @Test
    public void selectedAssetDeleteAcceptedTest() {
        assetPerspective.projectInfo = createProjectInfo();
        ObservablePath deletedAssetPath = createPath();
        assetPerspective.path = deletedAssetPath;

        assetPerspective.assetDeletedAccepted( new ConcurrentDeleteAcceptedEvent( deletedAssetPath ) );

        verify( placeManager ).goTo( LibraryPlaces.PROJECT_SCREEN );
        verify( projectDetailEvent ).fire( new ProjectDetailEvent( assetPerspective.projectInfo ) );
    }

    @Test
    public void anotherAssetDeleteAcceptedTest() {
        assetPerspective.projectInfo = createProjectInfo();
        ObservablePath deletedAssetPath = createPath();
        ObservablePath anotherAssetPath = createPath();
        assetPerspective.path = anotherAssetPath;

        assetPerspective.assetDeletedAccepted( new ConcurrentDeleteAcceptedEvent( deletedAssetPath ) );

        verify( placeManager, never() ).goTo( anyString() );
        verify( projectDetailEvent, never() ).fire( any( ProjectDetailEvent.class ) );
    }

    @Test
    public void selectedAssetRenameAcceptedTest() {
        assetPerspective.projectInfo = createProjectInfo();
        ObservablePath renamedAssetPath = createPath();
        assetPerspective.path = renamedAssetPath;

        assetPerspective.assetRenamedAccepted( new ConcurrentRenameAcceptedEvent( renamedAssetPath ) );

        verify( libraryBreadcrumbs ).setupLibraryBreadCrumbsForAsset( assetPerspective.projectInfo,
                                                                      assetPerspective.path );
    }

    @Test
    public void anotherAssetRenameAcceptedTest() {
        assetPerspective.projectInfo = createProjectInfo();
        ObservablePath renamedAssetPath = createPath();
        ObservablePath anotherAssetPath = createPath();
        assetPerspective.path = anotherAssetPath;

        assetPerspective.assetRenamedAccepted( new ConcurrentRenameAcceptedEvent( renamedAssetPath ) );

        verify( libraryBreadcrumbs, never() ).setupLibraryBreadCrumbsForAsset( assetPerspective.projectInfo,
                                                                               assetPerspective.path );
    }

    private PathPlaceRequest createPathPlaceRequest( final ObservablePath assetPath ) {
        final PathPlaceRequest pathPlaceRequest = mock( PathPlaceRequest.class );
        doReturn( assetPath ).when( pathPlaceRequest ).getPath();
        return pathPlaceRequest;
    }

    private ObservablePath createPath() {
        return mock( ObservablePath.class );
    }

    private ProjectInfo createProjectInfo() {
        final Project project = mock( Project.class );
        doReturn( "projectName" ).when( project ).getProjectName();
        doReturn( "projectPath" ).when( project ).getIdentifier();

        final OrganizationalUnit organizationalUnit = mock( OrganizationalUnit.class );
        final Repository repository = mock( Repository.class );
        final String branch = "master";
        return new ProjectInfo( organizationalUnit,
                                repository,
                                branch,
                                project );
    }
}
