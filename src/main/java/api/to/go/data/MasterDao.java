package api.to.go.data;


import javaslang.collection.List;
import javaslang.control.Option;

public interface MasterDao<T, I> extends Dao<T, I>{

	//eager
	Option<I> create(T t);
	Option<T> byIdLazily(I i);
	Option<List<T>> allLazily();

}
