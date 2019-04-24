/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
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
package org.neo4j.kernel.impl.storemigration;

import java.io.File;
import java.nio.file.OpenOption;
import java.util.EnumMap;
import java.util.function.LongSupplier;

import org.neo4j.internal.id.IdGenerator;
import org.neo4j.internal.id.IdGeneratorFactory;
import org.neo4j.internal.id.IdType;

/**
 * {@link IdGeneratorFactory} that ignores the underlying id file and only uses the provided highIdScanner in
 * {@link #open(File, IdType, LongSupplier, long, OpenOption...)}, instantiating {@link IdGenerator} that will return that highId
 * and do nothing else. This is of great convenience when migrating between id file formats.
 */
class ScanOnOpenReadOnlyIdGeneratorFactory implements IdGeneratorFactory
{
    private final EnumMap<IdType,ReadOnlyHighIdGenerator> idGenerators = new EnumMap<>( IdType.class );

    @Override
    public IdGenerator open( File filename, IdType idType, LongSupplier highIdScanner, long maxId, OpenOption... openOptions )
    {
        long highId = highIdScanner.getAsLong();
        ReadOnlyHighIdGenerator idGenerator = new ReadOnlyHighIdGenerator( highId );
        idGenerators.put( idType, idGenerator );
        return idGenerator;
    }

    @Override
    public IdGenerator create( File filename, IdType idType, long highId, boolean throwIfFileExists, long maxId, OpenOption... openOptions )
    {
        return open( filename, idType, () -> highId, maxId );
    }

    @Override
    public IdGenerator get( IdType idType )
    {
        ReadOnlyHighIdGenerator idGenerator = idGenerators.get( idType );
        if ( idGenerator == null )
        {
            throw new IllegalStateException( "IdGenerator for " + idType + " not opened" );
        }
        return idGenerator;
    }
}
