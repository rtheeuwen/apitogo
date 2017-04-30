package api.to.go.model;

@Resource
public class Phone {

	@Id
	private long id;
	private long number;

	public Phone(){

	}

	public Phone(long phone){
		this.number = phone;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getNumber() {
		return number;
	}

	public void setNumber(long number) {
		this.number = number;
	}

	@Override
	public String toString(){
		return String.valueOf(number);
	}

	@Override
	public int hashCode(){
		return (int) id;
	}

	@Override
	public boolean equals(Object o){
		return o instanceof Phone && o.hashCode() == this.hashCode() && ((Phone) o).number==number;
	}
}
