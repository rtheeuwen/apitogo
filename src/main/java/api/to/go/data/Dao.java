package api.to.go.data;

import javaslang.collection.List;
import javaslang.control.Option;

public interface Dao <T, I> extends ExistenceValidator<T, I>{

	//lazy
	Option<?> updateLazily(I id, T t);

	//eager
	Option<?> delete(I id);

	//eager
	Option<T> byId(I i);

	//eager
	Option<List<T>> all();

	Class<T> typeClass();
	Class<I> idClass();

	void setDetailDaos(List<MasterDetailDao<?, ?, T, I>> detailDaos);

	static <T, I> MasterDao<T, I> crud(Class<T> clazz, Class<I> idClass){
		MasterDao<T, I> dao = new MasterDaoImpl<T, I>(clazz, idClass);
		Data.addDao(clazz, dao);
		return dao;
	}

	static <T, I, M, J> MasterDetailDao<T, I, M, J> masterDetailDao(Class<T> clazz, Class<I> idClass, Class<M> masterClazz, Class<J> masterIdClass, Dao<M, J> existenceValidator){
		MasterDetailDao<T, I, M, J> dao = new MasterDetailDaoImpl(clazz, idClass, masterClazz, masterIdClass, existenceValidator);
		Data.addDao(clazz, dao);
		return dao;
	}

}
