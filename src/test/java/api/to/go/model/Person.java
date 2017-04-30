package api.to.go.model;

import java.util.List;

@Resource
public class Person implements Validable{

	@Id
	private long id;

	private String name;

	@OneToMany
	private List<Contact> contacts;

	public Person(){

	}

	public long getId() {
		return id;
	}

	public Person(String name, long id, javaslang.collection.List<Contact> contacts) {
		this.name = name;
		this.id = id;
		this.contacts = contacts.toJavaList();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setId(long id){
		this.id = id;
	}

	public List<Contact> getContacts() {
		return contacts;
	}

	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}

	@Override
	public boolean isValid() {
		return !name.trim().isEmpty();
	}

	@Override
	public String toString(){
		return String.format("[Person: %s, %s]", name, id);
	}

	@Override
	public int hashCode(){
		return (int) id;
	}

	@Override
	public boolean equals(Object o){
		return o instanceof Person && o.hashCode() == this.hashCode() && ((Person) o).name.equals(name);
	}
}
