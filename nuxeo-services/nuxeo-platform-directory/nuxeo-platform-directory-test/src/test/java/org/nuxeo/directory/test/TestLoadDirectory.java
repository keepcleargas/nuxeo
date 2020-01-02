/*
 *  (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Thierry Casanova
 */

package org.nuxeo.directory.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.DataLoadingPolicy.ERROR_ON_DUPLICATE;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.DataLoadingPolicy.IGNORE_DUPLICATE;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.DataLoadingPolicy.NEVER_LOAD;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.DataLoadingPolicy.UPDATE_DUPLICATE;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ DirectoryFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.directory.tests:csv-loaded-directory-contrib.xml")
public class TestLoadDirectory {

    protected static final String CSV_LOAD_DIRECTORY = "csvLoadedDirectory";

    protected static final String CSV_LOAD_DIRECTORY2 = "csvLoadedDirectory2";

    protected static final String CSV_FILE = "test-append-directory.csv";

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected HotDeployer hotDeployer;

    //---- Service Tests ----
    @Test
    public void testUpdateDirectoryFromCsv() throws IOException {
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            DocumentModel docEurope = session.getEntry("1");
            assertEquals("Europe", docEurope.getPropertyValue("label"));

            Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext(CSV_FILE), "text/csv", UTF_8.name(),
                    CSV_FILE);
            directoryService.loadCSV(CSV_LOAD_DIRECTORY, blob, UPDATE_DUPLICATE.toString());

            // check that document initially loaded is still present
            assertNotNull("entry with id '2' should not be null", session.getEntry("2"));
            // check a new directory entry has been created
            DocumentModel newDoc = session.getEntry("8");
            assertEquals("Italy", newDoc.getPropertyValue("label"));
            // check one directory entry has been updated
            docEurope = session.getEntry("1");
            assertEquals("European Union", docEurope.getPropertyValue("label"));
        }
    }

    @Test(expected = DirectoryException.class)
    public void testIgnoreThenErrorOnDuplicate() throws IOException {
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext(CSV_FILE), "text/csv", UTF_8.name(),
                    CSV_FILE);
            directoryService.loadCSV(CSV_LOAD_DIRECTORY, blob, IGNORE_DUPLICATE.toString());
            // verify that new entry was create
            assertEquals("Italy", session.getEntry("8").getPropertyValue("label"));
            // verify existing entry ws not update
            assertEquals("Europe", session.getEntry("1").getPropertyValue("label"));

            // should through Exception:
            directoryService.loadCSV(CSV_LOAD_DIRECTORY, blob, ERROR_ON_DUPLICATE.toString());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testNeverLoad() throws IOException {
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext(CSV_FILE), "text/csv", UTF_8.name(),
                    CSV_FILE);
            // should through Exception (to stop the load loop immediately):
            directoryService.loadCSV(CSV_LOAD_DIRECTORY, blob, NEVER_LOAD.toString());
        }
    }

    //--- Initialisation Tests ----
    @Test
    public void testInitDirectoryWithOnMissingColumns() throws Exception {
        assertDirectoryIsInitialised(CSV_LOAD_DIRECTORY);
        //First we test with dataLoadingPolicy = never_load
        hotDeployer.deploy(
                "org.nuxeo.ecm.directory.tests:csv-on-missing-columns-and-never-load-directory-override-contrib.xml");
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            // We verify that the 2 csv files have been correctly merged
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("obsolete", false);
            DocumentModelList entries = session.query(filter);
            assertEquals(2, entries.size());
        }
        // Then we test with dataLoadingPolicy = ignore_duplicate
        hotDeployer.deploy(
                "org.nuxeo.ecm.directory.tests:csv-on-missing-columns-and-ignore-duplicate-directory-override-contrib.xml");
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            // We verify that the 2 csv files have been correctly merged with duplicate line ignored
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("obsolete", false);
            DocumentModelList entries = session.query(filter);
            assertEquals(4, entries.size());
            //assert unchanged existing entry:
            assertEquals("Europe", session.getEntry("1").getPropertyValue("label"));
        }
        // Then we test with dataLoadingPolicy = update_duplicate
        hotDeployer.deploy(
                "org.nuxeo.ecm.directory.tests:csv-on-missing-columns-and-update-duplicate-directory-override-contrib.xml");
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            // We verify that the 2 csv files have been correctly merged
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("obsolete", false);
            DocumentModelList entries = session.query(filter);
            assertEquals(4, entries.size());
            assertNotNull(session.getEntry("8"));
            assertEquals("European Union", session.getEntry("1").getPropertyValue("label"));
        }
        //Then we test with dataLoadingPolicy = error_on_duplicate
        hotDeployer.deploy(
                "org.nuxeo.ecm.directory.tests:csv-on-missing-columns-and-error-on-duplicate-directory-override-contrib.xml");
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            //since Exception is nesed by hotDeployer we can only check that Directory is not changed
            assertEquals("European Union", session.getEntry("1").getPropertyValue("label"));
        }
    }

    private void assertDirectoryIsInitialised(String directoryName) {
        try (Session session = directoryService.open(directoryName)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("obsolete", false);
            DocumentModelList entries = session.query(filter);
            assertEquals(2, entries.size());
            assertEquals("Europe", session.getEntry("1").getPropertyValue("label"));
        }
    }
}
