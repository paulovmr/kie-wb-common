/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.enterprise.event.Event;

import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.guvnor.common.services.project.model.Project;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.guvnor.structure.repositories.Repository;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.screens.library.api.LibraryContextSwitchEvent;
import org.kie.workbench.common.screens.library.api.LibraryInfo;
import org.kie.workbench.common.screens.library.api.LibraryService;
import org.kie.workbench.common.screens.library.client.events.ProjectDetailEvent;
import org.kie.workbench.common.screens.library.client.util.LibraryBreadcrumbs;
import org.kie.workbench.common.screens.library.client.util.LibraryPlaces;
import org.kie.workbench.common.screens.library.client.widgets.LibraryBreadCrumbToolbarPresenter;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.mocks.CallerMock;
import org.uberfire.mvp.Command;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.security.authz.AuthorizationManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LibraryScreenTest {

    @Mock
    private LibraryScreen.View view;

    @Mock
    private LibraryService libraryService;
    private Caller<LibraryService> libraryServiceCaller;

    @Mock
    private PlaceManager placeManager;

    @Mock
    private OrganizationalUnit defaultOU1;

    @Mock
    private OrganizationalUnit defaultOU2;

    @Mock
    private Repository defaultRepositoryOU2;

    @Mock
    private LibraryBreadcrumbs libraryBreadcrumbs;

    @Mock
    private LibraryBreadCrumbToolbarPresenter breadCrumbToolbarPresenter;

    @Mock
    private SessionInfo sessionInfo;

    @Mock
    private AuthorizationManager authorizationManager;

    @Mock
    private TranslationService ts;

    @Mock
    private Event<LibraryContextSwitchEvent> libraryContextSwitchEventEvent;

    @Mock
    private Event<ProjectDetailEvent> projectDetailEventEvent;

    @Mock
    private Event<ProjectContextChangeEvent> projectContextChangeEventEvent;

    private LibraryScreen libraryScreen;

    private String ouAlias;
    private Project proj1 = mock( Project.class );
    private Project proj2 = mock( Project.class );
    private Project proj3 = mock( Project.class );

    @Before
    public void setup() {
        libraryServiceCaller = new CallerMock<>( libraryService );
        when( libraryService.getDefaultLibraryInfo() ).thenReturn( getDefaultLibraryMock() );
        when( proj1.getProjectName() ).thenReturn( "a" );
        when( proj2.getProjectName() ).thenReturn( "b" );
        when( proj3.getProjectName() ).thenReturn( "c" );

        libraryScreen = spy( new LibraryScreen( view,
                                                breadCrumbToolbarPresenter,
                                                placeManager,
                                                libraryBreadcrumbs,
                                                libraryContextSwitchEventEvent,
                                                sessionInfo,
                                                authorizationManager,
                                                ts,
                                                projectDetailEventEvent,
                                                projectContextChangeEventEvent,
                                                libraryServiceCaller ) );
    }

    @Test
    public void onStartupLoadLibraryTest() {
        libraryScreen.onStartup();

        verify( view ).clearProjects();
        verify( view, times( getProjects().size() ) ).addProject( any(), any(), any() );
        verify( libraryBreadcrumbs ).setupToolBar( breadCrumbToolbarPresenter );
        verify( breadCrumbToolbarPresenter ).init( any(), any() );
    }

    @Test
    public void filterProjects() {

        libraryScreen.onStartup();

        assertEquals( getDefaultLibraryMock().getProjects().size(), libraryScreen.libraryInfo.getProjects().size() );

        assertEquals( 1, libraryScreen.filterProjects( "a" ).size() );
    }

    @Test
    public void selectCommand() {
        final Command detailsCommand = mock( Command.class );
        doReturn( detailsCommand ).when( libraryScreen ).detailsCommand( any( Project.class ) );

        final Project project = mock( Project.class );
        doReturn( "projectName" ).when( project ).getProjectName();
        doReturn( "projectPath" ).when( project ).getIdentifier();

        libraryScreen.selectCommand( project ).execute();

        verify( placeManager ).goTo( LibraryPlaces.PROJECT_SCREEN );
        verify( projectDetailEventEvent ).fire( any( ProjectDetailEvent.class ) );
        verify( detailsCommand ).execute();
    }

    private LibraryInfo getDefaultLibraryMock() {

        OrganizationalUnit defaultOrganizationUnit = defaultOU1;
        OrganizationalUnit selectedOrganizationUnit = defaultOU2;
        Repository selectedRepository = defaultRepositoryOU2;
        String selectedBranch = "master";

        ouAlias = "alias";

        LibraryInfo libraryInfo = new LibraryInfo( defaultOrganizationUnit,
                                                   selectedOrganizationUnit,
                                                   selectedRepository,
                                                   selectedBranch,
                                                   getProjects(),
                                                   getOus(), ouAlias );
        return libraryInfo;
    }

    private List<OrganizationalUnit> getOus() {
        return Arrays.asList( defaultOU1, defaultOU2 );
    }

    private Set<Project> getProjects() {
        Set<Project> projects = new HashSet<>();
        projects.add( proj1 );
        projects.add( proj2 );
        projects.add( proj3 );
        return projects;
    }
}