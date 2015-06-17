/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.runner.client.tabs.templates;

import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentLeaf;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.api.project.shared.dto.RunnersDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.actions.ChooseRunnerAction;
import org.eclipse.che.ide.ext.runner.client.callbacks.AsyncCallbackBuilder;
import org.eclipse.che.ide.ext.runner.client.callbacks.FailureCallback;
import org.eclipse.che.ide.ext.runner.client.callbacks.SuccessCallback;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManagerView;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetProjectEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetSystemEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.state.PanelState;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.container.PropertiesContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.environment.EnvironmentWidget;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.filterwidget.FilterWidget;
import org.eclipse.che.ide.ext.runner.client.util.GetEnvironmentsUtil;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.state.State.RUNNERS;
import static org.eclipse.che.ide.ext.runner.client.state.State.TEMPLATE;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
@RunWith(MockitoJUnitRunner.class)
public class TemplatesPresenterTest {
    private static final String DOCKER_TEMPLATE = "docker";
    private static final String SOME_TEXT       = "some Text";

    //constructor mocks
    @Mock
    private TemplatesView                           view;
    @Mock
    private FilterWidget                            filter;
    @Mock
    private EnvironmentWidget                       defaultEnvWidget;
    @Mock
    private RunnerLocalizationConstant              locale;
    @Mock
    private GetProjectEnvironmentsAction            projectEnvironmentsAction;
    @Mock
    private GetSystemEnvironmentsAction             systemEnvironmentsAction;
    @Mock
    private GetEnvironmentsUtil                     environmentUtil;
    @Mock
    private PropertiesContainer                     propertiesContainer;
    @Mock
    private AppContext                              appContext;
    @Mock
    private SelectionManager                        selectionManager;
    @Mock
    private RunnerManagerView                       runnerManagerView;
    @Mock
    private RunnerUtil                              runnerUtil;
    @Mock
    private PanelState                              panelState;
    @Mock
    private AsyncCallbackBuilder<ProjectDescriptor> asyncDescriptorCallbackBuilder;
    @Mock
    private ChooseRunnerAction                      chooseRunnerAction;
    @Mock
    private ProjectServiceClient                    projectService;
    @Mock
    private RunnerResources                         resources;

    //additional mocks
    @Mock
    private Environment                             environment;
    @Mock
    private RunnerEnvironmentTree                   tree;
    @Mock
    private CurrentProject                          currentProject;
    @Mock
    private ProjectDescriptor                       descriptor;
    @Mock
    private Environment                             systemEnvironment1;
    @Mock
    private Environment                             systemEnvironment2;
    @Mock
    private Environment                             projectEnvironment1;
    @Mock
    private ItemReference                           itemReference1;
    @Mock
    private Environment                             projectEnvironment2;
    @Mock
    private List<RunnerEnvironmentLeaf>             leaves;
    @Mock
    private Throwable                               exception;
    @Mock
    private AcceptsOneWidget                        container;
    @Mock
    private RunnersDescriptor                       runnersDescriptor;
    @Mock
    private AsyncRequestCallback<ProjectDescriptor> asyncRequestCallback;
    @Mock
    private AsyncRequestCallback<ItemReference>     asyncRequestCallbackItemReference;
    @Mock
    private AsyncCallbackBuilder<ItemReference>     asyncCallbackBuilder;
    @Mock
    private TextResource                            textResource;
    @Mock
    private NotificationManager                     notificationManager;

    @Captor
    private ArgumentCaptor<SuccessCallback<ProjectDescriptor>> successCaptor;
    @Captor
    private ArgumentCaptor<SuccessCallback<ItemReference>>     successCallbackArgumentCaptor;
    @Captor
    private ArgumentCaptor<FailureCallback>                    failureCallbackArgumentCaptor;

    private TemplatesPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new TemplatesPresenter(view,
                                           filter,
                                           defaultEnvWidget,
                                           appContext,
                                           projectEnvironmentsAction,
                                           systemEnvironmentsAction,
                                           environmentUtil,
                                           propertiesContainer,
                                           selectionManager,
                                           asyncCallbackBuilder,
                                           runnerManagerView,
                                           notificationManager,
                                           runnerUtil,
                                           resources,
                                           panelState,
                                           projectService,
                                           asyncDescriptorCallbackBuilder,
                                           chooseRunnerAction);

        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getProjectDescription()).thenReturn(descriptor);
        when(descriptor.getPath()).thenReturn("/path");
        when(descriptor.getName()).thenReturn("project");
        when(currentProject.getRunner()).thenReturn(SOME_TEXT);
        when(descriptor.getType()).thenReturn(SOME_TEXT);
        when(descriptor.getPath()).thenReturn(SOME_TEXT);
        when(descriptor.getRunners()).thenReturn(runnersDescriptor);
        when(exception.getMessage()).thenReturn(SOME_TEXT);
        when(resources.dockerTemplate()).thenReturn(textResource);
        when(textResource.getText()).thenReturn(DOCKER_TEMPLATE);

        when(asyncCallbackBuilder.unmarshaller(ItemReference.class)).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.success(Matchers.<SuccessCallback<ItemReference>>anyObject())).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.failure(any(FailureCallback.class))).thenReturn(asyncCallbackBuilder);
        when(asyncCallbackBuilder.build()).thenReturn(asyncRequestCallbackItemReference);

        when(environmentUtil.getEnvironmentsByProjectType(tree, SOME_TEXT, SYSTEM)).thenReturn(Arrays.asList(systemEnvironment1,
                                                                                                             systemEnvironment2));
        when(environmentUtil.getEnvironmentsByProjectType(tree, SOME_TEXT, PROJECT)).thenReturn(Arrays.asList(projectEnvironment1,
                                                                                                              projectEnvironment2));
        when(environmentUtil.getAllEnvironments(tree)).thenReturn(leaves);
        when(environmentUtil.getEnvironmentsFromNodes(leaves, SYSTEM)).thenReturn(Arrays.asList(systemEnvironment1, systemEnvironment2));

        when(panelState.getState()).thenReturn(TEMPLATE);
        when(runnerUtil.hasRunPermission()).thenReturn(true);
        when(asyncDescriptorCallbackBuilder.success(Matchers.<SuccessCallback<ProjectDescriptor>>anyObject()))
                .thenReturn(asyncDescriptorCallbackBuilder);
        when(asyncDescriptorCallbackBuilder.failure(Matchers.<FailureCallback>anyObject())).thenReturn(asyncDescriptorCallbackBuilder);
        when(asyncDescriptorCallbackBuilder.build()).thenReturn(asyncRequestCallback);

        when(systemEnvironment1.getName()).thenReturn("systemEnvironment1");
        when(systemEnvironment1.getScope()).thenReturn(SYSTEM);
        when(systemEnvironment2.getName()).thenReturn("systemEnvironment2");
        when(systemEnvironment2.getScope()).thenReturn(SYSTEM);
        when(projectEnvironment1.getName()).thenReturn("projectEnvironment1");
        when(projectEnvironment1.getScope()).thenReturn(PROJECT);
        when(projectEnvironment2.getName()).thenReturn("projectEnvironment2");
        when(projectEnvironment2.getScope()).thenReturn(PROJECT);

    }

    @Test
    public void constructorShouldBeVerified() throws Exception {
        verify(view).setFilterWidget(filter);
    }

    @Test
    public void environmentShouldBeSelected() throws Exception {
        presenter.select(environment);

        verify(propertiesContainer).show(environment);
        verify(view).selectEnvironment(environment);
    }

    @Test(expected = IllegalStateException.class)
    public void illegalStateExceptionShouldBeThrownWhenCurrentProjectIsNull() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(null);

        presenter.addEnvironments(tree, SYSTEM);

        verify(currentProject, never()).getProjectDescription();
    }

    private void getProjectDescriptorShouldBeVerified() {
        verify(appContext).getCurrentProject();
        verify(currentProject).getProjectDescription();
    }

    @Test
    public void environmentShouldNotBeSelectedWhenEnvironmentListIsEmpty() throws Exception {
        when(environmentUtil.getEnvironmentsByProjectType(tree, SOME_TEXT, SYSTEM)).thenReturn(Collections.<Environment>emptyList());
        when(filter.getMatchesProjectType()).thenReturn(true);
        presenter.onValueChanged();
        reset(view, propertiesContainer, selectionManager);

        assertThat(presenter.addEnvironments(tree, SYSTEM).isEmpty(), is(true));

        verify(propertiesContainer).setVisible(true);
        verify(propertiesContainer).show(isNull(Environment.class));
        verify(selectionManager).setEnvironment(isNull(Environment.class));
    }

    @Test
    public void environmentWithScopeProjectShouldBeAdded() throws Exception {
        assertThat(presenter.addEnvironments(tree, PROJECT), hasItems(projectEnvironment1, projectEnvironment2));

        getProjectDescriptorShouldBeVerified();

        verify(environmentUtil).getEnvironmentsByProjectType(tree, SOME_TEXT, PROJECT);
        verify(view).addEnvironment(Matchers.<Map<Scope, List<Environment>>>anyObject());

        verify(propertiesContainer, times(2)).show(projectEnvironment1);
        verify(view, times(2)).selectEnvironment(projectEnvironment1);
        verify(selectionManager, times(2)).setEnvironment(projectEnvironment1);

        verify(panelState).getState();
        verify(runnerUtil).hasRunPermission();
        verify(runnerManagerView).setEnableRunButton(true);
        verify(view, times(2)).scrollTop(anyInt());
    }

    @Test
    public void environmentsShouldBeShown() throws Exception {
        presenter.showEnvironments();

        assertThat(presenter.addEnvironments(tree, PROJECT), hasItems(projectEnvironment1, projectEnvironment2));

        verify(view).setDefaultProjectWidget(null);
        verify(view).clearEnvironmentsPanel();
        verify(projectEnvironmentsAction).perform();
        verify(filter).setMatchesProjectType(true);

        verify(propertiesContainer, times(2)).show(projectEnvironment1);
        verify(view, times(2)).selectEnvironment(projectEnvironment1);
        verify(selectionManager, times(2)).setEnvironment(projectEnvironment1);
    }

    @Test
    public void firstEnvironmentShouldBeSelectedIfSelectedEnvIsNull() throws Exception {
        when(selectionManager.getEnvironment()).thenReturn(null);

        presenter.showEnvironments();
        assertThat(presenter.addEnvironments(tree, PROJECT), hasItems(projectEnvironment1, projectEnvironment2));

        verify(view).clearEnvironmentsPanel();
        verify(projectEnvironmentsAction).perform();
        verify(filter).setMatchesProjectType(true);

        verify(propertiesContainer, times(2)).show(projectEnvironment1);
        verify(view, times(2)).selectEnvironment(projectEnvironment1);
        verify(selectionManager, times(2)).setEnvironment(projectEnvironment1);
    }

    @Test
    public void previousEnvironmentShouldBeSelected() throws Exception {
        when(selectionManager.getEnvironment()).thenReturn(environment);

        presenter.showEnvironments();
        assertThat(presenter.addEnvironments(tree, PROJECT), hasItems(projectEnvironment1, projectEnvironment2));

        verify(view).clearEnvironmentsPanel();
        verify(projectEnvironmentsAction).perform();
        verify(filter).setMatchesProjectType(true);

        verify(selectionManager).setEnvironment(environment);
    }

    @Test
    public void systemEnvironmentsShouldBePerformedWhenTypeAll() throws Exception {
        when(filter.getMatchesProjectType()).thenReturn(false);
        presenter.onValueChanged();

        presenter.addEnvironments(tree, SYSTEM);
        reset(view);

        presenter.onValueChanged();

        verify(view).clearEnvironmentsPanel();
        systemEnvironmentsPerformShouldBeVerified();
    }

    private void systemEnvironmentsPerformShouldBeVerified() {
        verify(environmentUtil).getAllEnvironments(tree);
        verify(environmentUtil).getEnvironmentsFromNodes(leaves, SYSTEM);
        verify(view).addEnvironment(Matchers.<Map<Scope, List<Environment>>>anyObject());
    }

    @Test
    public void systemEnvironmentsShouldBePerformedWhenTypeIsNotAll() throws Exception {
        when(filter.getMatchesProjectType()).thenReturn(true);
        presenter.onValueChanged();

        verify(systemEnvironmentsAction).perform();
    }

    @Test
    public void allEnvironmentShouldBeSelectedWhenScopeIsAllAndTypeIsNotAll() throws Exception {
        when(filter.getMatchesProjectType()).thenReturn(true);

        presenter.onValueChanged();

        verify(projectEnvironmentsAction).perform();
        verify(systemEnvironmentsAction).perform();
    }

    @Test
    public void allEnvironmentShouldBeSelectedWhenScopeIsAllAndTypeIsAll() throws Exception {
        when(filter.getMatchesProjectType()).thenReturn(false);
        presenter.onValueChanged();
        reset(projectEnvironmentsAction);

        presenter.addEnvironments(tree, SYSTEM);
        reset(view);

        presenter.onValueChanged();

        systemEnvironmentsPerformShouldBeVerified();
        verify(projectEnvironmentsAction).perform();
    }

    @Test
    public void firstEnvironmentShouldBeSelectedWhenSelectedEnvironmentIsNull() {
        prepareMocks();
        when(selectionManager.getEnvironment()).thenReturn(null);

        presenter.selectEnvironment();

        verify(propertiesContainer).setVisible(true);
        verify(selectionManager).setEnvironment(systemEnvironment1);
    }

    private void prepareMocks() {
        when(filter.getMatchesProjectType()).thenReturn(true);

        presenter.onValueChanged();
        presenter.addEnvironments(tree, SYSTEM);
        assertThat(presenter.addEnvironments(tree, SYSTEM), hasItems(systemEnvironment1, systemEnvironment1));

        reset(propertiesContainer, selectionManager);
    }

    @Test
    public void environmentShouldBeSelectedWhenSelectedEnvironmentIsNotNull() throws Exception {
        prepareMocks();
        when(selectionManager.getEnvironment()).thenReturn(environment);

        presenter.selectEnvironment();

        verify(selectionManager).setEnvironment(environment);
        verify(selectionManager, never()).setEnvironment(systemEnvironment1);
    }

    @Test
    public void widgetShouldBeSetToContainer() throws Exception {
        presenter.go(container);

        verify(container).setWidget(view);
    }

    @Test
    public void viewShouldBeReturned() throws Exception {
        TemplatesView widget = (TemplatesView)presenter.getView();

        assertThat(widget, equalTo(view));
    }

    @Test
    public void visibleParameterShouldBeSet() throws Exception {
        presenter.setVisible(true);

        verify(view).setVisible(true);
    }

    @Test
    public void runButtonShouldBeEnable() {
        presenter.addEnvironments(tree, SYSTEM);
        reset(runnerUtil, runnerManagerView);

        presenter.changeEnableStateRunButton();

        verify(runnerUtil).hasRunPermission();
        runnerManagerView.setEnableRunButton(true);
    }

    @Test
    public void runButtonShouldBeDisableBecauseUserHasNotPermission() {
        when(runnerUtil.hasRunPermission()).thenReturn(false);

        presenter.changeEnableStateRunButton();

        verify(runnerUtil).hasRunPermission();

        verifyNoMoreInteractions(runnerManagerView);
    }

    @Test
    public void runnerStateShouldNotBeChangedIfOpenRunnerPropertiesPanel1() {
        when(panelState.getState()).thenReturn(RUNNERS);

        assertThat(presenter.addEnvironments(tree, PROJECT), hasItems(projectEnvironment1, projectEnvironment2));

        getProjectDescriptorShouldBeVerified();

        verify(environmentUtil).getEnvironmentsByProjectType(tree, SOME_TEXT, PROJECT);
        verify(view).addEnvironment(Matchers.<Map<Scope, List<Environment>>>anyObject());

        verify(propertiesContainer, times(2)).show(projectEnvironment1);
        verify(view, times(2)).selectEnvironment(projectEnvironment1);
        verify(selectionManager, times(2)).setEnvironment(projectEnvironment1);

        verify(panelState).getState();

        verifyNoMoreInteractions(panelState, runnerUtil, runnerManagerView);
    }

    @Test(expected = IllegalStateException.class)
    public void defaultEnvironmentShouldNotBeSetWhenCurrentProjectIsNull() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(null);

        presenter.setDefaultEnvironment(environment);

        verify(view, never()).setDefaultProjectWidget(defaultEnvWidget);
    }

    @Test
    public void defaultRunnerShouldNotBeSetWhenEnvironmentIsNull() throws Exception {
        reset(view);

        presenter.setDefaultEnvironment(null);

        verify(appContext).getCurrentProject();
        verify(currentProject).getProjectDescription();
        verify(descriptor).getRunners();
        verify(runnersDescriptor).setDefault(null);

        verify(projectService).updateProject(SOME_TEXT, descriptor, asyncRequestCallback);
        verify(asyncDescriptorCallbackBuilder).success(successCaptor.capture());

        successCaptor.getValue().onSuccess(descriptor);

        verify(view).setDefaultProjectWidget(null);

        verify(chooseRunnerAction).selectDefaultRunner();
    }

    private void updateProjectShouldBeVerified() {
        verify(projectService).updateProject(SOME_TEXT, descriptor, asyncRequestCallback);
        verify(asyncDescriptorCallbackBuilder).success(successCaptor.capture());

        successCaptor.getValue().onSuccess(descriptor);

        verify(view).setDefaultProjectWidget(defaultEnvWidget);

        verify(chooseRunnerAction).selectDefaultRunner();
    }

    @Test
    public void defaultEnvironmentShouldNotBeSetWhenDefaultRunnerIsNotChanged() throws Exception {
        when(environment.getId()).thenReturn("project:/runner");

        presenter.setDefaultEnvironment(environment);

        verify(appContext).getCurrentProject();
        verify(currentProject).getProjectDescription();

        verify(runnersDescriptor, never()).setDefault(SOME_TEXT);

        verify(currentProject).getRunner();
        verify(defaultEnvWidget).update(environment);
        verify(environment).getId();

        verify(defaultEnvWidget).update(environment);
    }

    @Test
    public void defaultEnvironmentShouldNotBeSetWhenDefaultRunnerIsChanged() throws Exception {
        when(environment.getId()).thenReturn("project:/runner");

        presenter.setDefaultEnvironment(environment);

        verify(currentProject).getRunner();
        verify(defaultEnvWidget).update(environment);
        verify(environment).getId();
        verify(descriptor).getRunners();
        verify(runnersDescriptor).setDefault("project:/runner");

        updateProjectShouldBeVerified();

        verify(defaultEnvWidget).update(environment);
    }

    @Test
    public void defaultEnvironmentInfoShouldBeShownWhenDefaultEnvironmentIsNotNull() throws Exception {
        when(environment.getId()).thenReturn("project:/runner");
        presenter.setDefaultEnvironment(environment);

        presenter.onDefaultRunnerMouseOver();

        verify(view).showDefaultEnvironmentInfo(environment);
    }

    @Test
    public void defaultEnvironmentInfoShouldBeShownWhenDefaultEnvironmentIsNull() throws Exception {
        when(environment.getId()).thenReturn("project:/runner");
        presenter.setDefaultEnvironment(null);

        presenter.onDefaultRunnerMouseOver();

        verify(view, never()).showDefaultEnvironmentInfo(environment);
    }

    @Test
    public void environmentShouldNotBeCreatedIfCurrentProjectIsNull() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(null);

        presenter.createNewEnvironment();

        verify(projectService, never()).createFolder(anyString(), (AsyncRequestCallback<ItemReference>)any());
    }

    @Test
    public void environmentShouldBeCreated() throws Exception {
        presenter.createNewEnvironment();

        verify(asyncCallbackBuilder).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder).success(successCallbackArgumentCaptor.capture());

        successCallbackArgumentCaptor.getValue().onSuccess(itemReference1);

        verify(projectService).createFolder(anyString(), eq(asyncRequestCallbackItemReference));
        verify(descriptor, times(2)).getPath();

        verify(asyncCallbackBuilder, times(2)).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder, times(2)).success(successCallbackArgumentCaptor.capture());

        successCallbackArgumentCaptor.getValue().onSuccess(itemReference1);

        verify(projectEnvironmentsAction).perform();
    }

    @Test
    public void notificationMessageShouldBeShowedIfParentFolderDoesNotCreate() throws Exception {
        presenter.createNewEnvironment();

        verify(asyncCallbackBuilder).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder).failure(failureCallbackArgumentCaptor.capture());

        failureCallbackArgumentCaptor.getValue().onFailure(exception);

        verify(notificationManager).showError(SOME_TEXT);
    }

    @Test
    public void environmentDoesNotCreatedIfDockerFileNotCrested() throws Exception {
        presenter.createNewEnvironment();

        verify(asyncCallbackBuilder).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder).success(successCallbackArgumentCaptor.capture());

        successCallbackArgumentCaptor.getValue().onSuccess(itemReference1);

        verify(projectService).createFolder(anyString(), eq(asyncRequestCallbackItemReference));
        verify(descriptor, times(2)).getPath();

        verify(asyncCallbackBuilder, times(2)).unmarshaller(ItemReference.class);
        verify(asyncCallbackBuilder, times(2)).failure(failureCallbackArgumentCaptor.capture());

        failureCallbackArgumentCaptor.getValue().onFailure(exception);

        verify(projectEnvironmentsAction, never()).perform();
    }
}