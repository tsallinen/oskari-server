package flyway.userlayer;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Checks if feature id config is already present in the db and inserts it if not
 */
public class V1_0_2__Insert_id_for_userlayerdata extends BaseJavaMigration {

    public void migrate(Context context)
            throws Exception {
        // check existing value before inserting
        Connection conn = context.getConnection();
        final boolean hasIdSpecInPlace = hasExistingConfig(conn);
        if (!hasIdSpecInPlace) {
            makeInsert(conn);
        }
    }

    private boolean hasExistingConfig(Connection connection)
            throws Exception {
        final PreparedStatement statement =
                connection.prepareStatement("SELECT * FROM gt_pk_metadata_table " +
                        "WHERE table_schema='public' " +
                        "AND table_name='vuser_layer_data' " +
                        "AND pk_column='id'");
        try (ResultSet rs = statement.executeQuery()) {
            return rs.next();
        } finally {
            statement.close();
        }
    }

    private void makeInsert(Connection connection)
            throws Exception {

        final PreparedStatement statement =
                connection.prepareStatement("INSERT INTO gt_pk_metadata_table (" +
                        " table_schema, table_name, pk_column, pk_column_idx, pk_policy, pk_sequence)" +
                        " VALUES (" +
                        " 'public', 'vuser_layer_data', 'id', NULL, 'assigned', NULL)");
        try {
            statement.execute();
        } finally {
            statement.close();
        }
    }
}
