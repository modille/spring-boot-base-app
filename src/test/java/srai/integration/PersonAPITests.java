package srai.integration;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;

import javax.servlet.http.HttpServletResponse;

import org.flywaydb.test.annotation.FlywayTest;
import org.flywaydb.test.junit.FlywayTestExecutionListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;
import com.jayway.restassured.module.mockmvc.response.MockMvcResponse;

import srai.Application;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
	FlywayTestExecutionListener.class })
@IntegrationTest("server.port:0")
@WebAppConfiguration
@FlywayTest(locationsForMigrate = {"db/data"})
@Transactional
public class PersonAPITests {

	@Autowired
	private WebApplicationContext context;

	@Value("${local.server.port}")
	private int port;

	@Before
	public void setUp() {
		RestAssured.port = port;
		RestAssuredMockMvc.webAppContextSetup(context);
	}

	@Test
	public void getPersonRecordFixtureSuccess() {
		//@formatter:off
		given().
		log().all(true).
		when().
		get("/people/{person_id}", 1).
		then().
		log().all(true).
		contentType(ContentType.JSON).
		statusCode(HttpServletResponse.SC_OK).
		body("firstName", equalTo("Stuart"));
		//@formatter:on
	}

	@Test
	public void getPersonRecordFixtureMissing() {
		//@formatter:off
		given().
		when().
		get("/people/{person_id}", 0).
		then().
		statusCode(HttpServletResponse.SC_NOT_FOUND);
		//@formatter:on
	}

	@Test
	public void createPersonRecordValidationFailure() {
		// @formatter:off
		given().
		contentType(ContentType.JSON).
		body("{ \"firstName\" : \"Frodo\",  \"lastName\" : \"\" }").
		when().
		post("/people").
		then().
		contentType(ContentType.JSON).
		statusCode(HttpServletResponse.SC_BAD_REQUEST);
		// @formatter:on
	}

	@Test
	public void createPersonRecordSuccess() {
		MockMvcResponse response =
				// @formatter:off
				given().
				log().all(true).
				contentType(ContentType.JSON).
				body("{ \"firstName\" : \"Frodo\",  \"lastName\" : \"Baggins\" }").
				when().
				post("/people").
				then().
				log().all(true).
				contentType(ContentType.JSON).
				statusCode(HttpServletResponse.SC_CREATED).
				extract().response();

		int personId = response.path("id");

		given().
		when().
		get("/people/{person_id}", personId).
		then().
		contentType(ContentType.JSON).
		statusCode(HttpServletResponse.SC_OK).
		body("firstName", equalTo("bar_Frodo")).
		body("lastName", equalTo("Baggins"));

		// @formatter:on

	}
}
