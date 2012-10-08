package eu.janmuller.android.dao.api;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import eu.janmuller.android.dao.CreateTableSqlBuilderExtended;
import eu.janmuller.android.dao.DaoService;
import eu.janmuller.android.dao.exceptions.DaoConstraintException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Coder: Jan Müller
 * Date: 03.10.12
 * Time: 13:48
 */
public abstract class GenericModel<T extends BaseModel> implements ISimpleDroidDao<T> {

    public static int getCount() {

        return 0;
    }

    public static void findObjectById(BaseModel model, Id id) {

        Cursor cursor = getSQLiteDatabase().query(getTableName(model.getClass()),null
                , SimpleDaoSystemFieldsEnum.ID + "=?",
                new String[]{id.getId().toString()}, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {

            ((GenericModel)model).getObjectFromCursor(cursor);

            cursor.close();

        }

    }

    public static <U extends BaseModel> List<U> getAllObjects() {

        List<U> list = new ArrayList<U>();
        return list;
    }

    private ContentValues getContentValuesFromObject() {

        ContentValues cv = new ContentValues();

        Date now = new Date();

        for (Field field : getClass().getFields()) {

            DataType dt = field.getAnnotation(DataType.class);
            if (dt != null) {

                switch (dt.type()) {

                    case BLOB:
                        cv.put(field.getName(), (byte[])getValueFromField(field));
                        break;
                    case INTEGER:
                        cv.put(field.getName(), (Integer)getValueFromField(field));
                        break;
                    case TEXT:
                        cv.put(field.getName(), (String)getValueFromField(field));
                        break;
                    case DOUBLE:
                        cv.put(field.getName(), (Double)getValueFromField(field));
                        break;
                    case DATE:
                        cv.put(field.getName(), ((Date) getValueFromField(field)).getTime());
                        break;
                    case ENUM:
                        cv.put(field.getName(), ((Enum) getValueFromField(field)).ordinal());
                        break;
                }
            }

            InternalFieldType ift = field.getAnnotation(InternalFieldType.class);
            if (ift != null) {
                switch (ift.type()) {

                    case CREATE:

                        BaseDateModel bdm = (BaseDateModel) this;
                        if (bdm.creationDate == null) {

                            cv.put(ift.type().getName(), now.getTime());
                        }
                        break;
                    case MODIFY:
                        cv.put(ift.type().getName(), now.getTime());

                        break;
                    case ID:
                        BaseModel bm = (BaseModel) this;

                        Id id;
                        // pokud jeste neni idcko, pak se jedna o novy objekt
                        if (bm.id == null) {
                            switch (getIdType(((T)this).getClass())) {

                                case LONG:
                                    id = new LongId(0l);
                                    cv.put(ift.type().getName(), (Long)id.getId());
                                    break;
                                case UUID:
                                    id = new UUIDId();
                                    cv.put(ift.type().getName(), (String)id.getId());
                                    ((UUIDId)bm.id.getId()).create = true;
                                    break;
                                default:
                                    throw new IllegalStateException("you shouldnt be here");
                            }
                        } else {

                            id = bm.id;
                            switch (getIdType(((T)this).getClass())) {

                                case LONG:
                                    cv.put(ift.type().getName(), (Long)id.getId());
                                    break;
                                case UUID:
                                    cv.put(ift.type().getName(), (String)id.getId());
                                    break;
                                default:
                                    throw new IllegalStateException("you shouldnt be here");
                            }
                        }

                }
            }
        }
        return cv;
    }

    private void getObjectFromCursor(Cursor cursor) {

       // T instance = null;
        try {
            //instance = (T) ((Class) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]).newInstance();

            for (Field field : this.getClass().getFields()) {

                int columnIndex = cursor.getColumnIndex(field.getName());
                DataType dt = field.getAnnotation(DataType.class);
                if (dt != null) {

                    switch (dt.type()) {

                        case BLOB:
                            field.set(this, cursor.getBlob(columnIndex));
                            break;
                        case DOUBLE:
                            field.set(this, cursor.getDouble(columnIndex));
                            break;
                        case INTEGER:
                            field.set(this, cursor.getInt(columnIndex));
                            break;
                        case TEXT:
                            field.set(this, cursor.getString(columnIndex));
                            break;
                        case DATE:
                            field.set(this, new Date(cursor.getLong(columnIndex)));
                            break;
                        case ENUM:
                            int i = cursor.getInt(columnIndex);
                            Class c = field.getType();
                            field.set(this, c.getFields()[i].get(this));
                            break;
                    }
                }

                InternalFieldType ift = field.getAnnotation(InternalFieldType.class);
                if (ift != null) {
                    switch (ift.type()) {

                        case CREATE:
                        case MODIFY:
                            Date date = new Date(cursor.getLong(columnIndex));
                            field.set(this, date);
                            break;
                        case ID:

                            // pokud jeste neni idcko, pak se jedna o novy objekt
                            switch (getIdType(((T)this).getClass())) {

                                case LONG:
                                    field.set(this, new LongId(cursor.getLong(columnIndex)));
                                    break;
                                case UUID:
                                    field.set(this, new UUIDId(cursor.getString(columnIndex)));
                                    break;
                                default:
                                    throw new IllegalStateException("you shouldnt be here");
                            }
                            break;
                    }
                }
            }

        } catch (IllegalAccessException e) {

            e.printStackTrace();
        }

    }


    private void getObjectFromContentValues(Map<String, Object> cv) {

        //T instance = null;
        try {
            //instance = (T) ((Class) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]).newInstance();

            for (Field field : this.getClass().getFields()) {

                DataType dt = field.getAnnotation(DataType.class);
                if (dt != null) {

                    switch (dt.type()) {

                        case BLOB:
                        case DOUBLE:
                        case INTEGER:
                        case TEXT:
                            field.set(this, cv.get(field.getName()));
                            break;
                        case DATE:
                            field.set(this, new Date((Long) cv.get(field.getName())));
                            break;
                        case ENUM:
                            int i = (Integer) cv.get(field.getName());
                            Class c = field.getType();
                            field.set(this, c.getFields()[i].get(this));
                            break;
                    }
                }

                InternalFieldType ift = field.getAnnotation(InternalFieldType.class);
                if (ift != null) {
                    switch (ift.type()) {

                        case CREATE:
                        case MODIFY:
                            Date date = new Date((Long)cv.get(field.getName()));
                            field.set(this, date);
                            break;
                        case ID:

                            // pokud jeste neni idcko, pak se jedna o novy objekt
                            switch (getIdType(((T)this).getClass())) {

                                case LONG:
                                    field.set(this, new LongId((Long) cv.get(field.getName())));
                                    break;
                                case UUID:
                                    field.set(this, new UUIDId((String) cv.get(field.getName())));
                                    break;
                                default:
                                    throw new IllegalStateException("you shouldnt be here");
                            }
                            break;
                    }
                }
            }

        } catch (IllegalAccessException e) {

            e.printStackTrace();
        }
    }

    private Object getValueFromField(Field f) {

        Object value;
        try {

            value = f.get(this);
        } catch (IllegalAccessException e) {

            throw new IllegalStateException("field is not accessible");
        }

        if (value == null) {

            throw new IllegalStateException("value is null!");
        }

        return value;
    }

    private static <T extends BaseModel> String getCreateTableSQL(Class<T> clazz) {

        CreateTableSqlBuilderExtended ctsb = new CreateTableSqlBuilderExtended(getTableName(clazz));
        for (Field field : clazz.getFields()) {

            DataType dt = field.getAnnotation(DataType.class);
            if (dt != null) {

                NotNull notNull = field.getAnnotation(NotNull.class);
                Unique unique = field.getAnnotation(Unique.class);
                Index index = field.getAnnotation(Index.class);

                switch (dt.type()) {

                    case BLOB:
                        ctsb.addBlobColumn(field.getName(), notNull != null);
                        break;
                    case DATE:
                    case INTEGER:
                    case ENUM:
                        ctsb.addIntegerColumn(field.getName(), notNull != null);
                        break;
                    case DOUBLE:
                        ctsb.addRealColumn(field.getName(), notNull != null);
                        break;
                    case TEXT:
                        ctsb.addTextColumn(field.getName(), notNull != null);
                        break;

                }

                if (unique != null) {

                    ctsb.addUniqueConstrain(field.getName());
                }

                if (index != null) {

                    ctsb.addSimpleIndex(field.getName());
                }
            }

            InternalFieldType ift = field.getAnnotation(InternalFieldType.class);
            if (ift != null) {
                switch (ift.type()) {

                    case CREATE:
                    case MODIFY:
                        ctsb.addIntegerColumn(field.getName());
                        break;
                    case ID:

                        switch (getIdType(clazz)) {

                            case LONG:
                                ctsb.addIntegerPrimaryColumn(field.getName());
                                break;
                            case UUID:
                                ctsb.addTextPrimaryColumn(field.getName());
                                break;
                        }
                }
            }
        }

        return ctsb.create();
    }

    /**
     * Vraci instanci objektu pro praci s DB
     */
    protected static SQLiteDatabase getSQLiteDatabase() {

        return DaoService.getDatabaseAdapter().getOpenedDatabase();
    }

    @Override
    public T save() {

        // namapuji objekt na db objekt
        ContentValues cv = getContentValuesFromObject();

        T object = (T) this;

        boolean isUpdate = false;

        try {
            // pokud nema objekt vyplnene id, pak vytvarime novy zaznam do DB
            if ((object.id instanceof UUIDId && ((UUIDId)object.id).create) || (object.id instanceof LongId && ((LongId)object.id).getId() == 0l)) {

                // insertujem do db
                long id = getSQLiteDatabase().insertOrThrow(getTableName(object.getClass()), null, cv);

                // pokud nedoslo k chybe
                if (id != -1) {

                    // vratime vygenerovane id
                    if (object.id instanceof LongId) {

                        object.id = new LongId(id);
                    }
                    return object;
                } else {

                    // jinak vyhodime runtime exception
                    throw new RuntimeException("insert failed");
                }

            } else {

                // pokud jiz existuje id, pak provedem update
                long updatedID = getSQLiteDatabase().update(getTableName(object.getClass()), cv, SimpleDaoSystemFieldsEnum.ID.getName() + "='" + object.getId() + "'", null);

                isUpdate = true;

                // pokud update probehl v poradku
                if (updatedID > 0) {

                    // vratime vygenerovane id
                    return object;
                } else {

                    // jinak vyhodime runtime exception
                    throw new RuntimeException("update failed for object id " + object.getId());
                }
            }
        } catch (SQLiteConstraintException sce) {


            throw new DaoConstraintException(isUpdate ? DaoConstraintException.ConstraintsExceptionType.UPDATE :
                    DaoConstraintException.ConstraintsExceptionType.INSERT, sce);
        }
    }


    public T load() {

        return null;
    }

    static <T extends BaseModel> void createTable(Class<T> clazz) {

        getSQLiteDatabase().execSQL(getCreateTableSQL(clazz));
    }

    public void delete() {

    }

    private static <T extends BaseModel> String getTableName(Class<T> clazz) {

        TableName tn = clazz.getAnnotation(TableName.class);

        if (tn == null) {

            throw new IllegalStateException("no table name annotation defined!");
        }
        return tn.name();
    }

    private static <T extends BaseModel> IdTypeEnum getIdType(Class<T> clazz) {

        IdType idType = clazz.getAnnotation(IdType.class);
        if (idType == null) {

            throw new IllegalStateException("no id type annotation defined");
        }
        return idType.type();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface TableName {

        String name();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface IdType {

        IdTypeEnum type();
    }

    public enum IdTypeEnum {
        LONG,
        UUID
    }

    public enum DataTypeEnum {

        INTEGER,
        DOUBLE,
        TEXT,
        BLOB,
        DATE,
        ENUM
    }

    public enum SimpleDaoSystemFieldsEnum {

        ID("id"),
        MODIFY("modifiedDate"),
        CREATE("creationDate");

        private String name;

        private SimpleDaoSystemFieldsEnum(String name) {
            this.name = name;
        }

        public String getName() {

            return name;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface DataType {

        DataTypeEnum type();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface InternalFieldType {

        SimpleDaoSystemFieldsEnum type();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface Index {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface Unique {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface NotNull {
    }


}