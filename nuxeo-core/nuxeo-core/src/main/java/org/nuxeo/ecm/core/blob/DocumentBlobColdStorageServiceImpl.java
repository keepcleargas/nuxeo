/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 * Contributors:
 *     Salem Aouana
 */

package org.nuxeo.ecm.core.blob;

import static java.util.Objects.requireNonNull;
import static org.nuxeo.ecm.core.schema.FacetNames.COLD_STORAGE;

import java.io.Serializable;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.1
 */
public class DocumentBlobColdStorageServiceImpl implements DocumentBlobColdStorageService {

    private static final Logger log = LogManager.getLogger(DocumentBlobColdStorageServiceImpl.class);

    @Override
    public void moveBlob(CoreSession session, DocumentModel document, StorageClass storageClass) {
        requireNonNull(session);
        requireNonNull(document);
        requireNonNull(storageClass);

        // FIXME / TODO
        // --> 1) how to deal with the `file:content` modification
        // --> 2) as we expose the notion of storage class we should add controls to avoid any inconsistent storage
        // class and the Blob holder
        Serializable content = document.getPropertyValue("file:content");
        if (!document.hasFacet(COLD_STORAGE) && content != null) {
            document.addFacet(COLD_STORAGE);
            Map<String, Object> data = Map.of("content", content, //
                    "contentStorageClass", storageClass.getId(), //
                    "beingRetrieved", false);
            document.setProperties("coldstorage", data);

            ThumbnailService thumbnailService = Framework.getService(ThumbnailService.class);
            document.setPropertyValue("file:content", (Serializable) thumbnailService.getThumbnail(document, session));
            session.saveDocument(document);
        }
    }

}
