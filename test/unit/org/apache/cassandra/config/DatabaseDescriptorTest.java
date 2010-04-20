/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.cassandra.config;

import static org.junit.Assert.assertNotNull;

import org.apache.cassandra.db.DefsTable;
import org.apache.cassandra.db.migration.AddKeyspace;
import org.apache.cassandra.db.migration.Migration;
import org.apache.cassandra.locator.RackAwareStrategy;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

public class DatabaseDescriptorTest
{
    @Test
    public void testShouldHaveConfigFileNameAvailable()
    {
        assertNotNull(DatabaseDescriptor.getConfigFileName(), "DatabaseDescriptor should always be able to return the file name of the config file");
    }

    @Test
    public void testCFMetaDataSerialization() throws IOException
    {
        // test serialization of all defined test CFs.
        for (String table : DatabaseDescriptor.getNonSystemTables())
        {
            for (CFMetaData cfm : DatabaseDescriptor.getTableMetaData(table).values())
            {
                byte[] ser = CFMetaData.serialize(cfm);
                CFMetaData cfmDupe = CFMetaData.deserialize(new ByteArrayInputStream(ser));
                assert cfmDupe != null;
                assert cfmDupe.equals(cfm);
            }
        }

    }

    @Test
    public void testKSMetaDataSerialization() throws IOException 
    {
        for (KSMetaData ksm : DatabaseDescriptor.tables.values())
        {
            byte[] ser = KSMetaData.serialize(ksm);
            KSMetaData ksmDupe = KSMetaData.deserialize(new ByteArrayInputStream(ser));
            assert ksmDupe != null;
            assert ksmDupe.equals(ksm);
        }
    }
    
    // this came as a result of CASSANDRA-995
    @Test
    public void testTransKsMigration() throws IOException, ConfigurationException
    {
        DatabaseDescriptor.loadSchemas();
        assert DatabaseDescriptor.getNonSystemTables().size() == 0;
        
        // add a few.
        AddKeyspace ks0 = new AddKeyspace(new KSMetaData("ks0", RackAwareStrategy.class, 3));
        ks0.apply();
        AddKeyspace ks1 = new AddKeyspace(new KSMetaData("ks1", RackAwareStrategy.class, 3));
        ks1.apply();
        
        assert DatabaseDescriptor.getTableDefinition("ks0") != null;
        assert DatabaseDescriptor.getTableDefinition("ks1") != null;
        
        DatabaseDescriptor.clearTableDefinition(DatabaseDescriptor.getTableDefinition("ks0"), new UUID(4096, 0));
        DatabaseDescriptor.clearTableDefinition(DatabaseDescriptor.getTableDefinition("ks1"), new UUID(4096, 0));
        
        assert DatabaseDescriptor.getTableDefinition("ks0") == null;
        assert DatabaseDescriptor.getTableDefinition("ks1") == null;
        
        DatabaseDescriptor.loadSchemas();
        
        assert DatabaseDescriptor.getTableDefinition("ks0") != null;
        assert DatabaseDescriptor.getTableDefinition("ks1") != null;
    }
}
