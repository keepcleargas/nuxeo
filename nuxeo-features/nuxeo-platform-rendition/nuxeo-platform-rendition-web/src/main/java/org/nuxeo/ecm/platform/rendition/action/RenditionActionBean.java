/*
 * (C) Copyright 2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam bean used to hold Factory used by summary widget
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@Name("renditionAction")
@Scope(ScopeType.PAGE)
public class RenditionActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String RENDITION_REST_URL_FORMAT = "%sapi/v1/id/%s/@rendition/%s";

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @Factory(value = "currentDocumentRenditions", scope = ScopeType.EVENT)
    public List<Rendition> getCurrentDocumentRenditions() throws Exception {
        DocumentModel doc = navigationContext.getCurrentDocument();
        RenditionService rs = Framework.getLocalService(RenditionService.class);
        return rs.getAvailableRenditions(doc);
    }

    @Factory(value = "currentDocumentVisibleRenditionDefinitions", scope = ScopeType.EVENT)
    public List<RenditionDefinition> getVisibleRenditionDefinitions() throws Exception {

        List<RenditionDefinition> result = new ArrayList<>();
        DocumentModel doc = navigationContext.getCurrentDocument();
        RenditionService rs = Framework.getLocalService(RenditionService.class);
        for (RenditionDefinition rd : rs.getAvailableRenditionDefinitions(doc)) {
            if (rd.isVisible()) {
                result.add(rd);
            }
        }
        return result;
    }

    /**
     * @since 7.3
     */
    public List<Rendition> getVisibleRenditions(String excludedKinds) {
        DocumentModel doc = navigationContext.getCurrentDocument();
        RenditionService rs = Framework.getLocalService(RenditionService.class);
        List<Rendition> availableRenditions = rs.getAvailableRenditions(doc, true);

        List<Rendition> filteredRenditions = availableRenditions;
        if (StringUtils.isNotBlank(excludedKinds)) {
            filteredRenditions = new ArrayList<>();
            List<String> excludedKindList = Arrays.asList(excludedKinds.split(","));
            for (Rendition rendition : availableRenditions) {
                if (!excludedKindList.contains(rendition.getKind())) {
                    filteredRenditions.add(rendition);
                }
            }
        }
        return filteredRenditions;
    }

    public boolean hasVisibleRenditions(String excludedKinds) {
        return !getVisibleRenditions(excludedKinds).isEmpty();
    }

    /**
     * @since 7.3
     */
    public String getRenditionURL(String renditionName) {
        return getRenditionURL(navigationContext.getCurrentDocument(), renditionName);
    }

    /**
     * @since 7.3
     */
    public String getRenditionURL(DocumentModel doc, String renditionName) {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        return String.format(RENDITION_REST_URL_FORMAT, BaseURL.getBaseURL(request), doc.getId(), renditionName);
    }
}