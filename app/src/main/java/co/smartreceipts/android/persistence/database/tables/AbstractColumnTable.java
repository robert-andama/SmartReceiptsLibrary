package co.smartreceipts.android.persistence.database.tables;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.util.List;

import co.smartreceipts.android.model.Column;
import co.smartreceipts.android.model.ColumnDefinitions;
import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.persistence.database.defaults.TableDefaultsCustomizer;
import co.smartreceipts.android.persistence.database.operations.DatabaseOperationMetadata;
import co.smartreceipts.android.persistence.database.tables.adapters.ColumnDatabaseAdapter;
import co.smartreceipts.android.persistence.database.tables.keys.ColumnPrimaryKey;
import co.smartreceipts.android.utils.ListUtils;
import co.smartreceipts.android.utils.log.Logger;
import rx.Observable;
import rx.functions.Func1;

/**
 * Since our CSV and PDF tables share almost all of the same logic, this class purely acts as a wrapper around
 * each to centralize where all logic is managed
 */
public abstract class AbstractColumnTable extends AbstractSqlTable<Column<Receipt>, Integer> {

    private final int mTableExistsSinceDatabaseVersion;
    private final ColumnDefinitions<Receipt> mReceiptColumnDefinitions;
    private final String mIdColumnName;
    private final String mTypeColumnName;

    public AbstractColumnTable(@NonNull SQLiteOpenHelper sqLiteOpenHelper, @NonNull String tableName, int tableExistsSinceDatabaseVersion,
                               @NonNull ColumnDefinitions<Receipt> receiptColumnDefinitions, @NonNull String idColumnName, @NonNull String typeColumnName) {
        super(sqLiteOpenHelper, tableName, new ColumnDatabaseAdapter(receiptColumnDefinitions, idColumnName, typeColumnName), new ColumnPrimaryKey(idColumnName));
        mTableExistsSinceDatabaseVersion = tableExistsSinceDatabaseVersion;
        mReceiptColumnDefinitions = Preconditions.checkNotNull(receiptColumnDefinitions);
        mIdColumnName = Preconditions.checkNotNull(idColumnName);
        mTypeColumnName = Preconditions.checkNotNull(typeColumnName);
    }

    @Override
    public synchronized void onCreate(@NonNull SQLiteDatabase db, @NonNull TableDefaultsCustomizer customizer) {
        super.onCreate(db, customizer);
        final String columnsTable = "CREATE TABLE " + getTableName() + " ("
                + mIdColumnName + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + mTypeColumnName + " TEXT, "
                + AbstractSqlTable.COLUMN_DRIVE_SYNC_ID + " TEXT, "
                + AbstractSqlTable.COLUMN_DRIVE_IS_SYNCED + " BOOLEAN DEFAULT 0, "
                + AbstractSqlTable.COLUMN_DRIVE_MARKED_FOR_DELETION + " BOOLEAN DEFAULT 0, "
                + AbstractSqlTable.COLUMN_LAST_LOCAL_MODIFICATION_TIME + " DATE"+ ");";
        Logger.debug(this, columnsTable);

        db.execSQL(columnsTable);
        insertDefaults(customizer);
    }

    @Override
    public synchronized void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion, @NonNull TableDefaultsCustomizer customizer) {
        super.onUpgrade(db, oldVersion, newVersion, customizer);
        if (oldVersion <= mTableExistsSinceDatabaseVersion) {
            final String columnsTable = "CREATE TABLE " + getTableName() + " ("
                    + mIdColumnName + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + mTypeColumnName + " TEXT"
                    + ");";
            Logger.debug(this, columnsTable);

            db.execSQL(columnsTable);
            insertDefaults(customizer);
        }
        if (oldVersion <= 14) {
            onUpgradeToAddSyncInformation(db, oldVersion, newVersion);
        }
    }

    /**
     * Inserts the default column as defined by {@link ColumnDefinitions#getDefaultInsertColumn()}
     *
     * @return the inserted {@link Column} or {@code null} if the insert failed
     */
    @NonNull
    public final Observable<Column<Receipt>> insertDefaultColumn() {
        return insert(mReceiptColumnDefinitions.getDefaultInsertColumn(), new DatabaseOperationMetadata());
    }

    /**
     * Attempts to delete the last column in the list
     *
     * @return {@code true} if it could be delete. {@code false} otherwise (e.g. there are no more columns)
     */
    @NonNull
    public final Observable<Boolean> deleteLast(@NonNull final DatabaseOperationMetadata databaseOperationMetadata) {
        return get().flatMap(new Func1<List<Column<Receipt>>, Observable<? extends Boolean>>() {
            @Override
            public Observable<? extends Boolean> call(List<Column<Receipt>> columns) {
                return AbstractColumnTable.this.removeLastColumnIfPresent(columns, databaseOperationMetadata);
            }
        });
    }

    /**
     * Passes alongs a call to insert our "table" defaults to the appropriate sub implementation
     *
     * @param customizer the {@link TableDefaultsCustomizer} implementation
     */
    protected abstract void insertDefaults(@NonNull TableDefaultsCustomizer customizer);

    @NonNull
    private Observable<Boolean> removeLastColumnIfPresent(@NonNull List<Column<Receipt>> columns, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        final Column<Receipt> lastColumn = ListUtils.removeLast(columns);
        if (lastColumn != null) {
            return Observable.just(deleteBlocking(lastColumn, databaseOperationMetadata) != null);
        } else {
            return Observable.just(false);
        }
    }

}
