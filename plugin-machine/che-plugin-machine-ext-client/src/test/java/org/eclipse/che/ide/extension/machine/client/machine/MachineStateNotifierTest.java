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
package org.eclipse.che.ide.extension.machine.client.machine;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.machine.shared.dto.event.MachineStatusEvent;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.panel.MachinePanelPresenter;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.events.MessageHandler;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.eclipse.che.api.machine.shared.MachineStatus.CREATING;
import static org.eclipse.che.api.machine.shared.MachineStatus.DESTROYING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(MockitoJUnitRunner.class)
public class MachineStateNotifierTest {

    private static final String SOME_TEXT = "someText";

    //constructor mocks
    @Mock
    private MessageBus                  messageBus;
    @Mock
    private DtoUnmarshallerFactory      dtoUnmarshallerFactory;
    @Mock
    private NotificationManager         notificationManager;
    @Mock
    private MachineServiceClient        service;
    @Mock
    private MachineLocalizationConstant locale;
    @Mock
    private MachinePanelPresenter       machinePanelPresenter;

    //additional mocks
    @Mock
    private Unmarshallable<MachineStatusEvent> unmarshaller;
    @Mock
    private Promise<MachineDescriptor>         machinePromise;
    @Mock
    private MachineDescriptor                  descriptor;
    @Mock
    private MachineStatusEvent                 stateEvent;

    @Captor
    private ArgumentCaptor<Operation<MachineDescriptor>>            operationCaptor;
    @Captor
    private ArgumentCaptor<Notification>                            notificationCaptor;
    @Captor
    private ArgumentCaptor<SubscriptionHandler<MachineStatusEvent>> handlerCaptor;

    @InjectMocks
    private MachineStatusNotifier stateNotifier;

    @Before
    public void setUp() {
        when(dtoUnmarshallerFactory.newWSUnmarshaller(MachineStatusEvent.class)).thenReturn(unmarshaller);

        when(service.getMachine(SOME_TEXT)).thenReturn(machinePromise);

        when(locale.notificationCreatingMachine(SOME_TEXT)).thenReturn(SOME_TEXT);
        when(locale.notificationDestroyingMachine(SOME_TEXT)).thenReturn(SOME_TEXT);
    }

    @Test
    public void machineShouldBeTrackedWhenMachineStateIsCreating() throws Exception {
        when(descriptor.getStatus()).thenReturn(CREATING);
        when(descriptor.getDisplayName()).thenReturn(SOME_TEXT);
        stateNotifier.trackMachine(SOME_TEXT);

        verify(machinePromise).then(operationCaptor.capture());
        operationCaptor.getValue().apply(descriptor);

        verify(notificationManager).showNotification(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();

        assertThat(notification.isFinished(), equalTo(false));
        assertThat(notification.getMessage(), equalTo(SOME_TEXT));

        verify(locale).notificationCreatingMachine(SOME_TEXT);
        verify(locale, never()).notificationDestroyingMachine(SOME_TEXT);

        verify(messageBus).subscribe(anyString(), Matchers.<MessageHandler>anyObject());
    }

    @Test
    public void machineShouldBeTrackedWhenMachineStateIsDestroying() throws Exception {
        when(descriptor.getStatus()).thenReturn(DESTROYING);
        when(descriptor.getDisplayName()).thenReturn(SOME_TEXT);
        stateNotifier.trackMachine(SOME_TEXT);

        verify(machinePromise).then(operationCaptor.capture());
        operationCaptor.getValue().apply(descriptor);

        verify(notificationManager).showNotification(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();

        assertThat(notification.isFinished(), equalTo(false));
        assertThat(notification.getMessage(), equalTo(SOME_TEXT));

        verify(locale).notificationDestroyingMachine(SOME_TEXT);
        verify(locale, never()).notificationCreatingMachine(SOME_TEXT);

        verify(messageBus).subscribe(anyString(), Matchers.<MessageHandler>anyObject());
    }
}