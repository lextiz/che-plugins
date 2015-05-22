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
package org.eclipse.che.ide.ext.runner.client.manager;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.api.project.shared.dto.RunnerConfiguration;
import org.eclipse.che.api.project.shared.dto.RunnersDescriptor;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.RunOptions;
import org.eclipse.che.ide.api.action.permits.ActionDenyAccessDialog;
import org.eclipse.che.ide.api.action.permits.ResourcesLockedActionPermit;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStack;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.actions.ChooseRunnerAction;
import org.eclipse.che.ide.ext.runner.client.constants.TimeInterval;
import org.eclipse.che.ide.ext.runner.client.inject.factories.ModelsFactory;
import org.eclipse.che.ide.ext.runner.client.inject.factories.RunnerActionFactory;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.models.Runner;
import org.eclipse.che.ide.ext.runner.client.models.RunnerCounter;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.CheckRamAndRunAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.GetRunningProcessesAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.StopAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.environments.GetSystemEnvironmentsAction;
import org.eclipse.che.ide.ext.runner.client.runneractions.impl.launch.LaunchAction;
import org.eclipse.che.ide.ext.runner.client.selection.SelectionManager;
import org.eclipse.che.ide.ext.runner.client.state.PanelState;
import org.eclipse.che.ide.ext.runner.client.state.State;
import org.eclipse.che.ide.ext.runner.client.tabs.common.Tab;
import org.eclipse.che.ide.ext.runner.client.tabs.common.TabBuilder;
import org.eclipse.che.ide.ext.runner.client.tabs.common.TabPresenter;
import org.eclipse.che.ide.ext.runner.client.tabs.console.container.ConsoleContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.container.TabContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.container.tab.TabType;
import org.eclipse.che.ide.ext.runner.client.tabs.history.HistoryPanel;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.container.PropertiesContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.templates.TemplatesContainer;
import org.eclipse.che.ide.ext.runner.client.tabs.terminal.container.TerminalContainer;
import org.eclipse.che.ide.ext.runner.client.util.RunnerUtil;
import org.eclipse.che.ide.ext.runner.client.util.TimerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.eclipse.che.ide.ext.runner.client.manager.menu.SplitterState.SPLITTER_OFF;
import static org.eclipse.che.ide.ext.runner.client.manager.menu.SplitterState.SPLITTER_ON;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.IN_PROGRESS;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.IN_QUEUE;
import static org.eclipse.che.ide.ext.runner.client.models.Runner.Status.STOPPED;
import static org.eclipse.che.ide.ext.runner.client.selection.Selection.ENVIRONMENT;
import static org.eclipse.che.ide.ext.runner.client.selection.Selection.RUNNER;
import static org.eclipse.che.ide.ext.runner.client.state.State.RUNNERS;
import static org.eclipse.che.ide.ext.runner.client.state.State.TEMPLATE;
import static org.eclipse.che.ide.ext.runner.client.tabs.common.Tab.VisibleState.REMOVABLE;
import static org.eclipse.che.ide.ext.runner.client.tabs.common.Tab.VisibleState.VISIBLE;
import static org.eclipse.che.ide.ext.runner.client.tabs.container.tab.TabType.LEFT;
import static org.eclipse.che.ide.ext.runner.client.tabs.container.tab.TabType.RIGHT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.DEFAULT;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.PROJECT;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Andrienko Alexander
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class RunnerManagerPresenterTest {

    private static final String TEXT           = "any text";
    private static final String HISTORY_TAB    = "history";
    private static final String TEMPLATES      = "templates";
    private static final String CONSOLE        = "console";
    private static final String TERMINAL       = "terminal";
    private static final String PROPERTIES     = "properties";
    private static final String STOPPED_RUNNER = "application shut down";
    private static final String APP_URL        = "http://runner1.codenvy.com/";
    private static final String ENV_ID         = "project:/project a";
    private static final int    RAM_SIZE       = 1000;
    private static final long   PROCESS_ID     = 1234567L;

    //mocks for constructor
    @Mock
    private RunnerManagerView            view;
    @Mock
    private RunnerActionFactory          actionFactory;
    @Mock
    private ModelsFactory                modelsFactory;
    @Mock
    private AppContext                   appContext;
    @Mock
    private DtoFactory                   dtoFactory;
    @Mock
    private EventBus                     eventBus;
    @Mock
    private RunnerLocalizationConstant   locale;
    @Mock
    private TabContainer                 leftTabContainer;
    @Mock
    private TabContainer                 rightPropertiesContainer;
    @Mock
    private TabContainer                 leftPropertiesContainer;
    @Mock
    private PanelState                   panelState;
    @Mock
    private Provider<TabBuilder>         tabBuilderProvider;
    @Mock
    private ConsoleContainer             consoleContainer;
    @Mock
    private TerminalContainer            terminalContainer;
    @Mock
    private PropertiesContainer          propertiesContainer;
    @Mock
    private HistoryPanel                 history;
    @Mock
    private TemplatesContainer           templates;
    @Mock
    private SelectionManager             selectionManager;
    @Mock
    private ApplicationProcessDescriptor processDescriptor;
    @Mock
    private TimerFactory                 timerFactory;
    @Mock
    private RunnerCounter                runnerCounter;
    @Mock
    private GetSystemEnvironmentsAction  getSystemEnvironmentsAction;
    @Mock
    private RunnerUtil                   runnerUtil;
    @Mock
    private ResourcesLockedActionPermit  runActionPermit;
    @Mock
    private ActionDenyAccessDialog       runActionDenyAccessDialog;

    //tab builder mocks
    @Mock
    private TabBuilder tabBuilderHistory;
    @Mock
    private TabBuilder tabBuilderTemplate;
    @Mock
    private TabBuilder tabBuilderConsole;
    @Mock
    private TabBuilder tabBuilderTerminal;
    @Mock
    private TabBuilder tabBuilderProperties;

    //tab mocks
    @Mock
    private Tab historyTab;
    @Mock
    private Tab templateTab;
    @Mock
    private Tab consoleTab;
    @Mock
    private Tab terminalTab;
    @Mock
    private Tab propertiesTab;

    @Mock
    private Runner                    runner;
    @Mock
    private RunOptions                runOptions;
    @Mock
    private LaunchAction              launchAction;
    @Mock
    private CheckRamAndRunAction      checkRamAndRunAction;
    @Mock
    private CurrentProject            currentProject;
    @Mock
    private ProjectDescriptor         descriptor;
    @Mock
    private Environment               runnerEnvironment;
    @Mock
    private ProjectActionEvent        projectActionEvent;
    @Mock
    private GetRunningProcessesAction getRunningProcessAction;
    @Mock
    private PartStack                 partStack;
    @Mock
    private StopAction                stopAction;
    @Mock
    private PartPresenter             activePart;
    @Mock
    private Timer                     runnerTimer;
    @Mock
    private Timer                     runnerInQueueTimer;
    @Mock
    private ProjectTypeDefinition     definition;
    @Mock
    private ChooseRunnerAction        chooseRunnerAction;
    @Mock
    private RunnersDescriptor         runnersDescriptor;

    @Captor
    private ArgumentCaptor<TimerFactory.TimerCallBack> timerCaptor;

    private RunnerManagerPresenter presenter;

    @Before
    public void setUp() {
        when(locale.runnerTabHistory()).thenReturn(HISTORY_TAB);
        when(locale.runnerTabTemplates()).thenReturn(TEMPLATES);
        when(locale.runnerTabConsole()).thenReturn(CONSOLE);
        when(locale.runnerTabTerminal()).thenReturn(TERMINAL);
        when(locale.runnerTabProperties()).thenReturn(PROPERTIES);

        when(consoleTab.getTitle()).thenReturn(CONSOLE);
        when(propertiesTab.getTitle()).thenReturn(PROPERTIES);

        //order of return mocks must match of order of initialize mocks
        when(tabBuilderProvider.get()).thenReturn(tabBuilderHistory)
                                      .thenReturn(tabBuilderTemplate)
                                      .thenReturn(tabBuilderConsole)
                                      .thenReturn(tabBuilderProperties)
                                      .thenReturn(tabBuilderTerminal);
        //init new historyTab
        initTab(tabBuilderHistory, history, REMOVABLE, LEFT, EnumSet.allOf(State.class), HISTORY_TAB);
        when(tabBuilderHistory.build()).thenReturn(historyTab);

        //init template tab
        initTab(tabBuilderTemplate, templates, REMOVABLE, LEFT, EnumSet.allOf(State.class), TEMPLATES);
        when(tabBuilderTemplate.build()).thenReturn(templateTab);

        //init console tab
        initTab(tabBuilderConsole, consoleContainer, REMOVABLE, RIGHT, EnumSet.of(RUNNERS), CONSOLE);
        when(tabBuilderConsole.build()).thenReturn(consoleTab);

        //init terminal tab
        initTab(tabBuilderTerminal, terminalContainer, VISIBLE, RIGHT, EnumSet.of(RUNNERS), TERMINAL);
        when(tabBuilderTerminal.build()).thenReturn(terminalTab);

        //init properties tab
        initTab(tabBuilderProperties, propertiesContainer, REMOVABLE, RIGHT, EnumSet.allOf(State.class), PROPERTIES);
        when(tabBuilderProperties.build()).thenReturn(propertiesTab);

        when(timerFactory.newInstance(any(TimerFactory.TimerCallBack.class))).thenReturn(runnerTimer)
                                                                             .thenReturn(runnerInQueueTimer);

        presenter = new RunnerManagerPresenter(view,
                                               actionFactory,
                                               modelsFactory,
                                               appContext,
                                               dtoFactory,
                                               chooseRunnerAction,
                                               eventBus,
                                               locale,
                                               leftTabContainer,
                                               leftPropertiesContainer,
                                               rightPropertiesContainer,
                                               panelState,
                                               tabBuilderProvider,
                                               consoleContainer,
                                               terminalContainer,
                                               propertiesContainer,
                                               history,
                                               templates,
                                               runnerCounter,
                                               selectionManager,
                                               timerFactory,
                                               getSystemEnvironmentsAction,
                                               runnerUtil,
                                               runActionPermit,
                                               runActionDenyAccessDialog);

        //adding runner
        when(dtoFactory.createDto(RunOptions.class)).thenReturn(runOptions);
        when(modelsFactory.createRunner(runOptions)).thenReturn(runner);
        when(processDescriptor.getProcessId()).thenReturn(PROCESS_ID);
        when(processDescriptor.getMemorySize()).thenReturn(DEFAULT.getValue());
        when(actionFactory.createLaunch()).thenReturn(launchAction);
        when(runner.getTimeout()).thenReturn(TEXT);
        when(selectionManager.getRunner()).thenReturn(runner);
        when(runner.getActiveTab()).thenReturn(TEXT);
        when(runner.getStatus()).thenReturn(Runner.Status.IN_PROGRESS);

        //application url
        when(locale.uplAppWaitingForBoot()).thenReturn(TEXT);
        when(locale.urlAppRunnerStopped()).thenReturn(STOPPED_RUNNER);
        when(locale.urlAppRunning()).thenReturn(APP_URL);

        //init run options
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getProjectDescription()).thenReturn(descriptor);
        when(descriptor.getRunners()).thenReturn(runnersDescriptor);
        when(runnersDescriptor.getDefault()).thenReturn(ENV_ID);
        when(descriptor.getType()).thenReturn(TEXT);
        when(definition.getRunnerCategories()).thenReturn(Arrays.asList(TEXT));
        when(currentProject.getAttributeValue("runner:skipBuild")).thenReturn("true");
        when(runOptions.withSkipBuild(true)).thenReturn(runOptions);
        when(runOptions.withOptions(any(Map.class))).thenReturn(runOptions);
        when(runOptions.withMemorySize(anyInt())).thenReturn(runOptions);
        when(runOptions.withEnvironmentId(anyString())).thenReturn(runOptions);

        when(actionFactory.createCheckRamAndRun()).thenReturn(checkRamAndRunAction);

        //part stack
        when(partStack.getActivePart()).thenReturn(activePart);

        when(actionFactory.createGetRunningProcess()).thenReturn(getRunningProcessAction);
        when(runner.getTimeout()).thenReturn(TEXT);
        when(panelState.getState()).thenReturn(RUNNERS);
        when(history.isRunnerExist(runner)).thenReturn(true);

        when(runnerUtil.hasRunPermission()).thenReturn(true);
    }

    private void initTab(TabBuilder tabBuilder,
                         TabPresenter tabPresenter,
                         Tab.VisibleState state,
                         TabType tabType,
                         Set<State> stateSet,
                         String title) {
        when(tabBuilder.presenter(tabPresenter)).thenReturn(tabBuilder);
        when(tabBuilder.selectHandler(any(TabContainer.TabSelectHandler.class))).thenReturn(tabBuilder);
        when(tabBuilder.title(title)).thenReturn(tabBuilder);
        when(tabBuilder.visible(state)).thenReturn(tabBuilder);
        when(tabBuilder.scope(stateSet)).thenReturn(tabBuilder);
        when(tabBuilder.tabType(tabType)).thenReturn(tabBuilder);
    }

    @Test
    public void constructorOperationsShouldBePerformed() throws Exception {
        verify(view).setDelegate(presenter);
        verify(selectionManager).addListener(presenter);
        verify(timerFactory, times(2)).newInstance(any(TimerFactory.TimerCallBack.class));
        verify(eventBus).addHandler(ProjectActionEvent.TYPE, presenter);

        verify(view).setLeftPanel(leftTabContainer);
        verify(panelState).setSplitterState(SPLITTER_OFF);
        verify(view).setGeneralPropertiesPanel(rightPropertiesContainer);
    }

    @Test
    public void verifyCreationHistoryTab() {
        verifyInitTab(tabBuilderHistory, history, REMOVABLE, LEFT, EnumSet.allOf(State.class), HISTORY_TAB);
        verify(locale).runnerTabHistory();
        verify(leftTabContainer).addTab(historyTab);
    }

    @Test
    public void verifyCreationTemplateTab() {
        verifyInitTab(tabBuilderTemplate, templates, REMOVABLE, LEFT, EnumSet.allOf(State.class), TEMPLATES);
        verify(locale).runnerTabTemplates();
        verify(leftTabContainer).addTab(templateTab);
    }

    @Test
    public void verifyCreationConsoleTab() {
        verifyInitTab(tabBuilderConsole, consoleContainer, REMOVABLE, RIGHT, EnumSet.of(RUNNERS), CONSOLE);
        verify(locale).runnerTabConsole();
        verify(leftPropertiesContainer).addTab(consoleTab);

    }

    @Test
    public void verifyCreationTerminalTab() {
        verifyInitTab(tabBuilderTerminal, terminalContainer, VISIBLE, RIGHT, EnumSet.of(RUNNERS), TERMINAL);
        verify(locale).runnerTabTerminal();
        verify(rightPropertiesContainer).addTab(terminalTab);
    }

    @Test
    public void verifyCreationPropertiesTab() {
        verifyInitTab(tabBuilderProperties, propertiesContainer, REMOVABLE, RIGHT, EnumSet.allOf(State.class), PROPERTIES);
        verify(locale).runnerTabProperties();
        verify(leftPropertiesContainer).addTab(propertiesTab);

        verify(rightPropertiesContainer).showTabTitle(CONSOLE, false);
        verify(rightPropertiesContainer).showTabTitle(PROPERTIES, false);
    }

    private void verifyInitTab(TabBuilder tabBuilder,
                               TabPresenter tabPresenter,
                               Tab.VisibleState state,
                               TabType type,
                               Set<State> states,
                               String title) {
        verify(tabBuilder).presenter(tabPresenter);
        verify(tabBuilder).title(title);
        verify(tabBuilder).visible(state);
        verify(tabBuilder).scope(states);
        verify(tabBuilder).tabType(type);
    }

    @Test
    public void terminalHandlerShouldPerformWhenRunnerIsNull() {
        verifyTabSelectHandler(tabBuilderProperties);
        verify(propertiesContainer).show((Runner)null);
        verify(locale).runnerTabProperties();
        verify(runner, never()).setActiveTab(PROPERTIES);
    }

    @Test
    public void propertiesHandlerShouldPerformWhenRunnerIsNull() {
        verifyTabSelectHandler(tabBuilderProperties);
        verify(propertiesContainer).show((Runner)null);
        verify(locale).runnerTabProperties();
        verify(runner, never()).setActiveTab(PROPERTIES);
    }

    @Test
    public void historyHandlerShouldPerformActionWhenUserHasPermission() {
        verifyTabSelectHandler(tabBuilderHistory);
        verify(panelState).setState(RUNNERS);
        verify(view).setEnableRunButton(true);
        verify(view).showOtherButtons();
    }

    @Test
    public void historyHandlerShouldPerformActionWhenUserHasNotPermission() {
        when(runnerUtil.hasRunPermission()).thenReturn(false);

        verifyTabSelectHandler(tabBuilderHistory);
        verify(panelState).setState(RUNNERS);
        verify(view).setEnableRunButton(false);
        verify(view).showOtherButtons();
    }

    @Test
    public void templatesHandlerShouldPerformAction() {
        verifyTabSelectHandler(tabBuilderTemplate);
        verify(panelState).setState(TEMPLATE);
        verify(templates).changeEnableStateRunButton();

        verify(view).hideOtherButtons();
    }

    @Test
    public void consoleHandlerShouldPerformActionWhenSelectedRunnerIsNull() {
        reset(locale);
        verifyTabSelectHandler(tabBuilderConsole);

        verifyZeroInteractions(locale);
    }

    @Test
    public void terminalHandlerShouldPerformActionWhenSelectedRunnerIsNull() {
        reset(locale);
        verifyTabSelectHandler(tabBuilderTerminal);

        verifyZeroInteractions(locale);
    }

    @Test
    public void projectScopeShouldBeSetAfterReloadProjectWithProjectScopeRunner() {
        when(processDescriptor.getEnvironmentId()).thenReturn("project://");

        presenter.addRunner(processDescriptor);

        verify(runner).setScope(PROJECT);
    }

    @Test
    public void projectScopeShouldBeSetWhenRunProjectEnvironment() {
        when(panelState.getState()).thenReturn(TEMPLATE);
        when(runnerEnvironment.getScope()).thenReturn(PROJECT);
        when(selectionManager.getEnvironment()).thenReturn(runnerEnvironment);
        when(modelsFactory.createRunner(eq(runOptions), anyString())).thenReturn(runner);

        presenter.onSelectionChanged(ENVIRONMENT);

        presenter.onRunButtonClicked();

        verify(runner).setScope(PROJECT);
    }

    @Test
    public void verifyTimer() {
        presenter.addRunner(processDescriptor);

        verify(timerFactory, times(2)).newInstance(timerCaptor.capture());
        timerCaptor.getAllValues().get(0).onRun();

        verify(runner, times(2)).getTimeout();
        verify(view, times(2)).updateMoreInfoPopup(runner);
        verify(view, times(2)).setTimeout(TEXT);
        verify(runnerTimer, times(2)).schedule(TimeInterval.ONE_SEC.getValue());
    }

    @Test
    public void verifyInQueueTimer() {
        when(locale.messageRunnerInQueue()).thenReturn(TEXT);
        when(runner.getStatus()).thenReturn(IN_QUEUE);

        presenter.onSelectionChanged(RUNNER);

        verify(timerFactory, times(2)).newInstance(timerCaptor.capture());
        timerCaptor.getAllValues().get(1).onRun();

        verify(runner, times(2)).getStatus();
        verify(locale).messageRunnerInQueue();
        verify(consoleContainer).printInfo(runner, TEXT);
    }

    @Test
    public void messageShouldNotBeShownIfRunnerIsNotInQueue() {
        when(runner.getStatus()).thenReturn(IN_PROGRESS);

        presenter.onSelectionChanged(RUNNER);

        verify(timerFactory, times(2)).newInstance(timerCaptor.capture());
        timerCaptor.getAllValues().get(1).onRun();

        verify(runner, times(2)).getStatus();
        verify(locale, never()).messageRunnerInQueue();
        verify(consoleContainer, never()).printInfo(runner, TEXT);
    }

    @Test
    public void consoleHandlerShouldPerformActionWhenSelectedRunnerIsNotNull() {
        presenter.addRunner(processDescriptor);

        verifyTabSelectHandler(tabBuilderConsole);
        verify(locale, times(2)).runnerTabConsole();
        verify(runner).setActiveTab(CONSOLE);
    }

    @Test
    public void terminalHandlerShouldPerformActionWhenSelectedRunnerIsNotNull() {
        presenter.addRunner(processDescriptor);

        verifyTabSelectHandler(tabBuilderTerminal);
        verify(locale, times(3)).runnerTabTerminal();
        verify(runner).setActiveTab(TERMINAL);
        verify(terminalContainer, times(1)).update(runner);
    }

    @Test
    public void propertiesHandlerShouldPerformActionWhenSelectedRunnerIsNotNullAndPanelStateIsRunner() {
        presenter.addRunner(processDescriptor);

        verifyTabSelectHandler(tabBuilderProperties);
        verify(propertiesContainer).show(runner);
        verify(runner).setActiveTab(PROPERTIES);
        verify(locale, times(2)).runnerTabProperties();
        verify(runner).setActiveTab(PROPERTIES);
    }

    @Test
    public void propertiesHandlerShouldPerformActionWhenSelectedRunnerIsNullAndPanelStateIsRunner() {
        reset(locale);
        verifyTabSelectHandler(tabBuilderProperties);

        verify(propertiesContainer).show(isNull(Runner.class));
        verify(locale, never()).runnerTabProperties();
        verify(runner, never()).setActiveTab(PROPERTIES);
    }

    @Test
    public void propertiesHandlerShouldPerformActionWhenSelectedRunnerIsNotNullAndPanelStateIsProperties() {
        when(panelState.getState()).thenReturn(TEMPLATE);
        presenter.addRunner(processDescriptor);
        reset(locale);

        verifyTabSelectHandler(tabBuilderProperties);
        verify(propertiesContainer).show(isNull(Environment.class));
    }

    private void verifyTabSelectHandler(TabBuilder tabBuilder) {
        ArgumentCaptor<TabContainer.TabSelectHandler> tabSelectHandlerCaptor = ArgumentCaptor.forClass(TabContainer.TabSelectHandler.class);
        verify(tabBuilder).selectHandler(tabSelectHandlerCaptor.capture());
        tabSelectHandlerCaptor.getValue().onTabSelected();
    }

    @Test
    public void viewShouldBeReturned() {
        assertThat(presenter.getView(), is(view));
    }

    @Test
    public void selectedEnvironmentShouldBeShownWhenOneClicksOnPropertiesPanel() throws Exception {
        when(panelState.getState()).thenReturn(TEMPLATE);

        verifyTabSelectHandler(tabBuilderProperties);

        verify(propertiesContainer).show(isNull(Environment.class));
    }

    @Test
    public void runnerShouldBeAdded() {
        presenter.addRunner(processDescriptor);

        verify(dtoFactory).createDto(RunOptions.class);
        verify(modelsFactory).createRunner(runOptions);
        verify(processDescriptor).getProcessId();
        verify(runner).setProcessDescriptor(processDescriptor);
        verify(runner).setRAM(DEFAULT.getValue());
        verify(runner).setStatus(Runner.Status.DONE);
        verify(runner).resetCreationTime();
        verify(history).addRunner(runner);

        verify(panelState).setState(RUNNERS);
        verify(view).setEnableRunButton(anyBoolean());
        verify(view).showOtherButtons();

        verifyRunnerSelected();

        verify(runnerTimer).schedule(TimeInterval.ONE_SEC.getValue());

        verify(actionFactory).createLaunch();
        verify(launchAction).perform(runner);
    }

    @Test
    public void runnerIsExist1() {
        presenter.addRunner(processDescriptor);
        assertThat(presenter.isRunnerExist(PROCESS_ID), is(true));
    }

    @Test
    public void runnerIsExist2() {
        presenter.addRunnerId(PROCESS_ID);

        assertThat(presenter.isRunnerExist(PROCESS_ID), is(true));
    }

    @Test
    public void runnerIsNotExist1() {
        assertThat(presenter.isRunnerExist(PROCESS_ID), is(false));
    }

    @Test
    public void runnerIsNotExist2() {
        presenter.addRunnerId(Long.MIN_VALUE);

        assertThat(presenter.isRunnerExist(PROCESS_ID), is(false));
    }

    @Test
    public void runnerShouldBeUpdated() {
        presenter.onSelectionChanged(RUNNER);

        verify(history).update(runner);
        verify(view).update(runner);
    }

    @Test
    public void runnerWhichIsAlreadyExistWithStatusInProgressShouldBeUpdated() {
        presenter.addRunner(processDescriptor);
        reset(history, terminalContainer, view);
        when(history.isRunnerExist(runner)).thenReturn(true);

        presenter.update(runner);

        verify(history).update(runner);
        verify(view).update(runner);
        verify(view).setApplicationURl(TEXT);
    }

    @Test
    public void runnerWhichIsAlreadyExistWithStatusInQueueShouldBeUpdated() {
        presenter.addRunner(processDescriptor);
        reset(history, terminalContainer, view);
        when(runner.getStatus()).thenReturn(Runner.Status.IN_QUEUE);
        when(history.isRunnerExist(runner)).thenReturn(true);

        presenter.update(runner);

        verify(history).update(runner);
        verify(view).setApplicationURl(TEXT);
    }

    @Test
    public void runnerWhichIsAlreadyExistWithStatusStoppedShouldBeUpdated() {
        presenter.addRunner(processDescriptor);
        reset(history, terminalContainer, view);
        when(runner.getStatus()).thenReturn(STOPPED);
        when(history.isRunnerExist(runner)).thenReturn(true);

        presenter.update(runner);

        verify(history).update(runner);
        verify(view).update(runner);
        verify(view).setApplicationURl(STOPPED_RUNNER);
    }

    @Test
    public void runnerWhichIsAlreadyExistWithStatusFailedShouldBeUpdated() {
        presenter.addRunner(processDescriptor);
        reset(history, terminalContainer, view);
        when(runner.getStatus()).thenReturn(Runner.Status.FAILED);
        when(history.isRunnerExist(runner)).thenReturn(true);

        presenter.update(runner);

        verify(history).update(runner);
        verify(view).setApplicationURl(null);
    }

    @Test
    public void runnerWhichIsAlreadyExistWithStatusDoneAndUrlAppIsNotNullShouldBeUpdated() {
        presenter.addRunner(processDescriptor);
        reset(history, terminalContainer, view);
        when(runner.getStatus()).thenReturn(Runner.Status.DONE);
        when(history.isRunnerExist(runner)).thenReturn(true);

        presenter.update(runner);

        verify(history).update(runner);
        verify(view).setApplicationURl(APP_URL);
    }

    @Test
    public void runnerWhichIsAlreadyExistWithStatusDoneAndUrlAppIsNullShouldBeUpdated() {
        presenter.addRunner(processDescriptor);
        reset(history, terminalContainer, view);
        when(runner.getStatus()).thenReturn(Runner.Status.DONE);
        when(runner.getApplicationURL()).thenReturn(null);
        when(history.isRunnerExist(runner)).thenReturn(true);

        presenter.update(runner);

        verify(history).update(runner);
        verify(view).setApplicationURl(APP_URL);
    }

    @Test
    public void runnerWhichIsAlreadyExistWithStatusTimeOutShouldBeUpdated() {
        presenter.addRunner(processDescriptor);
        reset(history, terminalContainer, view);
        when(runner.getStatus()).thenReturn(Runner.Status.TIMEOUT);
        when(history.isRunnerExist(runner)).thenReturn(true);

        presenter.update(runner);

        verify(history).update(runner);
        verify(view).update(runner);
        verify(view).setApplicationURl(APP_URL);
    }

    @Test
    public void runnerShouldNotBeRunIfRunnerAndSelectedEnvironmentAreNull() {
        when(runActionPermit.isAllowed()).thenReturn(true);

        presenter.onRunButtonClicked();

        verifyLaunchRunnerWithNotNullCurrentProject();
    }

    @Test
    public void shouldShowDebugPort() {
        int debugPort = 777_777;
        presenter.addRunner(processDescriptor);
        reset(history, terminalContainer, view);
        when(runner.getStatus()).thenReturn(Runner.Status.DONE);
        when(runner.getDescriptor()).thenReturn(processDescriptor);
        when(processDescriptor.getDebugPort()).thenReturn(debugPort);
        when(history.isRunnerExist(runner)).thenReturn(true);

        presenter.update(runner);

        verify(view).setDebugPort(eq(String.valueOf(debugPort)));
    }

    @Test
    public void shouldNotShowDebugPort() {
        presenter.addRunner(processDescriptor);
        reset(history, terminalContainer, view);
        when(runner.getStatus()).thenReturn(Runner.Status.DONE);
        when(runner.getDescriptor()).thenReturn(processDescriptor);
        when(processDescriptor.getDebugPort()).thenReturn(-1);

        presenter.update(runner);

        verify(view, never()).setDebugPort(anyString());
    }

    @Test
    public void shouldHideDebugPortAfterCloseProject() {
        presenter.addRunner(processDescriptor);
        presenter.onRunButtonClicked();
        presenter.onProjectOpened(projectActionEvent);
        presenter.setPartStack(partStack);

        presenter.onProjectClosed(projectActionEvent);

        verify(view).setDebugPort((String)isNull());
    }

    /**
     * IDEX-2319 Check that if runner is selected in choose options this will run this one.
     */
    @Test
    public void runnerShouldRunSelectedEnvironment() {
        when(chooseRunnerAction.selectEnvironment()).thenReturn(runnerEnvironment);
        when(runnerEnvironment.getId()).thenReturn("myEnvId");
        Map<String, String> options = Collections.emptyMap();
        when(runnerEnvironment.getOptions()).thenReturn(options);
        when(runnerEnvironment.getRam()).thenReturn(2);

        // click on Run
        presenter.onRunButtonClicked();

        // check run options contains the env ID
        verify(runOptions).withOptions(any(Map.class));
        verify(runOptions).withEnvironmentId("myEnvId");
    }

    @Test
    public void runnerShouldBeRunIfSelectedRunnerNotNullAndStatusIsInProgress() {
        when(runner.getStatus()).thenReturn(Runner.Status.IN_PROGRESS);
        when(runActionPermit.isAllowed()).thenReturn(true);
        presenter.addRunner(processDescriptor);
        reset(view, history);

        presenter.onRunButtonClicked();

        verify(panelState).getState();

        verify(appContext, times(2)).getCurrentProject();
        verify(currentProject).getProjectDescription();
        verify(descriptor).getRunners();
        verify(runnersDescriptor).getDefault();

        verify(dtoFactory, times(2)).createDto(RunOptions.class);
        verify(runOptions).withSkipBuild(true);
        verify(runOptions).withMemorySize(DEFAULT.getValue());
        verify(modelsFactory, times(2)).createRunner(runOptions);

        //verify launch runner
        verify(panelState, times(2)).setState(RUNNERS);
        verify(view).showOtherButtons();
        verify(history).addRunner(runner);
        verify(actionFactory).createCheckRamAndRun();
        verify(checkRamAndRunAction).perform(runner);
        verify(runner, times(2)).resetCreationTime();
    }

    @Test
    public void defaultRunnerShouldBeLaunchedButWithCustomOptions() {
        RunOptions defaultRunOptions = mock(RunOptions.class);
        RunnerConfiguration runnerConfiguration = mock(RunnerConfiguration.class);
        Map<String, RunnerConfiguration> configs = new HashMap<>();
        configs.put(TEXT, runnerConfiguration);

        when(runner.getStatus()).thenReturn(Runner.Status.IN_PROGRESS);
        when(runnersDescriptor.getDefault()).thenReturn(TEXT);
        when(runnersDescriptor.getConfigs()).thenReturn(configs);
        when(runnerConfiguration.getRam()).thenReturn(RAM_SIZE);
        when(runOptions.withMemorySize(RAM_SIZE)).thenReturn(defaultRunOptions);
        when(modelsFactory.createRunner(defaultRunOptions)).thenReturn(runner);
        when(runActionPermit.isAllowed()).thenReturn(true);

        presenter.addRunner(processDescriptor);
        reset(view, history);

        presenter.launchRunner();

        verify(appContext, times(2)).getCurrentProject();
        verify(currentProject).getProjectDescription();
        verify(descriptor).getRunners();
        verify(runnersDescriptor).getDefault();

        verify(runnersDescriptor).getConfigs();

        verify(dtoFactory, times(2)).createDto(RunOptions.class);
        verify(runOptions).withSkipBuild(true);
        verify(runOptions).withMemorySize(RAM_SIZE);
        verify(runOptions).withEnvironmentId(anyString());
        verify(modelsFactory).createRunner(runOptions);

        //verify launch runner
        verify(panelState, times(2)).setState(RUNNERS);
        verify(view).showOtherButtons();
        verify(history).addRunner(runner);
        verify(actionFactory).createCheckRamAndRun();
        verify(checkRamAndRunAction).perform(runner);
        verify(runner, times(2)).resetCreationTime();
    }

    @Test
    public void defaultRunnerShouldBeLaunchedIfThisRunnerWasSelect() {
        RunnerConfiguration runnerConfiguration = mock(RunnerConfiguration.class);
        Map<String, RunnerConfiguration> configs = new HashMap<>();
        configs.put(TEXT, runnerConfiguration);

        when(runner.getStatus()).thenReturn(Runner.Status.IN_PROGRESS);
        when(runnersDescriptor.getDefault()).thenReturn(TEXT);
        when(runnersDescriptor.getConfigs()).thenReturn(configs);
        when(runnerConfiguration.getRam()).thenReturn(RAM_SIZE);
        when(runOptions.withMemorySize(RAM_SIZE)).thenReturn(runOptions);
        when(runActionPermit.isAllowed()).thenReturn(true);
        when(chooseRunnerAction.selectEnvironment()).thenReturn(runnerEnvironment);
        when(runnerEnvironment.getId()).thenReturn(TEXT);
        Map<String, String> options = Collections.emptyMap();
        when(runnerEnvironment.getOptions()).thenReturn(options);
        when(runnerEnvironment.getRam()).thenReturn(2);
        when(runnerEnvironment.getName()).thenReturn(TEXT);
        when(modelsFactory.createRunner(runOptions, TEXT)).thenReturn(runner);

        presenter.addRunner(processDescriptor);
        reset(view, history);

        presenter.launchRunner();

        verify(appContext, times(2)).getCurrentProject();
        verify(currentProject).getProjectDescription();
        verify(descriptor).getRunners();
        verify(runnersDescriptor).getDefault();

        verify(runnersDescriptor).getConfigs();

        verify(dtoFactory, times(2)).createDto(RunOptions.class);
        verify(runOptions).withSkipBuild(true);
        verify(runOptions).withMemorySize(RAM_SIZE);
        verify(modelsFactory, times(1)).createRunner(runOptions);

        //verify launch runner
        verify(panelState, times(2)).setState(RUNNERS);
        verify(view).showOtherButtons();
        verify(history).addRunner(runner);
        verify(actionFactory).createCheckRamAndRun();
        verify(checkRamAndRunAction).perform(runner);
        verify(runner, times(2)).resetCreationTime();
    }

    @Test
    public void newRunnerShouldBeRunIfPanelStateIsNotTemplate() {
        when(panelState.getState()).thenReturn(RUNNERS);
        when(runActionPermit.isAllowed()).thenReturn(true);
        presenter.addRunner(processDescriptor);
        reset(view, history);

        presenter.onRunButtonClicked();

        //verify launch runner
        verify(panelState, times(2)).setState(RUNNERS);
        verify(view).showOtherButtons();
        verify(history).addRunner(runner);
        verify(actionFactory).createCheckRamAndRun();
        verify(checkRamAndRunAction).perform(runner);
        verify(runner, times(2)).resetCreationTime();
    }

    @Test
    public void newRunnerShouldBeRunIfPanelStateIsTemplate() {
        Map<String, String> options = new HashMap<>();
        when(selectionManager.getEnvironment()).thenReturn(runnerEnvironment);
        when(runnerEnvironment.getRam()).thenReturn(RAM_SIZE);
        when(runnerEnvironment.getOptions()).thenReturn(options);
        when(runnerEnvironment.getName()).thenReturn(TEXT);
        when(runnerEnvironment.getId()).thenReturn(TEXT);
        when(runOptions.withOptions(options)).thenReturn(runOptions);
        when(runOptions.withEnvironmentId(TEXT)).thenReturn(runOptions);
        when(runOptions.withMemorySize(RAM_SIZE)).thenReturn(runOptions);
        when(modelsFactory.createRunner(runOptions, TEXT)).thenReturn(runner);
        when(panelState.getState()).thenReturn(TEMPLATE);
        when(runActionPermit.isAllowed()).thenReturn(true);

        presenter.onSelectionChanged(ENVIRONMENT);

        presenter.onRunButtonClicked();

        verify(panelState).getState();
        verify(runnerEnvironment).getOptions();
        verify(runnerEnvironment).getName();
        verify(dtoFactory).createDto(RunOptions.class);
        verify(runOptions).withOptions(options);
        verify(runOptions).withEnvironmentId(TEXT);
        verify(runOptions).withMemorySize(RAM_SIZE);
        verify(modelsFactory).createRunner(runOptions, TEXT);

        //verify launch runner
        verify(panelState).setState(RUNNERS);
        verify(view).showOtherButtons();
        verify(history).addRunner(runner);
        verify(actionFactory).createCheckRamAndRun();
        verify(checkRamAndRunAction).perform(runner);
        verify(runner).resetCreationTime();
    }

    @Test
    public void runnerShouldBeRerunIfRunnerActionIsNull() throws Exception {
        when(runActionPermit.isAllowed()).thenReturn(true);
        presenter.addRunner(processDescriptor);
        reset(view, history);
        when(history.isRunnerExist(runner)).thenReturn(true);

        presenter.launchRunner(runOptions);
        presenter.onRerunButtonClicked();

        verify(runner).setStatus(Runner.Status.IN_QUEUE);
        verify(runner, times(2)).getStatus();
        verify(history).update(runner);
        verify(view).update(runner);
        verify(view).setApplicationURl(TEXT);

        verify(runner, times(3)).resetCreationTime();

    }

    @Test
    public void runnerShouldBeRerunIfRunnerNotNullAndStatusIsStopped() {
        when(runner.getStatus()).thenReturn(STOPPED);
        when(runActionPermit.isAllowed()).thenReturn(true);
        presenter.addRunner(processDescriptor);
        reset(view, history);

        presenter.onRerunButtonClicked();

        verify(runner).setStatus(Runner.Status.IN_QUEUE);
        verify(runner).getStatus();

        verify(panelState, times(2)).setState(RUNNERS);
        verify(view).showOtherButtons();
        verify(history).addRunner(runner);
        verify(actionFactory).createCheckRamAndRun();
        verify(checkRamAndRunAction).perform(runner);
        verify(runner, times(2)).resetCreationTime();
    }

    @Test
    public void runnerShouldBeRunIfRunnerNotNullAndStatusIsTemplate() {
        Map<String, String> options = new HashMap<>();
        when(selectionManager.getEnvironment()).thenReturn(runnerEnvironment);
        when(runnerEnvironment.getRam()).thenReturn(RAM_SIZE);
        when(runnerEnvironment.getOptions()).thenReturn(options);
        when(runnerEnvironment.getName()).thenReturn(TEXT);
        when(runnerEnvironment.getId()).thenReturn(TEXT);
        when(runOptions.withOptions(options)).thenReturn(runOptions);
        when(runOptions.withEnvironmentId(TEXT)).thenReturn(runOptions);
        when(runOptions.withMemorySize(RAM_SIZE)).thenReturn(runOptions);
        when(modelsFactory.createRunner(runOptions, TEXT)).thenReturn(runner);
        when(panelState.getState()).thenReturn(TEMPLATE);
        when(runActionPermit.isAllowed()).thenReturn(true);

        presenter.addRunner(processDescriptor);
        reset(view, history);
        presenter.onSelectionChanged(ENVIRONMENT);

        presenter.onRunButtonClicked();

        verify(panelState).getState();
        verify(runnerEnvironment).getOptions();
        verify(runnerEnvironment).getName();
        verify(dtoFactory, times(2)).createDto(RunOptions.class);
        verify(runOptions).withOptions(options);
        verify(runOptions).withEnvironmentId(TEXT);
        verify(runOptions).withMemorySize(RAM_SIZE);
        verify(modelsFactory).createRunner(runOptions, TEXT);

        //verify launch runner
        verify(panelState, times(2)).setState(RUNNERS);
        verify(view).showOtherButtons();
        verify(history).addRunner(runner);
        verify(actionFactory).createCheckRamAndRun();
        verify(checkRamAndRunAction).perform(runner);
        verify(runner, times(2)).resetCreationTime();
    }

    @Test
    public void runnerShouldBeStoppedWhenButtonStopIsClicked() {
        StopAction stopAction = mock(StopAction.class);
        when(actionFactory.createStop()).thenReturn(stopAction);
        presenter.addRunner(processDescriptor);
        reset(view);

        presenter.onStopButtonClicked();

        verify(launchAction).stop();
        verify(terminalContainer).removeTerminalUrl(runner);
        verify(actionFactory).createStop();
        verify(stopAction).perform(runner);
        verify(view).updateMoreInfoPopup(runner);
    }

    @Test
    public void runnerShouldBeStopped() {
        when(actionFactory.createStop()).thenReturn(stopAction);

        presenter.addRunner(processDescriptor);

        presenter.stopRunner(runner);
        verify(launchAction).stop();
        verify(stopAction).perform(runner);
    }

    @Test
    public void moreInfoPopupShouldBeShownWhenMouseIsOver() {
        presenter.addRunner(processDescriptor);

        presenter.onMoreInfoBtnMouseOver();

        verify(view).showMoreInfoPopup(runner);
    }

    @Test(expected = IllegalStateException.class)
    public void runnerShouldNotBeLaunchedIfCurrentProjectIsNull() {
        when(appContext.getCurrentProject()).thenReturn(null);

        presenter.launchRunner();
    }

    @Test
    public void runnerShouldNotBeLaunched() {
        when(runActionPermit.isAllowed()).thenReturn(true);

        presenter.launchRunner();

        verifyLaunchRunnerWithNotNullCurrentProject();
    }

    @Test
    public void shouldCreateAndLaunchRunnerFromRunOptions() {
        presenter.launchRunner(runOptions);

        verify(modelsFactory).createRunner(runOptions);
    }

    @Test
    public void shouldCreateAndLaunchRunnerFromRunOptionsAndEnvironmentName() {
        when(modelsFactory.createRunner(runOptions, TEXT)).thenReturn(runner);
        presenter.launchRunner(runOptions, TEXT);

        verify(modelsFactory).createRunner(runOptions, TEXT);
    }

    @Test
    public void presenterShouldGoneContainer() {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);
        presenter.go(container);
        verify(container).setWidget(view);
    }

    @Test
    public void partStackShouldBeActive() {
        presenter.setPartStack(partStack);

        presenter.setActive();
        verify(partStack).getActivePart();
        verify(partStack).setActivePart(presenter);
    }

    @Test
    public void partStackShouldNotBeActive() {
        when(partStack.getActivePart()).thenReturn(presenter);
        presenter.setPartStack(partStack);

        presenter.setActive();

        verify(partStack).getActivePart();
        verify(partStack, never()).setActivePart(presenter);
    }

    @Test
    public void titleShouldBeReturned() {
        when(locale.runnerTitle()).thenReturn(TEXT);

        presenter.getTitle();

        verify(locale).runnerTitle();
        assertThat(presenter.getTitle(), is(TEXT));
    }

    @Test
    public void titleImageShouldBeReturned() {
        assertThat(presenter.getTitleImage(), nullValue());
    }

    @Test
    public void titleToolTipShouldBeReturned() {
        when(locale.tooltipRunnerPanel()).thenReturn(TEXT);

        presenter.getTitleToolTip();

        verify(locale).tooltipRunnerPanel();
        assertThat(presenter.getTitleToolTip(), is(TEXT));
    }

    @Test
    public void selectionShouldNotBeChangedWhenSelectionIsRunnerAndRunnerIsNull() {
        when(selectionManager.getRunner()).thenReturn(null);
        reset(history, rightPropertiesContainer, view);

        presenter.onSelectionChanged(RUNNER);

        verify(selectionManager).getRunner();

        verify(terminalContainer).setVisibleNoRunnerLabel(true);
        verify(consoleContainer).setVisibleNoRunnerLabel(true);
        verify(propertiesContainer).setVisibleNoRunnerLabel(true);

        verifyNoMoreInteractions(history, rightPropertiesContainer, view);
    }

    @Test
    public void selectionShouldBeChangedWhenSelectionIsRunner() {
        presenter.onSelectionChanged(RUNNER);

        verify(selectionManager).getRunner();

        verifyRunnerSelected();
    }

    @Test
    public void selectionShouldBeChangedWhenSelectionIsEnvironmentAndRunnerEnvironmentIsNull() {
        presenter.onSelectionChanged(ENVIRONMENT);

        verify(selectionManager).getEnvironment();
        verifyNoMoreInteractions(templates);
    }

    @Test
    public void selectionShouldBeChangedWhenSelectionIsEnvironment() {
        when(selectionManager.getEnvironment()).thenReturn(runnerEnvironment);
        presenter.onSelectionChanged(ENVIRONMENT);

        verify(selectionManager).getEnvironment();
        verify(templates).select(runnerEnvironment);
    }

    @Test
    public void openProjectActionsShouldBePerformedWhenCurrentProjectIsNotNull() {
        when(descriptor.getPermissions()).thenReturn(Arrays.asList("run"));

        presenter.onProjectOpened(projectActionEvent);

        verify(view).setEnableRunButton(true);
        verify(templates).setVisible(true);

        verify(view).setEnableRunButton(true);
        verify(view).setEnableReRunButton(false);
        verify(view).setEnableStopButton(false);
        verify(view).setEnableLogsButton(false);

        verify(actionFactory).createGetRunningProcess();
        verify(runnerUtil).hasRunPermission();
        verify(view).setEnableRunButton(true);

        verify(getRunningProcessAction).perform();
        verify(getSystemEnvironmentsAction).perform();
        verify(templates).showEnvironments();

        verify(runnerTimer).schedule(TimeInterval.ONE_SEC.getValue());
    }

    @Test
    public void runningProcessActionShouldNotBePerformedWhenRunPermissionIsDenied() {
        when(runnerUtil.hasRunPermission()).thenReturn(false);

        presenter.onProjectOpened(projectActionEvent);

        verify(templates).setVisible(true);

        verify(view).setEnableRunButton(false);
        verify(view).setEnableReRunButton(false);
        verify(view).setEnableStopButton(false);
        verify(view).setEnableLogsButton(false);

        verify(actionFactory).createGetRunningProcess();

        verifyNoMoreInteractions(getRunningProcessAction, getSystemEnvironmentsAction);
    }

    @Test
    public void projectShouldBeClosed() {
        presenter.addRunner(processDescriptor);
        presenter.onRunButtonClicked();
        presenter.setPartStack(partStack);
        presenter.onProjectOpened(projectActionEvent);

        reset(view);

        presenter.onProjectClosed(projectActionEvent);

        verify(partStack).hidePart(presenter);
        verify(getRunningProcessAction).stop();
        verify(selectionManager).setRunner(null);

        verify(templates).setVisible(false);

        verify(view).setEnableRunButton(false);
        verify(view).setEnableReRunButton(false);
        verify(view).setEnableStopButton(false);
        verify(view).setEnableLogsButton(false);

        verify(view).setApplicationURl(null);
        verify(view).setTimeout(RunnerManagerPresenter.TIMER_STUB);
        verify(history).clear();

        verify(runnerCounter).reset();
        verify(terminalContainer).reset();
        verify(consoleContainer).reset();
        verify(propertiesContainer).reset();
        verify(propertiesContainer).show((Runner)null);
    }

    private void verifyLaunchRunnerWithNotNullCurrentProject() {
        verify(appContext, times(2)).getCurrentProject();
        verify(currentProject).getProjectDescription();
        verify(descriptor).getRunners();
        verify(runnersDescriptor).getDefault();

        verify(dtoFactory).createDto(RunOptions.class);
        verify(runOptions).withSkipBuild(true);
        verify(runOptions).withMemorySize(DEFAULT.getValue());
        verify(modelsFactory).createRunner(runOptions);

        //verify launch runner
        verify(panelState).setState(RUNNERS);
        verify(view).showOtherButtons();
        verify(history).addRunner(runner);
        verify(actionFactory).createCheckRamAndRun();
        verify(checkRamAndRunAction).perform(runner);
        verify(runner).resetCreationTime();
        verify(runnerTimer).schedule(TimeInterval.ONE_SEC.getValue());
    }

    @Test(expected = IllegalStateException.class)
    public void runnerShouldNotLaunchWhenCurrentProjectIsNull() {
        when(appContext.getCurrentProject()).thenReturn(null);
        when(runActionPermit.isAllowed()).thenReturn(true);

        presenter.launchRunner(runOptions);
    }

    private void verifyRunnerSelected() {
        verify(history).selectRunner(runner);

        verify(terminalContainer).setVisibleNoRunnerLabel(false);
        verify(consoleContainer).setVisibleNoRunnerLabel(false);
        verify(propertiesContainer).setVisibleNoRunnerLabel(false);

        //update
        verify(history).update(runner);
        verify(view).update(runner);
        verify(runner).getStatus();
        verify(view).setApplicationURl(TEXT);

        //update runner timer
        verify(runner).getTimeout();
        verify(view).setTimeout(TEXT);
        verify(view).updateMoreInfoPopup(runner);
    }

    @Test
    public void timerShouldNotUpdateIfRunnerIsNull() {
        when(selectionManager.getRunner()).thenReturn(null);
        presenter.onSelectionChanged(RUNNER);
        reset(view, runnerTimer);

        verify(timerFactory, times(2)).newInstance(timerCaptor.capture());
        timerCaptor.getAllValues().get(0).onRun();

        verifyNoMoreInteractions(view);
        verify(runnerTimer).schedule(TimeInterval.ONE_SEC.getValue());
    }

    @Test
    public void logsShouldBeNotShownWhenLogLinkIsNull() throws Exception {
        presenter.onSelectionChanged(RUNNER);
        presenter.onLogsButtonClicked();

        verify(view, never()).showLog(anyString());
    }

    @Test
    public void logsShouldBeShown() throws Exception {
        Link link = mock(Link.class);
        when(link.getHref()).thenReturn(TEXT);

        when(runner.getLogUrl()).thenReturn(link);

        presenter.onSelectionChanged(RUNNER);

        presenter.onLogsButtonClicked();

        verify(view).showLog(TEXT);
    }

    @Test
    public void splitterShouldBeShownWhenPanelStateIsHistory() throws Exception {
        reset(leftPropertiesContainer, view, panelState);

        presenter.onToggleSplitterClicked(true);

        verify(terminalTab).setScopes(EnumSet.allOf(State.class));

        verify(panelState).setSplitterState(SPLITTER_ON);

        verify(view).setLeftPropertiesPanel(leftPropertiesContainer);
        verify(view).setRightPropertiesPanel(rightPropertiesContainer);

        verify(panelState, never()).setSplitterState(SPLITTER_OFF);
        verify(view, never()).setGeneralPropertiesPanel(Matchers.<TabContainer>anyObject());

        verify(panelState, never()).setState(TEMPLATE);
        verify(leftTabContainer, never()).showTab(TEMPLATES);
    }

    @Test
    public void splitterShouldNotBeShownAndPanelStateIsTemplate() throws Exception {
        verifyTabSelectHandler(tabBuilderTemplate);
        reset(leftPropertiesContainer, view, panelState);

        when(panelState.getSplitterState()).thenReturn(SPLITTER_OFF);

        presenter.onToggleSplitterClicked(false);

        verify(terminalTab).setScopes(EnumSet.of(RUNNERS));

        verify(panelState, never()).setSplitterState(SPLITTER_ON);
        verify(view, never()).setLeftPropertiesPanel(leftPropertiesContainer);
        verify(view, never()).setRightPropertiesPanel(rightPropertiesContainer);

        verify(panelState).setSplitterState(SPLITTER_OFF);

        verify(view).setGeneralPropertiesPanel(rightPropertiesContainer);

        verify(panelState).setState(TEMPLATE);
        verify(leftTabContainer).showTab(TEMPLATES);
        verify(rightPropertiesContainer).showTab(PROPERTIES);
    }

}