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

import java.util.Arrays;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.model.Document;

/**
 * Service managing the cold storage of a {@link Blob} associated to a given {@link Document}.
 * 
 * @since 11.1
 */
public interface DocumentBlobColdStorageService {

    void moveBlob(CoreSession session, DocumentModel document, StorageClass storageClass);

    enum StorageClass {
        S3_STANDARD("S3_STANDARD"), //
        S3_GLACIER("S3_GLACIER");

        StorageClass(String id) {
            this.id = id;
        }

        protected final String id;

        public String getId() {
            return id;
        }

        public static StorageClass fromId(String id) {
            return Arrays.stream(values()).filter(v -> v.getId().equals(id)).findAny().orElseGet(null);
        }
    }

}
