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
package org.kie.workbench.common.screens.library.api;

import org.guvnor.common.services.project.model.Project;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.guvnor.structure.repositories.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.uberfire.commons.validation.PortablePreconditions.checkNotNull;

public class LibraryInfo {

    private OrganizationalUnit defaultOrganizationUnit;
    private OrganizationalUnit selectedOrganizationUnit;
    private Repository selectedRepository;
    private String selectedBranch;
    private Set<Project> projects = new HashSet<>();
    private Collection<OrganizationalUnit> organizationUnits = new ArrayList<>();
    private String ouAlias;

    public LibraryInfo() {
    }

    public LibraryInfo( final OrganizationalUnit defaultOrganizationUnit,
                        final OrganizationalUnit selectedOrganizationUnit,
                        final Repository selectedRepository,
                        final String selectedBranch,
                        final Set<Project> projects,
                        final Collection<OrganizationalUnit> organizationUnits,
                        final String ouAlias) {
        this.defaultOrganizationUnit = checkNotNull( "defaultOrganizationUnit", defaultOrganizationUnit );
        this.selectedOrganizationUnit = checkNotNull( "selectedOrganizationUnit", selectedOrganizationUnit );
        this.selectedRepository = checkNotNull( "selectedRepository", selectedRepository );
        this.selectedBranch = checkNotNull( "selectedBranch", selectedBranch );
        this.projects = checkNotNull( "projects", projects );
        this.organizationUnits = checkNotNull( "organizationUnits", organizationUnits );
        this.ouAlias = checkNotNull( "ouAlias", ouAlias );
    }

    public OrganizationalUnit getDefaultOrganizationUnit() {
        return defaultOrganizationUnit;
    }

    public Set<Project> getProjects() {
        return projects;
    }

    public Collection<OrganizationalUnit> getOrganizationUnits() {
        return organizationUnits;
    }

    public boolean isFullLibrary() {
        return hasDefaultOu() && !getProjects().isEmpty();
    }

    public boolean hasProjects() {
        return getProjects() != null && !getProjects().isEmpty();
    }

    public boolean hasDefaultOu() {
        return defaultOrganizationUnit != null;
    }

    public OrganizationalUnit getSelectedOrganizationUnit() {
        return selectedOrganizationUnit;
    }

    public Repository getSelectedRepository() {
        return selectedRepository;
    }

    public String getSelectedBranch() {
        return selectedBranch;
    }

    public String getOuAlias() {
        return ouAlias;
    }
}
