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
package org.eclipse.che.ide.ext.help.client.about;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.ext.help.client.Resources;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Action for showing About application information.
 *
 * @author Ann Shumilova
 */
@Singleton
public class ShowAboutAction extends Action {

    private final AboutPresenter       presenter;
    private final AnalyticsEventLogger eventLogger;

    @Inject
    public ShowAboutAction(AboutPresenter presenter, AboutLocalizationConstant locale, AnalyticsEventLogger eventLogger,
                           Resources resources) {
        super(locale.aboutControlTitle(), "Show about application", null, resources.about());
        this.presenter = presenter;
        this.eventLogger = eventLogger;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        presenter.showAbout();
    }

}
