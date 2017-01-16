/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.workbench.common.screens.library.client.perspective;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.workbench.common.screens.library.client.monitor.LibraryMonitor;
import org.kie.workbench.common.screens.library.client.util.LibraryPlaces;
import org.uberfire.client.annotations.Perspective;
import org.uberfire.client.annotations.WorkbenchPerspective;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.workbench.panels.impl.StaticWorkbenchPanelPresenter;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.ConditionalPlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;
import org.uberfire.workbench.model.PerspectiveDefinition;
import org.uberfire.workbench.model.impl.PartDefinitionImpl;
import org.uberfire.workbench.model.impl.PerspectiveDefinitionImpl;

@ApplicationScoped
@WorkbenchPerspective(identifier = "LibraryPerspective")
public class LibraryPerspective {

    private PlaceManager placeManager;

    private LibraryMonitor libraryMonitor;

    public LibraryPerspective() {
    }

    @Inject
    public LibraryPerspective( final PlaceManager placeManager,
                               final LibraryMonitor libraryMonitor ) {
        this.placeManager = placeManager;
        this.libraryMonitor = libraryMonitor;
    }

    @Perspective
    public PerspectiveDefinition buildPerspective() {
        final PerspectiveDefinition perspectiveDefinition = new PerspectiveDefinitionImpl( StaticWorkbenchPanelPresenter.class.getName() );
        perspectiveDefinition.setName( "Library Perspective" );
        perspectiveDefinition.getRoot().addPart( new PartDefinitionImpl( getLibraryPlaceRequest() ) );

        return perspectiveDefinition;
    }

    PlaceRequest getLibraryPlaceRequest() {
        final DefaultPlaceRequest emptyLibraryPlaceRequest = new DefaultPlaceRequest( LibraryPlaces.EMPTY_LIBRARY_SCREEN );
        return new ConditionalPlaceRequest( LibraryPlaces.LIBRARY_SCREEN ).when( p -> libraryMonitor.thereIsAtLeastOneProjectAccessible() ).orElse( emptyLibraryPlaceRequest );
    }
}
