import Entities.Booking;
import Entities.BookingDates;
import Entities.User;
import com.github.javafaker.Faker;
import io.github.cdimascio.dotenv.Dotenv;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.*;
import static org.hamcrest.Matchers.*;

public class BookingTests {
    public static Faker faker;
    public static String token;
    private static RequestSpecification request;
    private static Booking booking;

    @BeforeAll
    public static void Setup(){
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
        faker = new Faker();
        User user = new User(
                faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                faker.internet().safeEmailAddress(),
                faker.internet().password(8, 10),
                faker.phoneNumber().toString());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date checkin = faker.date().future(90, 1, TimeUnit.DAYS);
        Date checkout = faker.date().future(90, TimeUnit.DAYS, checkin);
        BookingDates bookingDates = new BookingDates(dateFormat.format(checkin), dateFormat.format(checkout));
        booking = new Booking(
            user.getFirstName(),
            user.getLastName(),
            (float)faker.number().randomDouble(2, 50, 100000),
            true,
                bookingDates,
            "");
        RestAssured.filters(new RequestLoggingFilter(),new ResponseLoggingFilter(), new ErrorLoggingFilter());

        Dotenv dotenv = Dotenv.load();
        String username = dotenv.get("USUARIO");
        String password = dotenv.get("SENHA");


        request = given().config(RestAssured.config().logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails()))
            .contentType(ContentType.JSON)
            .auth().basic(username, password);

        JSONObject tokenAuthRequestBody = new JSONObject()
            .put("username", username)
            .put("password", password);

        token = request
            .when()
                .body(tokenAuthRequestBody.toString())
                .post("/auth")
            .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    @Test
    public void getAllBookingsById_returnOk(){
        Response response = request
            .when()
                .get("/booking")
            .then()
                .extract()
                .response();


        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test
    public void  getAllBookingsByUserFirstName_BookingExists_returnOk(){
        request
            .when()
                .queryParam("firstName", "Carol")
                .get("/booking")
            .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
            .and()
                .body("results", hasSize(greaterThan(0)));

    }

    @Test
    public void  CreateBooking_WithValidData_returnOk(){
        request
            .contentType(ContentType.JSON)
                .when()
                    .body(booking)
                    .post("/booking")
                .then()
                    .body(matchesJsonSchemaInClasspath("createBookingRequestSchema.json"))
                .and()
                    .assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON).and().time(lessThan(2000L));
    }

}
