package api.to.go;

import api.to.go.data.*;
import api.to.go.rest.ServiceController;
import javaslang.collection.List;
import api.to.go.rest.Controller;
import api.to.go.rest.ControllerDetail;

import java.util.Arrays;

import static api.to.go.data.ClassUtil.detailClasses;
import static api.to.go.data.ClassUtil.isResource;
import static api.to.go.data.ClassUtil.isService;
import static api.to.go.data.Dao.masterDetailDao;
import static api.to.go.rest.IdConverter.getIdClass;
import static spark.Spark.internalServerError;
import static spark.Spark.notFound;
import static spark.Spark.port;

public class API<T, I> {

	static{
		notFound("");
		internalServerError("");
		port(8080);
	}

	public static void toGo(Class<?>... classes){
		Arrays.stream(classes).forEach(API::new);
	}

	private API(Class<T> clazz){

		if(isResource(clazz)) {
			MasterDao<T, I> dao = Dao.crud(clazz, getIdClass(clazz));
			new Controller<T, I>(clazz)
					.getOne(dao::byIdLazily)
					.getAll(dao::allLazily)
					.post(dao::create)
					.put(dao::updateLazily)
					.delete(dao::delete);

			recursiveApiForDetail(List.of(clazz), detailClasses(clazz), (Dao) dao);
		} else if(isService(clazz)){
			ServiceController.of(clazz);
		} else {
			throw new IllegalArgumentException(String.format("%s is neither a resource nor a service", clazz.getSimpleName()));
		}
	}

	@SuppressWarnings("unchecked")
	private <A, B, C, D> void recursiveApiForDetail(List<Class<?>> masterClasses, List<Class<?>> detailClasses, Dao masterDao){

		masterDao.setDetailDaos(detailClasses.map(detail -> {
			MasterDetailDao<A, B, C, D> dao = masterDetailDao((Class<A>)detail, (Class<B>)getIdClass(detail), (Class<C>)masterClasses.head(), (Class<D>)masterDao.idClass(), (Dao<C, D>)masterDao);
			new ControllerDetail<A, B, C, D>((Class<A>) detail, masterClasses)
					.getOne(dao::byId)
					.getAll(dao::all)
					.post(dao::create)
					.put(dao::update)
					.delete(dao::delete);

			recursiveApiForDetail(masterClasses.push(detail), detailClasses(detail), dao);
			return dao;
		}));

	}
}
