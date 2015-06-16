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
package org.eclipse.che.plugin.docker.machine;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/**
 * Guice module for terminal feature in docker machines
 *
 * @author Alexander Garagatyi
 */
// Not a DynaModule, install manually
public class DockerTerminalModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DockerMachineTerminalLauncher.class).asEagerSingleton();

        Multibinder<ServerConf> machineServers = Multibinder.newSetBinder(binder(), ServerConf.class);
        machineServers.addBinding().toInstance(new ServerConf("terminal", "4411", "http"));

        Multibinder<String> volumesMultibinder =
                Multibinder.newSetBinder(binder(), String.class, Names.named("machine.docker.system_volumes"));
        // :ro removed because of bug in a docker 1.6:L
        //TODO add :ro when bug is fixed or rework terminal binding mechanism to provide copy of the terminal to each machine
        volumesMultibinder.addBinding().toInstance("/usr/local/che/terminal.zip:/mnt/che/terminal.zip");

        bindConstant().annotatedWith(Names.named(DockerMachineTerminalLauncher.START_TERMINAL_COMMAND))
                      .to("mkdir -p ~/che && unzip /mnt/che/terminal.zip -d ~/che/terminal && " +
                          "~/che/terminal/terminal -addr :4411 -cmd /bin/sh -static ~/che/terminal/");
    }
}