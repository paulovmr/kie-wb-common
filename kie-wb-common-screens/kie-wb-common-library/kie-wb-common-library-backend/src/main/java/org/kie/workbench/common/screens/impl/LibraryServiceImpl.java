/*
 * Copyright 2016 JBoss, by Red Hat, Inc
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

package org.kie.workbench.common.screens.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

import org.ext.uberfire.social.activities.model.SocialUser;
import org.ext.uberfire.social.activities.service.SocialUserRepositoryAPI;
import org.guvnor.common.services.project.backend.server.utils.PathUtil;
import org.guvnor.common.services.project.events.NewProjectEvent;
import org.guvnor.common.services.project.model.GAV;
import org.guvnor.common.services.project.model.POM;
import org.guvnor.common.services.project.model.Package;
import org.guvnor.common.services.project.model.WorkspaceProject;
import org.guvnor.common.services.project.service.DeploymentMode;
import org.guvnor.common.services.project.service.WorkspaceProjectService;
import org.guvnor.structure.backend.repositories.ConfiguredRepositories;
import org.guvnor.structure.config.SystemRepositoryChangedEvent;
import org.guvnor.structure.organizationalunit.OrganizationalUnit;
import org.guvnor.structure.organizationalunit.OrganizationalUnitService;
import org.guvnor.structure.repositories.Branch;
import org.guvnor.structure.repositories.NewBranchEvent;
import org.guvnor.structure.repositories.Repository;
import org.guvnor.structure.repositories.RepositoryCopier;
import org.guvnor.structure.repositories.RepositoryEnvironmentConfigurations;
import org.guvnor.structure.repositories.RepositoryService;
import org.guvnor.structure.repositories.impl.git.GitRepository;
import org.guvnor.structure.security.OrganizationalUnitAction;
import org.guvnor.structure.security.RepositoryAction;
import org.jboss.errai.bus.server.annotations.Service;
import org.jboss.errai.security.shared.exception.UnauthorizedException;
import org.kie.workbench.common.screens.examples.model.ExampleOrganizationalUnit;
import org.kie.workbench.common.screens.examples.model.ExampleProject;
import org.kie.workbench.common.screens.examples.model.ExampleRepository;
import org.kie.workbench.common.screens.examples.service.ExamplesService;
import org.kie.workbench.common.screens.explorer.backend.server.ExplorerServiceHelper;
import org.kie.workbench.common.screens.explorer.model.FolderItem;
import org.kie.workbench.common.screens.explorer.model.FolderItemType;
import org.kie.workbench.common.screens.library.api.AssetInfo;
import org.kie.workbench.common.screens.library.api.AssetQueryResult;
import org.kie.workbench.common.screens.library.api.LibraryInfo;
import org.kie.workbench.common.screens.library.api.LibraryService;
import org.kie.workbench.common.screens.library.api.OrganizationalUnitRepositoryInfo;
import org.kie.workbench.common.screens.library.api.ProjectAssetsQuery;
import org.kie.workbench.common.screens.library.api.index.LibraryValueFileExtensionIndexTerm;
import org.kie.workbench.common.screens.library.api.index.LibraryValueFileNameIndexTerm;
import org.kie.workbench.common.screens.library.api.index.LibraryValueRepositoryRootIndexTerm;
import org.kie.workbench.common.screens.library.api.preferences.LibraryInternalPreferences;
import org.kie.workbench.common.screens.library.api.preferences.LibraryPreferences;
import org.kie.workbench.common.screens.projecteditor.util.NewWorkspaceProjectUtils;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueIndexTerm;
import org.kie.workbench.common.services.refactoring.model.query.RefactoringPageRequest;
import org.kie.workbench.common.services.refactoring.model.query.RefactoringPageRow;
import org.kie.workbench.common.services.refactoring.service.RefactoringQueryService;
import org.kie.workbench.common.services.shared.project.KieModuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.FileSystem;
import org.uberfire.java.nio.file.NoSuchFileException;
import org.uberfire.java.nio.file.attribute.BasicFileAttributes;
import org.uberfire.java.nio.file.attribute.FileTime;
import org.uberfire.java.nio.file.spi.FileSystemProvider;
import org.uberfire.paging.PageResponse;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.security.authz.AuthorizationManager;

import static java.util.stream.Collectors.toList;
import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;

@Service
@ApplicationScoped
public class LibraryServiceImpl implements LibraryService {

    private static final Pattern STRIP_DOT_GIT = Pattern.compile("\\.git$");

    private static final Logger log = LoggerFactory.getLogger(LibraryServiceImpl.class);

    private RefactoringQueryService refactoringQueryService;
    private OrganizationalUnitService ouService;
    private LibraryPreferences preferences;

    private LibraryInternalPreferences internalPreferences;

    private AuthorizationManager authorizationManager;
    private SessionInfo sessionInfo;
    private ExplorerServiceHelper explorerServiceHelper;
    private WorkspaceProjectService projectService;
    private KieModuleService moduleService;
    private ExamplesService examplesService;
    private IOService ioService;
    private SocialUserRepositoryAPI socialUserRepositoryAPI;
    private IndexStatusOracle indexOracle;
    private RepositoryService repoService;
    private Event<NewProjectEvent> newProjectEvent;
    private PathUtil pathUtil;
    private Event<NewBranchEvent> newBranchEvent;
    private ConfiguredRepositories configuredRepositories;
    private Event<SystemRepositoryChangedEvent> systemRepositoryChangedEvent;

    public LibraryServiceImpl() {
    }

    @Inject
    public LibraryServiceImpl(final OrganizationalUnitService ouService,
                              final RefactoringQueryService refactoringQueryService,
                              final LibraryPreferences preferences,
                              final AuthorizationManager authorizationManager,
                              final SessionInfo sessionInfo,
                              final ExplorerServiceHelper explorerServiceHelper,
                              final WorkspaceProjectService projectService,
                              final KieModuleService moduleService,
                              final ExamplesService examplesService,
                              final RepositoryService repoService,
                              @Named("ioStrategy") final IOService ioService,
                              final LibraryInternalPreferences internalPreferences,
                              final SocialUserRepositoryAPI socialUserRepositoryAPI,
                              final IndexStatusOracle indexOracle,
                              final Event<NewProjectEvent> newProjectEvent,
                              final PathUtil pathUtil,
                              final Event<NewBranchEvent> newBranchEvent,
                              final ConfiguredRepositories configuredRepositories,
                              @org.guvnor.structure.backend.config.Repository final Event<SystemRepositoryChangedEvent> systemRepositoryChangedEvent) {
        this.ouService = ouService;
        this.refactoringQueryService = refactoringQueryService;
        this.preferences = preferences;
        this.authorizationManager = authorizationManager;
        this.sessionInfo = sessionInfo;
        this.explorerServiceHelper = explorerServiceHelper;
        this.projectService = projectService;
        this.moduleService = moduleService;
        this.examplesService = examplesService;
        this.repoService = repoService;
        this.ioService = ioService;
        this.internalPreferences = internalPreferences;
        this.socialUserRepositoryAPI = socialUserRepositoryAPI;
        this.indexOracle = indexOracle;
        this.newProjectEvent = newProjectEvent;
        this.pathUtil = pathUtil;
        this.newBranchEvent = newBranchEvent;
        this.configuredRepositories = configuredRepositories;
        this.systemRepositoryChangedEvent = systemRepositoryChangedEvent;
    }

    @Override
    public OrganizationalUnitRepositoryInfo getDefaultOrganizationalUnitRepositoryInfo() {
        return getOrganizationalUnitRepositoryInfo(getDefaultOrganizationalUnit());
    }

    @Override
    public OrganizationalUnitRepositoryInfo getOrganizationalUnitRepositoryInfo(final OrganizationalUnit selectedOrganizationalUnit) {
        if (selectedOrganizationalUnit == null) {
            return null;
        }

        final List<OrganizationalUnit> organizationalUnits = getOrganizationalUnits();
        final OrganizationalUnit organizationalUnit = getOrganizationalUnit(selectedOrganizationalUnit.getIdentifier(),
                                                                            organizationalUnits).get();
        final List<Repository> repositories = new ArrayList<>(organizationalUnit.getRepositories());

        return new OrganizationalUnitRepositoryInfo(organizationalUnits,
                                                    organizationalUnit,
                                                    repositories);
    }

    @Override
    public LibraryInfo getLibraryInfo(final OrganizationalUnit organizationalUnit) {
        final Collection<WorkspaceProject> projects = projectService.getAllWorkspaceProjects(organizationalUnit).stream()
                .filter(p -> authorizationManager.authorize(p.getRepository(),
                                                            RepositoryAction.READ,
                                                            sessionInfo.getIdentity()))
                .collect(Collectors.toList());

        projects.stream().forEach(p -> {
            if (p.getMainModule() != null) {
                p.getMainModule().setNumberOfAssets(getNumberOfAssets(p));
            }
        });

        return new LibraryInfo(projects);
    }

    @Override
    public WorkspaceProject createProject(final String projectName,
                                          final OrganizationalUnit selectedOrganizationalUnit,
                                          final String projectDescription,
                                          final DeploymentMode deploymentMode) {

        final GAV gav = createGAV(projectName,
                                  selectedOrganizationalUnit);
        final POM pom = createPOM(projectName,
                                  projectDescription,
                                  gav);

        return projectService.newProject(selectedOrganizationalUnit,
                                         pom,
                                         deploymentMode);
    }

    @Override
    public WorkspaceProject createProject(final OrganizationalUnit activeOrganizationalUnit,
                                          final POM pom,
                                          final DeploymentMode mode) {
        return projectService.newProject(activeOrganizationalUnit,
                                         pom,
                                         mode);
    }

    @Override
    public WorkspaceProject importProject(final OrganizationalUnit targetOU,
                                          final String repositoryURL,
                                          final String username,
                                          final String password) {
        final RepositoryEnvironmentConfigurations config = new RepositoryEnvironmentConfigurations();
        config.setOrigin(repositoryURL);
        if (username != null && password != null) {
            config.setUserName(username);
            config.setPassword(password);
        }

        final String targetProjectName = inferProjectName(repositoryURL);

        final Repository repo = repoService.createRepository(targetOU,
                                                             GitRepository.SCHEME.toString(),
                                                             targetProjectName,
                                                             config);
        return projectService.resolveProject(repo);
    }

    @Override
    public WorkspaceProject importProject(final OrganizationalUnit organizationalUnit,
                                          final ExampleProject exampleProject) {
        final org.uberfire.java.nio.file.Path rootPath = getExampleRepoRoot(exampleProject);
        final String niogitRepoURI = pathUtil.getNiogitRepoPath(rootPath);
        if (pathUtil.convert(exampleProject.getRoot()).equals(rootPath)) {
            return importProject(organizationalUnit, niogitRepoURI, null, null);
        } else {
            final RepositoryEnvironmentConfigurations configurations = new RepositoryEnvironmentConfigurations();
            configurations.setInit(false);
            configurations.setOrigin(niogitRepoURI);
            configurations.setBranches(getBranches(rootPath, exampleProject.getRoot()));
            configurations.setMirror(false);
            final String subdirectoryPath = pathUtil.stripRepoNameAndSpace(pathUtil.stripProtocolAndBranch(exampleProject.getRoot().toURI()));
            configurations.setSubdirectory(subdirectoryPath);

            final Repository importedRepo = repoService.createRepository(organizationalUnit,
                                                                         GitRepository.SCHEME.toString(),
                                                                         exampleProject.getName(),
                                                                         configurations);

            // Signal creation of new Project (Creation of OU and Repository, if applicable,
            // are already handled in the corresponding services).
            final WorkspaceProject project = projectService.resolveProject(importedRepo);
            newProjectEvent.fire(new NewProjectEvent(project));

            return project;
        }
    }

    private List<String> getBranches(final org.uberfire.java.nio.file.Path rootPath, final Path projectPath) {
        final FileSystem fs = rootPath.getFileSystem();
        final String exampleRootPath = pathUtil.stripRepoNameAndSpace(pathUtil.stripProtocolAndBranch(projectPath.toURI()));
        return StreamSupport.stream(fs.getRootDirectories().spliterator(), false)
                            .filter(root -> exists(root.resolve(exampleRootPath)))
                            .map(pathUtil::convert)
                            .map(root -> pathUtil.extractBranch(root.toURI()))
                            .flatMap(oBranch -> oBranch.map(Stream::of).orElse(Stream.empty()))
                            .collect(toList());
    }

    private boolean exists(org.uberfire.java.nio.file.Path path) {
        try {
            final FileSystemProvider provider = path.getFileSystem().provider();
            provider.readAttributes(path, BasicFileAttributes.class);

            return true;
        } catch (NoSuchFileException nfe) {
            return false;
        }
    }

    private org.uberfire.java.nio.file.Path getExampleRepoRoot(final ExampleProject exampleProject) {
        return Stream.iterate(pathUtil.convert(exampleProject.getRoot()),
                              p -> p.getParent())
                     .filter(p -> p != null && p.getParent() == null)
                     .findFirst()
                     .get();
    }

    @Override
    public List<WorkspaceProject> importProjects(OrganizationalUnit targetOU, List<ExampleProject> projects) {
        return projects.stream()
                       .map(proj -> importProject(targetOU, proj))
                       .collect(toList());
    }

    private String inferProjectName(String repositoryURL) {
        return Optional.of(repositoryURL)
                .map(url -> java.nio.file.Paths.get(repositoryURL))
                .map(path -> path.getFileName().toString())
                .map(fileName -> STRIP_DOT_GIT.matcher(fileName))
                .map(matcher -> matcher.replaceFirst(""))
                .orElse("new-project");
    }

    @Override
    public Boolean thereIsAProjectInTheWorkbench() {
        return !projectService.getAllWorkspaceProjects().isEmpty();
    }

    @Override
    public AssetQueryResult getProjectAssets(final ProjectAssetsQuery query) {
        checkNotNull("query",
                     query);

        final boolean projectStillExists = ioService.exists(Paths.convert(query.getProject().getBranch().getPath()));
        if (!projectStillExists) {
            log.info("Asset lookup result: project [{}] does not exist.",
                     projectIdentifierFrom(query));
            return AssetQueryResult.nonexistent();
        } else if (!indexOracle.isIndexed(query.getProject())) {
            log.info("Asset lookup result: project [{}] is not indexed.",
                     projectIdentifierFrom(query));
            return AssetQueryResult.unindexed();
        }

        final HashSet<ValueIndexTerm> queryTerms = buildProjectAssetsQuery(query);

        final PageResponse<RefactoringPageRow> findRulesByProjectQuery = refactoringQueryService.query(new RefactoringPageRequest(FindAllLibraryAssetsQuery.NAME,
                                                                                                                                  queryTerms,
                                                                                                                                  query.getStartIndex(),
                                                                                                                                  query.getAmount(),
                                                                                                                                  Boolean.TRUE));
        final List<FolderItem> assets = findRulesByProjectQuery
                .getPageRowList()
                .stream()
                .map(row -> {
                    final Path path = (Path) row.getValue();
                    return new FolderItem(path,
                                          path.getFileName(),
                                          FolderItemType.FILE,
                                          false,
                                          Paths.readLockedBy(path),
                                          Collections.<String>emptyList(),
                                          explorerServiceHelper.getRestrictedOperations(path));
                })
                .collect(Collectors.toList());

        log.info("Asset lookup result: project [{}] is indexed with {} index hits.",
                 projectIdentifierFrom(query),
                 assets.size());
        return AssetQueryResult.normal(assets.stream()
                                               .map(asset -> {
                                                   AssetInfo info = null;
                                                   try {
                                                       final Map<String, Object> attributes = ioService.readAttributes(Paths.convert((Path) asset.getItem()));

                                                       final FileTime lastModifiedFileTime = (FileTime) getAttribute(LibraryService.LAST_MODIFIED_TIME,
                                                                                                                     attributes).get();
                                                       final FileTime createdFileTime = (FileTime) getAttribute(LibraryService.CREATED_TIME,
                                                                                                                attributes).get();
                                                       final Date lastModifiedTime = new Date(lastModifiedFileTime.toMillis());
                                                       final Date createdTime = new Date(createdFileTime.toMillis());
                                                       info = new AssetInfo(asset,
                                                                            lastModifiedTime,
                                                                            createdTime);
                                                   } catch (NoSuchFileException nfe) {
                                                       log.debug("File '" + asset.getFileName() + "' in LibraryIndex but not VFS. Suspected deletion. Skipping.");
                                                   }
                                                   return Optional.ofNullable(info);
                                               })
                                               .filter(Optional::isPresent)
                                               .map(Optional::get)
                                               .collect(Collectors.toList()));
    }

    private static String projectIdentifierFrom(final ProjectAssetsQuery query) {
        return Optional.ofNullable(query.getProject().getRepository())
                .map(repo -> repo.getIdentifier())
                .orElseGet(() -> query.getProject().getName());
    }

    private HashSet<ValueIndexTerm> buildProjectAssetsQuery(ProjectAssetsQuery query) {
        final HashSet<ValueIndexTerm> queryTerms = new HashSet<>();

        queryTerms.add(new LibraryValueRepositoryRootIndexTerm(query.getProject().getRootPath().toURI()));

        if (query.hasFilter()) {
            queryTerms.add(new LibraryValueFileNameIndexTerm("*" + query.getFilter() + "*",
                                                             ValueIndexTerm.TermSearchType.WILDCARD));
        }

        if (query.hasExtension()) {
            queryTerms.add(new LibraryValueFileExtensionIndexTerm(query.getExtensions()));
        }
        return queryTerms;
    }

    @Override
    public Boolean hasProjects(final OrganizationalUnit organizationalUnit) {
        return !projectService.getAllWorkspaceProjects(organizationalUnit).isEmpty();
    }

    @Override
    public Boolean hasAssets(final WorkspaceProject project) {
        checkNotNull("LibraryServiceImpl.project",
                     project);

        final boolean projectStillExists = ioService.exists(Paths.convert(project.getRootPath()));
        if (!projectStillExists) {
            return false;
        }

        final Package defaultPackage = moduleService.resolveDefaultPackage(project.getMainModule());
        return explorerServiceHelper.hasAssets(defaultPackage);
    }

    @Override
    public Set<ExampleProject> getExampleProjects() {
        final String importProjectsUrl = getCustomImportProjectsUrl();
        final ExampleRepository repository = importProjectsUrl == null || importProjectsUrl.isEmpty()
                ? examplesService.getPlaygroundRepository()
                : new ExampleRepository(importProjectsUrl);

        final Set<ExampleProject> projects = examplesService.getProjects(repository);

        return projects;
    }

    @Override
    public Set<ExampleProject> getProjects(final String repositoryUrl) {
        return getProjects(repositoryUrl,
                           null,
                           null);
    }

    @Override
    public Set<ExampleProject> getProjects(final String repositoryUrl,
                                           final String userName,
                                           final String password) {
        if (repositoryUrl == null) {
            return getExampleProjects();
        }

        final ExampleRepository repository = new ExampleRepository(repositoryUrl,
                                                                   userName,
                                                                   password);
        return examplesService.getProjects(repository);
    }

    @Override
    public List<OrganizationalUnit> getOrganizationalUnits() {
        return new ArrayList<>(ouService.getOrganizationalUnits());
    }

    @Override
    public GAV createGAV(final String projectName,
                         final OrganizationalUnit selectedOrganizationalUnit) {
        final LibraryPreferences preferences = getPreferences();
        final String artifactId = NewWorkspaceProjectUtils.sanitizeProjectName(projectName);
        return new GAV(selectedOrganizationalUnit.getDefaultGroupId(),
                       artifactId,
                       preferences.getProjectPreferences().getVersion());
    }

    @Override
    public List<SocialUser> getAllUsers() {
        return socialUserRepositoryAPI.findAllUsers().stream()
                .filter(user -> !user.getUserName().equals("system"))
                .collect(Collectors.toList());
    }

    @Override
    public void addBranch(final String newBranchName,
                          final String baseBranchName,
                          final WorkspaceProject project) {
        Branch baseBranch = project.getRepository().getBranch(baseBranchName)
                .orElseThrow(() -> new IllegalStateException("The base branch does not exists"));

        final org.uberfire.java.nio.file.Path baseBranchPath = pathUtil.convert(baseBranch.getPath());
        final String newBranchPathURI = pathUtil.replaceBranch(newBranchName,
                                                               baseBranch.getPath().toURI());
        try {
            final org.uberfire.java.nio.file.Path newBranchPath = ioService.get(new URI(newBranchPathURI));
            baseBranchPath.getFileSystem().provider().copy(baseBranchPath, newBranchPath);
            fireNewBranchEvent(pathUtil.convert(newBranchPath),
                               newBranchPath);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeBranch(Branch branch) {
        final org.uberfire.java.nio.file.Path branchPath = pathUtil.convert(branch.getPath());
        branchPath.getFileSystem().provider().delete(branchPath);
        systemRepositoryChangedEvent.fire(new SystemRepositoryChangedEvent());
    }

    private void fireNewBranchEvent(final Path targetRoot,
                                    final org.uberfire.java.nio.file.Path nioTargetRepositoryRoot) {

        configuredRepositories.reloadRepositories();

        final Repository repository = repoService.getRepository(targetRoot);

        final Optional<Branch> branch = repository.getBranch(Paths.convert(nioTargetRepositoryRoot.getRoot()));

        if (branch.isPresent()) {
            newBranchEvent.fire(new NewBranchEvent(repository,
                                                   branch.get().getName(),
                                                   sessionInfo.getIdentity()));
        } else {
            throw new IllegalStateException("Could not find a branch that was just created. The Path used was " + nioTargetRepositoryRoot.getRoot());
        }
    }

    String getCustomImportProjectsUrl() {
        return System.getProperty("org.kie.project.examples.repository.url");
    }

    LibraryPreferences getPreferences() {
        preferences.load();
        return preferences;
    }

    LibraryInternalPreferences getInternalPreferences() {
        internalPreferences.load();
        return internalPreferences;
    }

    POM createPOM(final String projectName,
                  final String projectDescription,
                  final GAV gav) {
        return new POM(projectName,
                       projectDescription,
                       "",
                       gav);
    }

    private Optional<Object> getAttribute(final String attribute,
                                          final Map<String, Object> attributes) {
        return attributes.entrySet().stream()
                .filter(entry -> attribute.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    @Override
    public int getNumberOfAssets(final ProjectAssetsQuery query) {
        HashSet<ValueIndexTerm> queryTerms = this.buildProjectAssetsQuery(query);
        return refactoringQueryService.queryHitCount(new RefactoringPageRequest(FindAllLibraryAssetsQuery.NAME,
                                                                                queryTerms,
                                                                                0,
                                                                                null));
    }

    @Override
    public int getNumberOfAssets(final WorkspaceProject module) {
        final HashSet<ValueIndexTerm> queryTerms = new HashSet<>();
        queryTerms.add(new LibraryValueRepositoryRootIndexTerm(module.getRootPath().toURI()));

        return refactoringQueryService.queryHitCount(new RefactoringPageRequest(FindAllLibraryAssetsQuery.NAME,
                                                                                queryTerms,
                                                                                0,
                                                                                null,
                                                                                Boolean.TRUE));
    }

    @Override
    public OrganizationalUnit getDefaultOrganizationalUnit() {
        String defaultOUIdentifier = getInternalPreferences().getLastOpenedOrganizationalUnit();
        if (defaultOUIdentifier == null || defaultOUIdentifier.isEmpty()) {
            defaultOUIdentifier = getPreferences().getOrganizationalUnitPreferences().getName();
        }

        final List<OrganizationalUnit> organizationalUnits = getOrganizationalUnits();
        final Optional<OrganizationalUnit> defaultOU = getOrganizationalUnit(defaultOUIdentifier,
                                                                             organizationalUnits);

        if (defaultOU.isPresent()) {
            return defaultOU.get();
        } else if (!organizationalUnits.isEmpty()) {
            return organizationalUnits.get(0);
        } else {
            return createDefaultOrganizationalUnit();
        }
    }

    private OrganizationalUnit createDefaultOrganizationalUnit() {
        if (!authorizationManager.authorize(OrganizationalUnit.RESOURCE_TYPE,
                                            OrganizationalUnitAction.CREATE,
                                            sessionInfo.getIdentity())) {
            throw new UnauthorizedException("User :user has no permissions to :type -> :action"
                                                    .replace(":user",
                                                             sessionInfo.getIdentity().getIdentifier())
                                                    .replace(":type",
                                                             OrganizationalUnit.RESOURCE_TYPE.getName())
                                                    .replace(":action",
                                                             OrganizationalUnitAction.CREATE.getName()));
        }

        final LibraryPreferences preferences = getPreferences();

        final List<String> contributors = new ArrayList<>();
        contributors.add(preferences.getOrganizationalUnitPreferences().getOwner());

        return ouService.createOrganizationalUnit(preferences.getOrganizationalUnitPreferences().getName(),
                                                  preferences.getOrganizationalUnitPreferences().getOwner(),
                                                  preferences.getOrganizationalUnitPreferences().getGroupId(),
                                                  Collections.emptyList(),
                                                  contributors);
    }

    private Optional<OrganizationalUnit> getOrganizationalUnit(final String identifier,
                                                               final Collection<OrganizationalUnit> organizationalUnits) {
        return organizationalUnits.stream()
                .filter(p -> p.getIdentifier().equalsIgnoreCase(identifier))
                .findFirst();
    }
}

