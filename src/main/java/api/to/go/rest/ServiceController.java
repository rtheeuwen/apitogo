package api.to.go.rest;


import api.to.go.model.Action;
import api.to.go.model.Param;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.control.Option;
import spark.Spark;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ServiceController {

	private static final Serializer<?> serializer = new Serializer<>(Object.class, Serializer.ContentType.Json);

	private static final Map<String, Class<?>> mapping = HashMap.of(
			"boolean", Boolean.class
			, "byte", Byte.class
			, "short", Short.class
			, "int", Integer.class
			, "long", Long.class
			, "float", Float.class
			, "double", Double.class
			, "char", Character.class
	);

	public static void of(Class<?> clazz) {

		Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Action.class)).forEach(me -> {

			try {
				List<String> parameterNames = List.ofAll(Arrays.stream(me.getParameterAnnotations())).map(a -> a[0]).filter(a -> a.annotationType().equals(Param.class)).map(a -> (Param) a)
						.map(p -> p.name());
				List<Class<?>> parameterTypes = List.ofAll(Arrays.stream(me.getParameterTypes())).map(p -> p.getTypeName()).map(mapping::get).map(Option::get);
				Map<String, Class<?>> params = parameterNames.zip(parameterTypes).toLinkedMap(Function.identity());

				get(me, clazz.newInstance(), params);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static <T> void get(Method method, T service, Map<String, Class<?>> params) {

		Spark.get(method.getName(), (req, res) -> {
			List<String> queryParams = params.map(e -> e._1()).map(p -> req.queryParams(p)).toList();
			Object[] args = queryParams.zipWith(params.values(), (BiFunction<? super String, ? super Class<?>, ?>) (String v, Class<?> c) -> {
				try {
					Method m = c.getDeclaredMethod("valueOf", String.class);
					return m.invoke(c, v);
				} catch (Exception e) {
					return null;
				}
			}).toJavaArray();
			for (Object o : args) {
				if (o == null) {
					res.status(400);
					return Void.TYPE;
				}
			}
			return method.invoke(service, args);
		}, serializer::marshall);
	}
}
