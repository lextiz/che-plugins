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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.shared.MachineStatus;
import org.eclipse.che.api.machine.shared.dto.MachineStateDescriptor;
import org.eclipse.che.api.machine.shared.dto.event.MachineStatusEvent;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.panel.MachinePanelPresenter;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.events.MessageHandler;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.eclipse.che.api.machine.shared.MachineStatus.CREATING;
import static org.eclipse.che.api.machine.shared.MachineStatus.DESTROYING;
import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Status.PROGRESS;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/**
 * Notifies about changing machine state.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
class MachineStatusNotifier {

    /** WebSocket channel to receive messages about changing machine state. */
    public static final String MACHINE_STATUS_WS_CHANNEL = "machine:status:";

    private final MessageBus                  messageBus;
    private final EventBus                    eventBus;
    private final DtoUnmarshallerFactory      dtoUnmarshallerFactory;
    private final NotificationManager         notificationManager;
    private final MachineServiceClient        service;
    private final MachineLocalizationConstant locale;

    @Inject
    MachineStatusNotifier(MessageBus messageBus,
                         EventBus eventBus,
                         DtoUnmarshallerFactory dtoUnmarshallerFactory,
                         NotificationManager notificationManager,
                         MachineServiceClient service,
                         MachineLocalizationConstant locale) {
        this.messageBus = messageBus;
        this.eventBus = eventBus;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.notificationManager = notificationManager;
        this.service = service;
        this.locale = locale;
    }

    /**
     * Start tracking machine state and notify about state changing.
     *
     * @param machineId
     *         ID of the machine to track
     */
    void trackMachine(@Nonnull final String machineId) {
        trackMachine(machineId, null);
    }

    /**
     * Start tracking machine state and notify about state changing.
     *
     * @param machineId
     *         ID of the machine to track
     * @param runningListener
     *         listener that will be notified when machine is running
     */
    void trackMachine(@Nonnull final String machineId, @Nullable final RunningListener runningListener) {
        final String wsChannel = MACHINE_STATUS_WS_CHANNEL + machineId;
        final Notification notification = new Notification("", INFO, true);

        final Unmarshallable<MachineStatusEvent> unmarshaller = dtoUnmarshallerFactory.newWSUnmarshaller(MachineStatusEvent.class);
        final MessageHandler handler = new SubscriptionHandler<MachineStatusEvent>(unmarshaller) {
            @Override
            protected void onMessageReceived(MachineStatusEvent result) {
                switch (result.getEventType()) {
                    case RUNNING:
                        unsubscribe(wsChannel, this);
                        if (runningListener != null) {
                            runningListener.onRunning();
                        }

                        notification.setMessage(locale.notificationMachineIsRunning(result.getMachineId()));
                        notification.setStatus(FINISHED);
                        notification.setType(INFO);

                        eventBus.fireEvent(org.eclipse.che.ide.extension.machine.client.machine.events.MachineStatusEvent
                                                   .createMachineRunningEvent(result.getMachineId()));

                        break;
                    case DESTROYED:
                        unsubscribe(wsChannel, this);

                        notification.setMessage(locale.notificationMachineDestroyed(result.getMachineId()));
                        notification.setStatus(FINISHED);
                        notification.setType(INFO);

                        eventBus.fireEvent(
                                org.eclipse.che.ide.extension.machine.client.machine.events.MachineStatusEvent.createMachineDestroyedEvent(
                                        result.getMachineId()));

                        break;
                    case ERROR:
                        unsubscribe(wsChannel, this);
                        notification.setMessage(result.getError());
                        notification.setStatus(FINISHED);
                        notification.setType(ERROR);
                        break;
                }
            }

            @Override
            protected void onErrorReceived(Throwable exception) {
                unsubscribe(wsChannel, this);
                notification.setMessage(exception.getMessage());
                notification.setStatus(FINISHED);
                notification.setType(ERROR);
            }
        };

        service.getMachineState(machineId).then(new Operation<MachineStateDescriptor>() {
            @Override
            public void apply(MachineStateDescriptor arg) throws OperationException {
                final MachineStatus status = arg.getStatus();
                if (status == CREATING || status == DESTROYING) {
                    notification.setMessage(status == CREATING ? locale.notificationCreatingMachine(arg.getDisplayName())
                                                              : locale.notificationDestroyingMachine(arg.getDisplayName()));
                    notification.setStatus(PROGRESS);
                    notificationManager.showNotification(notification);

                    try {
                        messageBus.subscribe(wsChannel, handler);
                    } catch (WebSocketException e) {
                        Log.error(getClass(), e);
                    }
                }
            }
        });
    }

    private void unsubscribe(String wsChannel, MessageHandler handler) {
        try {
            messageBus.unsubscribe(wsChannel, handler);
        } catch (WebSocketException e) {
            Log.error(getClass(), e);
        }
    }

    /** If provided to {@link #trackMachine(String, RunningListener)} method - will be invoked when machine is running. */
    interface RunningListener {
        void onRunning();
    }
}
