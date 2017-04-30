package api.to.go.data;


import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Stream;
import javaslang.control.Option;
import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import api.to.go.rest.IdConverter;

import static api.to.go.data.ClassUtil.*;
import static api.to.go.data.DBConfig.dataSource;
import static api.to.go.data.DBConfig.properties;


class MasterDaoImpl<T, I> implements MasterDao<T, I>{

	private final Sql2o sql2o;
	private final List<String> fields;
	private final String ID;
	private final String TABLE;
	private final Class<T> clazz;
	private final Class<I> idClazz;
	private final List<String> parameters = Stream.iterate('a', c -> ++c).take(26).map(String::valueOf).toList();
	private final IdConverter<I> idConverter;
	private Map<Class<?>, MasterDetailDao<?, ?, T, I>> detailDaos;

	private static final Logger log = LoggerFactory.getLogger(MasterDaoImpl.class);

	MasterDaoImpl(Class<T> clazz, Class<I> idClass){
		this.sql2o = new Sql2o(dataSource(properties()));
		this.clazz = clazz;
		this.idClazz = idClass;
		this.TABLE = table(clazz);
		Map<String, Option<String>> fieldNamesAndTypes = fieldNamesAndTypes(clazz);
		this.fields = List.ofAll(fieldNamesAndTypes.keySet());
		Tuple2<String, String> idName = idAndKind(clazz);
		this.ID = idName._1();
		this.idConverter = IdConverter.getInstance(idClass);

		createTable(fieldNamesAndTypes, idName);
	}

	private void createTable(Map<String, Option<String>> fieldNamesAndTypes, Tuple2<String, String> idName) {
		StringBuilder sb = new StringBuilder("DROP TABLE IF EXISTS " + TABLE +";\n CREATE TABLE IF NOT EXISTS ")
				.append(TABLE).append(" (").append(ID).append(" ").append(idName._2()).append(" AUTO_INCREMENT, ");

		fieldNamesAndTypes.forEach(f -> sb.append(f._1()).append(" ").append(f._2().getOrElseThrow(RuntimeException::new)).append(", "));
		sb.append("PRIMARY KEY (").append(ID).append("))");

		try(Connection con = sql2o.open()){
			Query query = con.createQuery(sb.toString());
			log.debug(query.toString());
			query.executeUpdate();
		}
	}

	//lazy
	@Override
	public Option<T> byIdLazily(I i) {
		String sql = "SELECT " + ID + ", "
				+ fields.intersperse(", ").reduce(String::concat)
				+ " FROM " + TABLE
				+ " WHERE " + ID + " = :id";

		try(Connection con = sql2o.open()) {
			Query query = con.createQuery(sql)
					.addParameter("id", i);
			log.debug(query.toString());
			T t = query.executeAndFetchFirst(clazz);
			if(t == null)
				return Option.none();
			else
				return Option.of(t);
		}
	}

	//lazy
	@Override
	public Option<List<T>> allLazily() {
		String sql = "SELECT " + ID + ", "
				+ fields.intersperse(", ").reduce(String::concat)
				+ " FROM " + TABLE;

		try(Connection con = sql2o.open()) {
			Query query = con.createQuery(sql);
			log.debug(query.toString());
			return Try.of(() -> List.ofAll(query.executeAndFetch(clazz))).getOption();
		}
	}

	//eager
	@Override
	public Option<I> create(T t) {

		String sql = "INSERT INTO " + TABLE +"(" + fields.intersperse(", ").reduce(String::concat)
				+ ") VALUES (" + parameters.take(fields.length()).map(":"::concat).intersperse(", ").reduce(String::concat) + ")";

		try(Connection con = sql2o.beginTransaction()) {
			return Try.of(() -> {
					Query query = con.createQuery(sql);
					log.debug(query.toString());
					parameters.zip(fields.map(f -> ClassUtil.fieldValue(f, clazz, t))).forEach(f -> query.addParameter(f._1(), f._2()));
					I id = (I) query.executeUpdate().getKey(idClazz);

					detailDaos.keySet()
							.map(Class::getSimpleName)
							.map(String::toLowerCase)
							.map(s -> s.concat("s"))
							.map(s -> fieldValue(s, clazz, t))
							.zipWith(detailDaos.values(), (value, dao) -> value!=null? (dao.cascadedCreate(id, value, con)):Option.of(null))
							.reduce((a, b) -> a.isEmpty() || b.isEmpty() ? Option.none() : a);

					con.commit();
					return id;

			}).getOption();
		}
	}

	//lazy
	@Override
	public Option<?> updateLazily(I id, T t) {

		String sql = "UPDATE " + TABLE + " SET "
				+ fields.zip(parameters).map(p -> p._1() + "=:" + p._2()).intersperse(", ").reduce(String::concat)
				+ " WHERE " + ID + "=:id";

		try (Connection connection = sql2o.open()) {

			Query query = connection.createQuery(sql);
			log.debug(query.toString());

			parameters.zip(fields.map(f -> ClassUtil.fieldValue(f, clazz, t))).forEach(f -> query.addParameter(f._1(), f._2()));
			query.addParameter("id", id);

			return query.executeUpdate().getResult()!=0?Option.of(Void.TYPE):Option.none();
		}
	}


	//eager
	@Override
	public Option<?> delete(I id) {
		String sql = "DELETE " + TABLE + " WHERE " + ID + "=:id";
		try (Connection connection = sql2o.beginTransaction()) {
			detailDaos.values()
					.map(dao -> dao.cascadedDelete(id, connection));
			Option<?> o = connection.createQuery(sql).addParameter("id", id)
					.executeUpdate().getResult()!=0?Option.of(Void.TYPE):Option.none();

			connection.commit();
			return o;
		}
	}

	//eager
	@Override
	public Option<T> byId(I i) {

		String sql = "SELECT " + ID + ", "
				+ fields.intersperse(", ").reduce(String::concat)
				+ " FROM " + TABLE
				+ " WHERE " + ID + " = :id";

		try(Connection con = sql2o.beginTransaction()) {
			Query query = con.createQuery(sql)
					.addParameter("id", i);
			log.debug(query.toString());

			return Try.of(() -> {
				try {
					T t = query.executeAndFetchFirst(clazz);
					log.debug(t.toString());
					detailDaos.values().map(dao -> dao.byMasterIds(List.of(i), con))
							.zipWith(detailDaos.keySet(), (value, detailClass) -> setFieldValue(detailClass.getSimpleName().toLowerCase()+"s", clazz, t, value));

					con.commit();
					return t;
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}).getOption();
		}
	}

	//eager
	@Override
	public Option<List<T>> all() {

		String sql = "SELECT " + ID + ", "
				+ fields.intersperse(", ").reduce(String::concat)
				+ " FROM " + TABLE;

		try(Connection con = sql2o.beginTransaction()) {
			Query query = con.createQuery(sql);
			log.debug(query.toString());

			return Try.of(() -> {
				List<T> ts = List.ofAll(query.executeAndFetch(clazz)).map(t -> new Tuple2<>(fieldValue("id", clazz, t), t)).flatMap(pair ->
				detailDaos.values().map(dao -> dao.byMasterIds(List.of((I)pair._1()), con))
						.zipWith(detailDaos.keySet(), (value, detailClass) -> setFieldValue(detailClass.getSimpleName().toLowerCase()+"s", clazz, pair._2(), value)));

				con.commit();
				return ts;
			}).getOption();
		}
	}

	@Override
	public Class<T> typeClass() {
		return clazz;
	}

	@Override
	public Class<I> idClass() {
		return this.idClazz;
	}


	@Override
	public boolean exists(List<Tuple2<Class<?>, Object>> ids) {

		if(!ids.head()._1().getSimpleName().toLowerCase().equals(TABLE))
			return false;

		String sql = "SELECT COUNT(T." + ID + ") AS C FROM " + TABLE + " T WHERE " + ID + " =:id";
		try (Connection connection = sql2o.open()) {
			return connection.createQuery(sql)
					.addParameter("id", idConverter.convert(String.valueOf(ids.head()._2())))
					.executeAndFetchFirst(Integer.class) != null;
		}
	}

	@Override
	public IdConverter<I> getIdConverter() {
		return this.idConverter;
	}

	@Override
	public void setDetailDaos(List<MasterDetailDao<?, ?, T, I>> detailDaos) {
		this.detailDaos = detailDaos.map(d -> new Tuple2<>(d.typeClass(), d))
				.toMap(a -> a._1(), a -> a._2());
	}
}
