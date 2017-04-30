package api.to.go;


import api.to.go.model.ContactService;
import api.to.go.model.Person;

public class Example {

	public static void main(String[] args){

		API.toGo(Person.class, ContactService.class);

	}
}
