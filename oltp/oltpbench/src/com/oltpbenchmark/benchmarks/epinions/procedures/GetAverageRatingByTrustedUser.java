/******************************************************************************
 *  Copyright 2015 by OLTPBenchmark Project                                   *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *    http://www.apache.org/licenses/LICENSE-2.0                              *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 ******************************************************************************/

package com.oltpbenchmark.benchmarks.epinions.procedures;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;

public class GetAverageRatingByTrustedUser extends Procedure {

    // public final SQLStmt getAverageRating = new SQLStmt(
    //     "SELECT avg(rating) FROM review r, trust t WHERE r.u_id=t.target_u_id AND r.i_id=? AND t.source_u_id=?"
    // );

    public final SQLStmt getAverageRating1 = new SQLStmt(
        "SELECT target_u_id FROM trust WHERE source_u_id=?"
    );

    public final SQLStmt getAverageRating2 = new SQLStmt(
        "SELECT rating FROM review WHERE u_id=? AND i_id=?"
    );
    
    public void run(Connection conn, long iid, long uid) throws SQLException {
        PreparedStatement stmt1 = this.getPreparedStatement(conn, getAverageRating1);
        PreparedStatement stmt2 = this.getPreparedStatement(conn, getAverageRating2);
        stmt1.setLong(1, uid);
        ResultSet r1= stmt1.executeQuery();
        Long total = new Long(0);
        int count = 0;
        while (r1.next()) {
            int tuid = r1.getInt("target_u_id");
            stmt2.setInt(1, tuid);
            stmt2.setLong(2, iid);
            ResultSet r2 = stmt2.executeQuery();
        }
        r1.close();
    }
    
}
