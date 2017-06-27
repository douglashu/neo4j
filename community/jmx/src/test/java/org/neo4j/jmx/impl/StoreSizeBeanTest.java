/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.jmx.impl;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.fs.StoreChannel;
import org.neo4j.io.pagecache.PageCache;
import org.neo4j.jmx.StoreSize;
import org.neo4j.kernel.NeoStoreDataSource;
import org.neo4j.kernel.api.index.SchemaIndexProvider;
import org.neo4j.kernel.api.labelscan.LabelScanStore;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.api.LegacyIndexProviderLookup;
import org.neo4j.kernel.impl.transaction.log.PhysicalLogFiles;
import org.neo4j.kernel.impl.transaction.state.DataSourceManager;
import org.neo4j.kernel.impl.util.Dependencies;
import org.neo4j.kernel.internal.DefaultKernelData;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.kernel.internal.KernelData;
import org.neo4j.kernel.spi.legacyindex.IndexImplementation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.neo4j.helpers.collection.Iterables.iterable;
import static org.neo4j.kernel.impl.store.StoreFile.COUNTS_STORE_LEFT;
import static org.neo4j.kernel.impl.store.StoreFile.COUNTS_STORE_RIGHT;
import static org.neo4j.kernel.impl.store.StoreFile.LABEL_TOKEN_NAMES_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.LABEL_TOKEN_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.NODE_LABEL_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.NODE_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.PROPERTY_ARRAY_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.PROPERTY_KEY_TOKEN_NAMES_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.PROPERTY_KEY_TOKEN_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.PROPERTY_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.PROPERTY_STRING_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.RELATIONSHIP_GROUP_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.RELATIONSHIP_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.RELATIONSHIP_TYPE_TOKEN_NAMES_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.RELATIONSHIP_TYPE_TOKEN_STORE;
import static org.neo4j.kernel.impl.store.StoreFile.SCHEMA_STORE;
import static org.neo4j.kernel.impl.storemigration.StoreFileType.ID;
import static org.neo4j.kernel.impl.storemigration.StoreFileType.STORE;

public class StoreSizeBeanTest
{
    private FileSystemAbstraction fs = new EphemeralFileSystemAbstraction();
    private File storeDir = new File( "" );
    private PhysicalLogFiles physicalLogFiles = new PhysicalLogFiles( storeDir, fs );
    private LegacyIndexProviderLookup legacyIndexProviderLookup = mock( LegacyIndexProviderLookup.class );
    private SchemaIndexProvider schemaIndexProvider = mock( SchemaIndexProvider.class );
    private LabelScanStore labelScanStore = mock( LabelScanStore.class );
    private StoreSize storeSizeBean;
    private File storeDirAbsolute;

    @Before
    public void setUp() throws Throwable
    {
        DataSourceManager dataSourceManager = new DataSourceManager();
        GraphDatabaseAPI db = mock( GraphDatabaseAPI.class );
        NeoStoreDataSource dataSource = mock( NeoStoreDataSource.class );

        // Setup all dependencies
        Dependencies dependencies = new Dependencies();
        dependencies.satisfyDependency( fs );
        dependencies.satisfyDependencies( dataSourceManager );
        dependencies.satisfyDependency( physicalLogFiles );
        dependencies.satisfyDependency( legacyIndexProviderLookup );
        dependencies.satisfyDependency( schemaIndexProvider );
        dependencies.satisfyDependency( labelScanStore );
        when( db.getDependencyResolver() ).thenReturn( dependencies );
        when( dataSource.getDependencyResolver() ).thenReturn( dependencies );

        // Start DataSourceManager
        dataSourceManager.register( dataSource );
        dataSourceManager.start();

        // Create bean
        KernelData kernelData = new DefaultKernelData( fs, mock( PageCache.class ), storeDir, Config.empty(), db );
        ManagementData data = new ManagementData( new StoreSizeBean(), kernelData, ManagementSupport.load() );
        storeSizeBean = (StoreSize) new StoreSizeBean().createMBean( data );
    }

    private void createFakeStoreDirectory() throws IOException
    {
        Map<String,Integer> dummyStore = new HashMap<>();
        dummyStore.put( NODE_STORE.fileName( STORE ), 1 );
        dummyStore.put( NODE_STORE.fileName( ID ), 2 );
        dummyStore.put( NODE_LABEL_STORE.fileName( STORE ), 3 );
        dummyStore.put( NODE_LABEL_STORE.fileName( ID ), 4 );
        dummyStore.put( PROPERTY_STORE.fileName( STORE ), 5 );
        dummyStore.put( PROPERTY_STORE.fileName( ID ), 6 );
        dummyStore.put( PROPERTY_KEY_TOKEN_STORE.fileName( STORE ), 7 );
        dummyStore.put( PROPERTY_KEY_TOKEN_STORE.fileName( ID ), 8 );
        dummyStore.put( PROPERTY_KEY_TOKEN_NAMES_STORE.fileName( STORE ), 9 );
        dummyStore.put( PROPERTY_KEY_TOKEN_NAMES_STORE.fileName( ID ), 10 );
        dummyStore.put( PROPERTY_STRING_STORE.fileName( STORE ), 11 );
        dummyStore.put( PROPERTY_STRING_STORE.fileName( ID ), 12 );
        dummyStore.put( PROPERTY_ARRAY_STORE.fileName( STORE ), 13 );
        dummyStore.put( PROPERTY_ARRAY_STORE.fileName( ID ), 14 );
        dummyStore.put( RELATIONSHIP_STORE.fileName( STORE ), 15 );
        dummyStore.put( RELATIONSHIP_STORE.fileName( ID ), 16 );
        dummyStore.put( RELATIONSHIP_GROUP_STORE.fileName( STORE ), 17 );
        dummyStore.put( RELATIONSHIP_GROUP_STORE.fileName( ID ), 18 );
        dummyStore.put( RELATIONSHIP_TYPE_TOKEN_STORE.fileName( STORE ), 19 );
        dummyStore.put( RELATIONSHIP_TYPE_TOKEN_STORE.fileName( ID ), 20 );
        dummyStore.put( RELATIONSHIP_TYPE_TOKEN_NAMES_STORE.fileName( STORE ), 21 );
        dummyStore.put( RELATIONSHIP_TYPE_TOKEN_NAMES_STORE.fileName( ID ), 22 );
        dummyStore.put( LABEL_TOKEN_STORE.fileName( STORE ), 23 );
        dummyStore.put( LABEL_TOKEN_STORE.fileName( ID ), 24 );
        dummyStore.put( LABEL_TOKEN_NAMES_STORE.fileName( STORE ), 25 );
        dummyStore.put( LABEL_TOKEN_NAMES_STORE.fileName( ID ), 26 );
        dummyStore.put( SCHEMA_STORE.fileName( STORE ), 27 );
        dummyStore.put( SCHEMA_STORE.fileName( ID ), 28 );
        dummyStore.put( COUNTS_STORE_LEFT.fileName( STORE ), 29 );
        // COUNTS_STORE_RIGHT is created in the test

        storeDirAbsolute = storeDir.getCanonicalFile().getAbsoluteFile();
        for ( Map.Entry<String,Integer> dummyFile : dummyStore.entrySet() )
        {
            createFileOfSize( new File( storeDirAbsolute, dummyFile.getKey() ), dummyFile.getValue() );
        }
    }

    @Test
    public void sumAllNodeRelatedFiles() throws Exception
    {
        createFakeStoreDirectory();
        assertEquals( getExpected(1, 4 ), storeSizeBean.getNodeStoreSize() );
    }

    @Test
    public void sumAllPropertyRelatedFiles() throws Exception
    {
        createFakeStoreDirectory();
        assertEquals( getExpected( 5, 10 ), storeSizeBean.getPropertyStoreSize() );
    }

    @Test
    public void sumAllStringRelatedFiles() throws Exception
    {
        createFakeStoreDirectory();
        assertEquals( getExpected(11, 12 ), storeSizeBean.getStringStoreSize() );
    }

    @Test
    public void sumAllArrayRelatedFiles() throws Exception
    {
        createFakeStoreDirectory();
        assertEquals( getExpected(13, 14 ), storeSizeBean.getArrayStoreSize() );
    }

    @Test
    public void sumAllRelationshipRelatedFiles() throws Exception
    {
        createFakeStoreDirectory();
        assertEquals( getExpected( 15, 22 ), storeSizeBean.getRelationshipStoreSize() );
    }

    @Test
    public void sumAllLabelRelatedFiles() throws Exception
    {
        createFakeStoreDirectory();
        assertEquals( getExpected( 23, 26 ), storeSizeBean.getLabelStoreSize() );
    }

    @Test
    public void sumAllCountStoreRelatedFiles() throws Exception
    {
        createFakeStoreDirectory();
        assertEquals( getExpected( 29, 29), storeSizeBean.getCountStoreSize() );
        createFileOfSize( new File( storeDirAbsolute, COUNTS_STORE_RIGHT.fileName( STORE ) ), 30 );
        assertEquals( getExpected( 29, 30), storeSizeBean.getCountStoreSize() );
    }

    @Test
    public void sumAllSchemaRelatedFiles() throws Exception
    {
        createFakeStoreDirectory();
        assertEquals( getExpected( 27, 28 ), storeSizeBean.getSchemaStoreSize() );
    }

    @Test
    public void sumAllFiles() throws Exception
    {
        createFakeStoreDirectory();
        assertEquals( getExpected( 0, 29 ), storeSizeBean.getTotalStoreSize() );
    }

    @Test
    public void shouldCountAllLogFiles() throws Throwable
    {
        createFileOfSize( physicalLogFiles.getLogFileForVersion( 0 ), 1 );
        createFileOfSize( physicalLogFiles.getLogFileForVersion( 1 ), 2 );

        assertEquals( 3L, storeSizeBean.getTransactionLogsSize() );
    }

    @Test
    public void shouldCountAllIndexFiles() throws Exception
    {
        // Legacy index file
        File legacyIndex = new File( storeDir, "legacyIndex" );
        createFileOfSize( legacyIndex, 1 );

        IndexImplementation indexImplementation = mock( IndexImplementation.class );
        when( indexImplementation.getIndexImplementationDirectory( any() ) ).thenReturn( legacyIndex );
        when( legacyIndexProviderLookup.all() ).thenReturn( iterable( indexImplementation ) );

        // Legacy index file
        File schemaIndex = new File( storeDir, "schemaIndex" );
        createFileOfSize( schemaIndex, 2 );
        when( schemaIndexProvider.getSchemaIndexStoreDirectory( any() ) ).thenReturn( schemaIndex );

        // Label scan store
        File labelScan = new File( storeDir, "labelScanStore" );
        createFileOfSize( labelScan, 4 );
        when( labelScanStore.getLabelScanStoreFile() ).thenReturn( labelScan );

        // Count all files
        assertEquals( 7, storeSizeBean.getIndexStoreSize() );
    }

    private void createFileOfSize( File file, int size ) throws IOException
    {
        try ( StoreChannel storeChannel = fs.create( file ) )
        {
            byte[] bytes = new byte[size];
            ByteBuffer buffer = ByteBuffer.wrap( bytes );
            storeChannel.writeAll( buffer );
        }
    }

    private long getExpected( int lower, int upper )
    {
        long expected = 0;
        for ( int i = lower; i <= upper; i++ )
        {
            expected += i;
        }
        return expected;
    }
}
