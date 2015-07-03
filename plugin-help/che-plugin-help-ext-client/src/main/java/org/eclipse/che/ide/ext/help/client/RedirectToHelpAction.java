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
package org.eclipse.che.ide.ext.help.client;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;

import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;

import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Open a new window with the help URL
 *
 * @author Oleksii Orel
 */
@Singleton
public class RedirectToHelpAction extends Action {

    private final AnalyticsEventLogger              eventLogger;
    private final HelpExtensionLocalizationConstant locale;

    @Inject
    public RedirectToHelpAction(HelpExtensionLocalizationConstant locale,
                                AnalyticsEventLogger eventLogger,
                                Resources resources) {
        super(locale.actionRedirectToHelpTitle(), locale.actionRedirectToHelpDescription(), null, resources.help());
        this.eventLogger = eventLogger;
        this.locale = locale;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        Window.open(locale.actionRedirectToHelpUrl(), "_blank", "");
    }

}
