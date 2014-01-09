/*
 * Copyright 2012 Ross Bamford
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.roscopeco.ormdroid;

import java.nio.ByteBuffer;

import android.database.Cursor;

/*
 * Map java.util.Date to the database.
 * 
 * This implementation just stashes the number of seconds
 * since the epoch in a BIGINT.
 */
public class BlobTypeMapping implements TypeMapping {
  private Class<?> mJavaType; 
  private String mSqlType;
  
  public BlobTypeMapping(Class<?> type, String sqlType) {
    mJavaType = type;
    mSqlType = sqlType;      
  }

  public Class<?> javaType() {
    return mJavaType;
  }

  public String sqlType(Class<?> concreteType) {
    return mSqlType;
  }

  public String encodeValue(Object value) {
    // SQLite BLOB syntax is X'123ABCDEF', using either lower or uppercase X
    return "X'" + bytesToHex(((ByteBuffer)value).array()) + "'";
  }

  public Object decodeValue(Class<?> expectedType, Cursor c, int columnIndex) {
    return ByteBuffer.wrap((c.getBlob(columnIndex)));
  }
  
  /* 
   * Convert binary data to a hex string suitable for inclusing in an SQL statement
   * Taken from http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
   */
  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
  public static String bytesToHex(byte[] bytes) {
      char[] hexChars = new char[bytes.length * 2];
      for ( int j = 0; j < bytes.length; j++ ) {
          int v = bytes[j] & 0xFF;
          hexChars[j * 2] = hexArray[v >>> 4];
          hexChars[j * 2 + 1] = hexArray[v & 0x0F];
      }
      return new String(hexChars);
  }
  
}