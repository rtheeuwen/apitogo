package api.to.go.data;


import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Stream;
import javaslang.control.Option;
import api.to.go.rest.IdConverter;
import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import java.util.Objects;

import static api.to.go.data.ClassUtil.*;
import static api.to.go.data.DBConfig.dataSource;
import static api.to.go.data.DBConfig.properties;

class MasterDetailDaoImpl<T, I, M, J> implements MasterDetailDao<T, I, M, J>{

	private final Sql2o sql2o;
	private final ExistenceValidator<M, J> master;
	private final Class<T> typeClass;
	private final String TABLE;
	private final String MASTER_TABLE;
	private final String MASTER_ID;
	private final Class<J> MASTER_ID_TYPE;
	private final Class<I> ID_TYPE;
	private final List<String> fields;
	private final String ID;
	private final List<String> parameters = Stream.iterate('a', c -> ++c).take(26).map(String::valueOf).toList();
	private final IdConverter<I> idConverter;
	private final IdConverter<J> masterIdConverter;
	private Map<Class<?>, MasterDetailDao<?, ?, T, I>> detailDaos;


	private static final Logger log = LoggerFactory.getLogger(MasterDetailDaoImpl.class);

	MasterDetailDaoImpl(Class<T> typeClass, Class<I> idClass, Class<M> masterClass, Class<J> masterIdClass, ExistenceValidator<M, J> masterExistenceValidator){
		this.sql2o = new Sql2o(dataSource(properties()));
		this.master = masterExistenceValidator;
		this.typeClass = typeClass;
		this.TABLE = table(typeClass);
		this.MASTER_TABLE = table(masterClass);
		this.ID = idName(typeClass).get();
		Map<String, Option<String>> fieldNamesAndTypes = fieldNamesAndTypes(typeClass);
		this.fields = fieldNamesAndTypes.keySet().toList();
		this.MASTER_ID = ClassUtil.masterId(masterClass);
		this.idConverter = IdConverter.getInstance(idClass);
		this.masterIdConverter = master.getIdConverter();
		this.MASTER_ID_TYPE = masterIdClass;
		this.ID_TYPE = idClass;

		createTable(fieldNamesAndTypes);
	}

	private void createTable(Map<String, Option<String>> fieldNamesAndTypes) {

		String sql = "DROP TABLE IF EXISTS " + TABLE + ";\n"
				+ "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
				+ ID + " " + sqlType(ID_TYPE) + " AUTO_INCREMENT NOT NULL, "
				+ MASTER_ID + " " + sqlType(MASTER_ID_TYPE) + " NOT NULL, "
				+ fieldNamesAndTypes.map(f -> f._1() + " " + f._2()
				.getOrElseThrow(RuntimeException::new) + ", ")
				.reduce(String::concat)
				+ "CONSTRAINT PK_"+ TABLE + " PRIMARY KEY (" + ID + "), "
				+ "CONSTRAINT FK_" + TABLE + "_" + MASTER_ID + " FOREIGN KEY (" + MASTER_ID + ") REFERENCES " + MASTER_TABLE + " (id) "
				+ "ON DELETE RESTRICT ON UPDATE RESTRICT);\n"
				+ "CREATE INDEX IF NOT EXISTS IX_" + TABLE + "_" + MASTER_ID + " ON " + TABLE + "(" + MASTER_ID + ");";

		try(Connection con = sql2o.open()){
			Query query = con.createQuery(sql);
			log.debug(query.toString());
			query.executeUpdate();
		}
	}

	//lazy
	@Override
	public Option<T> byId(List<Tuple2<Class<?>, Object>> ids){
		if(!exists(ids))
			return Option.none();

		String sql = "SELECT " + ID + ", "
				+ fields.intersperse(", ").reduce(String::concat)
				+ " FROM " + TABLE
				+ " WHERE " + ID + " = :id AND " + MASTER_ID + " =:masterid";

		try(Connection con = sql2o.open()){
			Query query = con.createQuery(sql)
					.addParameter("id", idConverter.convert(String.valueOf(ids.head()._2())))
					.addParameter("masterid", idConverter.convert(String.valueOf(ids.get(1)._2())));
			log.debug(query.toString());

			return Option.of(query.executeAndFetchFirst(typeClass));
		}
	}

	//lazy
	@Override
	public Option<List<T>> all(List<Tuple2<Class<?>, Object>> ids){

		if(!exists(ids))
			return Option.none();

		String sql = "SELECT " + ID + ", "
				+ fields.intersperse(", ").reduce(String::concat)
				+ " FROM " + TABLE
				+ " WHERE " + MASTER_ID + " =:masterid";

		try(Connection con = sql2o.open()) {
			Query query = con.createQuery(sql)
					.addParameter("masterid", masterIdConverter.convert(String.valueOf(ids.head()._2())));
			log.debug(query.toString());

			return Try.of(() -> List.ofAll(query.executeAndFetch(typeClass))).getOption();
		}
	}

	//eager
	@Override
	public Option<I> create(T t, List<Tuple2<Class<?>, Object>> ids){

		if(!exists(ids))
			return Option.none();

		String sql = "INSERT INTO " + TABLE +"(" + MASTER_ID + ", " + fields.intersperse(", ").reduce(String::concat)
				+ ") VALUES (:masterid, " + parameters.take(fields.length()).map(":"::concat).intersperse(", ").reduce(String::concat) + ")";

		try(Connection con = sql2o.beginTransaction()) {
			return Try.of(() -> {
					Query query = con.createQuery(sql)
							.addParameter("masterid", masterIdConverter.convert(String.valueOf(ids.head()._2())));
					log.debug(query.toString());
					parameters.zip(fields.map(f -> ClassUtil.fieldValue(f, typeClass, t))).forEach(f -> query.addParameter(f._1(), f._2()));
					I id = query.executeUpdate().getKey(ID_TYPE);

					detailDaos.keySet()
							.map(Class::getSimpleName)
							.map(String::toLowerCase)
							.map(s -> s.concat("s"))
							.map(s -> fieldValue(s, typeClass, t))
							.filter(Objects::nonNull)
							.zipWith(detailDaos.values(), (value, dao) -> dao.cascadedCreate(id, value, con));

					con.commit();
					return id;

			}).getOption();
		}
	}

	@Override
	public Option<I> create(T t, J masterId) {
		String sql = "INSERT INTO " + TABLE +"(" + MASTER_ID + ", " + fields.intersperse(", ").reduce(String::concat)
				+ ") VALUES (:masterid, " + parameters.take(fields.length()).map(":"::concat).intersperse(", ").reduce(String::concat) + ")";

		try(Connection con = sql2o.beginTransaction()) {
			return Try.of(() -> {
				Query query = con.createQuery(sql)
						.addParameter("masterid", masterId);
				log.debug(query.toString());
				parameters.zip(fields.map(f -> ClassUtil.fieldValue(f, typeClass, t))).forEach(f -> query.addParameter(f._1(), f._2()));
				I id = query.executeUpdate().getKey(ID_TYPE);

				detailDaos.keySet()
						.map(Class::getSimpleName)
						.map(String::toLowerCase)
						.map(s -> s.concat("s"))
						.map(s -> fieldValue(s, typeClass, t))
						.filter(Objects::nonNull)
						.zipWith(detailDaos.values(), (value, dao) -> dao.cascadedCreate(id, value, con));

				con.commit();
				return id;
			}).getOption();
		}
	}

	//lazy
	@Override
	public Option<?> update(List<Tuple2<Class<?>, Object>> ids, T t) {
		if(!exists(ids))
			return Option.none();

		String sql = "UPDATE " + TABLE + " SET "
				+ fields.zip(parameters).map(p -> p._1() + "=:" + p._2()).intersperse(", ").reduce(String::concat)
				+ " WHERE " + ID + "=:id AND " + MASTER_ID + " =:masterid";

		try (Connection connection = sql2o.open()) {
			Query query = connection.createQuery(sql)
				.addParameter("id", idConverter.convert(String.valueOf(ids.head()._2())))
				.addParameter("masterid", idConverter.convert(String.valueOf(ids.get(1)._2())));
			parameters.zip(fields.map(f -> ClassUtil.fieldValue(f, typeClass, t))).forEach(f -> query.addParameter(f._1(), f._2()));
			log.debug(query.toString());

			return query.executeUpdate().getResult()!=0?Option.of(Void.TYPE):Option.none();
		}
	}

	@Override
	public Option<?> update(T t) {
		return updateLazily(ClassUtil.<I, T>idValue(typeClass, t).get(), t);
	}

	//eager
	@Override
	public Option<?> delete(List<Tuple2<Class<?>, Object>> ids){
		if(!exists(ids))
			return Option.none();

		String sql = "DELETE " + TABLE + " WHERE " + ID + "=:id AND " + MASTER_ID + " =:masterid";
		try (Connection connection = sql2o.beginTransaction()) {
			detailDaos.values()
					.map(dao -> dao.cascadedDelete(idConverter.convert(String.valueOf(ids.get(1)._2())), connection));

			Option<?> o = connection.createQuery(sql)
					.addParameter("id", idConverter.convert(String.valueOf(ids.head()._2())))
					.addParameter("masterid", idConverter.convert(String.valueOf(ids.get(1)._2())))
					.executeUpdate().getResult()!=0?Option.of(Void.TYPE):Option.none();

			connection.commit();
			return o;
		}
	}

	@Override
	public boolean exists(List<Tuple2<Class<?>, Object>> ids){

		if(!ids.head()._1().equals(typeClass))
			return master.exists(ids);

		String sql = "SELECT " + MASTER_ID + " M FROM " + TABLE + " T WHERE " + ID + " =:id";

		try(Connection con = sql2o.open()){
			J masterId = con.createQuery(sql).addParameter("id", idConverter.convert(String.valueOf(ids.head()._2())))
					.executeAndFetchFirst(MASTER_ID_TYPE);
			return masterId != null ? master.exists(ids.tail()) : false;
		}
	}

	@Override
	public IdConverter<I> getIdConverter() {
		return this.idConverter;
	}

	@Override
	public Option<?> cascadedCreate(J masterId, Object details, Connection con) {

		String sql = "INSERT INTO " + TABLE +"(" + MASTER_ID + ", " + fields.intersperse(", ").reduce(String::concat)
				+ ") VALUES (:masterid, " + parameters.take(fields.length()).map(":"::concat).intersperse(", ").reduce(String::concat) + ")";

		return (List.ofAll((java.util.List<T>)details).map(t -> {

			Query query = con.createQuery(sql)
					.addParameter("masterid", masterId);
			log.debug(query.toString());
			parameters.zip(fields.map(f -> ClassUtil.fieldValue(f, typeClass, t))).forEach(f -> query.addParameter(f._1(), f._2()));
			I id = query.executeUpdate().getKey(ID_TYPE);
			detailDaos.keySet()
					.map(Class::getSimpleName)
					.map(String::toLowerCase)
					.map(s -> s.concat("s"))
					.map(s -> fieldValue(s, typeClass, t))
					.filter(Objects::nonNull)
					.zipWith(detailDaos.values(), (value, dao) -> dao.cascadedCreate(id, value, con));
					return Option.of(null);
		}).reduce((a, b) -> Option.of(null)));
	}

	@Override
	public Option<?> cascadedDelete(J masterId, Connection con) {

		List<I> ids = byMasterIds(List.of(masterId), con).map(x -> (T)x).map(t -> ClassUtil.<I, T>idValue(typeClass, t).get());
		String sql = "DELETE " + TABLE + " WHERE " + MASTER_ID + "=:masterid";

		ids.forEach( i -> detailDaos.values().forEach(dao -> dao.cascadedDelete(i, con)));
		Query query = con.createQuery(sql).addParameter("masterid", masterId);
		log.debug(query.toString());
		query.executeUpdate();


		return Option.of(Void.TYPE);
	}

	//eager
	@Override
	public List<?> byMasterIds(List<J> masterIds, Connection con) {

		String sql = "SELECT " + ID + ", "
				+ fields.intersperse(", ").reduce(String::concat)
				+ " FROM " + TABLE
				+ " WHERE " + MASTER_ID + " =:masterid";

		return masterIds.flatMap(masterId -> {

			Query query = con.createQuery(sql)
					.addParameter("masterid", masterId);
			log.debug(query.toString());

			List<T> ts = List.ofAll(query.executeAndFetch(typeClass));

			List<I> recursiveMasterIds = ts.map(t -> ClassUtil.<I, T>idValue(typeClass, t).get());

			ts.map(t -> {
			return detailDaos.values().map(dao -> dao.byMasterIds(recursiveMasterIds, con))
						.zipWith(detailDaos.keySet(), (value, detailClass) -> setFieldValue(detailClass.getSimpleName().toLowerCase()+"s", typeClass, t, ((List)value).toJavaList()));
			});

			return ts;
		});
	}





	@Override
	public Option<?> updateLazily(I id, T t) {
		String sql = "UPDATE " + TABLE + " SET "
				+ fields.zip(parameters).map(p -> p._1() + "=:" + p._2()).intersperse(", ").reduce(String::concat)
				+ " WHERE " + ID + "=:id";

		try (Connection connection = sql2o.open()) {
			Query query = connection.createQuery(sql)
					.addParameter("id", id);
			parameters.zip(fields.map(f -> ClassUtil.fieldValue(f, typeClass, t))).forEach(f -> query.addParameter(f._1(), f._2()));
			log.debug(query.toString());

			return query.executeUpdate().getResult()!=0?Option.of(Void.TYPE):Option.none();
		}
	}

	//eager
	@Override
	public Option<?> delete(I id) {

		String sql = "DELETE " + TABLE + " WHERE " + ID + "=:id";
		try (Connection connection = sql2o.beginTransaction()) {
				detailDaos.values().map(dao -> dao.cascadedDelete(id, connection));
				Query query = connection.createQuery(sql)
					.addParameter("id", id);
					log.debug(query.toString());
					query.executeUpdate();

			connection.commit();
			return Option.of(null);
		}
	}


	//eager
	@Override
	public Option<T> byId(I i) {
		throw new UnsupportedOperationException();
	}

	//eager
	@Override
	public Option<List<T>> all() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<T> typeClass() {
		return typeClass;
	}

	@Override
	public Class<I> idClass() {
		return this.ID_TYPE;
	}

	@Override
	public void setDetailDaos(List<MasterDetailDao<?, ?, T, I>> detailDaos) {
		this.detailDaos = detailDaos.map(d -> new Tuple2<>(d.typeClass(), d))
				.toMap(a -> a._1(), a -> a._2());
	}
}
