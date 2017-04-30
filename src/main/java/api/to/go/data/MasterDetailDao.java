package api.to.go.data;


import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.control.Option;
import org.sql2o.Connection;

public interface MasterDetailDao<T, I, M, J> extends Dao<T, I>{

	Option<?> cascadedCreate(J masterId, Object details, Connection con);
	Option<?> cascadedDelete(J masterId, Connection con);
	List<?> byMasterIds(List<J> masterIds, Connection con);

	Option<T> byId(List<Tuple2<Class<?>, Object>> ids);
	Option<List<T>> all(List<Tuple2<Class<?>, Object>> ids);
	Option<I> create(T t, List<Tuple2<Class<?>, Object>> ids);
	Option<I> create(T t, J masterId);
	Option<?> update(List<Tuple2<Class<?>, Object>> ids, T t);
	Option<?> update(T t);
	Option<?> delete(List<Tuple2<Class<?>, Object>> ids);
}
