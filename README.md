**# Smart Campus API

**Student ID:** 20241400  
**Module:** Client-Server Architectures (5COSC022W)

This is a REST API for the Smart Campus project. It lets you manage rooms on campus and the sensors inside them. Sensors can record readings like temperature or CO2 levels. Everything is stored in memory using HashMaps with no database.

Built with Java 17, JAX-RS (Jersey), and a Grizzly embedded server.

## How to Run

You need Java 17 and Maven installed.

```bash
git clone https://github.com/Thanukamax/smart-campus-api-20241400.git
cd smart-campus-api-20241400
mvn clean compile
mvn exec:java
```

Server starts at `http://localhost:8080/api/v1/`.

## Endpoints

- `GET /api/v1` — API info and links
- `GET /api/v1/rooms` — all rooms
- `POST /api/v1/rooms` — create a room
- `GET /api/v1/rooms/{id}` — one room
- `DELETE /api/v1/rooms/{id}` — delete a room
- `GET /api/v1/sensors` — all sensors (can filter with `?type=`)
- `POST /api/v1/sensors` — create a sensor
- `GET /api/v1/sensors/{id}` — one sensor
- `GET /api/v1/sensors/{id}/readings` — readings for a sensor
- `POST /api/v1/sensors/{id}/readings` — add a reading

## Sample curl Commands

```bash
# 1. Check the API is running
curl http://localhost:8080/api/v1

# 2. Create a room
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'

# 3. See all rooms
curl http://localhost:8080/api/v1/rooms

# 4. Add a temperature sensor to that room
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","roomId":"LIB-301"}'

# 5. Filter sensors by type
curl "http://localhost:8080/api/v1/sensors?type=Temperature"

# 6. Post a reading
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":22.5}'

# 7. Get readings
curl http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

---

## Report

### Part 1 — Setup and Discovery

**Lifecycle of a JAX-RS Resource Class**

JAX-RS creates a new instance of each resource class for every request that comes in. So when someone sends a GET request to `/rooms`, Jersey makes a fresh `RoomResource` object, handles the request, and then throws that object away. This means you can't just store data in regular instance variables because they'd be gone by the next request.

To get around this, I used a singleton called `DataStore`. It gets created once when the app starts and stays in memory the whole time the server is running. All the resource classes share this single DataStore instance, so data sticks around between requests. It uses HashMaps to store rooms, sensors, and readings.

**Why HATEOAS matters**

HATEOAS is basically the idea of putting links in your API responses so clients can figure out where to go next without hardcoding URLs. My discovery endpoint at `GET /api/v1` does this — it returns links pointing to `/api/v1/rooms` and `/api/v1/sensors`. The benefit is that if I ever changed the URL structure, clients following those links would still work. It also makes the API easier to explore because you can start at the root and just follow links to find everything.

### Part 2 — Room Management

**Returning IDs vs full objects**

If you only return room IDs in a list, the response is smaller but the client has to make a separate request for each room to get the details. That's a lot of extra network calls (the N+1 problem). Returning full objects means a bigger response but the client gets everything in one go. For a campus with maybe a few hundred rooms, returning full objects makes more sense because the data isn't that big and it saves a lot of back-and-forth.

**Is DELETE idempotent?**

Yes. If you delete a room that exists (and has no sensors), you get 204 No Content. If you try to delete it again, the room is already gone, but you still get 204 — not an error. So calling DELETE multiple times has the same effect as calling it once. If the room has sensors attached, you get 409 Conflict every time, which is also consistent.

### Part 3 — Sensors

**What happens with wrong Content-Type**

If someone sends a POST request with `Content-Type: text/plain` instead of `application/json`, JAX-RS automatically rejects it with a 415 Unsupported Media Type error. The request never even reaches my code — the framework handles it during routing. This is useful because it prevents bad data from getting into the business logic.

**Query params vs path-based filtering**

I used `?type=CO2` as a query parameter instead of putting it in the URL path like `/sensors/type/CO2`. Query parameters make more sense for filtering because they're optional (you can leave them out to get everything), they combine easily (`?type=CO2&status=ACTIVE`), and they don't mess up the URL hierarchy. Putting filter values in the path makes it look like they're sub-resources, which they're not.

### Part 4 — Sub-Resources

**Benefits of the sub-resource locator pattern**

Instead of putting all the reading-related endpoints inside `SensorResource`, I have a method that returns a `SensorReadingResource` object. This keeps things organized — sensor stuff in one class, reading stuff in another. Without this pattern, `SensorResource` would have methods for sensors AND readings, which gets messy fast. The locator method also passes the sensor ID to the sub-resource class, so it always knows which sensor it's working with.

### Part 5 — Error Handling and Logging

**Why stack traces are dangerous**

If an API returns a raw Java stack trace to the user, it leaks a lot of information. Attackers can see what classes and libraries you're using, what versions they are, and sometimes even database table names from SQL exceptions. They can use this to look up known vulnerabilities. My `GenericExceptionMapper` catches all unhandled exceptions, logs the details on the server side for debugging, and just sends back a generic "something went wrong" message to the client.

**Why filters are better than manual logging**

I used a JAX-RS filter class that implements `ContainerRequestFilter` and `ContainerResponseFilter`. This automatically logs every incoming request and every outgoing response in one place. The alternative would be adding `Logger.info()` calls inside every single resource method, which is tedious and easy to forget. With the filter approach, all logging happens in one class and resource methods can focus on the actual business logic.
**
