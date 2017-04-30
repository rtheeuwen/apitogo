package api.to.go.rest;

import api.to.go.model.Id;

import java.lang.reflect.Field;
import java.util.Arrays;

public abstract class IdConverter<I>{
	public abstract I convert(String id);

	@SuppressWarnings("unchecked")
	public static <I> IdConverter<I> getInstance(Class<I> clazz){
//		clazz = getIdType(clazz);
		if(clazz.equals(Long.class)||clazz.toString().equals("long"))
			return (IdConverter<I>) new LongConverter();
		if(clazz.equals(String.class))
			return (IdConverter<I>) new StringConverter();
		throw new IllegalArgumentException("Resource must have an Id of type java.lang.String or java.lang.Long");
	}

	private static class StringConverter extends IdConverter<String>{

		@Override
		public String convert(String id) {
			return id;
		}
	}

	private static class LongConverter extends IdConverter<Long>{

		@Override
		public Long convert(String id) {
			return Long.valueOf(id);
		}
	}

	@SuppressWarnings("unchecked")
	private static <I> Class<I> getIdType(Class<?> resource){
		return (Class<I>) Arrays.stream(resource.getDeclaredFields())
				.filter(f -> f.isAnnotationPresent(Id.class))
				.findFirst().map(Field::getType)
				.orElseThrow(() -> new RuntimeException(resource.getCanonicalName() + " must have a field annotated with @Id"));
	}

	public static <T> Class<T> getIdClass(Class<?> entityClass){
		entityClass = getIdType(entityClass);
		if(entityClass.equals(Long.class)||entityClass.toString().equals("long"))
			return (Class<T>) Long.class;
		if(entityClass.equals(String.class))
			return (Class<T>) String.class;
		throw new IllegalArgumentException("Resource must have an Id of type java.lang.String or java.lang.Long");
	}
}


