package api.to.go.data;


import api.to.go.rest.IdConverter;
import javaslang.Tuple2;
import javaslang.collection.List;

public interface ExistenceValidator<T, I> {

	boolean exists(List<Tuple2<Class<?>, Object>> ids);
	IdConverter<I> getIdConverter();
	Class<I> idClass();

}
