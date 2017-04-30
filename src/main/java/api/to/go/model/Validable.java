package api.to.go.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Validable {

	@JsonIgnore
	default boolean isValid(){
		return true;
	}

}
