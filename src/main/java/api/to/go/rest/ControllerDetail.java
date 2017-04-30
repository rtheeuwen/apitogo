package api.to.go.rest;


import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.collection.Stream;
import javaslang.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Spark;

import java.util.function.BiFunction;
import java.util.function.Function;

import static api.to.go.rest.IdConverter.getIdClass;
import static spark.Spark.after;

public class ControllerDetail<T, I, M, J>{

	private final Serializer<T> serializer;
	private final String endpoint;
	private final IdConverter<I> idConverter;
	private static final List<String> parameters = Stream.iterate('a', c -> ++c).take(26).map(String::valueOf).toList();
	private final List<Class<?>> classes;

	private static final Logger log = LoggerFactory.getLogger(ControllerDetail.class);

	public ControllerDetail(Class<T> clazz, List<Class<?>> masterClasses, Serializer.ContentType contentType){
		this.classes = masterClasses.push(clazz);
		this.idConverter = IdConverter.<I>getInstance(getIdClass(clazz));
		this.serializer = new Serializer<>(clazz, contentType);
		this.endpoint = endpoint(clazz, masterClasses);
		after(((req, res) -> res.type(contentType.value())));
	}

	public ControllerDetail(Class<T> clazz, List<Class<?>> masterClasses){
		this(clazz, masterClasses, Serializer.ContentType.Json);
	}

	public ControllerDetail<T, I, M, J>getAll(Function<List<Tuple2<Class<?>, Object>>, Option<List<T>>> f){
		Spark.get(endpoint, (req, res) -> {
			Option<List<T>> option = f.apply(requestParams(req, classes.tail()));
			if(option.isDefined())
				return option.get();
			else{
				res.status(404);
				return Void.TYPE;
			}
		}, serializer::marshall);
		log.info(String.format("GET at %s", endpoint));
		return this;
	}

	public ControllerDetail<T, I, M, J> getOne(Function<List<Tuple2<Class<?>, Object>>, Option<T>> f){
		Spark.get(endpoint + "/:id", (req, res) -> {
			Option<T> option = f.apply(requestParams(req, classes.tail()).push(new Tuple2<>(classes.head(), req.params(":id"))));
			if(option.isDefined())
				return option.get();
			else{
				res.status(404);
				return Void.TYPE;
			}
		}, serializer::marshall);
		log.info(String.format("GET at %s/{id}", endpoint));
		return this;
	}

	public ControllerDetail<T, I, M, J> post(BiFunction<T, List<Tuple2<Class<?>, Object>>, Option<I>> f){
		Spark.post(endpoint, serializer.contentType(),
				(req, res) -> {
					Option<I> option = serializer.unmarshall(req).flatMap(t -> f.apply(t, requestParams(req, classes.tail())));
					if(option.isDefined()){
						res.status(200);
						return option.get();
					} else {
						res.status(400);
						return Void.TYPE;
					}
				}, serializer::marshall);
		log.info(String.format("POST at %s", endpoint));
		return this;
	}

	public ControllerDetail<T, I, M, J> put(BiFunction<List<Tuple2<Class<?>, Object>>, T, Option<?>> f){
		Spark.put(endpoint + "/:id", serializer.contentType(), (req, res) -> {
			if(serializer.unmarshall(req)
					.flatMap(o -> f.apply(requestParams(req, classes.tail()).push(new Tuple2<>(classes.head(), req.params(":id"))),o))
					.isDefined())
				res.status(204);
			else
				res.status(404);

			return Void.TYPE;
		}, serializer::marshall);
		log.info(String.format("PUT at %s/{id}", endpoint));
		return this;
	}

	public ControllerDetail<T, I, M, J> delete(Function<List<Tuple2<Class<?>, Object>>, Option<?>> f){
		Spark.delete(endpoint + "/:id", serializer.contentType(), (req, res) -> {
			if(f.apply(requestParams(req, classes.tail()).push(new Tuple2<>(classes.head(), req.params(":id")))).isDefined())
				res.status(204);
			else
				res.status(404);

			return Void.TYPE;
		}, serializer::marshall);
		log.info(String.format("DELETE at %s/{id}", endpoint));
		return this;
	}

	private static String endpoint(Class<?> typeClass, List<Class<?>> masterClasses){
		return masterClasses.reverse()
				.zipWith(parameters, (c, p) -> "/" + c.getSimpleName()
						.toLowerCase() + "s/:" + p)
				.reduce(String::concat) + "/" + typeClass.getSimpleName().toLowerCase() + "s";
	}

	private static List<Tuple2<Class<?>, Object>> requestParams(Request req, List<Class<?>> classes){
		return classes.zip(parameters.take(classes.length()).map(p -> req.params(":"+ p)).reverse());
	}
}
