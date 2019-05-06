/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.system.space.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.guvnor.structure.backend.backcompat.BackwardCompatibleUtil;
import org.guvnor.structure.contributors.Contributor;
import org.guvnor.structure.contributors.ContributorType;
import org.guvnor.structure.organizationalunit.config.RepositoryConfiguration;
import org.guvnor.structure.organizationalunit.config.RepositoryInfo;
import org.guvnor.structure.organizationalunit.config.SpaceConfigStorageRegistry;
import org.guvnor.structure.organizationalunit.config.SpaceInfo;
import org.guvnor.structure.server.config.ConfigGroup;
import org.guvnor.structure.server.config.ConfigItem;
import org.guvnor.structure.server.config.ConfigType;
import org.guvnor.structure.server.config.ConfigurationService;

@ApplicationScoped
public class ConfigGroupsMigrationService {

    private ConfigurationService configurationService;

    private SpaceConfigStorageRegistry spaceConfigStorageRegistry;

    private BackwardCompatibleUtil backwardCompatibleUtil;

    public ConfigGroupsMigrationService() {
    }

    @Inject
    public ConfigGroupsMigrationService(final ConfigurationService configurationService,
                                        final SpaceConfigStorageRegistry spaceConfigStorageRegistry,
                                        final BackwardCompatibleUtil backwardCompatibleUtil) {
        this.configurationService = configurationService;
        this.spaceConfigStorageRegistry = spaceConfigStorageRegistry;
        this.backwardCompatibleUtil = backwardCompatibleUtil;
    }

    public void moveDataToSpaceConfigRepo() {
        Collection<ConfigGroup> groups = configurationService.getConfiguration(ConfigType.SPACE);
        if (groups != null) {
            for (ConfigGroup groupConfig : groups) {
                final String name = extractName(groupConfig);
                final String defaultGroupId = extractDefaultGroupId(groupConfig);
                final Collection<Contributor> contributors = extractContributors(groupConfig);
                final List<RepositoryInfo> repositories = extractRepositories(groupConfig);
                final List<String> securityGroups = extractSecurityGroups(groupConfig);

                spaceConfigStorageRegistry.get(name).saveSpaceInfo(new SpaceInfo(name,
                                                                                 false,
                                                                                 defaultGroupId,
                                                                                 contributors,
                                                                                 repositories,
                                                                                 securityGroups));

                configurationService.removeConfiguration(groupConfig);
            }
        }
    }

    private String extractName(final ConfigGroup groupConfig) {
        return groupConfig.getName();
    }

    private String extractDefaultGroupId(final ConfigGroup groupConfig) {
        String defaultGroupId = groupConfig.getConfigItemValue("defaultGroupId");

        if (defaultGroupId == null || defaultGroupId.trim().isEmpty()) {
            defaultGroupId = getSanitizedDefaultGroupId(extractName(groupConfig));
        }

        return defaultGroupId;
    }

    private String getSanitizedDefaultGroupId(final String proposedGroupId) {
        //Only [A-Za-z0-9_\-.] are valid so strip everything else out
        return proposedGroupId != null ? proposedGroupId.replaceAll("[^A-Za-z0-9_\\-.]",
                                                                    "") : proposedGroupId;
    }

    private Collection<Contributor> extractContributors(final ConfigGroup configGroup) {
        final List<Contributor> contributors = new ArrayList<>();
        boolean oldConfigGroup = false;

        final String oldOwner = configGroup.getConfigItemValue("owner");
        if (oldOwner != null) {
            oldConfigGroup = true;
            contributors.add(new Contributor(oldOwner,
                                             ContributorType.OWNER));
        }

        ConfigItem<List<String>> oldContributors = configGroup.getConfigItem("contributors");
        if (oldContributors != null) {
            oldConfigGroup = true;

            for (String userName : oldContributors.getValue()) {
                if (!userName.equals(oldOwner)) {
                    contributors.add(new Contributor(userName,
                                                     ContributorType.CONTRIBUTOR));
                }
            }
        }

        if (!oldConfigGroup) {
            ConfigItem<List<Contributor>> newContributorsConfigItem = configGroup.getConfigItem("space-contributors");
            contributors.addAll(newContributorsConfigItem.getValue());
        }

        return contributors;
    }

    private List<RepositoryInfo> extractRepositories(final ConfigGroup groupConfig) {
        List<String> repos = (List<String>) groupConfig.getConfigItem("repositories").getValue();
        return repos.stream().map(r -> new RepositoryInfo(r,
                                                          false,
                                                          new RepositoryConfiguration())).collect(Collectors.toList());
    }

    private List<String> extractSecurityGroups(final ConfigGroup groupConfig) {
        ConfigItem<List<String>> securityGroups = backwardCompatibleUtil.compat(groupConfig).getConfigItem("security:groups");
        return securityGroups.getValue();
    }
}
