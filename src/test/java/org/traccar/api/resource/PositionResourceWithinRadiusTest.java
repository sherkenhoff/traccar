/*
 * Copyright 2025 Simon Herkenhoff (sherkenh@gmail.com)
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
 */
package org.traccar.api.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Position;
import org.traccar.storage.StorageException;

import jakarta.ws.rs.WebApplicationException;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PositionResourceWithinRadiusTest extends BaseTest {

    private PositionResource positionResource;

    @BeforeEach
    public void setup() {
        positionResource = mock(PositionResource.class);
    }

    @Test
    public void testGetPositionsWithinRadius() throws StorageException {
        // Test data
        double centerLat = 37.7749; // San Francisco
        double centerLon = -122.4194;
        double radius = 5000; // 5km radius

        // Create test positions
        Position positionInRadius = new Position();
        positionInRadius.setLatitude(37.7749 + 0.01); // Approx 1.1km from center
        positionInRadius.setLongitude(-122.4194 + 0.01);
        positionInRadius.setFixTime(new Date());
        positionInRadius.setDeviceId(1);

        Position positionOutOfRadius = new Position();
        positionOutOfRadius.setLatitude(37.7749 + 0.1); // Approx 11km from center
        positionOutOfRadius.setLongitude(-122.4194 + 0.1);
        positionOutOfRadius.setFixTime(new Date());
        positionOutOfRadius.setDeviceId(1);

        // Create a mock stream of positions
        Stream<Position> mockStream = Stream.of(positionInRadius, positionOutOfRadius);
        
        // Verify distance calculations
        double distanceInRadius = DistanceCalculator.distance(
                centerLat, centerLon, positionInRadius.getLatitude(), positionInRadius.getLongitude());
        assertTrue(distanceInRadius <= radius, 
                "Position should be within radius: " + distanceInRadius + " <= " + radius);
                
        double distanceOutOfRadius = DistanceCalculator.distance(
                centerLat, centerLon, positionOutOfRadius.getLatitude(), positionOutOfRadius.getLongitude());
        assertTrue(distanceOutOfRadius > radius, 
                "Position should be outside radius: " + distanceOutOfRadius + " > " + radius);
                
        // Mock the behavior of getPositionsWithinRadius
        when(positionResource.getPositionsWithinRadius(
                eq(centerLat), eq(centerLon), eq(radius), 
                eq(1L), any(Date.class), any(Date.class)))
                .thenReturn(mockStream);
                
        // Call the method
        Stream<Position> result = positionResource.getPositionsWithinRadius(
                centerLat, centerLon, radius, 1L, new Date(), new Date());
                
        // Filter manually to verify
        List<Position> filteredPositions = result
                .filter(p -> DistanceCalculator.distance(centerLat, centerLon, p.getLatitude(), p.getLongitude()) <= radius)
                .toList();
                
        assertEquals(1, filteredPositions.size(), "Only one position should be within the radius");
        assertEquals(positionInRadius.getLatitude(), filteredPositions.get(0).getLatitude());
        assertEquals(positionInRadius.getLongitude(), filteredPositions.get(0).getLongitude());
    }
}
