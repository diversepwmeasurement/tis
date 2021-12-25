/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import com.qlangtech.tis.TestTIS;
import com.qlangtech.tis.datax.impl.TestDataxReader;
import com.qlangtech.tis.extension.TestDescriptor;
import com.qlangtech.tis.extension.impl.TestXmlFile;
import com.qlangtech.tis.extension.model.TestUpdateCenter;
import com.qlangtech.tis.extension.util.TestGroovyShellEvaluate;
import com.qlangtech.tis.extension.util.TestPluginExtraProps;
import com.qlangtech.tis.plugin.TestComponentMeta;
import com.qlangtech.tis.plugin.TestPluginStore;
import com.qlangtech.tis.plugin.TestValidatorCommons;
import com.qlangtech.tis.plugin.annotation.TestValidator;
import com.qlangtech.tis.plugin.ds.TestDataSourceFactoryPluginStore;
import com.qlangtech.tis.util.TestAttrValMap;
import com.qlangtech.tis.util.TestHeteroList;
import com.qlangtech.tis.util.TestUploadPluginMeta;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestAll extends TestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(TestValidatorCommons.class);
        suite.addTestSuite(TestHeteroList.class);
        suite.addTestSuite(TestUploadPluginMeta.class);
        suite.addTestSuite(TestValidator.class);
        suite.addTestSuite(TestTIS.class);
        suite.addTestSuite(TestComponentMeta.class);
        suite.addTestSuite(TestXmlFile.class);
        suite.addTestSuite(TestPluginStore.class);
        suite.addTestSuite(TestGroovyShellEvaluate.class);
        suite.addTestSuite(TestPluginExtraProps.class);
        suite.addTestSuite(TestDataSourceFactoryPluginStore.class);
        suite.addTestSuite(TestUpdateCenter.class);
        suite.addTestSuite(TestDescriptor.class);
        suite.addTestSuite(TestAttrValMap.class);
        suite.addTestSuite(TestDataxReader.class);

        return suite;
    }
}
