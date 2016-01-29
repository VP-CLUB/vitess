package com.youtube.vitess.client.cursor;

import com.google.common.primitives.UnsignedLong;
import com.google.protobuf.ByteString;

import com.youtube.vitess.proto.Query;
import com.youtube.vitess.proto.Query.Field;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;

import java.math.BigDecimal;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Type-converting wrapper around raw
 * {@link com.youtube.vitess.proto.Query.Row} proto.
 *
 * <p>Usually you get Row objects from a {@link Cursor}, which builds
 * them by combining {@link com.youtube.vitess.proto.Query.Row} with
 * the list of {@link Field}s from the corresponding
 * {@link com.youtube.vitess.proto.Query.QueryResult}.
 */
public class Row {
  private FieldMap fieldMap;
  private List<ByteString> values;
  private Query.Row rawRow;
  private boolean lastGetWasNull;

  /**
   * Construct a Row from {@link com.youtube.vitess.proto.Query.Row}
   * proto with a pre-built {@link FieldMap}.
   *
   * <p>{@link Cursor} uses this to share a {@link FieldMap}
   * among multiple rows.
   */
  public Row(FieldMap fieldMap, Query.Row rawRow) {
    this.fieldMap = fieldMap;
    this.rawRow = rawRow;
    this.values = extractValues(rawRow.getLengthsList(), rawRow.getValues());
  }

  /**
   * Construct a Row from {@link com.youtube.vitess.proto.Query.Row} proto.
   */
  public Row(List<Field> fields, Query.Row rawRow) {
    this.fieldMap = new FieldMap(fields);
    this.rawRow = rawRow;
    this.values = extractValues(rawRow.getLengthsList(), rawRow.getValues());
  }

  /**
   * Construct a Row manually (not from proto).
   *
   * <p>The primary purpose of this Row class is to wrap the
   * {@link com.youtube.vitess.proto.Query.Row} proto, which stores values
   * in a packed format. However, when writing tests you may want to create
   * a Row from unpacked data.
   *
   * <p>Note that {@link #getRowProto()} will return null in this case,
   * so a Row created in this way can't be used with code that requires
   * access to the raw row proto.
   */
  public Row(List<Field> fields, List<ByteString> values) {
    this.fieldMap = new FieldMap(fields);
    this.values = values;
  }

  public int size() {
    return values.size();
  }

  public List<Field> getFields() {
    return fieldMap.getList();
  }

  public Query.Row getRowProto() {
    return rawRow;
  }

  public FieldMap getFieldMap() {
    return fieldMap;
  }

  public int findColumn(String columnLabel) throws SQLException {
    Integer columnIndex = fieldMap.getIndex(columnLabel);
    if (columnIndex == null) {
      throw new SQLDataException("column not found:" + columnLabel);
    }
    return columnIndex;
  }

  public Object getObject(String columnLabel) throws SQLException {
    return getObject(findColumn(columnLabel));
  }

  public Object getObject(int columnIndex) throws SQLException {
    if (columnIndex >= values.size()) {
      throw new SQLDataException("invalid columnIndex: " + columnIndex);
    }
    Object value = convertFieldValue(fieldMap.get(columnIndex), values.get(columnIndex));
    lastGetWasNull = (value == null);
    return value;
  }

  /**
   * Returns the column value, or 0 if the value is SQL NULL.
   *
   * <p>To distinguish between 0 and SQL NULL, use either {@link #wasNull()}
   * or {@link #getObject(String,Class)}.
   */
  public int getInt(String columnLabel) throws SQLException {
    return getInt(findColumn(columnLabel));
  }

  /**
   * Returns the column value, or 0 if the value is SQL NULL.
   *
   * <p>To distinguish between 0 and SQL NULL, use either {@link #wasNull()}
   * or {@link #getObject(int,Class)}.
   */
  public int getInt(int columnIndex) throws SQLException {
    Integer value = getObject(columnIndex, Integer.class);
    return value == null ? 0 : value;
  }

  public UnsignedLong getULong(String columnLabel) throws SQLException {
    return getULong(findColumn(columnLabel));
  }

  public UnsignedLong getULong(int columnIndex) throws SQLException {
    return getObject(columnIndex, UnsignedLong.class);
  }

  public String getString(String columnLabel) throws SQLException {
    return getString(findColumn(columnLabel));
  }

  public String getString(int columnIndex) throws SQLException {
    return getObject(columnIndex, String.class);
  }

  /**
   * Returns the column value, or 0 if the value is SQL NULL.
   *
   * <p>To distinguish between 0 and SQL NULL, use either {@link #wasNull()}
   * or {@link #getObject(String,Class)}.
   */
  public long getLong(String columnLabel) throws SQLException {
    return getLong(findColumn(columnLabel));
  }

  /**
   * Returns the column value, or 0 if the value is SQL NULL.
   *
   * <p>To distinguish between 0 and SQL NULL, use either {@link #wasNull()}
   * or {@link #getObject(int,Class)}.
   */
  public long getLong(int columnIndex) throws SQLException {
    Long value = getObject(columnIndex, Long.class);
    return value == null ? 0 : value;
  }

  /**
   * Returns the column value, or 0 if the value is SQL NULL.
   *
   * <p>To distinguish between 0 and SQL NULL, use either {@link #wasNull()}
   * or {@link #getObject(String,Class)}.
   */
  public double getDouble(String columnLabel) throws SQLException {
    return getDouble(findColumn(columnLabel));
  }

  /**
   * Returns the column value, or 0 if the value is SQL NULL.
   *
   * <p>To distinguish between 0 and SQL NULL, use either {@link #wasNull()}
   * or {@link #getObject(int,Class)}.
   */
  public double getDouble(int columnIndex) throws SQLException {
    Double value = getObject(columnIndex, Double.class);
    return value == null ? 0 : value;
  }

  /**
   * Returns the column value, or 0 if the value is SQL NULL.
   *
   * <p>To distinguish between 0 and SQL NULL, use either {@link #wasNull()}
   * or {@link #getObject(String,Class)}.
   */
  public float getFloat(String columnLabel) throws SQLException {
    return getFloat(findColumn(columnLabel));
  }

  /**
   * Returns the column value, or 0 if the value is SQL NULL.
   *
   * <p>To distinguish between 0 and SQL NULL, use either {@link #wasNull()}
   * or {@link #getObject(int,Class)}.
   */
  public float getFloat(int columnIndex) throws SQLException {
    Float value = getObject(columnIndex, Float.class);
    return value == null ? 0 : value;
  }

  public DateTime getDateTime(String columnLabel) throws SQLException {
    return getDateTime(findColumn(columnLabel));
  }

  public DateTime getDateTime(int columnIndex) throws SQLException {
    return getObject(columnIndex, DateTime.class);
  }

  public byte[] getBytes(String columnLabel) throws SQLException {
    return getBytes(findColumn(columnLabel));
  }

  public byte[] getBytes(int columnIndex) throws SQLException {
    return getObject(columnIndex, byte[].class);
  }

  public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
    return getBigDecimal(findColumn(columnLabel));
  }

  public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
    return getObject(columnIndex, BigDecimal.class);
  }

  /**
   * Returns the column value, or 0 if the value is SQL NULL.
   *
   * <p>To distinguish between 0 and SQL NULL, use either {@link #wasNull()}
   * or {@link #getObject(String,Class)}.
   */
  public short getShort(String columnLabel) throws SQLException {
    return getShort(findColumn(columnLabel));
  }

  /**
   * Returns the column value, or 0 if the value is SQL NULL.
   *
   * <p>To distinguish between 0 and SQL NULL, use either {@link #wasNull()}
   * or {@link #getObject(int,Class)}.
   */
  public short getShort(int columnIndex) throws SQLException {
    Short value = getObject(columnIndex, Short.class);
    return value == null ? 0 : value;
  }

  /**
   * Returns the column value, cast to the specified type.
   *
   * <p>This can be used as an alternative to getters that return primitive
   * types, if you need to distinguish between 0 and SQL NULL. For example:
   *
   * <blockquote><pre>
   * Long value = row.getObject(0, Long.class);
   * if (value == null) {
   *   // The value was SQL NULL, not 0.
   * }
   * </pre></blockquote>
   *
   * @throws SQLDataException if the type doesn't match the actual value.
   */
  @SuppressWarnings("unchecked") // by runtime check
  public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
    Object o = getObject(columnIndex);
    if (o != null && !type.isInstance(o)) {
      throw new SQLDataException(
          "type mismatch, expected:" + type.getName() + ", actual: " + o.getClass().getName());
    }
    return (T) o;
  }

  /**
   * Returns the column value, cast to the specified type.
   *
   * <p>This can be used as an alternative to getters that return primitive
   * types, if you need to distinguish between 0 and SQL NULL. For example:
   *
   * <blockquote><pre>
   * Long value = row.getObject("col0", Long.class);
   * if (value == null) {
   *   // The value was SQL NULL, not 0.
   * }
   * </pre></blockquote>
   *
   * @throws SQLDataException if the type doesn't match the actual value.
   */
  public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
    return getObject(findColumn(columnLabel), type);
  }

  /**
   * Reports whether the last column read had a value of SQL NULL.
   *
   * <p>Getter methods that return primitive types, such as {@link #getLong(int)},
   * will return 0 if the value is SQL NULL. To distinguish 0 from SQL NULL,
   * you can call {@code wasNull()} immediately after retrieving the value.
   *
   * <p>Note that this is not thread-safe: the value of {@code wasNull()} is only
   * trustworthy if there are no concurrent calls on this {@code Row} between the
   * call to {@code get*()} and the call to {@code wasNull()}.
   *
   * <p>As an alternative to {@code wasNull()}, you can use {@link #getObject(int,Class)}
   * (e.g. {@code getObject(0, Long.class)} instead of {@code getLong(0)}) to get a
   * wrapped {@code Long} value that will be {@code null} if the column value was SQL NULL.
   *
   * @throws SQLException
   */
  public boolean wasNull() throws SQLException {
    // Note: lastGetWasNull is currently set only in getObject(int),
    // which means this relies on the fact that all other get*() methods
    // eventually call into getObject(int). The unit tests help to ensure
    // this by checking wasNull() after each get*().
    return lastGetWasNull;
  }

  private static Object convertFieldValue(Field field, ByteString value) throws SQLException {
    if (value == null) {
      // Only a MySQL NULL value should return null.
      // A zero-length value should return the appropriate type.
      return null;
    }

    // Note: We don't actually know the charset in which the value is encoded.
    // For dates and numeric values, we just assume UTF-8 because they (hopefully) don't contain
    // anything outside 7-bit ASCII, which (hopefully) is a subset of the actual charset.
    // For strings, we return byte[] and the application is responsible for using the right charset.
    switch (field.getType()) {
      case DECIMAL:
        return new BigDecimal(value.toStringUtf8());
      case INT8: // fall through
      case UINT8: // fall through
      case INT16: // fall through
      case UINT16: // fall through
      case INT24: // fall through
      case UINT24: // fall through
      case INT32:
        return Integer.valueOf(value.toStringUtf8());
      case UINT32: // fall through
      case INT64:
        return Long.valueOf(value.toStringUtf8());
      case UINT64:
        return UnsignedLong.valueOf(value.toStringUtf8());
      case FLOAT32:
        return Float.valueOf(value.toStringUtf8());
      case FLOAT64:
        return Double.valueOf(value.toStringUtf8());
      case NULL_TYPE:
        return null;
      case DATE:
        return DateTime.parse(value.toStringUtf8(), ISODateTimeFormat.date());
      case TIME:
        return DateTime.parse(value.toStringUtf8(), DateTimeFormat.forPattern("HH:mm:ss"));
      case DATETIME: // fall through
      case TIMESTAMP:
        return DateTime.parse(value.toStringUtf8().replace(' ', 'T'));
      case YEAR:
        return Short.valueOf(value.toStringUtf8());
      case ENUM: // fall through
      case SET: // fall through
      case BIT:
        return value.toStringUtf8();
      case TEXT: // fall through
      case BLOB: // fall through
      case VARCHAR: // fall through
      case VARBINARY: // fall through
      case CHAR: // fall through
      case BINARY:
        return value.toByteArray();
      default:
        throw new SQLDataException("unknown field type: " + field.getType());
    }
  }

  /**
   * Extract cell values from the single-buffer wire format.
   *
   * <p>See the docs for the {@code Row} message in {@code query.proto}.
   */
  private static List<ByteString> extractValues(List<Long> lengths, ByteString buf) {
    List<ByteString> list = new ArrayList<ByteString>(lengths.size());

    int start = 0;
    for (long len : lengths) {
      if (len < 0) {
        // This indicates a MySQL NULL value, to distinguish it from a zero-length string.
        list.add((ByteString) null);
      } else {
        // Lengths are returned as long, but ByteString.substring() only supports int.
        list.add(buf.substring(start, start + (int) len));
        start += len;
      }
    }

    return list;
  }
}