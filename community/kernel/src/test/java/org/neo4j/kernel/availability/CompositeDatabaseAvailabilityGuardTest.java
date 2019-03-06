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
package org.neo4j.kernel.availability;

import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.time.Clock;

import org.neo4j.logging.internal.NullLogService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;
import static org.neo4j.configuration.GraphDatabaseSettings.SYSTEM_DATABASE_NAME;

class CompositeDatabaseAvailabilityGuardTest
{

    private final DescriptiveAvailabilityRequirement requirement = new DescriptiveAvailabilityRequirement( "testRequirement" );
    private CompositeDatabaseAvailabilityGuard compositeGuard;
    private DatabaseAvailabilityGuard defaultGuard;
    private DatabaseAvailabilityGuard systemGuard;
    private Clock mockClock;

    @BeforeEach
    void setUp()
    {
        mockClock = mock( Clock.class );
        compositeGuard = new CompositeDatabaseAvailabilityGuard( mockClock, NullLogService.getInstance() );
        defaultGuard = compositeGuard.createDatabaseAvailabilityGuard( DEFAULT_DATABASE_NAME );
        systemGuard = compositeGuard.createDatabaseAvailabilityGuard( SYSTEM_DATABASE_NAME );
    }

    @Test
    void availabilityRequirementOnMultipleGuards()
    {
        assertTrue( defaultGuard.isAvailable() );
        assertTrue( systemGuard.isAvailable() );

        compositeGuard.require( new DescriptiveAvailabilityRequirement( "testRequirement" ) );

        assertFalse( defaultGuard.isAvailable() );
        assertFalse( systemGuard.isAvailable() );
    }

    @Test
    void availabilityFulfillmentOnMultipleGuards()
    {
        compositeGuard.require( requirement );

        assertFalse( defaultGuard.isAvailable() );
        assertFalse( systemGuard.isAvailable() );

        compositeGuard.fulfill( requirement );

        assertTrue( defaultGuard.isAvailable() );
        assertTrue( systemGuard.isAvailable() );
    }

    @Test
    void availableWhenAllGuardsAreAvailable()
    {
        assertTrue( compositeGuard.isAvailable() );

        defaultGuard.require( requirement );

        assertFalse( compositeGuard.isAvailable() );
    }

    @Test
    void compositeGuardDoesNotSupportShutdownCheck()
    {
        assertThrows( UnsupportedOperationException.class, () -> compositeGuard.isShutdown() );
    }

    @Test
    void compositeGuardDoesNotSupportListeners()
    {
        AvailabilityListener listener = mock( AvailabilityListener.class );
        assertThrows( UnsupportedOperationException.class, () -> compositeGuard.addListener( listener ) );
        assertThrows( UnsupportedOperationException.class, () -> compositeGuard.removeListener( listener ) );
    }

    @Test
    void availabilityTimeoutSharedAcrossAllGuards()
    {
        compositeGuard.require( requirement );
        MutableLong counter = new MutableLong(  );

        when( mockClock.millis() ).thenAnswer( (Answer<Long>) invocation ->
        {
            if ( counter.longValue() == 7 )
            {
                defaultGuard.fulfill( requirement );
            }
            return counter.incrementAndGet();
        } );

        assertFalse( compositeGuard.isAvailable( 10 ) );

        assertThat( counter.getValue(), lessThan( 20L ) );
        assertTrue( defaultGuard.isAvailable() );
        assertFalse( systemGuard.isAvailable() );
    }

    @Test
    void awaitCheckTimeoutSharedAcrossAllGuards()
    {
        compositeGuard.require( requirement );
        MutableLong counter = new MutableLong(  );

        when( mockClock.millis() ).thenAnswer( (Answer<Long>) invocation ->
        {
            if ( counter.longValue() == 7 )
            {
                defaultGuard.fulfill( requirement );
            }
            return counter.incrementAndGet();
        } );

        assertThrows( UnavailableException.class, () -> compositeGuard.await( 10 ) );

        assertThat( counter.getValue(), lessThan( 20L ) );
        assertTrue( defaultGuard.isAvailable() );
        assertFalse( systemGuard.isAvailable() );
    }
}
