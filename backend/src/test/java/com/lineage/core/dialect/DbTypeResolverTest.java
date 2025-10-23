package com.lineage.core.dialect;

import com.alibaba.druid.DbType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DbTypeResolverTest {

    private DbTypeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DbTypeResolver();
    }

    @ParameterizedTest
    @CsvSource({
        "mysql, mysql",
        "MYSQL, mysql",
        "MySQL, mysql",
        "hive, hive",
        "HIVE, hive",
        "postgresql, postgresql",
        "PostgreSQL, postgresql",
        "  mysql  , mysql"
    })
    void testResolveWithValidTypes(String input, String expectedType) {
        DbType result = resolver.resolve(input);
        assertNotNull(result);
        assertEquals(expectedType, result.name().toLowerCase());
    }

    @Test
    void testResolveMySql() {
        DbType result = resolver.resolve("mysql");
        assertEquals(DbType.mysql, result);
    }

    @Test
    void testResolveHive() {
        DbType result = resolver.resolve("hive");
        assertEquals(DbType.hive, result);
    }

    @Test
    void testResolvePostgreSQL() {
        DbType result = resolver.resolve("postgresql");
        assertEquals(DbType.postgresql, result);
    }

    @Test
    void testResolveCaseInsensitive() {
        assertEquals(DbType.mysql, resolver.resolve("MySQL"));
        assertEquals(DbType.hive, resolver.resolve("HIVE"));
        assertEquals(DbType.postgresql, resolver.resolve("PostgreSQL"));
    }

    @Test
    void testResolveWithWhitespace() {
        assertEquals(DbType.mysql, resolver.resolve("  mysql  "));
        assertEquals(DbType.hive, resolver.resolve("\thive\t"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void testResolveWithBlankInput(String input) {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> resolver.resolve(input)
        );
        assertEquals("Database type cannot be blank", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "db2", "informix", "invalid_type"})
    void testResolveWithUnsupportedType(String unsupportedType) {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> resolver.resolve(unsupportedType)
        );
        assertTrue(exception.getMessage().contains("Unsupported database type"));
        assertTrue(exception.getMessage().contains(unsupportedType));
    }

    @Test
    void testIsSupported() {
        assertTrue(resolver.isSupported("mysql"));
        assertTrue(resolver.isSupported("MYSQL"));
        assertTrue(resolver.isSupported("hive"));
        assertTrue(resolver.isSupported("postgresql"));
        
        assertFalse(resolver.isSupported("spark"));
        assertFalse(resolver.isSupported("unknown"));
        assertFalse(resolver.isSupported("db2"));
        assertFalse(resolver.isSupported(""));
        assertFalse(resolver.isSupported(null));
        assertFalse(resolver.isSupported("  "));
    }

    @Test
    void testGetSupportedTypes() {
        Map<String, DbType> supportedTypes = resolver.getSupportedTypes();
        
        assertNotNull(supportedTypes);
        assertTrue(supportedTypes.size() >= 3);
        assertTrue(supportedTypes.containsKey("mysql"));
        assertTrue(supportedTypes.containsKey("hive"));
        assertTrue(supportedTypes.containsKey("postgresql"));
        
        assertEquals(DbType.mysql, supportedTypes.get("mysql"));
        assertEquals(DbType.hive, supportedTypes.get("hive"));
        assertEquals(DbType.postgresql, supportedTypes.get("postgresql"));
    }

    @Test
    void testGetSupportedTypesReturnsNewMap() {
        Map<String, DbType> map1 = resolver.getSupportedTypes();
        Map<String, DbType> map2 = resolver.getSupportedTypes();
        
        assertNotSame(map1, map2);
        
        map1.clear();
        assertFalse(resolver.getSupportedTypes().isEmpty());
    }
}
