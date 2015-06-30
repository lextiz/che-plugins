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
package org.eclipse.che.ide.ext.runner.client.actions;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.runner.dto.RunOptions;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.permits.ActionDenyAccessDialog;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.runner.client.RunnerLocalizationConstant;
import org.eclipse.che.ide.ext.runner.client.RunnerResources;
import org.eclipse.che.ide.ext.runner.client.manager.RunnerManager;
import org.eclipse.che.ide.ext.runner.client.models.Environment;
import org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.RAM.MB_200;
import static org.eclipse.che.ide.ext.runner.client.tabs.properties.panel.common.Scope.SYSTEM;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(MockitoJUnitRunner.class)
public class RunActionTest {
    private final String SOME_STRING = "some_string";

    @Mock
    private RunnerManager              runnerManager;
    @Mock
    private ActionEvent                actionEvent;
    @Mock
    private RunnerLocalizationConstant locale;
    @Mock
    private RunnerResources            resources;
    @Mock
    private DtoFactory                 dtoFactory;
    @Mock
    private RunOptions                 runOptions;
    @Mock
    private AnalyticsEventLogger       eventLogger;
    @Mock
    private NotificationManager        notificationManager;

    @Mock
    private ChooseRunnerAction chooseRunnerAction;
    @Mock
    private AppContext         appContext;
    @Mock
    private CurrentProject     currentProject;
    @Mock
    private Environment        environment;
    @Mock
    private ActionDenyAccessDialog runActionDenyAccessDialog;

    @InjectMocks
    private RunAction action;

    @Test
    public void constructorShouldBeVerified() throws Exception {
        verify(locale).actionRun();
        verify(locale).actionRunDescription();
        verify(resources).run();
    }

    @Test
    public void actionShouldNotBePerformedIfCurrentProjectIsNull() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(null);

        action.actionPerformed(actionEvent);

        verify(eventLogger).log(action);
        verify(runnerManager, never()).launchRunner();
        verify(runnerManager, never()).launchRunner(Matchers.<RunOptions>any(), Matchers.<Scope>any(),anyString());
    }

    @Test
    public void actionShouldBeLaunchDefaultRunner1() throws Exception {
        when(chooseRunnerAction.selectEnvironment()).thenReturn(null);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getRunner()).thenReturn('/' + SOME_STRING);
        when(environment.getName()).thenReturn(SOME_STRING);

        action.actionPerformed(actionEvent);

        verify(eventLogger).log(action);
        verify(runnerManager).launchRunner();
    }

    @Test
    public void actionShouldBeLaunchDefaultRunner2() throws Exception {
        when(chooseRunnerAction.selectEnvironment()).thenReturn(environment);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getRunner()).thenReturn(SOME_STRING);
        when(environment.getId()).thenReturn(SOME_STRING);
        when(environment.getName()).thenReturn(SOME_STRING);

        action.actionPerformed(actionEvent);

        verify(eventLogger).log(action);
        verify(runnerManager).launchRunner();
    }

    @Test
    public void actionShouldBeLaunchDefaultRunnerIfEnvironmentIsNull() throws Exception {
        when(currentProject.getRunner()).thenReturn(SOME_STRING);
        when(chooseRunnerAction.selectEnvironment()).thenReturn(null);
        when(appContext.getCurrentProject()).thenReturn(currentProject);

        action.actionPerformed(actionEvent);

        verify(eventLogger).log(action);
        verify(runnerManager).launchRunner();
    }

    @Test
    public void actionShouldBeLaunchCustomEnvironment() throws Exception {
        when(chooseRunnerAction.selectEnvironment()).thenReturn(environment);
        when(environment.getScope()).thenReturn(SYSTEM);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getRunner()).thenReturn(SOME_STRING + SOME_STRING);

        when(environment.getName()).thenReturn(SOME_STRING);
        when(environment.getId()).thenReturn(SOME_STRING);
        when(environment.getRam()).thenReturn(MB_200.getValue());

        when(dtoFactory.createDto(RunOptions.class)).thenReturn(runOptions);
        when(runOptions.withOptions(Matchers.<Map<String, String>>any())).thenReturn(runOptions);
        when(runOptions.withEnvironmentId(anyString())).thenReturn(runOptions);
        when(runOptions.withMemorySize(MB_200.getValue())).thenReturn(runOptions);

        action.actionPerformed(actionEvent);

        verify(eventLogger).log(action);
        verify(environment).getOptions();
        verify(environment, times(2)).getId();
        verify(environment).getRam();
        verify(environment).getName();

        verify(dtoFactory).createDto(RunOptions.class);
        verify(runOptions).withOptions(Matchers.<Map<String, String>>any());
        verify(runOptions).withEnvironmentId(anyString());
        verify(runOptions).withMemorySize(MB_200.getValue());

        verify(runOptions).withOptions(Matchers.<Map<String, String>>any());
        verify(runnerManager).launchRunner(runOptions, SYSTEM, SOME_STRING);
    }

    @Test
    public void notificationShouldBeShownWhenRunnerIsNotSpecified() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(locale.actionRunnerNotSpecified()).thenReturn(SOME_STRING);
        when(currentProject.getRunner()).thenReturn(null);

        action.actionPerformed(actionEvent);

        verify(eventLogger).log(action);
        verify(locale).actionRunnerNotSpecified();
        verify(notificationManager).showError(SOME_STRING);
    }

}