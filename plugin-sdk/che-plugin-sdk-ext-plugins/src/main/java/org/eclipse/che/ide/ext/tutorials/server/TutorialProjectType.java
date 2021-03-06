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
package org.eclipse.che.ide.ext.tutorials.server;

import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.ide.extension.maven.server.projecttype.MavenProjectType;
import com.google.inject.Inject;

import static org.eclipse.che.ide.ext.tutorials.shared.Constants.TUTORIAL_ID;
import static org.eclipse.che.ide.ext.tutorials.shared.Constants.TUTORIAL_NAME;

/** @author Artem Zatsarynnyy */
public class TutorialProjectType extends ProjectType {

    @Inject
    public TutorialProjectType(MavenProjectType mavenProjectType) {
        super(TUTORIAL_ID, TUTORIAL_NAME, true, false);

        addParent(mavenProjectType);
        setDefaultBuilder("maven");
        setDefaultRunner("system:/sdk/tomcat7");
    }
}
