/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.screens.library.client.util.breadcrumb;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import elemental2.dom.DomGlobal;
import org.guvnor.common.services.project.model.WorkspaceProject;
import org.guvnor.structure.repositories.Branch;
import org.uberfire.client.mvp.UberElemental;
import org.uberfire.ext.widgets.common.client.breadcrumbs.widget.BreadcrumbPresenter;

@Dependent
public class ProjectBranchBreadcrumb implements BreadcrumbPresenter {

    private final ProjectBranchBreadcrumbView view;

    private WorkspaceProject project;

    @Inject
    public ProjectBranchBreadcrumb(final ProjectBranchBreadcrumbView view) {
        this.view = view;
    }

    public ProjectBranchBreadcrumb setup(final WorkspaceProject project) {
        this.project = project;
        view.init(this);
        return this;
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    public WorkspaceProject getProject() {
        return project;
    }

    @Override
    public UberElemental<? extends BreadcrumbPresenter> getView() {
        return view;
    }

    public void onBranchChanged(final Branch branch) {
        DomGlobal.console.info("Branch changed to: " + branch.getName());
    }
}
