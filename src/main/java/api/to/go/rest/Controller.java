package api.to.go.rest;

import javaslang.collection.List;
import javaslang.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static api.to.go.rest.IdConverter.getIdClass;
import static spark.Spark.*;

public class Controller<T, I> {

	private final Serializer<T> serializer;
	private final String endpoint;
	private final IdConverter<I> idConverter;

	private static final Logger log = LoggerFactory.getLogger(Controller.class);

	public Controller(Class<T> clazz, Serializer.ContentType contentType){
		this.idConverter = IdConverter.getInstance(getIdClass(clazz));
		this.serializer = new Serializer<>(clazz, contentType);
		this.endpoint = String.format("/%ss", clazz.getSimpleName().toLowerCase());
		after(((req, res) -> res.type(contentType.value())));
	}

	public Controller(Class<T> clazz){
		this(clazz, Serializer.ContentType.Json);
	}

	public Controller<T, I> getAll(Supplier<Option<List<T>>> sup){
		get(endpoint, (req, res) -> sup.get().getOrElseThrow(RuntimeException::new), serializer::marshall);
		log.debug(String.format("GET at %s", endpoint));
		return this;
	}

	public Controller<T, I> getOne(Function<I, Option<T>> f){
		get(endpoint + "/:id", (req, res) -> {
			Option<T> option = f.apply(idConverter.convert(req.params(":id")));
			if(option.isDefined())
				return option.get();
			else{
				res.status(404);
				return Void.TYPE;
			}
		}, serializer::marshall);
		log.debug(String.format("GET at %s/{id}", endpoint));
		return this;
	}

	public Controller<T, I> post(Function<T, Option<I>> f){
		Spark.post(endpoint, serializer.contentType(),
				(req, res) -> {
					Option<I> option = serializer.unmarshall(req).flatMap(f);
					if(option.isDefined()){
						res.status(200);
						return option.get();
					} else {
						res.status(400);
						return Void.TYPE;
					}
				}, serializer::marshall);
		log.debug(String.format("POST at %s", endpoint));
		return this;
	}

	public Controller<T, I> put(BiFunction<I, T, Option<?>> f){
		Spark.put(endpoint + "/:id", serializer.contentType(), (req, res) -> {
			if(serializer.unmarshall(req)
					.flatMap(o -> f.apply(idConverter.convert(req.params(":id")), o))
					.isDefined())
				res.status(204);
			else
				res.status(404);

			return Void.TYPE;
		}, serializer::marshall);
		log.debug(String.format("PUT at %s/{id}", endpoint));
		return this;
	}

	public Controller<T, I> delete(Function<I, Option<?>> f){
		Spark.delete(endpoint + "/:id", serializer.contentType(), (req, res) -> {
			if(f.apply(idConverter.convert(req.params(":id"))).isDefined())
				res.status(204);
			else
				res.status(404);

			return Void.TYPE;
		}, serializer::marshall);
		log.debug(String.format("DELETE at %s/{id}", endpoint));
		return this;
	}

}
