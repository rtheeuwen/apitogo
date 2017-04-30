package api.to.go.data;


import javaslang.collection.List;

import java.util.HashMap;
import java.util.Map;

import static api.to.go.data.ClassUtil.idValue;

public final class Data {

	private static final Map<Class<?>, Dao<?, ?>> daos = new HashMap<>();
	private Data(){

	}
	static <T> void addDao(Class<T> clazz, Dao<T, ?> dao){
		daos.put(clazz, dao);
	}

	public static <T, I> T byId(I id, Class<T> clazz){
		Dao<T, I> dao = (Dao<T, I>) daos.get(clazz);
		return dao.byId(id).get();
	}

	public static <T> List<T> all(Class<T> clazz){
		Dao<T, ?> dao = (Dao<T, ?>) daos.get(clazz);
		return dao.all().get();
	}

	public static <T> void update(T t){
		Dao<T, Object> dao = (Dao<T, Object>) daos.get(t.getClass());
		dao.updateLazily(idValue((Class<T>)t.getClass(), t).getClass(), t);
	}

	public static <T, I> void delete(I id, Class<T> clazz){
		Dao<T, Object> dao = (Dao<T, Object>) daos.get(clazz);
		dao.delete(id);
	}

}
