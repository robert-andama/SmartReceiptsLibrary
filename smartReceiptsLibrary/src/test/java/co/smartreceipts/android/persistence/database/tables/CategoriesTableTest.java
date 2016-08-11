package co.smartreceipts.android.persistence.database.tables;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

import co.smartreceipts.android.model.Category;
import co.smartreceipts.android.model.factory.CategoryBuilderFactory;
import co.smartreceipts.android.persistence.DatabaseHelper;
import co.smartreceipts.android.persistence.database.defaults.TableDefaultsCustomizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
public class CategoriesTableTest {

    private static final String NAME1 = "name1";
    private static final String NAME2 = "name2";
    private static final String CODE1 = "code1";
    private static final String CODE2 = "code2";

    // Class under test
    CategoriesTable mCategoriesTable;

    @Mock
    SQLiteDatabase mSQLiteDatabase;

    @Mock
    TableDefaultsCustomizer mTableDefaultsCustomizer;

    @Captor
    ArgumentCaptor<String> mSqlCaptor;

    SQLiteOpenHelper mSQLiteOpenHelper;

    Category mCategory1;

    Category mCategory2;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mSQLiteOpenHelper = new TestSQLiteOpenHelper(RuntimeEnvironment.application);
        mCategoriesTable = new CategoriesTable(mSQLiteOpenHelper);

        // Now create the table and insert some defaults
        mCategoriesTable.onCreate(mSQLiteOpenHelper.getWritableDatabase(), mTableDefaultsCustomizer);
        mCategory1 = mCategoriesTable.insert(new CategoryBuilderFactory().setName(NAME1).setCode(CODE1).build()).toBlocking().first();
        mCategory2 = mCategoriesTable.insert(new CategoryBuilderFactory().setName(NAME2).setCode(CODE2).build()).toBlocking().first();
        assertNotNull(mCategory1);
        assertNotNull(mCategory2);
    }

    @After
    public void tearDown() {
        mSQLiteOpenHelper.getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + mCategoriesTable.getTableName());
    }

    @Test
    public void getTableName() {
        assertEquals("categories", mCategoriesTable.getTableName());
    }

    @Test
    public void onCreate() {
        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        mCategoriesTable.onCreate(mSQLiteDatabase, customizer);
        verify(mSQLiteDatabase).execSQL(mSqlCaptor.capture());
        verify(customizer).insertCategoryDefaults(mCategoriesTable);

        assertTrue(mSqlCaptor.getValue().contains(CategoriesTable.TABLE_NAME));
        assertTrue(mSqlCaptor.getValue().contains(CategoriesTable.COLUMN_NAME));
        assertTrue(mSqlCaptor.getValue().contains(CategoriesTable.COLUMN_CODE));
        assertTrue(mSqlCaptor.getValue().contains(CategoriesTable.COLUMN_BREAKDOWN));
        assertTrue(mSqlCaptor.getValue().contains(AbstractSqlTable.COLUMN_SYNC_ID));
        assertTrue(mSqlCaptor.getValue().contains(AbstractSqlTable.COLUMN_MARKED_FOR_DELETION));
        assertTrue(mSqlCaptor.getValue().contains(AbstractSqlTable.COLUMN_LAST_LOCAL_MODIFICATION_TIME));
    }

    @Test
    public void onUpgradeFromV2() {
        final int oldVersion = 2;
        final int newVersion = DatabaseHelper.DATABASE_VERSION;

        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        mCategoriesTable.onUpgrade(mSQLiteDatabase, oldVersion, newVersion, customizer);
        verify(mSQLiteDatabase, atLeastOnce()).execSQL(mSqlCaptor.capture());
        verify(customizer, never()).insertCategoryDefaults(mCategoriesTable);

        assertTrue(mSqlCaptor.getAllValues().get(0).equals("ALTER TABLE categories ADD breakdown BOOLEAN DEFAULT 1"));
        assertTrue(mSqlCaptor.getAllValues().get(1).equals("ALTER TABLE " + mCategoriesTable.getTableName() + " ADD remote_sync_id TEXT"));
        assertTrue(mSqlCaptor.getAllValues().get(2).equals("ALTER TABLE " + mCategoriesTable.getTableName() + " ADD marked_for_deletion TEXT"));
        assertTrue(mSqlCaptor.getAllValues().get(3).equals("ALTER TABLE " + mCategoriesTable.getTableName() + " ADD last_local_modification_type DATE"));
    }

    @Test
    public void onUpgradeFromV14() {
        final int oldVersion = 14;
        final int newVersion = DatabaseHelper.DATABASE_VERSION;

        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        mCategoriesTable.onUpgrade(mSQLiteDatabase, oldVersion, newVersion, customizer);
        verify(mSQLiteDatabase, atLeastOnce()).execSQL(mSqlCaptor.capture());
        verify(customizer, never()).insertCategoryDefaults(mCategoriesTable);

        assertTrue(mSqlCaptor.getAllValues().get(0).equals("ALTER TABLE " + mCategoriesTable.getTableName() + " ADD remote_sync_id TEXT"));
        assertTrue(mSqlCaptor.getAllValues().get(1).equals("ALTER TABLE " + mCategoriesTable.getTableName() + " ADD marked_for_deletion TEXT"));
        assertTrue(mSqlCaptor.getAllValues().get(2).equals("ALTER TABLE " + mCategoriesTable.getTableName() + " ADD last_local_modification_type DATE"));
    }

    @Test
    public void onUpgradeAlreadyOccurred() {
        final int oldVersion = DatabaseHelper.DATABASE_VERSION;
        final int newVersion = DatabaseHelper.DATABASE_VERSION;

        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        mCategoriesTable.onUpgrade(mSQLiteDatabase, oldVersion, newVersion, customizer);
        verify(mSQLiteDatabase, never()).execSQL(mSqlCaptor.capture());
        verify(customizer, never()).insertCategoryDefaults(mCategoriesTable);
    }

    @Test
    public void get() {
        final List<Category> categories = mCategoriesTable.get().toBlocking().first();
        assertEquals(categories, Arrays.asList(mCategory1, mCategory2));
    }

    @Test
    public void findByPrimaryKey() {
        final Category foundCategory = mCategoriesTable.findByPrimaryKey(mCategory1.getName()).toBlocking().first();
        assertNotNull(foundCategory);
        assertEquals(mCategory1, foundCategory);
    }

    @Test
    public void findByPrimaryMissingKey() {
        final Category foundCategory = mCategoriesTable.findByPrimaryKey("xxx").toBlocking().first();
        assertNull(foundCategory);
    }

    @Test
    public void insert() {
        final String name = "NewName";
        final String code = "NewCode";
        final Category insertCategory = new CategoryBuilderFactory().setName(name).setCode(code).build();
        assertEquals(insertCategory, mCategoriesTable.insert(insertCategory).toBlocking().first());

        final List<Category> categories = mCategoriesTable.get().toBlocking().first();
        assertEquals(categories, Arrays.asList(mCategory1, mCategory2, insertCategory));
    }
    
    @Test
    public void update() {
        final String name = "NewName";
        final String code = "NewCode";
        final Category updateCategory = new CategoryBuilderFactory().setName(name).setCode(code).build();
        assertEquals(updateCategory, mCategoriesTable.update(mCategory1, updateCategory).toBlocking().first());

        final List<Category> categories = mCategoriesTable.get().toBlocking().first();
        assertTrue(categories.contains(updateCategory));
        assertTrue(categories.contains(mCategory2));
    }

    @Test
    public void delete() {
        final List<Category> oldCategories = mCategoriesTable.get().toBlocking().first();
        assertTrue(oldCategories.contains(mCategory1));
        assertTrue(oldCategories.contains(mCategory2));

        assertTrue(mCategoriesTable.delete(mCategory1).toBlocking().first());
        assertTrue(mCategoriesTable.delete(mCategory2).toBlocking().first());

        final List<Category> newCategories = mCategoriesTable.get().toBlocking().first();
        assertTrue(newCategories.isEmpty());
    }

}