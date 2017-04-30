package api.to.go.model;

import java.util.List;

@Resource
public class Contact {

	@Id
	private long id;
	private String name;

	@OneToMany
	private List<Phone> phones;

	public long getId() {
		return id;
	}

	public Contact(){

	}

	public Contact(String name, javaslang.collection.List<Phone> phones){
		this.name = name;
		this.phones = phones.toJavaList();
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Phone> getPhones() {
		return phones;
	}

	public void setPhones(List<Phone> phones) {
		this.phones = phones;
	}

	@Override
	public int hashCode(){
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o){
		return o instanceof Contact && o.hashCode() == this.hashCode() && ((Contact) o).name.equals(this.name);
	}
}
