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

import javax.inject.Inject;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.IsWidget;
import org.jboss.errai.common.client.dom.DOMUtil;
import org.jboss.errai.common.client.dom.Div;
import org.jboss.errai.common.client.dom.Form;
import org.jboss.errai.common.client.dom.Input;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.jboss.errai.ui.client.local.api.IsElement;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.SinkNative;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.kie.workbench.common.screens.library.client.resources.i18n.LibraryConstants;
import org.kie.workbench.common.screens.library.client.util.InfoPopup;
import org.kie.workbench.common.screens.library.client.widgets.AssetItemWidget;
import org.kie.workbench.common.screens.library.client.widgets.ProjectActionsWidget;
import org.uberfire.mvp.Command;

@Templated
public class ProjectView implements ProjectScreen.View, IsElement {

    private ProjectScreen presenter;

    @Inject
    private ProjectsDetailScreen projectsDetailScreen;

    @Inject
    private ProjectActionsWidget projectActionsWidget;

    @Inject
    @DataField
    Form toolbar;

    @Inject
    @DataField("details-container")
    Div detailsContainer;

    @Inject
    @DataField
    Div assetList;

    @Inject
    @DataField
    Input filterText;

    @Inject
    @DataField("project-name")
    Div projectNameContainer;

    @Inject
    ManagedInstance<AssetItemWidget> itemWidgetsInstances;

    @Inject
    TranslationService ts;

    @Override
    public void init( ProjectScreen presenter ) {
        this.presenter = presenter;
        filterText.setAttribute( "placeholder", ts.getTranslation( LibraryConstants.LibraryView_Filter ) );
        detailsContainer.appendChild( projectsDetailScreen.getView().getElement() );
        toolbar.appendChild( projectActionsWidget.getView().getElement() );
    }

    @Override
    public void setProjectName( final String projectName ) {
        projectNameContainer.setTextContent( projectName );
    }

    @Override
    public void clearAssets() {
        DOMUtil.removeAllChildren( assetList );
    }

    @Override
    public void addAsset( String assetName, String assetType, IsWidget assetIcon, Command details, Command select ) {
        AssetItemWidget assetItemWidget = itemWidgetsInstances.get();
        assetItemWidget.init( assetName, assetType, assetIcon, details, select );
        assetList.appendChild( assetItemWidget.getElement() );
    }

    @Override
    public void noRightsPopup() {
        InfoPopup.generate( ts.getTranslation( LibraryConstants.Error_NoAccessRights ) );
    }

    @SinkNative( Event.ONKEYUP )
    @EventHandler( "filterText" )
    public void filterTextChange( Event e ) {
        presenter.updateAssetsBy( filterText.getValue() );
    }
}