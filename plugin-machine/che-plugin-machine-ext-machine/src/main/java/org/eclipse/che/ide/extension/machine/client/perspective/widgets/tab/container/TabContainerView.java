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
package org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.container;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.content.TabPresenter;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.header.TabHeader;

import javax.annotation.Nonnull;

/**
 * Provides methods which allows add header of tab and tab's content to special container.
 *
 * @author Dmitry Shnurenko
 */
@ImplementedBy(TabContainerViewImpl.class)
public interface TabContainerView extends IsWidget {
    /**
     * Adds tab header to container.
     *
     * @param tabHeader
     *         header which need add
     */
    void addHeader(@Nonnull TabHeader tabHeader);

    /**
     * Adds tab content to container.
     *
     * @param content
     *         content which need add
     */
    void addContent(@Nonnull TabPresenter content);
}