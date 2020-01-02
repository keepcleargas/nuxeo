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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
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
public class TestInitAlwaysAndNeverLoadDirectory {

    protected static final String CSV_LOAD_DIRECTORY = "csvLoadedDirectory";

    protected static final String CSV_FILE = "test-append-directory.csv";

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    @Deploy("org.nuxeo.ecm.directory.tests:csv-always-loaded-directory-contrib.xml")
    public void testInitDirectoryWithAlways() throws Exception {
        assertDirectoryInitialised();
        hotDeployer.deploy("org.nuxeo.ecm.directory.tests:csv-always-loaded-directory-contrib.override.withAutoincrementId.xml");
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            // We verify that the 2 csv files have been correctly merged
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("obsolete", false);
            DocumentModelList entries = session.query(filter);
            assertEquals(3, entries.size());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.directory.tests:csv-always-loaded-directory-contrib.xml")
    public void testInitDirectoryWithNever() throws Exception {
        assertDirectoryInitialised();
        //Test case NEVER - NEVER_LOAD
        hotDeployer.deploy("org.nuxeo.ecm.directory.tests:csv-never-neverload-directory-contrib.xml");
        assertDirectoryInitialised();
        //Test that autoincrementId is never updated
        hotDeployer.deploy("org.nuxeo.ecm.directory.tests:csv-never-and-update-duplicate-and-AutoIncrementId-directory-override-contrib.xml");
        assertDirectoryInitialised();
        //Test case NEVER - UPDATE
        hotDeployer.deploy("org.nuxeo.ecm.directory.tests:csv-never-and-update-duplicate-directory-override-contrib.xml");
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            // We verify that the 2 csv files have been correctly merged
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("obsolete", false);
            DocumentModelList entries = session.query(filter);
            assertEquals(4, entries.size());
            assertNotNull(session.getEntry("8"));
            assertEquals("European Union", session.getEntry("1").getPropertyValue("label"));
        }
    }

    private void assertDirectoryInitialised() {
        try (Session session = directoryService.open(CSV_LOAD_DIRECTORY)) {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("obsolete", false);
            DocumentModelList entries = session.query(filter);
            assertEquals(2, entries.size());
            assertEquals("Europe", session.getEntry("1").getPropertyValue("label"));
        }
    }
}
