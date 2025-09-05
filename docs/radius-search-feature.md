# Radius Search Feature Documentation

## Overview

The Radius Search feature allows you to search for positions within a specified radius around a central location. This is useful for use cases like "List every time I was at Detroit Zoo" or finding all device positions near a particular point of interest.

## API Endpoint

A new endpoint has been added to the Traccar API:

```
GET /api/positions/radius
```

### Query Parameters

| Parameter  | Type    | Required | Description |
|------------|---------|----------|-------------|
| latitude   | double  | Yes      | Latitude of the center point |
| longitude  | double  | Yes      | Longitude of the center point |
| radius     | double  | Yes      | Radius in meters (must be > 0) |
| deviceId   | long    | No       | Optional device ID to filter results |
| from       | date    | No       | Start date for search period |
| to         | date    | No       | End date for search period |

### Response

The endpoint returns a JSON array of Position objects that are within the specified radius.

### Permissions

- If a specific `deviceId` is provided, the user must have permission to access that device
- If no `deviceId` is provided, only positions from devices the user has access to will be returned
- If using `from` and `to` parameters, the user must have permission for reports

### Example Usage

#### Request

```
GET /api/positions/radius?latitude=42.48089&longitude=-83.18341&radius=500&deviceId=1&from=2023-01-01T00:00:00Z&to=2023-12-31T23:59:59Z
```

This request searches for positions of device ID 1 within a 500-meter radius of the Detroit Zoo (42.48089, -83.18341) during the year 2023.

#### Response

```json
[
  {
    "id": 1,
    "deviceId": 1,
    "protocol": "osmand",
    "serverTime": "2023-05-15T14:30:45Z",
    "deviceTime": "2023-05-15T14:30:40Z",
    "fixTime": "2023-05-15T14:30:40Z",
    "valid": true,
    "latitude": 42.4815,
    "longitude": -83.1832,
    "altitude": 280,
    "speed": 0,
    "course": 0,
    "address": "Detroit Zoo, Royal Oak, MI",
    "attributes": {}
  },
  {
    "id": 2,
    "deviceId": 1,
    "protocol": "osmand",
    "serverTime": "2023-05-15T16:45:22Z",
    "deviceTime": "2023-05-15T16:45:15Z",
    "fixTime": "2023-05-15T16:45:15Z",
    "valid": true,
    "latitude": 42.4805,
    "longitude": -83.1841,
    "altitude": 280,
    "speed": 0,
    "course": 0,
    "address": "Detroit Zoo, Royal Oak, MI",
    "attributes": {}
  }
]
```

## Technical Implementation

The feature uses the Haversine formula to calculate the distance between positions and the central point. The calculation is performed at the database level for optimal performance.

### Distance Formula

The distance between two coordinates (lat1, lon1) and (lat2, lon2) is calculated using:

```
d = R * acos(sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(lon1 - lon2))
```

Where R is the Earth's radius (approximately 6371 km or 6371000 meters).
