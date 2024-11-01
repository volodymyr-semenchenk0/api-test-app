package org.example;

import static io.restassured.RestAssured.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AirportGapAPITests {

    private final String token = "8EYv6GfqXZTi5A3ifSWYW1Fy";

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "https://airportgap.com";
        RestAssured.basePath = "/api";
    }

    /**
     * Test to verify that the list of airports can be retrieved successfully.
     * Asserts that the response status is 200.
     */
    @Test
    public void testGetAirportsList() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/airports")
                .then()
                .statusCode(200);
    }

    /**
     * Test to retrieve a specific airport by ID.
     * Asserts that the airport with ID "GKA" is retrieved successfully
     * and the response contains the expected ID.
     */
    @Test
    public void testGetSpecificAirport() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/airports/GKA")
                .then().log().all()
                .statusCode(200)
                .body("data.id", equalTo("GKA"));
    }

    /**
     * Test to calculate the distance between two airports (from KIX to NRT).
     * Asserts that the response status is 200 and contains the expected distance ID.
     */
    @Test
    public void testCalculateDistanceBetweenAirports() {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("from", "KIX");
        requestParams.put("to", "NRT");

        given()
                .contentType(ContentType.JSON)
                .body(requestParams)
                .when()
                .post("/airports/distance")
                .then().log().all()
                .statusCode(200)
                .body("data.id", equalTo("KIX-NRT"));
    }

    /**
     * Test to delete all favorite airports.
     * Ensures that the delete operation returns a 204 No Content status.
     */
    @Test
    @Order(1)
    public void testDeleteAllFavoriteAirports() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/favorites/clear_all")
                .then().log().all()
                .statusCode(204);
    }

    /**
     * Test to create a new favorite airport with a specific ID.
     * Asserts that the airport is created successfully with a 201 status.
     */
    @Test
    @Order(2)
    public void testCreateFavoriteAirportByID() {
        Map<String, String> airport = new HashMap<>();
        airport.put("airport_id", "KGA");
        airport.put("note", "Best airport.");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(airport)
                .when()
                .post("/favorites")
                .then().log().all()
                .statusCode(201)
                .body("data.type", equalTo("favorite"));
    }

    /**
     * Test to attempt adding a duplicate favorite airport.
     * Asserts that the response returns a 422 status and an appropriate error message.
     */
    @Test
    @Order(3)
    public void testCreateFavoriteAirportWithExistingID() {
        Map<String, String> airport = new HashMap<>();
        airport.put("airport_id", "KGA");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(airport)
                .when()
                .post("/favorites")
                .then().log().all()
                .statusCode(422)
                .body("errors.detail", hasItem("Airport This airport is already in your favorites"));
    }

    /**
     * Test to edit the note of an already added favorite airport.
     * Retrieves the airport ID, updates the note to an empty value,
     * and asserts that the note is successfully updated.
     */
    @Test
    @Order(4)
    public void testEditNoteInAddedFavoriteAirport() {
        Map<String, String> note = new HashMap<>();
        note.put("note", "");

        int favoriteId = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/favorites")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getInt("data[0].id");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(note)
                .when()
                .patch("/favorites/" + favoriteId)
                .then().log().all()
                .statusCode(200)
                .body("data.note", equalTo(null));
    }

    /**
     * Test to attempt accessing a protected resource without a token.
     * Ensures that the response returns a 401 Unauthorized status.
     */
    @Test
    public void testAccessWithoutToken() {
        given()
                .when()
                .get("/favorites")
                .then().log().all()
                .statusCode(401);
    }

    /**
     * Test to attempt accessing a protected resource with an expired token.
     * Ensures that the response returns a 401 Unauthorized status.
     */
    @Test
    public void testAccessWithExpiredToken() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer PUtUvweA64c32bXqnSzfREZY")
                .when()
                .get("/favorites")
                .then().log().all()
                .statusCode(401);
    }

    /**
     * Test to retrieve an authorization token using credentials.
     * Asserts that the token request is successful with a 200 status.
     */
    @Test
    public void testReturnAuthorizationToken() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", "semenchenko.volodymyr1118@vu.cdu.edu.ua");
        credentials.put("password", "qwerty12");

        given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when()
                .post("/tokens")
                .then().log().all()
                .statusCode(200);
    }

    /**
     * Test to retrieve the list of favorite airports with a valid authorization token.
     * Ensures that the response is successful with a 200 status.
     */
    @Test
    public void testGetFavoritesAirportsWithAuthorizationToken() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/favorites")
                .then().log().all()
                .statusCode(200);
    }

    /**
     * Test to verify the presence of pagination links in the response.
     * Asserts that links like first, last, next, and prev are present in the response.
     */
    @Test
    public void testPaginationLinksPresence() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("page", 1)
                .when()
                .get("/airports")
                .then().log().all()
                .statusCode(200)
                .body("links", allOf(
                        hasKey("first"),
                        hasKey("last"),
                        hasKey("next"),
                        hasKey("prev")
                ));
    }

    /**
     * Test to check that the number of items on a page does not exceed 30.
     * Asserts that the response contains at most 30 items.
     */
    @Test
    public void testMaxItemsPerPage() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("page", 1)
                .when()
                .get("/airports")
                .then()
                .statusCode(200)
                .body("data.size()", lessThanOrEqualTo(30));
    }

    /**
     * Test to verify that data on different pages is unique.
     * Retrieves data from two different pages and asserts that they do not contain the same items.
     */
    @Test
    public void testDataChangesBetweenPages() {
        Response firstPageResponse = given()
                .contentType(ContentType.JSON)
                .queryParam("page", 1)
                .when()
                .get("/airports")
                .then()
                .statusCode(200)
                .extract().response();

        List<String> firstPageIds = firstPageResponse.jsonPath().getList("data.id");

        Response secondPageResponse = given()
                .contentType(ContentType.JSON)
                .queryParam("page", 2)
                .when()
                .get("/airports")
                .then()
                .statusCode(200)
                .extract().response();

        List<String> secondPageIds = secondPageResponse.jsonPath().getList("data.id");

        assertThat("The data on the first and second pages must be different",
                firstPageIds, not(equalTo(secondPageIds)));
    }
}
