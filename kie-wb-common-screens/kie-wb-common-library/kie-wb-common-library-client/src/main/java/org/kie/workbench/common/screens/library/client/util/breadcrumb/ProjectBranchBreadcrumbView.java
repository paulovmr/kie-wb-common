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

import java.util.List;

import javax.inject.Inject;

import org.guvnor.structure.repositories.Branch;
import org.jboss.errai.ui.client.local.api.elemental2.IsElement;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.kie.workbench.common.screens.library.client.settings.util.dropdown.KieDropdownElement;
import org.kie.workbench.common.screens.library.client.settings.util.dropdown.KieDropdownElement.Item;
import org.uberfire.client.mvp.UberElemental;

import static java.util.stream.Collectors.toList;
import static org.kie.workbench.common.screens.library.client.settings.util.dropdown.KieDropdownElement.Item.Status.CHECKED;
import static org.kie.workbench.common.screens.library.client.settings.util.dropdown.KieDropdownElement.Item.Status.UNCHECKED;

@Templated
public class ProjectBranchBreadcrumbView implements UberElemental<ProjectBranchBreadcrumb>,
                                                    IsElement {

    @Inject
    @DataField("branches-dropdown")
    private KieDropdownElement<Branch> branchesDropdown;

    @Override
    public void init(final ProjectBranchBreadcrumb presenter) {

        final Branch currentBranch = presenter.getProject().getBranch();

        final List<Item<Branch>> items = presenter.getProject().getRepository().getBranches().stream()
                .map(b -> new Item<>(b, b.getName(), b.equals(currentBranch) ? CHECKED : UNCHECKED))
                .collect(toList());

        branchesDropdown.setup(items,
                               currentBranch.getName(),
                               presenter::onBranchChanged);
    }
}
