package api.to.go.model;

import api.to.go.data.Data;
import javaslang.collection.List;

@Service
public class ContactService {

	@Action
	public long numberOfContacts(@Param(name="person") long id){
		Person person = Data.byId(id, Person.class);
		return person.getContacts().size();
	}

	@Action
	public List<Contact> contactsInCommon(@Param(name="1") long id1, @Param(name="2") long id2){
		Person p1 = Data.byId(id1, Person.class);
		Person p2 = Data.byId(id2, Person.class);

		return List.ofAll(p1.getContacts()).filter(c -> p2.getContacts().contains(c));
	}

}
