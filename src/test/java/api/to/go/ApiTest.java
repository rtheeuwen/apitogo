package api.to.go;


import api.to.go.model.Contact;
import api.to.go.model.ContactService;
import api.to.go.model.Person;
import api.to.go.model.Phone;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javaslang.collection.List;
import javaslang.jackson.datatype.JavaslangModule;
import org.javalite.http.Http;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import static org.junit.Assert.*;
import static spark.Spark.awaitInitialization;
import static spark.Spark.stop;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApiTest {

	private static final ObjectMapper mapper = new ObjectMapper();
	static {mapper.registerModule(new JavaslangModule());}

	@BeforeClass
	public static void setup() throws InterruptedException {
		API.toGo(Person.class, ContactService.class);
		awaitInitialization();
	}

	@Test
	public void test0() throws IOException {
		String json = Http.get("http://localhost:8080/persons").text();
		List<Person> persons = mapper.readValue(json, new TypeReference<List<Person>>() {});
		assertTrue(persons.size()==0);

		String id = Http.post("http://localhost:8080/persons", mapper.writeValueAsBytes(new Person("Test", 666, List.empty()))).text();
		assertEquals("1", id);

		json = Http.get("http://localhost:8080/persons").text();
		persons = mapper.readValue(json, new TypeReference<List<Person>>() {});
		assertTrue(persons.size()==1);
		assertEquals("Test", persons.head().getName());

		json = Http.get("http://localhost:8080/persons/1").text();
		Person person = mapper.readValue(json, Person.class);
		assertEquals("Test", person.getName());

		Http.delete("http://localhost:8080/persons/1").text();
		json = Http.get("http://localhost:8080/persons").text();
		persons = mapper.readValue(json, new TypeReference<List<Person>>() {});
		assertTrue(persons.size()==0);
	}

	@Test
	public void test1() throws IOException{
		Person p1 = new Person("Person1", 0,
				List.of(new Contact("A", List.of(new Phone(123), new Phone(345))),
						new Contact("B", List.of(new Phone(567), new Phone(789)))));

		Person p2 = new Person("Person2", 0,
				List.of(new Contact("C", List.of(new Phone(321), new Phone(543))),
						new Contact("D", List.of(new Phone(765), new Phone(987)))));


		String json = Http.get("http://localhost:8080/persons").text();
		List<Person> persons = mapper.readValue(json, new TypeReference<List<Person>>() {});
		assertTrue(persons.size()==0);

		String id1 = Http.post("http://localhost:8080/persons",
				"{\"id\":0,\"name\":\"Person1\",\"contacts\":[{\"id\":0,\"name\":\"A\",\"phones\":[{\"id\":0,\"" +
						"number\":123},{\"id\":0,\"number\":345}]},{\"id\":0,\"name\":\"B\",\"phones\":[{\"id\"" +
						":0,\"number\":567},{\"id\":0,\"number\":789}]}]}").text();

		p1.setId(Long.valueOf(id1));
		String id2 = Http.post("http://localhost:8080/persons", "{\"id\":0,\"name\":\"Person2\",\"contacts\":[{\"" +
				"id\":0,\"name\":\"C\",\"phones\":[{\"id\":0,\"number\":321},{\"id\":0,\"number\":543}]},{\"id\"" +
				":0,\"name\":\"D\",\"phones\":[{\"id\":0,\"number\":765},{\"id\":0,\"number\":987}]}]}").text();

		p2.setId(Long.valueOf(id2));

		Http.delete("http://localhost:8080/persons/"+id1).text();
		assertEquals(404, Http.get("http://localhost:8080/persons/"+id1).doConnect().responseCode());

		json = Http.get("http://localhost:8080/persons/"+id2).text();
		Person result = mapper.readValue(json, Person.class);
		assertEquals(p2, result);

		json = Http.get("http://localhost:8080/persons/"+id2+"/contacts").text();
		List<Contact> contacts = mapper.readValue(json, new TypeReference<List<Contact>>() {});
		assertEquals(2, contacts.size());
		assertEquals("C", contacts.get(0).getName());
		assertEquals("D", contacts.get(1).getName());

		String cId = Http.post("http://localhost:8080/persons/"+id2+"/contacts", mapper.writeValueAsBytes(new Contact("E", List.empty()))).text();
		json = Http.get("http://localhost:8080/persons/"+id2+"/contacts").text();
		contacts = mapper.readValue(json, new TypeReference<List<Contact>>() {});
		assertEquals(3, contacts.size());

		json = Http.get("http://localhost:8080/persons/"+id2+"/contacts/"+cId).text();
		Contact contact = mapper.readValue(json, Contact.class);
		assertEquals("E", contact.getName());

		Http.delete("http://localhost:8080/persons/"+id2 + "/contacts/" + cId).text();
		assertEquals(404, Http.get("http://localhost:8080/persons/"+id2 + "/contacts/" + cId).doConnect().responseCode());
		assertEquals(200, Http.get("http://localhost:8080/persons/"+id2).responseCode());
	}

	@Test
	public void test2() throws Exception{
		String id = Http.post("http://localhost:8080/persons",
				"{\"id\":0,\"name\":\"Person1\",\"contacts\":[{\"id\":0,\"name\":\"A\",\"phones\":[{\"id\":0,\"" +
						"number\":123},{\"id\":0,\"number\":345}]},{\"id\":0,\"name\":\"B\",\"phones\":[{\"id\"" +
						":0,\"number\":567},{\"id\":0,\"number\":789}]}]}").text();

		assertEquals("2", Http.get("http://localhost:8080/numberOfContacts?person=" + id).text());
	}

	@Test
	public void test3() throws Exception{
		String id1 = Http.post("http://localhost:8080/persons",
				"{\"id\":0,\"name\":\"Person1\",\"contacts\":[{\"id\":0,\"name\":\"A\",\"phones\":[{\"id\":0,\"" +
						"number\":123},{\"id\":0,\"number\":345}]},{\"id\":0,\"name\":\"B\",\"phones\":[{\"id\"" +
						":0,\"number\":567},{\"id\":0,\"number\":789}]}]}").text();

		String id2 = Http.post("http://localhost:8080/persons",
				"{\"id\":0,\"name\":\"Person1\",\"contacts\":[{\"id\":0,\"name\":\"A\",\"phones\":[{\"id\":0,\"" +
						"number\":123},{\"id\":0,\"number\":345}]},{\"id\":0,\"name\":\"C\",\"phones\":[{\"id\"" +
						":0,\"number\":567},{\"id\":0,\"number\":789}]}]}").text();

		String json = Http.get("http://localhost:8080/contactsInCommon?1="+id1+"&2="+id2).text();
		List<Contact> contacts = mapper.readValue(json, new TypeReference<List<Contact>>() {});
		assertEquals(1, contacts.size());
	}

	@AfterClass
	public static void shutdown(){
		stop();
	}
}
