package api.to.go.data;

import api.to.go.model.Id;
import api.to.go.model.OneToMany;
import api.to.go.model.Resource;
import api.to.go.model.Service;
import javaslang.Tuple2;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.control.Option;
import javaslang.control.Try;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;

public final class ClassUtil {

	private static final Map<String, String> mapping = HashMap.of(
			"boolean", "BOOLEAN"
			,"byte", "BIGINT"
			,"short", "BIGINT"
			,"int", "BIGINT"
			,"long", "BIGINT"
			,"float", "BIGINT"
			,"double", "BIGINT"
			,"char", "BIGINT"
			,"class java.lang.Long", "BIGINT"
			,"class java.lang.String", "VARCHAR(255)"
			);

	ClassUtil(){
		throw new AssertionError();
	}

	static Map<String, Option<String>> fieldNamesAndTypes(Class<?> clazz){
		return List.ofAll(Arrays.stream(clazz.getDeclaredFields()))
				.filter(f -> !Modifier.isStatic(f.getModifiers()))
				.filter(f -> !Modifier.isTransient(f.getModifiers()))
				.filter(f -> !f.isAnnotationPresent(Id.class))
				.filter(f -> !f.isAnnotationPresent(OneToMany.class))
				.map(f -> new Tuple2<>(f.getName(), mapping.get(f.getType().toString())))
				.toMap(Function.identity());
	}

	static Set<? extends Class<?>> detailFields(Class<?> resourceClass){
		return List.ofAll(Arrays.stream(resourceClass.getDeclaredFields()))
				.filter(f -> !Modifier.isStatic(f.getModifiers()))
				.filter(f -> !Modifier.isTransient(f.getModifiers()))
				.filter(f -> f.isAnnotationPresent(OneToMany.class))
				.map(f -> ((ParameterizedType) f.getGenericType()))
				.map(p -> p.getActualTypeArguments()[0])
				.map(Type::getTypeName)
				.map(s -> {
					try {
						return Class.forName(s);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}).toJavaSet();
	}

	static String sqlType(Class<?> javaType){
		return mapping.get(javaType.toString()).get();
	}

	static<T> Object fieldValue(String fieldName, Class<T> clazz, T t){
		return Try.of(() -> {
			Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(t);
		}).getOrElseThrow(e -> new RuntimeException(e));
	}

	static<T> T setFieldValue(String fieldName, Class<T> clazz, T t, Object value){
		java.util.List newValue = new ArrayList();
		if(value instanceof List) {
			newValue.addAll (((List) value).toJavaList());
		}
		return Try.of(() -> {

		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(t, newValue.isEmpty()?value:newValue);
			return t;
		}).getOrElseThrow(e -> new RuntimeException(e));
	}

	static<I, T> Option<I> idValue(Class<T> clazz, T t){
		return Try.of(() -> {
			Field id = List.ofAll(Arrays.stream(clazz.getDeclaredFields())).filter(f -> f.isAnnotationPresent(Id.class)).head();
			id.setAccessible(true);
			return (I) (id.get(t));
		}).getOption();
	}

	static<T> String table(Class<T> clazz){
		return clazz.getSimpleName().toLowerCase();
	}

	static<T> String masterId(Class<T> clazz){
		return table(clazz).concat("_id");
	}

	static <T> Option<String> idName(Class<T> clazz){
		return List.ofAll(Arrays.stream(clazz.getDeclaredFields())).filter(f -> f.isAnnotationPresent(Id.class)).map(Field::getName).getOption();
	}

	static Tuple2<String, String> idAndKind(Class<?> clazz){
		return List.ofAll(Arrays.stream(clazz.getDeclaredFields()))
				.filter(f -> f.isAnnotationPresent(Id.class))
				.map(f -> new Tuple2<>(f.getName(), mapping.get(f.getType().toString()).get())).head();
	}

	public static boolean isResource(Class<?> clazz){
		return clazz.isAnnotationPresent(Resource.class);
	}

	public static boolean isService(Class<?> clazz){
		return clazz.isAnnotationPresent(Service.class);
	}

	public static List<Class<?>> detailClasses(Class<?> masterClass){
		 return List.ofAll(Arrays.stream(masterClass.getDeclaredFields()))
				.filter(f -> f.isAnnotationPresent(OneToMany.class))
				.map(f -> ((ParameterizedType) f.getGenericType()))
				.map(p -> p.getActualTypeArguments()[0])
				.map(Type::getTypeName)
				.map(s -> {
					try {
						return Class.forName(s);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				});
	}
}
