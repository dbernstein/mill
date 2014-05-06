/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.ltp.dup;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.duracloud.mill.dup.DuplicationStorePolicy;
import org.duracloud.mill.ltp.StateManager;
import org.duracloud.mill.ltp.dup.DuplicationMorsel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Daniel Bernstein
 *	       Date: Nov 6, 2013
 */
public class StateManagerTest {
    private StateManager<DuplicationMorsel> stateManager;
    private File file;
    private String testdomain = "testdomain";
    private String testspace = "testspace";
    private String testmarker = "testmarker";
    private DuplicationStorePolicy testpolicy;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        file = new File(System.getProperty("java.io.tmpdir") + File.separator + System.currentTimeMillis()+".json");
        testpolicy  = new DuplicationStorePolicy();
        testpolicy.setDestStoreId("0");
        testpolicy.setSrcStoreId("1");
        stateManager = new StateManager<DuplicationMorsel>(file.getAbsolutePath());

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        file.delete();
    }

    /**
     * Test method for {@link org.duracloud.mill.ltp.StateManager#getMorsels()}.
     */
    @Test
    public void testGetMorsels() {
        Set<DuplicationMorsel> morsels = stateManager.getMorsels();
        Assert.assertEquals(0, morsels.size());
        morsels.add(new DuplicationMorsel(testdomain, testspace, testmarker, testpolicy));
        stateManager.setMorsels(morsels);

        morsels = stateManager.getMorsels();
        Assert.assertEquals(1, morsels.size());

        stateManager = new StateManager<DuplicationMorsel>(file.getAbsolutePath());
        morsels = stateManager.getMorsels();
        Assert.assertEquals(1, morsels.size());
        
        stateManager.setMorsels(new HashSet<DuplicationMorsel>());
        stateManager = new StateManager<DuplicationMorsel>(file.getAbsolutePath());
        morsels = stateManager.getMorsels();
        Assert.assertEquals(0, morsels.size());
        
    }

  

}