package api.to.go.rest;

import api.to.go.model.Validable;
import com.fasterxml.jackson.databind.ObjectMapper;
import javaslang.control.Option;
import javaslang.control.Try;
import javaslang.jackson.datatype.JavaslangModule;
import spark.Request;

import java.util.Objects;

import static com.google.json.JsonSanitizer.sanitize;

public class Serializer<T> {

	public enum ContentType{
		Json{
			@Override
			String value() {
				return "application/json";
			}

		};
		abstract String value();
	}

	private final ContentType contentType;
	private final ObjectMapper mapper = new ObjectMapper();
	private final Class<T> clazz;

	Serializer(Class<T> clazz, ContentType contentType){
		this.clazz = clazz;
		this.contentType = contentType;
		switch (contentType){
			case Json:
				mapper.registerModule(new JavaslangModule());
				break;
			default:
				throw new AssertionError();
		}
	}

	String contentType(){
		return contentType.value();
	}

	String marshall(Object o) throws Exception {
		return o.equals(Void.TYPE)?"":sanitize(mapper.writeValueAsString(o));
	}

	Option<T> unmarshall(Request req){
		return Try.of(() -> mapper.readValue(sanitize(req.body()), clazz)).filter(Objects::nonNull).filter(Serializer::isValid).getOption();
	}

	private static<T> boolean isValid(T t){
		return !(t instanceof Validable) || ((Validable) t).isValid();
	}

}
